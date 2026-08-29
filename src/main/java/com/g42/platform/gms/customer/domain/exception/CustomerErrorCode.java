package com.g42.platform.gms.customer.domain.exception;

public enum CustomerErrorCode {
    INVALID_ID,
    INVALID_CONTACT_INFO,
    INVALID_PHONE,
    INVALID_DOB,
    INVALID_CUSTOMER_PROFILE,
    /** Số điện thoại đã thuộc về hồ sơ khách khác — mỗi khách chỉ có một số. */
    DUPLICATE_PHONE,
    /** Email đã thuộc về hồ sơ khách khác — email là định danh đăng nhập thứ hai. */
    DUPLICATE_EMAIL,
}
