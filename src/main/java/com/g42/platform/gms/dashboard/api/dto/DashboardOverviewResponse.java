package com.g42.platform.gms.dashboard.api.dto;

import lombok.Data;

import java.util.List;

@Data
public class DashboardOverviewResponse {

    private StaffInfo staff;
    private TodayShiftDto todayShift;
    private MonthlyHoursDto monthlyHours;
    private CompletedServicesDto completedServices;
    private List<TaskSummaryDto> todayTasks;
    private List<ScheduleDayDto> upcomingSchedule;
    private List<AttendanceRecordDto> recentAttendance;
    private List<NotificationSummaryDto> notifications;

    @Data
    public static class StaffInfo {
        private Integer staffId;
        private String fullName;
        private String avatar;
        private String position;
    }

    @Data
    public static class TodayShiftDto {
        private String date;
        private String dayOfWeek;
        private String shiftName;
        private String startTime;
        private String endTime;
        private String status;
    }

    @Data
    public static class MonthlyHoursDto {
        private Double totalHours;
        private String month;
    }

    @Data
    public static class CompletedServicesDto {
        private Long count;
        private String month;
    }

    @Data
    public static class TaskSummaryDto {
        private Integer serviceTicketId;
        private String ticketCode;
        private String licensePlate;
        private String vehicleBrand;
        private String vehicleModel;
        private String customerName;
        private String ticketStatus;
        private String receivedAt;
    }

    @Data
    public static class ScheduleDayDto {
        private String date;
        private String dayOfWeek;
        private String shiftName;
        private String startTime;
        private String endTime;
        private String status; // SCHEDULED, CONFIRMED, CANCELLED, OFF
    }

    @Data
    public static class AttendanceRecordDto {
        private String date;
        private String dayOfWeek;
        private String shiftType;
        private String checkInTime;
        private String checkOutTime;
        private String status; // PRESENT, LATE, EARLY_LEAVE, ABSENT
    }

    @Data
    public static class NotificationSummaryDto {
        private Integer notificationId;
        private String title;
        private String message;
        private String notificationType;
        private Boolean isRead;
        private String sentAt;
    }
}
