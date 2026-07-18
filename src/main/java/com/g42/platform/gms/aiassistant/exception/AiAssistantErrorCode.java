package com.g42.platform.gms.aiassistant.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AiAssistantErrorCode {
    NOT_CONFIGURED("AI_NOT_CONFIGURED", "Trợ lý AI chưa được cấu hình"),
    UPSTREAM_ERROR("AI_UPSTREAM_ERROR", "Trợ lý AI đang gặp sự cố, vui lòng thử lại sau"),
    EMPTY_RESPONSE("AI_EMPTY_RESPONSE", "Trợ lý AI không trả về nội dung");

    private final String code;
    private final String message;
}
