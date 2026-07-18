package com.g42.platform.gms.attendancerequest.domain.exception;

import lombok.Getter;

@Getter
public class AttendanceRequestException extends RuntimeException {
    private final AttendanceRequestErrorCode errorCode;

    public AttendanceRequestException(AttendanceRequestErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public AttendanceRequestException(AttendanceRequestErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
    }
}
