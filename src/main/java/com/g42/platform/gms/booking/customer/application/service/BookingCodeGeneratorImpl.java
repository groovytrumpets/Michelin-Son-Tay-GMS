package com.g42.platform.gms.booking.customer.application.service;

import com.g42.platform.gms.booking.customer.domain.repository.BookingRepository;
import com.g42.platform.gms.booking.customer.domain.repository.BookingRequestRepository;
import com.g42.platform.gms.common.enums.CodePrefix;
import com.g42.platform.gms.common.exception.CodeGenerationException;
import com.g42.platform.gms.common.service.RandomCodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.function.Predicate;

/**
 * Implementation of BookingCodeGenerator.
 * Delegates to common RandomCodeGenerator service.
 * Supports both booking codes (BK_XXXXXX) and request codes (RQ_XXXXXX).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingCodeGeneratorImpl implements BookingCodeGenerator {

    private final RandomCodeGenerator randomCodeGenerator;
    private final BookingRepository bookingRepository;
    private final BookingRequestRepository bookingRequestRepository;

    @Override
    @Transactional
    public String generateCode(LocalDate date, CodePrefix prefix) throws CodeGenerationException {
        // Select appropriate existence checker based on prefix
        Predicate<String> existsChecker = getExistsChecker(prefix);
        
        return randomCodeGenerator.generateCode(
            date,
            prefix,
            existsChecker
        );
    }
    
    /**
     * Get the appropriate existence checker based on code prefix.
     * 
     * @param prefix the code prefix
     * @return predicate to check if code exists
     */
    private Predicate<String> getExistsChecker(CodePrefix prefix) {
        switch (prefix) {
            case BOOKING:
                return bookingRepository::existsByBookingCode;
            case REQUEST:
                return bookingRequestRepository::existsByRequestCode;
            default:
                throw new IllegalArgumentException("Unsupported code prefix: " + prefix);
        }
    }
}
