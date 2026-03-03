package com.g42.platform.gms.common.exception;

/**
 * Exception thrown when code generation fails after maximum retries.
 * Used by RandomCodeGenerator.
 */
public class CodeGenerationException extends RuntimeException {
    
    public CodeGenerationException(String message) {
        super(message);
    }
    
    public CodeGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
