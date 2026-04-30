package com.g42.platform.gms.dashboard.application.service;

import com.g42.platform.gms.auth.entity.StaffProfile;
import com.g42.platform.gms.auth.repository.StaffProfileRepo;
import com.g42.platform.gms.dashboard.api.dto.DashboardOverviewResponse;
import com.g42.platform.gms.dashboard.api.dto.DashboardOverviewResponse.*;
import com.g42.platform.gms.dashboard.infrastructure.entity.StaffNotificationJpa;
import com.g42.platform.gms.dashboard.infrastructure.repository.StaffNotificationRepository;
import com.g42.platform.gms.manager.attendance.infrastructure.entity.AttendanceCheckinJpa;
import com.g42.platform.gms.manager.attendance.infrastructure.repository.AttendanceCheckinJpaRepo;
import com.g42.platform.gms.manager.schedule.infrastructure.entity.WorkShiftJpa;
import com.g42.platform.gms.service_ticket_management.infrastructure.repository.ServiceTicketAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StaffDashboardService {

    private final StaffProfileRepo staffProfileRepo;
    private final AttendanceCheckinJpaRepo attendanceRepo;
    private final StaffNotificationRepository notificationRepo;
    private final ServiceTicketAssignmentRepository assignmentRepo;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    public DashboardOverviewResponse getDashboardOverview(Integer staffId) {
        StaffProfile profile = staffProfileRepo.findById(staffId).orElseThrow();
        LocalDate today = LocalDate.now();

        DashboardOverviewResponse response = new DashboardOverviewResponse();

        // Staff info
        StaffInfo info = new StaffInfo();
        info.setStaffId(profile.getStaffId());
        info.setFullName(profile.getFullName());
        info.setAvatar(profile.getAvatar());
        info.setPosition(profile.getPosition());
        response.setStaff(info);

        // Today shift
        List<AttendanceCheckinJpa> todayCheckins = attendanceRepo.findByStaffIdAndAttendanceDate(staffId, today);
        response.setTodayShift(buildTodayShift(todayCheckins, today));

        // Monthly hours
        MonthlyHoursDto hours = new MonthlyHoursDto();
        Double totalHours = attendanceRepo.sumMonthlyHours(staffId, today.getYear(), today.getMonthValue());
        hours.setTotalHours(totalHours != null ? totalHours : 0.0);
        hours.setMonth(today.getYear() + "-" + String.format("%02d", today.getMonthValue()));
        response.setMonthlyHours(hours);

        // Completed services
        CompletedServicesDto services = new CompletedServicesDto();
        services.setCount(assignmentRepo.countCompletedByStaffInMonth(staffId, today.getYear(), today.getMonthValue()));
        services.setMonth(hours.getMonth());
        response.setCompletedServices(services);

        // Today tasks — empty list (tickets handled by service_ticket module)
        response.setTodayTasks(List.of());

        // Upcoming schedule (next 7 days)
        response.setUpcomingSchedule(getWorkSchedule(staffId, today, today.plusDays(6)));

        // Recent attendance (last 7 days)
        response.setRecentAttendance(getAttendanceHistory(staffId, today.getMonthValue(), today.getYear()));

        // Notifications (top 5)
        response.setNotifications(getNotifications(staffId, 0, 5));

        return response;
    }

    public List<TaskSummaryDto> getTodayTasks(Integer staffId) {
        return List.of(); // tickets handled by service_ticket module
    }

    public List<ScheduleDayDto> getWorkSchedule(Integer staffId, LocalDate from, LocalDate to) {
        List<AttendanceCheckinJpa> checkins = attendanceRepo
                .findByStaffIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(staffId, from, to);

        return checkins.stream().map(c -> {
            ScheduleDayDto dto = new ScheduleDayDto();
            dto.setDate(c.getAttendanceDate().format(DATE_FMT));
            dto.setDayOfWeek(c.getAttendanceDate().getDayOfWeek().name());
            WorkShiftJpa shift = c.getShift();
            if (shift != null) {
                dto.setShiftName(shift.getShiftName());
                dto.setStartTime(shift.getStartTime().format(TIME_FMT));
                dto.setEndTime(shift.getEndTime().format(TIME_FMT));
            }
            dto.setStatus(c.getStatus());
            return dto;
        }).toList();
    }

    public List<AttendanceRecordDto> getAttendanceHistory(Integer staffId, int month, int year) {
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());

        return attendanceRepo.findByStaffIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(staffId, from, to)
                .stream().map(c -> {
                    AttendanceRecordDto dto = new AttendanceRecordDto();
                    dto.setDate(c.getAttendanceDate().format(DATE_FMT));
                    dto.setDayOfWeek(c.getAttendanceDate().getDayOfWeek().name());
                    dto.setShiftType(c.getShift() != null ? c.getShift().getShiftName() : null);
                    dto.setCheckInTime(c.getCheckInTime() != null ? c.getCheckInTime().format(TIME_FMT) : null);
                    dto.setCheckOutTime(c.getCheckOutTime() != null ? c.getCheckOutTime().format(TIME_FMT) : null);
                    dto.setStatus(c.getStatus());
                    return dto;
                }).toList();
    }

    public List<NotificationSummaryDto> getNotifications(Integer staffId, int page, int size) {
        return notificationRepo.findAllByStaffId(staffId, PageRequest.of(page, size))
                .stream().map(n -> {
                    NotificationSummaryDto dto = new NotificationSummaryDto();
                    dto.setNotificationId(n.getNotificationId());
                    dto.setTitle(n.getTitle());
                    dto.setMessage(n.getMessage());
                    dto.setNotificationType(n.getNotificationType().name());
                    dto.setIsRead(n.getIsRead());
                    dto.setSentAt(n.getSentAt() != null ? n.getSentAt().toString() : null);
                    return dto;
                }).toList();
    }

    public void markNotificationAsRead(Integer staffId, Integer notificationId) {
        notificationRepo.findById(notificationId).ifPresent(n -> {
            n.setIsRead(true);
            notificationRepo.save(n);
        });
    }

    public Map<String, Object> getPersonalStatistics(Integer staffId, int month, int year) {
        long completed = assignmentRepo.countCompletedByStaffInMonth(staffId, year, month);
        Double hours = attendanceRepo.sumMonthlyHours(staffId, year, month);
        return Map.of(
                "month", year + "-" + String.format("%02d", month),
                "completedTickets", completed,
                "totalHours", hours != null ? hours : 0.0
        );
    }

    // ===== Helpers =====

    private TodayShiftDto buildTodayShift(List<AttendanceCheckinJpa> checkins, LocalDate today) {
        TodayShiftDto dto = new TodayShiftDto();
        dto.setDate(today.format(DATE_FMT));
        dto.setDayOfWeek(today.getDayOfWeek().name());
        if (!checkins.isEmpty()) {
            AttendanceCheckinJpa c = checkins.get(0);
            WorkShiftJpa shift = c.getShift();
            if (shift != null) {
                dto.setShiftName(shift.getShiftName());
                dto.setStartTime(shift.getStartTime().format(TIME_FMT));
                dto.setEndTime(shift.getEndTime().format(TIME_FMT));
            }
            dto.setStatus(c.getStatus());
        } else {
            dto.setStatus("NOT_CHECKED_IN");
        }
        return dto;
    }
}
