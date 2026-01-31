package com.g42.platform.gms.auth.constant;

public enum AuthErrorCode { // Đổi class thành enum
    INVALID_PIN,
    OTP_EXPIRED,
    OTP_INVALID,
    OTP_TOO_MANY_ATTEMPTS,
    OTP_NOT_FOUND,           // Thêm cái này
    ACCOUNT_LOCKED,
    VALIDATION_ERROR,
    PHONE_ALREADY_REGISTERED, // Thêm cái này
    USER_NOT_FOUND,
    SYSTEM_ERROR,
    PIN_MISMATCH,
    PIN_NOT_SET
}