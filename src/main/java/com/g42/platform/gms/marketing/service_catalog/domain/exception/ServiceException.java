package com.g42.platform.gms.marketing.service_catalog.domain.exception;

import lombok.Getter;

@Getter
public class ServiceException extends RuntimeException {
        private final ServiceErrorCode code;
    public ServiceException(String message, ServiceErrorCode code) {
        super(message);
        this.code = code;
    }
}
