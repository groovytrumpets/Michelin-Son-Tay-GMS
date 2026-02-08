package com.g42.platform.gms.booking.customer.exception;

import lombok.Getter;

@Getter
public class BookingException extends RuntimeException {
    
    private final String code;
    
    public BookingException(String message) {
        super(message);
        this.code = "BOOKING_ERROR";
    }
    
    public BookingException(String code, String message) {
        super(message);
        this.code = code;
    }
}
