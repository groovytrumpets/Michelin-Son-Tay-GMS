package com.g42.platform.gms.staff.profile.domain.exception;

import lombok.Getter;

@Getter
public class StaffException extends RuntimeException {
        private final StaffErrorCode code;
    public StaffException(String message, StaffErrorCode code) {
        super(message);
        this.code = code;
    }
}
