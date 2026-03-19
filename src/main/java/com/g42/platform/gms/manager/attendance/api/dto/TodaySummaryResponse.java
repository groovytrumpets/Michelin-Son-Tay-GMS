package com.g42.platform.gms.manager.attendance.api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
public class TodaySummaryResponse {
    private LocalDate date;
    private int totalStaff;
    private int checkedIn;
    private int notCheckedIn;
    private List<StaffAttendanceStatus> staffList;

    @Data
    @Builder
    public static class StaffAttendanceStatus {
        private Integer staffId;
        private String fullName;
        private String position;
        private String avatar;
        private boolean hasCheckedIn;
        private Integer checkinId;
        private Integer shiftId;
        private String shiftName;
        private LocalTime checkInTime;
        private LocalTime checkOutTime;
        private String status;
    }
}
