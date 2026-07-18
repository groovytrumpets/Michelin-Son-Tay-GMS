package com.g42.platform.gms.dashboard.application.service;

import com.g42.platform.gms.auth.entity.StaffProfile;
import com.g42.platform.gms.auth.repository.StaffProfileRepo;
import com.g42.platform.gms.dashboard.infrastructure.entity.KpiConfigJpa;
import com.g42.platform.gms.dashboard.infrastructure.entity.KpiMonthlyResultJpa;
import com.g42.platform.gms.dashboard.infrastructure.repository.KpiConfigRepository;
import com.g42.platform.gms.dashboard.infrastructure.repository.KpiMonthlyResultRepository;
import com.g42.platform.gms.manager.attendance.infrastructure.entity.AttendanceCheckinJpa;
import com.g42.platform.gms.manager.attendance.infrastructure.repository.AttendanceCheckinJpaRepo;
import com.g42.platform.gms.service_ticket_management.infrastructure.entity.ServiceTicketAssignmentJpa;
import com.g42.platform.gms.service_ticket_management.infrastructure.repository.ServiceTicketAssignmentRepository;
import com.g42.platform.gms.feedback.infrastructure.entity.FeedbackJpa;
import com.g42.platform.gms.feedback.infrastructure.repository.FeedbackJpaRepo;
import com.g42.platform.gms.warehouse.infrastructure.repository.ReturnEntryItemJpaRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
public class KpiCalculationService {

    private final KpiConfigRepository kpiConfigRepository;
    private final KpiMonthlyResultRepository kpiMonthlyResultRepository;
    private final StaffProfileRepo staffProfileRepo;
    private final AttendanceCheckinJpaRepo attendanceRepo;
    private final ServiceTicketAssignmentRepository assignmentRepo;
    private final FeedbackJpaRepo feedbackRepo;
    private final ReturnEntryItemJpaRepo returnEntryItemRepo;

    private int getExpectedWorkDays(LocalDate from, LocalDate to) {
        int count = 0;
        LocalDate current = from;
        while (!current.isAfter(to)) {
            if (current.getDayOfWeek() != java.time.DayOfWeek.SUNDAY) {
                count++;
            }
            current = current.plusDays(1);
        }
        return count > 0 ? count : 26;
    }

    private String getStaffRoleCode(StaffProfile staff) {
        if (staff.getStaffRoles() == null || staff.getStaffRoles().isEmpty()) {
            return "TECHNICIAN";
        }
        return staff.getStaffRoles().get(0).getRole().getRoleCode();
    }

