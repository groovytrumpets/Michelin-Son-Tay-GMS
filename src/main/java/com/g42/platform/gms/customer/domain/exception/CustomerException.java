package com.g42.platform.gms.customer.domain.exception;

import lombok.Getter;

@Getter
public class CustomerException extends RuntimeException {
        private final CustomerErrorCode code;
    public CustomerException(String message, CustomerErrorCode code) {
        super(message);
        this.code = code;
    }
}
