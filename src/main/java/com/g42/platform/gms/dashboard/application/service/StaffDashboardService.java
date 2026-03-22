package com.g42.platform.gms.dashboard.application.service;

import com.g42.platform.gms.auth.entity.StaffProfile;
import com.g42.platform.gms.auth.repository.StaffProfileRepo;
import com.g42.platform.gms.dashboard.api.dto.DashboardOverviewResponse;
import com.g42.platform.gms.dashboard.api.dto.DashboardOverviewResponse.*;
import com.g42.platform.gms.dashboard.infrastructure.entity.StaffNotificationJpa;
import com.g42.platform.gms.dashboard.infrastructure.entity.StaffScheduleJpa;
import com.g42.platform.gms.dashboard.infrastructure.repository.StaffNotificationRepository;
import com.g42.platform.gms.dashboard.infrastructure.repository.StaffScheduleRepository;
import com.g42.platform.gms.service_ticket_management.infrastructure.entity.ServiceTicketAssignmentJpa;
import com.g42.platform.gms.service_ticket_management.infrastructure.entity.ServiceTicketJpa;
import com.g42.platform.gms.service_ticket_management.infrastructure.repository.ServiceTicketAssignmentRepository;
import com.g42.platform.gms.service_ticket_management.infrastructure.repository.ServiceTicketRepository;
import com.g42.platform.gms.staff.attendance.infrastructure.entity.StaffAttendanceJpa;
import com.g42.platform.gms.staff.attendance.infrastructure.repository.StaffAttendanceJpaRepo;
import com.g42.platform.gms.staff.attendance.domain.enums.AttendanceSlotEnum;
import com.g42.platform.gms.vehicle.entity.Vehicle;
import com.g42.platform.gms.vehicle.repository.VehicleRepository;
import com.g42.platform.gms.auth.entity.CustomerProfile;
import com.g42.platform.gms.auth.repository.CustomerProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StaffDashboardService {

    private final StaffProfileRepo staffProfileRepo;
    private final StaffScheduleRepository scheduleRepository;
    private final StaffAttendanceJpaRepo attendanceRepository;
    private final StaffNotificationRepository notificationRepository;
    private final ServiceTicketAssignmentRepository assignmentRepository;
    private final ServiceTicketRepository serviceTicketRepository;
    private final VehicleRepository vehicleRepository;
    private final CustomerProfileRepository customerProfileRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("MM/yyyy");
    private static final String[] DAY_NAMES = {"", "Chủ Nhật", "Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy"};

    @Transactional(readOnly = true)
    public DashboardOverviewResponse getDashboardOverview(Integer staffId) {
        log.info("Getting dashboard overview for staffId={}", staffId);

        LocalDate today = LocalDate.now();
        int year = today.getYear();
        int month = today.getMonthValue();

        DashboardOverviewResponse response = new DashboardOverviewResponse();

        // Staff info
        response.setStaff(buildStaffInfo(staffId));

        // Today's shift
        response.setTodayShift(buildTodayShift(staffId, today));

        // Monthly hours
        response.setMonthlyHours(buildMonthlyHours(staffId, year, month));

        // Completed services this month
        response.setCompletedServices(buildCompletedServices(staffId, year, month));

        // Today's tasks (top 5)
        response.setTodayTasks(buildTodayTasks(staffId, today));

        // Upcoming schedule (7 days)
        response.setUpcomingSchedule(buildUpcomingSchedule(staffId, today));

        // Recent attendance (5 records)
        response.setRecentAttendance(buildRecentAttendance(staffId));

        // Unread notifications (top 5)
        response.setNotifications(buildNotifications(staffId));

        return response;
    }

    public void markNotificationAsRead(Integer staffId, Integer notificationId) {
        StaffNotificationJpa notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy thông báo: " + notificationId));

        // Chỉ cho phép đánh dấu nếu notification thuộc về staff này hoặc là broadcast
        if (notification.getStaffId() != null && !notification.getStaffId().equals(staffId)) {
            throw new RuntimeException("Không có quyền truy cập thông báo này");
        }

        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<TaskSummaryDto> getTodayTasks(Integer staffId) {
        return buildTodayTasks(staffId, LocalDate.now());
    }

    @Transactional(readOnly = true)
    public List<ScheduleDayDto> getWorkSchedule(Integer staffId, LocalDate from, LocalDate to) {
        List<StaffScheduleJpa> schedules = scheduleRepository
            .findByStaffIdAndWorkDateBetweenOrderByWorkDateAsc(staffId, from, to);

        Map<LocalDate, StaffScheduleJpa> scheduleMap = schedules.stream()
            .collect(Collectors.toMap(StaffScheduleJpa::getWorkDate, s -> s));

        List<ScheduleDayDto> result = new ArrayList<>();
        LocalDate current = from;
        while (!current.isAfter(to)) {
            ScheduleDayDto dto = new ScheduleDayDto();
            dto.setDate(current.format(DATE_FMT));
            dto.setDayOfWeek(getDayOfWeek(current));
            StaffScheduleJpa schedule = scheduleMap.get(current);
            if (schedule != null) {
                dto.setStatus(schedule.getStatus());
                if (schedule.getShift() != null) {
                    dto.setShiftName(schedule.getShift().getShiftName());
                    dto.setStartTime(schedule.getShift().getStartTime().format(TIME_FMT));
                    dto.setEndTime(schedule.getShift().getEndTime().format(TIME_FMT));
                }
            } else {
                dto.setStatus("OFF");
            }
            result.add(dto);
            current = current.plusDays(1);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<AttendanceRecordDto> getAttendanceHistory(Integer staffId, int month, int year) {
        List<StaffAttendanceJpa> records = attendanceRepository.findByStaffIdAndMonth(staffId, year, month);
        return records.stream().map(this::toAttendanceRecordDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<NotificationSummaryDto> getNotifications(Integer staffId, int page, int size) {
        List<StaffNotificationJpa> notifications = notificationRepository
            .findAllByStaffId(staffId, PageRequest.of(page, size));
        return notifications.stream().map(n -> {
            NotificationSummaryDto dto = new NotificationSummaryDto();
            dto.setNotificationId(n.getNotificationId());
            dto.setTitle(n.getTitle());
            dto.setMessage(n.getMessage());
            dto.setNotificationType(n.getNotificationType());
            dto.setIsRead(n.getIsRead());
            dto.setSentAt(n.getSentAt() != null
                ? n.getSentAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
            return dto;
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getPersonalStatistics(Integer staffId, int month, int year) {
        Double totalHours = scheduleRepository.sumMonthlyHours(staffId, year, month);
        long completedCount = assignmentRepository.countCompletedByStaffInMonth(staffId, year, month);
        Map<String, Object> stats = new java.util.LinkedHashMap<>();
        stats.put("month", String.format("%02d/%d", month, year));
        stats.put("totalHours", totalHours != null ? totalHours : 0.0);
        stats.put("completedServices", completedCount);
        return stats;
    }

    // ===== Private helpers =====

    private StaffInfo buildStaffInfo(Integer staffId) {
        StaffInfo info = new StaffInfo();
        staffProfileRepo.findById(staffId).ifPresent(sp -> {
            info.setStaffId(sp.getStaffId());
            info.setFullName(sp.getFullName());
            info.setAvatar(sp.getAvatar());
            info.setPosition(sp.getPosition());
        });
        return info;
    }

    private TodayShiftDto buildTodayShift(Integer staffId, LocalDate today) {
        TodayShiftDto dto = new TodayShiftDto();
        dto.setDate(today.format(DATE_FMT));
        dto.setDayOfWeek(getDayOfWeek(today));

        scheduleRepository.findByStaffIdAndWorkDate(staffId, today).ifPresentOrElse(
            schedule -> {
                dto.setStatus(schedule.getStatus());
                if (schedule.getShift() != null) {
                    dto.setShiftName(schedule.getShift().getShiftName());
                    dto.setStartTime(schedule.getShift().getStartTime().format(TIME_FMT));
                    dto.setEndTime(schedule.getShift().getEndTime().format(TIME_FMT));
                }
            },
            () -> dto.setStatus("OFF")
        );
        return dto;
    }

    private MonthlyHoursDto buildMonthlyHours(Integer staffId, int year, int month) {
        MonthlyHoursDto dto = new MonthlyHoursDto();
        Double hours = scheduleRepository.sumMonthlyHours(staffId, year, month);
        dto.setTotalHours(hours != null ? hours : 0.0);
        dto.setMonth(String.format("%02d/%d", month, year));
        return dto;
    }

    private CompletedServicesDto buildCompletedServices(Integer staffId, int year, int month) {
        CompletedServicesDto dto = new CompletedServicesDto();
        long count = assignmentRepository.countCompletedByStaffInMonth(staffId, year, month);
        dto.setCount(count);
        dto.setMonth(String.format("%02d/%d", month, year));
        return dto;
    }

    private List<TaskSummaryDto> buildTodayTasks(Integer staffId, LocalDate today) {
        // Lấy tất cả assignment của staff
        List<ServiceTicketAssignmentJpa> assignments = assignmentRepository.findByStaffId(staffId);
        List<Integer> ticketIds = assignments.stream()
            .map(ServiceTicketAssignmentJpa::getServiceTicketId)
            .distinct()
            .collect(Collectors.toList());

        if (ticketIds.isEmpty()) return new ArrayList<>();

        // Lấy tickets có receivedAt trong ngày hôm nay
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(23, 59, 59);

        List<ServiceTicketJpa> todayTickets = serviceTicketRepository.findAllById(ticketIds).stream()
            .filter(t -> t.getReceivedAt() != null
                && !t.getReceivedAt().isBefore(startOfDay)
                && !t.getReceivedAt().isAfter(endOfDay))
            .limit(5)
            .collect(Collectors.toList());

        return todayTickets.stream().map(this::toTaskSummary).collect(Collectors.toList());
    }

    private TaskSummaryDto toTaskSummary(ServiceTicketJpa ticket) {
        TaskSummaryDto dto = new TaskSummaryDto();
        dto.setServiceTicketId(ticket.getServiceTicketId());
        dto.setTicketCode(ticket.getTicketCode());
        dto.setTicketStatus(ticket.getTicketStatus() != null ? ticket.getTicketStatus().name() : null);
        if (ticket.getReceivedAt() != null) {
            dto.setReceivedAt(ticket.getReceivedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        }

        vehicleRepository.findById(ticket.getVehicleId()).ifPresent(v -> {
            dto.setLicensePlate(v.getLicensePlate());
            dto.setVehicleBrand(v.getBrand());
            dto.setVehicleModel(v.getModel());
        });

        customerProfileRepository.findById(ticket.getCustomerId()).ifPresent(c ->
            dto.setCustomerName(c.getFullName())
        );

        return dto;
    }

    private List<ScheduleDayDto> buildUpcomingSchedule(Integer staffId, LocalDate today) {
        LocalDate endDate = today.plusDays(6);
        List<StaffScheduleJpa> schedules = scheduleRepository
            .findByStaffIdAndWorkDateBetweenOrderByWorkDateAsc(staffId, today, endDate);

        Map<LocalDate, StaffScheduleJpa> scheduleMap = schedules.stream()
            .collect(Collectors.toMap(StaffScheduleJpa::getWorkDate, s -> s));

        List<ScheduleDayDto> result = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate date = today.plusDays(i);
            ScheduleDayDto dto = new ScheduleDayDto();
            dto.setDate(date.format(DATE_FMT));
            dto.setDayOfWeek(getDayOfWeek(date));

            StaffScheduleJpa schedule = scheduleMap.get(date);
            if (schedule != null) {
                dto.setStatus(schedule.getStatus());
                if (schedule.getShift() != null) {
                    dto.setShiftName(schedule.getShift().getShiftName());
                    dto.setStartTime(schedule.getShift().getStartTime().format(TIME_FMT));
                    dto.setEndTime(schedule.getShift().getEndTime().format(TIME_FMT));
                }
            } else {
                dto.setStatus("OFF");
            }
            result.add(dto);
        }
        return result;
    }

    private List<AttendanceRecordDto> buildRecentAttendance(Integer staffId) {
        List<StaffAttendanceJpa> records = attendanceRepository
            .findRecentByStaffId(staffId, PageRequest.of(0, 5));
        return records.stream().map(this::toAttendanceRecordDto).collect(Collectors.toList());
    }

    private AttendanceRecordDto toAttendanceRecordDto(StaffAttendanceJpa r) {
        AttendanceRecordDto dto = new AttendanceRecordDto();
        dto.setDate(r.getAttendanceDate().format(DATE_FMT));
        dto.setDayOfWeek(getDayOfWeek(r.getAttendanceDate()));

        // Xác định ca dựa trên morningStatus/afternoonStatus
        if (r.getMorningStatus() != null && r.getAfternoonStatus() != null) {
            dto.setShiftType("FULL_DAY");
        } else if (r.getMorningStatus() != null) {
            dto.setShiftType("MORNING");
        } else {
            dto.setShiftType("AFTERNOON");
        }

        // created_at = giờ check-in, updated_at = giờ check-out
        ZoneId zone = ZoneId.systemDefault();
        if (r.getCreatedAt() != null) {
            LocalTime checkIn = r.getCreatedAt().atZone(zone).toLocalTime();
            dto.setCheckInTime(checkIn.format(TIME_FMT));
        }
        if (r.getUpdatedAt() != null) {
            LocalTime checkOut = r.getUpdatedAt().atZone(zone).toLocalTime();
            dto.setCheckOutTime(checkOut.format(TIME_FMT));
        }

        // Status: lấy từ morning hoặc afternoon (ưu tiên morning)
        AttendanceSlotEnum slot = r.getMorningStatus() != null ? r.getMorningStatus() : r.getAfternoonStatus();
        dto.setStatus(slot != null ? slot.name() : "NOT_YET");
        return dto;
    }

    private List<NotificationSummaryDto> buildNotifications(Integer staffId) {
        List<StaffNotificationJpa> notifications = notificationRepository
            .findUnreadByStaffId(staffId, PageRequest.of(0, 5));

        return notifications.stream().map(n -> {
            NotificationSummaryDto dto = new NotificationSummaryDto();
            dto.setNotificationId(n.getNotificationId());
            dto.setTitle(n.getTitle());
            dto.setMessage(n.getMessage());
            dto.setNotificationType(n.getNotificationType());
            dto.setIsRead(n.getIsRead());
            dto.setSentAt(n.getSentAt() != null
                ? n.getSentAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
            return dto;
        }).collect(Collectors.toList());
    }

    private String getDayOfWeek(LocalDate date) {
        int dow = date.getDayOfWeek().getValue(); // 1=Mon ... 7=Sun
        // Convert to our array index: Mon=2, Tue=3, ..., Sun=1
        int idx = (dow % 7) + 1;
        return DAY_NAMES[idx];
    }
}
