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
    INVALID_TIME_RANGE("INVALID_TIME_RANGE", "Giờ check-in phải trước giờ check-out"),
    LOCATION_NOT_FOUND("LOCATION_NOT_FOUND", "Không tìm thấy vị trí chấm công"),
    INVALID_QR_TOKEN("INVALID_QR_TOKEN", "Mã QR không hợp lệ"),
    LOCATION_INACTIVE("LOCATION_INACTIVE", "Vị trí chấm công đã bị vô hiệu hóa"),
    OUT_OF_RADIUS("OUT_OF_RADIUS", "Bạn đang ở ngoài phạm vi cho phép để chấm công");

    private final String code;
    private final String message;
}
