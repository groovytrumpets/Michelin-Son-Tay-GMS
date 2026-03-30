package com.g42.platform.gms.billing.domain.exception;

import lombok.Getter;

@Getter
public class BillingException extends RuntimeException {
        private final BillingErrorCode code;
    public BillingException(String message, BillingErrorCode code) {
        super(message);
        this.code = code;
    }
}
