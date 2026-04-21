package com.g42.platform.gms.common.handler;

import com.g42.platform.gms.auth.constant.AuthErrorCode;
import com.g42.platform.gms.billing.domain.exception.BillingException;
import com.g42.platform.gms.booking.customer.domain.exception.BookingException;
import com.g42.platform.gms.booking_management.domain.exception.BookingStaffException;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.auth.exception.AuthException;
import com.g42.platform.gms.customer.domain.exception.CustomerException;
import com.g42.platform.gms.estimation.domain.exception.EstimateException;
import com.g42.platform.gms.manager.attendance.domain.exception.AttendanceException;
import com.g42.platform.gms.manager.schedule.domain.exception.ScheduleException;
import com.g42.platform.gms.marketing.service_catalog.domain.exception.ServiceException;
import com.g42.platform.gms.marketing.service_combo.domain.exception.ComboItemException;
import com.g42.platform.gms.service_ticket_management.domain.exception.AssignmentException;
import com.g42.platform.gms.promotion.domain.exception.PromotionException;
import com.g42.platform.gms.staff.attendance.domain.exception.StaffAttendanceException;
import com.g42.platform.gms.staff.profile.domain.exception.StaffException;
import com.g42.platform.gms.warehouse.domain.exception.WarehouseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Arrays;

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
    @ExceptionHandler(StaffException.class)
    public ResponseEntity<ApiResponse<?>> handleBookingManagementException(StaffException ex) {

        System.err.println("Customer Error: " + ex.getCode() + " - " + ex.getMessage());


        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponses.error(ex.getCode().name(), ex.getMessage()));
    }
    @ExceptionHandler(AssignmentException.class)
    public ResponseEntity<ApiResponse<?>> handleBookingManagementException(AssignmentException ex) {

        System.err.println("Staff Assignment Error: " + ex.getCode() + " - " + ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponses.error(ex.getCode().name(), ex.getMessage()));
    }

    @ExceptionHandler(PromotionException.class)
    public ResponseEntity<ApiResponse<?>> handlePromotionException(PromotionException ex) {

        System.err.println("Customer Error: " + ex.getCode() + " - " + ex.getMessage());


        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponses.error(ex.getCode().name(), ex.getMessage()));
    }

    @ExceptionHandler(AttendanceException.class)
    public ResponseEntity<ApiResponse<?>> handleAttendanceException(AttendanceException ex) {
        System.err.println("Attendance Error: " + ex.getErrorCode().getCode() + " - " + ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponses.error(ex.getErrorCode().getCode(), ex.getMessage()));
    }

    @ExceptionHandler(ScheduleException.class)
    public ResponseEntity<ApiResponse<?>> handleScheduleException(ScheduleException ex) {
        System.err.println("Schedule Error: " + ex.getErrorCode().name() + " - " + ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponses.error(ex.getErrorCode().name(), ex.getMessage()));
    }
    @ExceptionHandler(WarehouseException.class)
    public ResponseEntity<ApiResponse<?>> handleScheduleException(WarehouseException ex) {
        System.err.println("Warehouse Error: " + ex.getCode().name() + " - " + ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponses.error(ex.getCode().name(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleEnumError(
            MethodArgumentTypeMismatchException ex) {

        if (ex.getRequiredType() != null &&
                ex.getRequiredType().isEnum()) {

            String validValues = Arrays.toString(
                    ex.getRequiredType().getEnumConstants()
            );

            return ResponseEntity.badRequest().body(
                    ApiResponses.error("FAIL",
                            "Invalid value for parameter '"
                                    + ex.getName() +
                                    "'. Allowed values: " + validValues
                    )
            );
        }

        return ResponseEntity.badRequest().body(
                ApiResponses.error("FAIL","Invalid request parameter")
        );
    }

    /**
     * Xử lý lỗi IllegalArgumentException (ví dụ: date range validation - ER021)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<?>> handleIllegalArgumentException(IllegalArgumentException ex) {
        System.err.println("Validation Error: " + ex.getMessage());
        
        // Extract error code if present in message (e.g., "Invalid Date Range (ER021): ...")
        String errorCode = "VALIDATION_ERROR";
        String message = ex.getMessage();
        
        if (message != null && message.contains("ER021")) {
            errorCode = "ER021";
            message = "Invalid Date Range";
        }
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponses.error(errorCode, message));
    }

    /**
     * Xử lý lỗi AccessDeniedException (ví dụ: non-technician access - ER034)
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<?>> handleAccessDeniedException(AccessDeniedException ex) {
        System.err.println("Access Denied: " + ex.getMessage());
        
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponses.error("ER034", "Access Denied"));
    }
    @ExceptionHandler(EstimateException.class)
    public ResponseEntity<ApiResponse<?>> handleBookingException(EstimateException ex) {
        System.err.println("Estimate Exception Error: " + ex.getCode() + " - " + ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponses.error(ex.getCode().name(), ex.getMessage()));
    }
    @ExceptionHandler(BillingException.class)
    public ResponseEntity<ApiResponse<?>> handleBookingException(BillingException ex) {
        System.err.println("Billing Error: " + ex.getCode() + " - " + ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponses.errorBill(ex.getCode().name(), ex.getMessage()));
    }
    @ExceptionHandler(BillingException.class)
    public ResponseEntity<ApiResponse<?>> handleBookingException(ComboItemException ex) {
        System.err.println("ComboItem Error: " + ex.getCode() + " - " + ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponses.errorBill(ex.getCode().name(), ex.getMessage()));
    }
}