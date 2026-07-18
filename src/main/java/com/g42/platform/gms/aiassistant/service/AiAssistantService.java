package com.g42.platform.gms.aiassistant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.g42.platform.gms.aiassistant.dto.AiChatRequest;
import com.g42.platform.gms.aiassistant.dto.AiChatResponse;
import com.g42.platform.gms.aiassistant.dto.AiChatTurn;
import com.g42.platform.gms.aiassistant.exception.AiAssistantErrorCode;
import com.g42.platform.gms.aiassistant.exception.AiAssistantException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiAssistantService {

    private static final String GEMINI_URL_TEMPLATE =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

    private static final String SYSTEM_PROMPT =
            "Bạn là trợ lý AI nội bộ của gara Michelin Sơn Tây (GMS). "
                    + "Hãy trả lời ngắn gọn, rõ ràng, thân thiện bằng tiếng Việt, "
                    + "hỗ trợ nhân viên trong công việc hàng ngày (tra cứu, tư vấn, giải thích quy trình).";

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.api.model:gemini-2.0-flash}")
    private String model;

    public AiChatResponse chat(AiChatRequest request) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new AiAssistantException(AiAssistantErrorCode.NOT_CONFIGURED);
        }

        Map<String, Object> body = Map.of(
                "contents", buildContents(request),
                "systemInstruction", Map.of("parts", List.of(Map.of("text", SYSTEM_PROMPT)))
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        String url = String.format(GEMINI_URL_TEMPLATE, model, apiKey);

        JsonNode response;
        try {
            ResponseEntity<JsonNode> result = restTemplate.postForEntity(url, entity, JsonNode.class);
            response = result.getBody();
        } catch (HttpStatusCodeException e) {
            System.err.println("Gemini API error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            throw new AiAssistantException(AiAssistantErrorCode.UPSTREAM_ERROR);
        } catch (RestClientException e) {
            System.err.println("Gemini API call failed: " + e.getMessage());
            throw new AiAssistantException(AiAssistantErrorCode.UPSTREAM_ERROR);
        }

        String reply = extractReply(response);
        if (reply == null || reply.isBlank()) {
            throw new AiAssistantException(AiAssistantErrorCode.EMPTY_RESPONSE);
        }

        return new AiChatResponse(reply);
    }

    private List<Map<String, Object>> buildContents(AiChatRequest request) {
        List<Map<String, Object>> contents = new ArrayList<>();
        if (request.getHistory() != null) {
            for (AiChatTurn turn : request.getHistory()) {
                if (turn.getText() == null || turn.getText().isBlank()) continue;
                String role = "model".equalsIgnoreCase(turn.getRole()) ? "model" : "user";
                contents.add(Map.of("role", role, "parts", List.of(Map.of("text", turn.getText()))));
            }
        }
        contents.add(Map.of("role", "user", "parts", List.of(Map.of("text", request.getMessage()))));
        return contents;
    }

    private String extractReply(JsonNode response) {
        if (response == null) return null;
        JsonNode candidates = response.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) return null;
        JsonNode parts = candidates.get(0).path("content").path("parts");
        if (!parts.isArray() || parts.isEmpty()) return null;
        return parts.get(0).path("text").asText(null);
    }
}
