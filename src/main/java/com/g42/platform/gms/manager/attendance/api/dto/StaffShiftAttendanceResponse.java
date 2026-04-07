package com.g42.platform.gms.manager.attendance.api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Response cho màn điểm danh thủ công của 1 nhân viên trong 1 ngày.
 * Hiển thị tất cả ca (kể cả chưa check-in) để manager có thể thao tác.
 */
@Data
@Builder
public class StaffShiftAttendanceResponse {

    private Integer staffId;
    private String fullName;
    private String position;
    private String avatar;
    private LocalDate date;
    private List<ShiftStatus> shifts;

    @Data
    @Builder
    public static class ShiftStatus {
        private Integer shiftId;
        private String shiftName;
        private LocalTime shiftStart;
        private LocalTime shiftEnd;

        // null nếu chưa check-in
        private Integer checkinId;
        private LocalTime checkInTime;
        private LocalTime checkOutTime;
        private String status; // null = chưa điểm danh, "PRESENT" = đã check-in
        private String source; // "MANUAL" hoặc "HIKVISION"
    }
}
