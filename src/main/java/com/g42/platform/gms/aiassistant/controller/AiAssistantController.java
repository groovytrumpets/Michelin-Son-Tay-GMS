package com.g42.platform.gms.aiassistant.controller;

import com.g42.platform.gms.aiassistant.dto.AiChatRequest;
import com.g42.platform.gms.aiassistant.dto.AiChatResponse;
import com.g42.platform.gms.aiassistant.dto.AiQuotaDto;
import com.g42.platform.gms.aiassistant.service.AiAssistantService;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai-assistant")
@RequiredArgsConstructor
public class AiAssistantController {

    private final AiAssistantService aiAssistantService;

    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<AiChatResponse>> chat(@Valid @RequestBody AiChatRequest request) {
        return ResponseEntity.ok(ApiResponses.success(aiAssistantService.chat(request)));
    }

    /** Mức sử dụng token/request trong ngày — đếm nội bộ, dùng chung quota với kênh khách hàng. */
    @GetMapping("/quota")
    public ResponseEntity<ApiResponse<AiQuotaDto>> quota() {
        return ResponseEntity.ok(ApiResponses.success(aiAssistantService.getQuota()));
    }

    /** Danh sách model hỗ trợ để người dùng/FE lựa chọn thủ công. */
    @GetMapping("/models")
    public ResponseEntity<ApiResponse<List<String>>> models() {
        return ResponseEntity.ok(ApiResponses.success(aiAssistantService.getAvailableModels()));
    }
}
