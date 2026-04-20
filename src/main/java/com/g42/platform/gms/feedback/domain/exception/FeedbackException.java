package com.g42.platform.gms.feedback.domain.exception;

import lombok.Getter;

@Getter
public class FeedbackException extends RuntimeException {
        private final FeedbackErrorCode code;
    public FeedbackException(String message, FeedbackErrorCode code) {
        super(message);
        this.code = code;
    }
}
