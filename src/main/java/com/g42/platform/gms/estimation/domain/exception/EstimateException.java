package com.g42.platform.gms.estimation.domain.exception;

import lombok.Getter;

@Getter
public class EstimateException extends RuntimeException {
        private final EstimateErrorCode code;
    public EstimateException(String message, EstimateErrorCode code) {
        super(message);
        this.code = code;
    }
}
