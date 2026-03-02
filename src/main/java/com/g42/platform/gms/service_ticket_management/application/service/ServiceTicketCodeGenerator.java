package com.g42.platform.gms.service_ticket_management.application.service;

import com.g42.platform.gms.common.enums.CodePrefix;
import com.g42.platform.gms.common.exception.CodeGenerationException;

import java.time.LocalDate;

/**
 * Interface for generating service ticket codes.
 * Pattern: ST_XXXXXX (6 random alphanumeric characters)
 */
public interface ServiceTicketCodeGenerator {
    
    /**
     * Generate a unique service ticket code.
     * 
     * @param date the date (for logging purposes, not used in code format)
     * @param prefix the code prefix (SERVICE_TICKET)
     * @return generated code (e.g., ST_A7X9K2)
     * @throws CodeGenerationException if code generation fails after max retries
     */
    String generateCode(LocalDate date, CodePrefix prefix) throws CodeGenerationException;
}
