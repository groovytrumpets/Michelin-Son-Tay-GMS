package com.g42.platform.gms.bugreport.service;

import com.g42.platform.gms.auth.entity.StaffProfile;
import com.g42.platform.gms.auth.repository.StaffProfileRepo;
import com.g42.platform.gms.bugreport.dto.BugReportDto;
import com.g42.platform.gms.bugreport.enums.BugReportSeverity;
import com.g42.platform.gms.dashboard.api.dto.NotificationCreateDto;
import com.g42.platform.gms.dashboard.application.service.StaffNotifyService;
import com.g42.platform.gms.dashboard.domain.enums.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Đẩy thông báo realtime khi có phiếu báo lỗi mới — chỉ tới các tài khoản ADMIN.
 * <p>
 * Hai kênh song song:
 * <ul>
 *   <li>{@link StaffNotifyService}: lưu DB + bắn STOMP {@code /user/queue/private-notifications}
 *       + Web Push (chuông thông báo chung của nhân viên).</li>
 *   <li>{@code /user/queue/bug-reports}: bắn nguyên phiếu để trang quản lý tự chèn
 *       dòng mới mà không cần tải lại.</li>
 * </ul>
 * Cả hai đều gửi theo user (principal = staffId) nên nhân viên không phải ADMIN
 * không thể subscribe nhận được.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BugReportNotifyService {

    public static final String ADMIN_ROLE_CODE = "ADMIN";
    private static final String BUG_REPORT_QUEUE = "/queue/bug-reports";
    private static final String BUG_REPORT_PAGE_URL = "/bug-report-management";

    private final StaffProfileRepo staffProfileRepo;
    private final StaffNotifyService staffNotifyService;
    private final SimpMessagingTemplate simpMessagingTemplate;

    public void notifyAdmins(BugReportDto report) {
        List<StaffProfile> admins = staffProfileRepo.findByRoleCode(ADMIN_ROLE_CODE);
        if (admins.isEmpty()) {
            log.warn("Có phiếu báo lỗi #{} nhưng hệ thống không có tài khoản ADMIN nào để thông báo.",
                    report.getReportId());
            return;
        }

        String title = "Báo lỗi phần mềm mới #" + report.getReportId();
        String message = "[" + severityLabel(report.getSeverity()) + "] " + report.getTitle()
                + " — người gửi: " + (report.getReporterName() == null ? "Không rõ" : report.getReporterName());

        for (StaffProfile admin : admins) {
            Integer adminStaffId = admin.getStaffId();
            if (adminStaffId == null) continue;

            try {
                // staff_notification.sent_by là FK NOT NULL tới staff_profile; khách hàng
                // không có staff_id nên ghi nhận chính admin nhận thông báo làm người gửi.
                Integer sentBy = report.getReporterStaffId() != null ? report.getReporterStaffId() : adminStaffId;
                staffNotifyService.createAndSendManual(new NotificationCreateDto(
                        adminStaffId, title, message, toNotificationType(report.getSeverity()),
                        false, sentBy, BUG_REPORT_PAGE_URL));

                simpMessagingTemplate.convertAndSendToUser(
                        String.valueOf(adminStaffId), BUG_REPORT_QUEUE, report);
            } catch (Exception ex) {
                // Không để lỗi thông báo làm hỏng việc ghi nhận phiếu báo lỗi.
                log.error("Không gửi được thông báo phiếu báo lỗi #{} tới admin {}: {}",
                        report.getReportId(), adminStaffId, ex.getMessage());
            }
        }
    }

    private static NotificationType toNotificationType(BugReportSeverity severity) {
        if (severity == BugReportSeverity.CRITICAL) return NotificationType.URGENT;
        if (severity == BugReportSeverity.HIGH) return NotificationType.WARNING;
        return NotificationType.INFO;
    }

    private static String severityLabel(BugReportSeverity severity) {
        if (severity == null) return "Không rõ";
        return switch (severity) {
            case LOW -> "Thấp";
            case MEDIUM -> "Trung bình";
            case HIGH -> "Cao";
            case CRITICAL -> "Nghiêm trọng";
        };
    }
}