    @Transactional
    public KpiMonthlyResultJpa calculateAndSaveKpi(Integer staffId, int month, int year) {
        StaffProfile staff = staffProfileRepo.findById(staffId)
                .orElseThrow(() -> new IllegalArgumentException("Staff not found: " + staffId));

        String roleCode = getStaffRoleCode(staff);
        KpiConfigJpa config = kpiConfigRepository.findByRoleName(roleCode)
                .orElseGet(() -> {
                    KpiConfigJpa fallback = new KpiConfigJpa();
                    fallback.setRoleName(roleCode);
                    fallback.setAttendanceWeight(0.25);
                    fallback.setCompletionWeight(0.25);
                    fallback.setSatisfactionWeight(0.25);
                    fallback.setQualityWeight(0.25);
                    fallback.setTargetTickets(40);
                    fallback.setTargetHours(160.0);
                    fallback.setTargetRating(4.8);
                    return kpiConfigRepository.save(fallback);
                });

        LocalDate fromDate = LocalDate.of(year, month, 1);
        LocalDate toDate = fromDate.withDayOfMonth(fromDate.lengthOfMonth());

        // 1. Attendance Score
        List<AttendanceCheckinJpa> checkins = attendanceRepo
                .findByStaffIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(staffId, fromDate, toDate);
        int expectedDays = getExpectedWorkDays(fromDate, toDate);

        // Đếm ngày duy nhất (nhiều ca/ngày chỉ tính 1 lần)
        long presentDays = checkins.stream()
                .map(AttendanceCheckinJpa::getAttendanceDate)
                .distinct()
                .count();
        long lateDays = checkins.stream()
                .filter(c -> "LATE".equalsIgnoreCase(c.getStatus()))
                .map(AttendanceCheckinJpa::getAttendanceDate)
                .distinct()
                .count();

        LocalDate today = LocalDate.now();
        double attendanceScore;

        if (fromDate.isAfter(today)) {
            // Tháng tương lai → chưa có dữ liệu, mặc định 100
            attendanceScore = 100.0;
        } else {
            // Tính số ngày phải làm đến thời điểm đánh giá
            // - Nếu tháng đã qua: đến toDate
            // - Nếu tháng đang chạy: đến hôm qua (hôm nay chưa kết thúc)
            LocalDate effectiveTo = toDate.isBefore(today) ? toDate : today.minusDays(1);
            // Nếu hôm nay là ngày đầu tháng, effectiveTo có thể trước fromDate
            if (effectiveTo.isBefore(fromDate)) {
                // Tháng vừa bắt đầu hôm nay, chưa có ngày làm việc nào trôi qua
                attendanceScore = 100.0;
            } else {
                int effectiveExpectedDays = getExpectedWorkDays(fromDate, effectiveTo);
                if (effectiveExpectedDays == 0) {
                    attendanceScore = 100.0;
                } else {
                    // Không checkin ngày nào → 0, không mặc định 100
                    double rawScore = ((double) presentDays / effectiveExpectedDays) * 100.0
                            - (lateDays * 5.0);
                    attendanceScore = Math.max(0.0, Math.min(100.0, rawScore));
                }
            }
        }

        // 2. Work Completion Score
        Instant startInstant = fromDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endInstant = toDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().minusNanos(1);
        List<ServiceTicketAssignmentJpa> allAssignments = assignmentRepo.findByStaffId(staffId);
        
        List<ServiceTicketAssignmentJpa> monthlyAssignments = allAssignments.stream()
                .filter(a -> a.getAssignedAt() != null && !a.getAssignedAt().isBefore(startInstant) && !a.getAssignedAt().isAfter(endInstant))
                .toList();

        double completionScore = 100.0;
        if (!monthlyAssignments.isEmpty()) {
            long totalTasks = monthlyAssignments.size();
            long completedTasks = monthlyAssignments.stream()
                    .filter(a -> "DONE".equalsIgnoreCase(a.getStatus().name()))
                    .count();
            completionScore = ((double) completedTasks / totalTasks) * 100.0;
        }

        // 3. Satisfaction Score
        List<Integer> ticketIds = monthlyAssignments.stream()
                .map(ServiceTicketAssignmentJpa::getServiceTicketId)
                .toList();

        double satisfactionScore = 100.0;
        if (!ticketIds.isEmpty()) {
            List<FeedbackJpa> feedbacks = feedbackRepo.findAll().stream()
                    .filter(f -> ticketIds.contains(f.getServiceTicketId()) && f.getStarRating() != null)
                    .toList();
            if (!feedbacks.isEmpty()) {
                double avgRating = feedbacks.stream()
                        .mapToInt(FeedbackJpa::getStarRating)
                        .average()
                        .orElse(5.0);
                satisfactionScore = (avgRating / 5.0) * 100.0;
            }
        }

        // 4. Quality Score
        LocalDateTime fromDateTime = fromDate.atStartOfDay();
        LocalDateTime toDateTime = toDate.atTime(23, 59, 59, 999999999);
        List<Object[]> defectDetails = returnEntryItemRepo.findDefectDetailsByStaff(staffId, fromDateTime, toDateTime);
        int defectCount = defectDetails.size();
        double qualityScore = Math.max(0.0, 100.0 - (defectCount * 10.0));

        // 5. Total KPI Score
        double totalKpiScore = (attendanceScore * config.getAttendanceWeight())
                + (completionScore * config.getCompletionWeight())
                + (satisfactionScore * config.getSatisfactionWeight())
                + (qualityScore * config.getQualityWeight());

        // ── Ranh giới cứng: chuyên cần < 60 → KPI tổng bị giới hạn ≤ 60 ──
        // Lý do: không đi làm đủ thì dù làm tốt cũng không thể đạt yêu cầu
        if (attendanceScore < 60.0) {
            totalKpiScore = Math.min(totalKpiScore, 60.0);
        }

        // ── Ranh giới cứng: chất lượng = 0 (≥10 lỗi hàng) → KPI tổng ≤ 50 ──
        if (qualityScore == 0.0 && config.getQualityWeight() > 0) {
            totalKpiScore = Math.min(totalKpiScore, 50.0);
        }

        attendanceScore = Math.round(attendanceScore * 100.0) / 100.0;
        completionScore = Math.round(completionScore * 100.0) / 100.0;
        satisfactionScore = Math.round(satisfactionScore * 100.0) / 100.0;
        qualityScore = Math.round(qualityScore * 100.0) / 100.0;
        totalKpiScore = Math.round(totalKpiScore * 100.0) / 100.0;

        String period = year + "-" + String.format("%02d", month);
        KpiMonthlyResultJpa result = kpiMonthlyResultRepository.findByStaffIdAndPeriodMonth(staffId, period)
                .orElse(new KpiMonthlyResultJpa());

        result.setStaffId(staffId);
        result.setPeriodMonth(period);
        result.setAttendanceScore(attendanceScore);
        result.setCompletionScore(completionScore);
        result.setSatisfactionScore(satisfactionScore);
        result.setQualityScore(qualityScore);
        result.setTotalKpiScore(totalKpiScore);

        return kpiMonthlyResultRepository.save(result);
    }

