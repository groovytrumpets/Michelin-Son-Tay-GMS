package com.g42.platform.gms.booking.customer.domain.exception;

public class CodeGenerationException extends RuntimeException {
    
    public CodeGenerationException(String message) {
        super(message);
    }
    
    public CodeGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
