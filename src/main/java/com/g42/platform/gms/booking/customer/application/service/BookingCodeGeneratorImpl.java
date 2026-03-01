package com.g42.platform.gms.booking.customer.application.service;

import com.g42.platform.gms.booking.customer.domain.enums.CodePrefix;
import com.g42.platform.gms.booking.customer.domain.exception.CodeGenerationException;
import com.g42.platform.gms.booking.customer.domain.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingCodeGeneratorImpl implements BookingCodeGenerator {

    // Bộ ký tự cho random code (loại bỏ O, 0, I, 1, l để tránh nhầm lẫn)
    private static final String ALPHANUMERIC = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";
    private static final int CODE_LENGTH = 6;
    private static final int MAX_RETRIES = 5;
    private static final String CODE_PATTERN = "^(BK|RQ)_[" + ALPHANUMERIC + "]{6}$";
    
    private final SecureRandom secureRandom = new SecureRandom();
    private final BookingRepository bookingRepository;

    @Override
    @Transactional
    public String generateCode(LocalDate date, CodePrefix prefix) throws CodeGenerationException {
        int retries = 0;
        
        while (retries < MAX_RETRIES) {
            try {
                // Tạo chuỗi random 6 ký tự
                String randomPart = generateRandomString(CODE_LENGTH);
                
                // Ghép prefix với random part: BK_A7X9K2
                String code = prefix.getPrefix() + "_" + randomPart;
                
                // Kiểm tra code đã tồn tại chưa
                if (!bookingRepository.existsByBookingCode(code)) {
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
            String.format("Không thể tạo mã booking sau %d lần thử", MAX_RETRIES)
        );
    }
    
    /**
     * Tạo chuỗi random với độ dài cho trước
     */
    private String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = secureRandom.nextInt(ALPHANUMERIC.length());
            sb.append(ALPHANUMERIC.charAt(index));
        }
        return sb.toString();
    }

//    @Override
//    public LocalDate parseDate(String code) throws InvalidBookingCodeException {
//        throw new InvalidBookingCodeException("Random code không chứa thông tin ngày");
//    }
//
//    @Override
//    public int parseSequence(String code) throws InvalidBookingCodeException {
//        throw new InvalidBookingCodeException("Random code không có sequence");
//    }
//
//    @Override
//    public boolean isValidFormat(String code) {
//        if (code == null) {
//            return false;
//        }
//
//        // Pattern: (BK hoặc RQ) + _ + 6 ký tự từ ALPHANUMERIC
//        return code.matches(CODE_PATTERN);
//    }
}

