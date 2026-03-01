package com.g42.platform.gms.booking.customer.application.service;

import com.g42.platform.gms.booking.customer.domain.enums.CodePrefix;
import com.g42.platform.gms.booking.customer.domain.exception.CodeGenerationException;


import java.time.LocalDate;

/**
 * Service for generating and validating booking codes
 */
public interface BookingCodeGenerator {
    
    /**
     * Generate a new booking code for the given date
     * @param date The date for which to generate the code
     * @param prefix The code prefix (BK or RQ)
     * @return Generated booking code in format PREFIX-YYYYMMDD-XXXX
     * @throws CodeGenerationException if generation fails after retries
     */
    String generateCode(LocalDate date, CodePrefix prefix) throws CodeGenerationException;
//
//    /**
//     * Parse a booking code and extract the date component
//     * @param code The booking code to parse
//     * @return The date extracted from the code
//     * @throws InvalidBookingCodeException if code format is invalid
//     */
//    LocalDate parseDate(String code) throws InvalidBookingCodeException;
//
//    /**
//     * Parse a booking code and extract the sequence number
//     * @param code The booking code to parse
//     * @return The sequence number extracted from the code
//     * @throws InvalidBookingCodeException if code format is invalid
//     */
//    int parseSequence(String code) throws InvalidBookingCodeException;
//
//    /**
//     * Validate booking code format
//     * @param code The code to validate
//     * @return true if valid, false otherwise
//     */
//    boolean isValidFormat(String code);
}
