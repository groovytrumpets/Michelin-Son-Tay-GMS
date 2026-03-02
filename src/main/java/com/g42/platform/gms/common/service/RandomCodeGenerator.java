package com.g42.platform.gms.common.service;

import com.g42.platform.gms.common.enums.CodePrefix;
import com.g42.platform.gms.common.exception.CodeGenerationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.function.Predicate;

/**
 * Common service for generating random unique codes.
 * 
 * Pattern: {PREFIX}_{RANDOM_6_CHARS}
 * Examples: BK_A7X9K2, ST_P3M8N4, RQ_K2H7D9
 * 
 * Character set excludes O, 0, I, 1, l to avoid confusion.
 * Uses SecureRandom for cryptographically strong randomness.
 * 
 * Usage:
 * <pre>
 * String code = randomCodeGenerator.generateCode(
 *     LocalDate.now(),
 *     CodePrefix.BOOKING,
 *     bookingRepository::existsByBookingCode
 * );
 * </pre>
 */
@Slf4j
@Service
public class RandomCodeGenerator {

    // Character set (excluding O, 0, I, 1, l to avoid confusion)
    private static final String ALPHANUMERIC = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";
    private static final int CODE_LENGTH = 6;
    private static final int MAX_RETRIES = 5;
    
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generate a unique random code.
     * 
     * @param date the date (for logging purposes, not used in code format)
     * @param prefix the code prefix (BOOKING, SERVICE_TICKET, etc.)
     * @param existsChecker function to check if code already exists in database
     * @return generated unique code
     * @throws CodeGenerationException if code generation fails after max retries
     */
    public String generateCode(LocalDate date, CodePrefix prefix, Predicate<String> existsChecker) 
            throws CodeGenerationException {
        int retries = 0;
        
        while (retries < MAX_RETRIES) {
            try {
                // Generate random 6-character string
                String randomPart = generateRandomString(CODE_LENGTH);
                
                // Combine prefix with random part: BK_A7X9K2
                String code = prefix.getPrefix() + "_" + randomPart;
                
                // Check if code already exists
                if (!existsChecker.test(code)) {
                    log.info("Generated random code: {} for prefix: {}", code, prefix);
                    return code;
                }
                
                log.warn("Code collision detected: {}, retry {}/{}", code, retries + 1, MAX_RETRIES);
                retries++;
                
            } catch (Exception e) {
                log.error("Error generating code: {}", e.getMessage());
                retries++;
            }
        }
        
        throw new CodeGenerationException(
            String.format("Không thể tạo mã %s sau %d lần thử", prefix.name(), MAX_RETRIES)
        );
    }
    
    /**
     * Generate random string with specified length.
     * 
     * @param length the length of random string
     * @return random string from ALPHANUMERIC character set
     */
    private String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = secureRandom.nextInt(ALPHANUMERIC.length());
            sb.append(ALPHANUMERIC.charAt(index));
        }
        return sb.toString();
    }
}
