package com.g42.platform.gms.staff.attendance.domain.exception;

import lombok.Getter;

@Getter
public class StaffAttendanceException extends RuntimeException {
        private final StaffAttendanceErrorCode code;
    public StaffAttendanceException(String message, StaffAttendanceErrorCode code) {
        super(message);
        this.code = code;
    }
}
