package com.g42.platform.gms.manager.employee.api.dto;

import lombok.Builder;
import lombok.Data;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
public class EmployeeDetailResponse {
    // Thông tin cơ bản
    private Integer staffId;
    private String fullName;
    private String phone;
    private String position;
    private String gender;
    private Date dob;
    private String avatar;
    private String employeeNo;
    private String employeeCode;
    private Date startDate;
    private Boolean isResigned;
    private String permanentAddress;
    private String placeOfBirth;
    private String address;
    private String representative;
    private String ethnicity;
    private String religion;
    private String nationality;
    private String identityCard;
    private String idIssuePlace;
    private Date idIssueDate;
    private String pitCode;
    private String pitIssuePlace;
    private Date pitIssueDate;
    private String socialInsuranceCode;
    private String siIssuePlace;
    private Date siIssueDate;
    private String siPaidPeriod;
    private String uiPaidPeriod;
    private String educationLevel;
    private String profession;
    private String department;

    // Hiệu năng (tháng hiện tại)
    private PerformanceSummary performance;

    // Lịch điểm danh gần đây
    private List<AttendanceRecord> recentAttendance;

    @Data
    @Builder
    public static class PerformanceSummary {
        private int totalWorkDays;       // Số ngày đi làm (tháng này)
        private int totalTicketsHandled; // Số phiếu dịch vụ đã xử lý (tháng này)
    }

    @Data
    @Builder
    public static class AttendanceRecord {
        private Integer checkinId;
        private LocalDate attendanceDate;
        private Integer shiftId;
        private String shiftName;
        private LocalTime checkInTime;
        private LocalTime checkOutTime;
        private String status;
    }
}
