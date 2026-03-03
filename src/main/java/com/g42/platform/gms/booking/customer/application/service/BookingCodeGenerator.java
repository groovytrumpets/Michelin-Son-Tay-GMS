package com.g42.platform.gms.booking.customer.application.service;

import com.g42.platform.gms.common.enums.CodePrefix;
import com.g42.platform.gms.common.exception.CodeGenerationException;

import java.time.LocalDate;

/**
 * Interface for generating booking codes.
 * Pattern: BK_XXXXXX or RQ_XXXXXX (6 random alphanumeric characters)
 */
public interface BookingCodeGenerator {
    
    /**
     * Generate a unique booking code.
     * 
     * @param date the date (for logging purposes, not used in code format)
     * @param prefix the code prefix (BOOKING or REQUEST)
     * @return generated code (e.g., BK_A7X9K2)
     * @throws CodeGenerationException if code generation fails after max retries
     */
    String generateCode(LocalDate date, CodePrefix prefix) throws CodeGenerationException;
}
