package com.g42.platform.gms.marketing.service_combo.domain.exception;

import lombok.Getter;

@Getter
public class ComboItemException extends RuntimeException {
        private final ComboItemErrorCode code;
    public ComboItemException(String message, ComboItemErrorCode code) {
        super(message);
        this.code = code;
    }
}
