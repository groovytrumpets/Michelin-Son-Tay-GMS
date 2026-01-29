package com.g42.platform.gms.common.handler;

import com.g42.platform.gms.auth.constant.AuthErrorCode;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.auth.exception.AuthException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Xử lý lỗi nghiệp vụ (AuthException)
     * Trả về HTTP 400 (Bad Request) hoặc 401 (Unauthorized) tùy logic
     */
    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiResponse<?>> handleAuthException(AuthException ex) {
        // Log lỗi để debug (tùy chọn)
        System.err.println("Auth Error: " + ex.getCode() + " - " + ex.getMessage());

        // Trả về JSON chuẩn
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
}