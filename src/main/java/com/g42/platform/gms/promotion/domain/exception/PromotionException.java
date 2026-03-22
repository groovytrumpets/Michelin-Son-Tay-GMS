package com.g42.platform.gms.promotion.domain.exception;

import lombok.Getter;

@Getter
public class PromotionException extends RuntimeException {
        private final PromotionErrorCode code;
    public PromotionException(String message, PromotionErrorCode code) {
        super(message);
        this.code = code;
    }
}
