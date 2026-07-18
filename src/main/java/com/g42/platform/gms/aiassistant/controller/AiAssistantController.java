package com.g42.platform.gms.aiassistant.controller;

import com.g42.platform.gms.aiassistant.dto.AiChatRequest;
import com.g42.platform.gms.aiassistant.dto.AiChatResponse;
import com.g42.platform.gms.aiassistant.service.AiAssistantService;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai-assistant")
@RequiredArgsConstructor
public class AiAssistantController {

    private final AiAssistantService aiAssistantService;

    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<AiChatResponse>> chat(@Valid @RequestBody AiChatRequest request) {
        return ResponseEntity.ok(ApiResponses.success(aiAssistantService.chat(request)));
    }
}