    @Transactional
    public void recalculateAllStaff(int month, int year) {
        String period = year + "-" + String.format("%02d", month);
        // Xóa kết quả cũ của kỳ này trước, rồi tính lại
        List<KpiMonthlyResultJpa> oldResults = kpiMonthlyResultRepository.findAllByPeriodMonth(period);
        kpiMonthlyResultRepository.deleteAll(oldResults);
        
        List<StaffProfile> allStaff = staffProfileRepo.findAll();
        for (StaffProfile staff : allStaff) {
            try {
                calculateAndSaveKpi(staff.getStaffId(), month, year);
            } catch (Exception e) {
                // log lỗi nhưng tiếp tục với nhân viên khác
            }
        }
    }

    public List<Map<String, Object>> getKpiDashboardForManager(int month, int year) {
        String period = year + "-" + String.format("%02d", month);
        List<StaffProfile> allStaff = staffProfileRepo.findAll();

        List<Map<String, Object>> dashboardData = new ArrayList<>();

        for (StaffProfile staff : allStaff) {
            KpiMonthlyResultJpa result = kpiMonthlyResultRepository.findByStaffIdAndPeriodMonth(staff.getStaffId(), period)
                    .orElseGet(() -> {
                        try {
                            return calculateAndSaveKpi(staff.getStaffId(), month, year);
                        } catch (Exception e) {
                            KpiMonthlyResultJpa fallback = new KpiMonthlyResultJpa();
                            fallback.setStaffId(staff.getStaffId());
                            fallback.setPeriodMonth(period);
                            fallback.setAttendanceScore(0.0);
                            fallback.setCompletionScore(0.0);
                            fallback.setSatisfactionScore(0.0);
                            fallback.setQualityScore(0.0);
                            fallback.setTotalKpiScore(0.0);
                            return fallback;
                        }
                    });

            Map<String, Object> staffData = new LinkedHashMap<>();
            staffData.put("staffId", staff.getStaffId());
            staffData.put("fullName", staff.getFullName());
            staffData.put("avatar", staff.getAvatar());
            staffData.put("position", staff.getPosition());
            staffData.put("role", getStaffRoleCode(staff));
            staffData.put("periodMonth", period);
            staffData.put("attendanceScore", result.getAttendanceScore());
            staffData.put("completionScore", result.getCompletionScore());
            staffData.put("satisfactionScore", result.getSatisfactionScore());
            staffData.put("qualityScore", result.getQualityScore());
            staffData.put("totalKpiScore", result.getTotalKpiScore());
            
            // Thêm defectCount thực tế cho bảng hiển thị
            LocalDate fd = LocalDate.of(year, month, 1);
            LocalDate td = fd.withDayOfMonth(fd.lengthOfMonth());
            int defectCount = returnEntryItemRepo.findDefectDetailsByStaff(
                    staff.getStaffId(), fd.atStartOfDay(), td.atTime(23, 59, 59)).size();
            staffData.put("defectCount", defectCount);
            
            dashboardData.add(staffData);
        }

        return dashboardData;
    }

