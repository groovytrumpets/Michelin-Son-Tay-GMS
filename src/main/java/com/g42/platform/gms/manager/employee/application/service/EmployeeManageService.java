package com.g42.platform.gms.manager.employee.application.service;

import com.g42.platform.gms.auth.entity.StaffProfile;
import com.g42.platform.gms.auth.repository.StaffProfileRepo;
import com.g42.platform.gms.manager.attendance.infrastructure.entity.AttendanceCheckinJpa;
import com.g42.platform.gms.manager.attendance.infrastructure.repository.AttendanceCheckinJpaRepo;
import com.g42.platform.gms.manager.employee.api.dto.EmployeeDetailResponse;
import com.g42.platform.gms.manager.employee.api.dto.EmployeeResponse;
import com.g42.platform.gms.manager.schedule.infrastructure.entity.WorkShiftJpa;
import com.g42.platform.gms.manager.schedule.infrastructure.repository.WorkShiftJpaRepo;
import com.g42.platform.gms.service_ticket_management.infrastructure.repository.ServiceTicketAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeManageService {

    private final StaffProfileRepo staffProfileRepo;
    private final AttendanceCheckinJpaRepo checkinJpaRepo;
    private final ServiceTicketAssignmentRepository assignmentRepo;
    private final WorkShiftJpaRepo workShiftJpaRepo;

    public List<EmployeeResponse> getAllEmployees() {
        return staffProfileRepo.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public EmployeeDetailResponse getEmployeeDetail(Integer staffId) {
        StaffProfile profile = staffProfileRepo.findById(staffId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên"));

        LocalDate now = LocalDate.now();
        LocalDate firstOfMonth = now.withDayOfMonth(1);

        // Performance tháng hiện tại
        int workDays = checkinJpaRepo.countWorkDays(staffId, firstOfMonth, now);
        long totalTickets = assignmentRepo.countCompletedByStaffInMonth(staffId, now.getYear(), now.getMonthValue());

        // Lịch điểm danh 30 ngày gần nhất
        LocalDate from = now.minusDays(29);
        List<AttendanceCheckinJpa> checkins = checkinJpaRepo.findByStaffAndDateRange(staffId, from, now);

        Map<Integer, String> shiftNames = workShiftJpaRepo.findAll().stream()
                .collect(Collectors.toMap(WorkShiftJpa::getShiftId, WorkShiftJpa::getShiftName));

        List<EmployeeDetailResponse.AttendanceRecord> records = checkins.stream()
                .map(c -> EmployeeDetailResponse.AttendanceRecord.builder()
                        .checkinId(c.getCheckinId())
                        .attendanceDate(c.getAttendanceDate())
                        .shiftId(c.getShiftId())
                        .shiftName(c.getShiftId() != null ? shiftNames.getOrDefault(c.getShiftId(), "") : null)
                        .checkInTime(c.getCheckInTime())
                        .checkOutTime(c.getCheckOutTime())
                        .status(c.getStatus())
                        .build())
                .toList();

        return EmployeeDetailResponse.builder()
                .staffId(profile.getStaffId())
                .fullName(profile.getFullName())
                .phone(profile.getPhone())
                .position(profile.getPosition())
                .gender(profile.getGender())
                .dob(profile.getDob())
                .avatar(profile.getAvatar())
                .performance(EmployeeDetailResponse.PerformanceSummary.builder()
                        .totalWorkDays(workDays)
                        .totalTicketsHandled((int) totalTickets)
                        .build())
                .recentAttendance(records)
                .build();
    }

    private EmployeeResponse toResponse(StaffProfile p) {
        return EmployeeResponse.builder()
                .staffId(p.getStaffId())
                .fullName(p.getFullName())
                .phone(p.getPhone())
                .position(p.getPosition())
                .gender(p.getGender())
                .dob(p.getDob())
                .avatar(p.getAvatar())
                .build();
    }
}
