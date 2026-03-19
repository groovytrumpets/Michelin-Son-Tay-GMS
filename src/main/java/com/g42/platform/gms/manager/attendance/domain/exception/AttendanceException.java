package com.g42.platform.gms.manager.attendance.domain.exception;

import lombok.Getter;

@Getter
public class AttendanceException extends RuntimeException {
    private final AttendanceErrorCode errorCode;

    public AttendanceException(AttendanceErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