    public Map<String, Object> getKpiDetailsForStaff(Integer staffId, int month, int year) {
        String period = year + "-" + String.format("%02d", month);
        StaffProfile staff = staffProfileRepo.findById(staffId)
                .orElseThrow(() -> new IllegalArgumentException("Staff not found: " + staffId));

        KpiMonthlyResultJpa result = kpiMonthlyResultRepository.findByStaffIdAndPeriodMonth(staffId, period)
                .orElseGet(() -> calculateAndSaveKpi(staffId, month, year));

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("staffId", staff.getStaffId());
        details.put("fullName", staff.getFullName());
        details.put("avatar", staff.getAvatar());
        details.put("position", staff.getPosition());
        details.put("role", getStaffRoleCode(staff));
        details.put("periodMonth", period);
        details.put("attendanceScore", result.getAttendanceScore());
        details.put("completionScore", result.getCompletionScore());
        details.put("satisfactionScore", result.getSatisfactionScore());
        details.put("qualityScore", result.getQualityScore());
        details.put("totalKpiScore", result.getTotalKpiScore());

        LocalDate fromDate = LocalDate.of(year, month, 1);
        LocalDate toDate = fromDate.withDayOfMonth(fromDate.lengthOfMonth());
        List<AttendanceCheckinJpa> checkins = attendanceRepo
                .findByStaffIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(staffId, fromDate, toDate);
        // Tính theo ngày duy nhất (nhiều ca trong 1 ngày chỉ tính 1 lần)
        long lateCount = checkins.stream()
                .filter(c -> "LATE".equalsIgnoreCase(c.getStatus()))
                .map(AttendanceCheckinJpa::getAttendanceDate)
                .distinct()
                .count();
        long presentCount = checkins.stream()
                .map(AttendanceCheckinJpa::getAttendanceDate)
                .distinct()
                .count();
        // Số ngày phải làm tính đến hôm nay (nếu tháng đang chạy)
        LocalDate today2 = LocalDate.now();
        LocalDate effectiveTo = toDate.isBefore(today2) ? toDate : today2.minusDays(1);
        int effectiveExpected = effectiveTo.isBefore(fromDate) ? 0 : getExpectedWorkDays(fromDate, effectiveTo);
        details.put("presentCount", presentCount);
        details.put("lateCount", lateCount);
        details.put("expectedWorkDays", effectiveExpected);

        Instant startInstant = fromDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endInstant = toDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().minusNanos(1);
        List<ServiceTicketAssignmentJpa> allAssignments = assignmentRepo.findByStaffId(staffId);
        List<ServiceTicketAssignmentJpa> monthlyAssignments = allAssignments.stream()
                .filter(a -> a.getAssignedAt() != null && !a.getAssignedAt().isBefore(startInstant) && !a.getAssignedAt().isAfter(endInstant))
                .toList();

        long totalTasks = monthlyAssignments.size();
        long completedTasks = monthlyAssignments.stream()
                .filter(a -> "DONE".equalsIgnoreCase(a.getStatus().name()))
                .count();
        details.put("totalTasks", totalTasks);
        details.put("completedTasks", completedTasks);

        List<Integer> ticketIds = monthlyAssignments.stream().map(ServiceTicketAssignmentJpa::getServiceTicketId).toList();
        double avgRating = 5.0;
        int ratingCount = 0;
        if (!ticketIds.isEmpty()) {
            List<FeedbackJpa> feedbacks = feedbackRepo.findAll().stream()
                    .filter(f -> ticketIds.contains(f.getServiceTicketId()) && f.getStarRating() != null)
                    .toList();
            ratingCount = feedbacks.size();
            avgRating = feedbacks.stream().mapToInt(FeedbackJpa::getStarRating).average().orElse(5.0);
        }
        details.put("averageRating", Math.round(avgRating * 10.0) / 10.0);
        details.put("ratingCount", ratingCount);

        LocalDateTime fromDateTime = fromDate.atStartOfDay();
        LocalDateTime toDateTime = toDate.atTime(23, 59, 59, 999999999);
        List<Object[]> defectDetails = returnEntryItemRepo.findDefectDetailsByStaff(staffId, fromDateTime, toDateTime);
        details.put("defectCount", defectDetails.size());

        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            LocalDate d = LocalDate.now().minusMonths(i);
            String p = d.getYear() + "-" + String.format("%02d", d.getMonthValue());
            KpiMonthlyResultJpa r = kpiMonthlyResultRepository.findByStaffIdAndPeriodMonth(staffId, p)
                    .orElse(null);
            Map<String, Object> tPoint = new LinkedHashMap<>();
            tPoint.put("periodMonth", p);
            tPoint.put("score", r != null ? r.getTotalKpiScore() : (r == null && i == 0 ? result.getTotalKpiScore() : 0.0));
            trend.add(tPoint);
        }
        details.put("trend", trend);

        return details;
    }
}
