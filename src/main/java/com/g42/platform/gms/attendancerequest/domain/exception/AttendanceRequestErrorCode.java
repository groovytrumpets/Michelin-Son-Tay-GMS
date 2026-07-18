package com.g42.platform.gms.attendancerequest.domain.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AttendanceRequestErrorCode {
    REQUEST_NOT_FOUND("REQUEST_NOT_FOUND", "Không tìm thấy yêu cầu"),
    INVALID_REQUEST_TYPE("INVALID_REQUEST_TYPE", "Loại yêu cầu không hợp lệ"),
    INVALID_DATE_RANGE("INVALID_DATE_RANGE", "Ngày kết thúc phải sau ngày bắt đầu"),
    REASON_REQUIRED("REASON_REQUIRED", "Vui lòng nhập lý do"),
    REVIEW_NOTE_REQUIRED("REVIEW_NOTE_REQUIRED", "Vui lòng nhập lý do từ chối"),
    NOT_PENDING("NOT_PENDING", "Yêu cầu này đã được xử lý, không thể thay đổi"),
    NOT_OWNER("NOT_OWNER", "Bạn không có quyền thao tác trên yêu cầu này");

    private final String code;
    private final String message;
}
