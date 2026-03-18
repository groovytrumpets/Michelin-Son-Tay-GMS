package com.g42.platform.gms.dashboard.api.controller;

import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.dashboard.api.dto.DashboardOverviewResponse;
import com.g42.platform.gms.dashboard.api.dto.DashboardOverviewResponse.*;
import com.g42.platform.gms.dashboard.application.service.StaffDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
public class StaffDashboardController {

    private final StaffDashboardService dashboardService;

    /**
     * GET /api/staff/dashboard
     * Lấy toàn bộ thông tin dashboard cho kỹ thuật viên đang đăng nhập.
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public ResponseEntity<ApiResponse<DashboardOverviewResponse>> getDashboard(
            @AuthenticationPrincipal StaffPrincipal principal) {

        Integer staffId = principal.getStaffId();
        DashboardOverviewResponse response = dashboardService.getDashboardOverview(staffId);
        return ResponseEntity.ok(ApiResponses.success(response));
    }

    /**
     * GET /api/staff/tasks/today
     * Lấy danh sách task hôm nay của kỹ thuật viên.
     */
    @GetMapping("/tasks/today")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public ResponseEntity<ApiResponse<List<TaskSummaryDto>>> getTodayTasks(
            @AuthenticationPrincipal StaffPrincipal principal) {

        Integer staffId = principal.getStaffId();
        List<TaskSummaryDto> tasks = dashboardService.getTodayTasks(staffId);
        return ResponseEntity.ok(ApiResponses.success(tasks));
    }

    /**
     * GET /api/staff/schedule?from=yyyy-MM-dd&to=yyyy-MM-dd
     * Lấy lịch làm việc theo khoảng ngày.
     */
    @GetMapping("/schedule")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public ResponseEntity<ApiResponse<List<ScheduleDayDto>>> getSchedule(
            @AuthenticationPrincipal StaffPrincipal principal,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        Integer staffId = principal.getStaffId();
        List<ScheduleDayDto> schedule = dashboardService.getWorkSchedule(staffId, from, to);
        return ResponseEntity.ok(ApiResponses.success(schedule));
    }

    /**
     * GET /api/staff/attendance/history?month=3&year=2026
     * Lấy lịch sử chấm công theo tháng.
     */
    @GetMapping("/attendance/history")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public ResponseEntity<ApiResponse<List<AttendanceRecordDto>>> getAttendanceHistory(
            @AuthenticationPrincipal StaffPrincipal principal,
            @RequestParam int month,
            @RequestParam int year) {

        Integer staffId = principal.getStaffId();
        List<AttendanceRecordDto> history = dashboardService.getAttendanceHistory(staffId, month, year);
        return ResponseEntity.ok(ApiResponses.success(history));
    }

    /**
     * GET /api/staff/notifications?page=0&size=10
     * Lấy danh sách thông báo (tất cả, có phân trang).
     */
    @GetMapping("/notifications")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public ResponseEntity<ApiResponse<List<NotificationSummaryDto>>> getNotifications(
            @AuthenticationPrincipal StaffPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Integer staffId = principal.getStaffId();
        List<NotificationSummaryDto> notifications = dashboardService.getNotifications(staffId, page, size);
        return ResponseEntity.ok(ApiResponses.success(notifications));
    }

    /**
     * PUT /api/staff/notifications/{notificationId}/read
     * Đánh dấu thông báo đã đọc.
     */
    @PutMapping("/notifications/{notificationId}/read")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public ResponseEntity<ApiResponse<String>> markNotificationAsRead(
            @AuthenticationPrincipal StaffPrincipal principal,
            @PathVariable Integer notificationId) {

        Integer staffId = principal.getStaffId();
        dashboardService.markNotificationAsRead(staffId, notificationId);
        return ResponseEntity.ok(ApiResponses.success("Đã đánh dấu đã đọc"));
    }

    /**
     * GET /api/staff/statistics?month=3&year=2026
     * Lấy thống kê cá nhân theo tháng.
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatistics(
            @AuthenticationPrincipal StaffPrincipal principal,
            @RequestParam int month,
            @RequestParam int year) {

        Integer staffId = principal.getStaffId();
        Map<String, Object> stats = dashboardService.getPersonalStatistics(staffId, month, year);
        return ResponseEntity.ok(ApiResponses.success(stats));
    }
}
