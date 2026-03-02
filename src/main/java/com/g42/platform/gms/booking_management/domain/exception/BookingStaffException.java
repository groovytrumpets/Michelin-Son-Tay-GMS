package com.g42.platform.gms.booking_management.domain.exception;

import lombok.Getter;

@Getter
public class BookingStaffException extends RuntimeException {
        private final BookingStaffErrorCode code;
    public BookingStaffException(String message, BookingStaffErrorCode code) {
        super(message);
        this.code = code;
    }
}
