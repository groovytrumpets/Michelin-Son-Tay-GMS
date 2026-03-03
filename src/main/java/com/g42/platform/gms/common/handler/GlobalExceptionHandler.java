package com.g42.platform.gms.common.handler;

import com.g42.platform.gms.auth.constant.AuthErrorCode;
import com.g42.platform.gms.booking.customer.domain.exception.BookingException;
import com.g42.platform.gms.booking_management.domain.exception.BookingStaffException;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.auth.exception.AuthException;
import com.g42.platform.gms.customer.domain.exception.CustomerException;
import com.g42.platform.gms.marketing.service_catalog.domain.exception.ServiceException;
import com.g42.platform.gms.staff.attendance.domain.exception.StaffAttendanceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiResponse<?>> handleAuthException(AuthException ex) {
        System.err.println("Auth Error: " + ex.getCode() + " - " + ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponses.error(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(BookingException.class)
    public ResponseEntity<ApiResponse<?>> handleBookingException(BookingException ex) {
        System.err.println("Booking Error: " + ex.getCode() + " - " + ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponses.error(ex.getCode(), ex.getMessage()));
    }

    /**
     * Xử lý lỗi Validation (@NotNull, @Size...)
     */
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidationException(
            org.springframework.web.bind.MethodArgumentNotValidException ex
    ) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(err -> err.getDefaultMessage()) // Chỉ lấy message lỗi
                .orElse("Dữ liệu không hợp lệ");

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponses.error(AuthErrorCode.VALIDATION_ERROR.name(), message));
    }

    /**
     * Xử lý lỗi hệ thống không mong muốn (NullPointer, DB Connection...)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleException(Exception ex) {
        ex.printStackTrace(); // In stack trace để debug server
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponses.error(
                        AuthErrorCode.SYSTEM_ERROR.name(),
                        "Lỗi hệ thống. Vui lòng thử lại sau."
                ));
    }

    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<ApiResponse<?>> handleServiceException(ServiceException ex) {

        System.err.println("Service Catalog Error: " + ex.getCode() + " - " + ex.getMessage());


        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponses.error(ex.getCode().name(), ex.getMessage()));
    }
    @ExceptionHandler(BookingStaffException.class)
    public ResponseEntity<ApiResponse<?>> handleBookingManagementException(BookingStaffException ex) {

        System.err.println("Booking Management Error: " + ex.getCode() + " - " + ex.getMessage());


        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponses.error(ex.getCode().name(), ex.getMessage()));
    }
    @ExceptionHandler(StaffAttendanceException.class)
    public ResponseEntity<ApiResponse<?>> handleBookingManagementException(StaffAttendanceException ex) {

        System.err.println("Staff Attendance Error: " + ex.getCode() + " - " + ex.getMessage());


        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponses.error(ex.getCode().name(), ex.getMessage()));
    }
    @ExceptionHandler(CustomerException.class)
    public ResponseEntity<ApiResponse<?>> handleBookingManagementException(CustomerException ex) {

        System.err.println("Customer Error: " + ex.getCode() + " - " + ex.getMessage());


        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponses.error(ex.getCode().name(), ex.getMessage()));
    }
}