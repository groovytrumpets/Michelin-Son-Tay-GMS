package com.g42.platform.gms.service_ticket_management.domain.exception;

import lombok.Getter;

/**
 * Exception thrown during check-in process.
 * Similar to BookingException pattern.
 */
@Getter
public class CheckInException extends RuntimeException {

    private final String code;

    /**
     * Tên ô nhập trên FE gây ra lỗi (VD "customerPhone"), để FE hiện lỗi ngay dưới ô đó
     * thay vì chỉ một dòng chung ở đầu trang. Null = lỗi chung, không gắn với ô nào.
     */
    private final String field;

    public CheckInException(String message) {
        this("CHECKIN_ERROR", message);
    }

    public CheckInException(String code, String message) {
        this(code, message, null);
    }

    private CheckInException(String code, String message, String field) {
        super(message);
        this.code = code;
        this.field = field;
    }

    /** Lỗi dữ liệu của một ô nhập cụ thể — trả về 400 kèm data.field. */
    public static CheckInException field(String field, String message) {
        return new CheckInException("INVALID_FIELD", message, field);
    }
}
