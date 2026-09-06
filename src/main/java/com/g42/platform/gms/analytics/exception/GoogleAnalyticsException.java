package com.g42.platform.gms.analytics.exception;

import lombok.Getter;

@Getter
public class GoogleAnalyticsException extends RuntimeException {
    private final GoogleAnalyticsErrorCode errorCode;

    public GoogleAnalyticsException(GoogleAnalyticsErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public GoogleAnalyticsException(GoogleAnalyticsErrorCode errorCode, String detail) {
        super(errorCode.getMessage() + ": " + detail);
        this.errorCode = errorCode;
    }
}
