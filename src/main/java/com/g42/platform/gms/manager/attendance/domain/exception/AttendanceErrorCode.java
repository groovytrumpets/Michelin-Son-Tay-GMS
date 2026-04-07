package com.g42.platform.gms.manager.attendance.domain.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AttendanceErrorCode {
    CHECKIN_NOT_FOUND("CHECKIN_NOT_FOUND", "Không tìm thấy bản ghi điểm danh"),
    ALREADY_CHECKED_IN("ALREADY_CHECKED_IN", "Nhân viên đã điểm danh ca này hôm nay"),
    NOT_CHECKED_IN("NOT_CHECKED_IN", "Nhân viên chưa điểm danh, không thể check-out"),
    SHIFT_NOT_FOUND("SHIFT_NOT_FOUND", "Không tìm thấy ca làm việc"),
    STAFF_NOT_FOUND("STAFF_NOT_FOUND", "Không tìm thấy nhân viên"),
    INVALID_TIME_RANGE("INVALID_TIME_RANGE", "Giờ check-in phải trước giờ check-out");

    private final String code;
    private final String message;
}
