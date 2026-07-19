package com.g42.platform.gms.aiassistant.controller;

import com.g42.platform.gms.aiassistant.dto.AiChatRequest;
import com.g42.platform.gms.aiassistant.dto.AiChatResponse;
import com.g42.platform.gms.aiassistant.dto.AiQuotaDto;
import com.g42.platform.gms.aiassistant.service.AiAssistantService;
import com.g42.platform.gms.aiassistant.service.PublicAiRateLimiter;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint AI công khai cho khách hàng trên website (không yêu cầu đăng nhập).
 * Nằm dưới /home/** nên đã permitAll sẵn trong SecurityConfig — chống lạm dụng
 * bằng {@link PublicAiRateLimiter} theo IP thay vì bằng auth.
 */
@RestController
@RequestMapping("/home/ai-assistant")
@RequiredArgsConstructor
public class PublicAiAssistantController {

    private final AiAssistantService aiAssistantService;
    private final PublicAiRateLimiter publicAiRateLimiter;

    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<AiChatResponse>> chat(
            @Valid @RequestBody AiChatRequest request,
            HttpServletRequest httpRequest
    ) {
        publicAiRateLimiter.checkAndRecord(getClientIp(httpRequest));
        return ResponseEntity.ok(ApiResponses.success(aiAssistantService.chatPublic(request)));
    }

    /**
     * Mức sử dụng token/request trong ngày — không tính vào rate limit (chỉ đọc bộ đếm,
     * không gọi Gemini) và dùng chung quota với kênh nhân viên (cùng 1 API key).
     */
    @GetMapping("/quota")
    public ResponseEntity<ApiResponse<AiQuotaDto>> quota() {
        return ResponseEntity.ok(ApiResponses.success(aiAssistantService.getQuota()));
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
