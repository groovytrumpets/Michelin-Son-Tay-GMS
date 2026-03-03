package com.g42.platform.gms.auth.exception;

import lombok.Getter;

@Getter
public class AuthException extends RuntimeException {
    
    private final String code;
    
    public AuthException(String message) {
        super(message);
        this.code = "AUTH_ERROR";
    }
    
    public AuthException(String code, String message) {
        super(message);
        this.code = code;
    }
}
