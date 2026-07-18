package com.g42.platform.gms.aiassistant.exception;

import lombok.Getter;

@Getter
public class AiAssistantException extends RuntimeException {
    private final AiAssistantErrorCode errorCode;

    public AiAssistantException(AiAssistantErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
