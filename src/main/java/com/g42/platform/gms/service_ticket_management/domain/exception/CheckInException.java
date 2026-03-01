package com.g42.platform.gms.service_ticket_management.domain.exception;

import lombok.Getter;

/**
 * Exception thrown during check-in process.
 * Similar to BookingException pattern.
 */
@Getter
public class CheckInException extends RuntimeException {
    
    private final String code;
    
    public CheckInException(String message) {
        super(message);
        this.code = "CHECKIN_ERROR";
    }
    
    public CheckInException(String code, String message) {
        super(message);
        this.code = code;
    }
}
