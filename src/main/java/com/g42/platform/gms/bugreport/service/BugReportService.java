package com.g42.platform.gms.bugreport.service;

import com.g42.platform.gms.auth.entity.CustomerPrincipal;
import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.bugreport.dto.BugReportCreateRequest;
import com.g42.platform.gms.bugreport.dto.BugReportDto;
import com.g42.platform.gms.bugreport.entity.BugReportJpa;
import com.g42.platform.gms.bugreport.enums.BugReportStatus;
import com.g42.platform.gms.bugreport.enums.ReporterType;
import com.g42.platform.gms.bugreport.repository.BugReportJpaRepo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** Tiếp nhận phiếu báo lỗi từ nhân viên hoặc khách hàng đã đăng nhập. */
@Service
@RequiredArgsConstructor
public class BugReportService {

    private static final int MAX_ATTACHMENTS = 5;

    private final BugReportJpaRepo bugReportJpaRepo;
    private final BugReportNotifyService bugReportNotifyService;

    @Transactional
    public BugReportDto create(BugReportCreateRequest request, HttpServletRequest httpRequest) {
        BugReportJpa entity = new BugReportJpa();
        entity.setTitle(request.getTitle().trim());
        entity.setDescription(request.getDescription().trim());
        entity.setCategory(request.getCategory());
        entity.setSeverity(request.getSeverity());
        entity.setModule(blankToNull(request.getModule()));
        entity.setStatus(BugReportStatus.NEW);
        entity.setReporterContact(blankToNull(request.getReporterContact()));
        entity.setPageUrl(truncate(request.getPageUrl(), 500));
        entity.setScreenSize(truncate(request.getScreenSize(), 40));
        entity.setAppVersion(truncate(request.getAppVersion(), 40));
        entity.setUserAgent(truncate(httpRequest.getHeader("User-Agent"), 300));
        entity.setIpAddress(truncate(resolveIp(httpRequest), 64));
        entity.setCreatedAt(LocalDateTime.now());

        applyReporter(entity);

        List<String> urls = request.getAttachmentUrls();
        if (urls != null) {
            urls.stream()
                    .filter(url -> url != null && !url.isBlank())
                    .limit(MAX_ATTACHMENTS)
                    .forEach(url -> entity.addAttachment(truncate(url.trim(), 500)));
        }

        BugReportDto dto = BugReportMapper.toDto(bugReportJpaRepo.save(entity));
        bugReportNotifyService.notifyAdmins(dto);
        return dto;
    }

    /** Gắn danh tính người báo lỗi từ token đang đăng nhập (nhân viên hoặc khách hàng). */
    private void applyReporter(BugReportJpa entity) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication == null ? null : authentication.getPrincipal();

        if (principal instanceof StaffPrincipal staff) {
            entity.setReporterType(ReporterType.STAFF);
            entity.setReporterStaffId(staff.getStaffId());
            if (staff.getStaffAuth() != null && staff.getStaffAuth().getStaffProfile() != null) {
                entity.setReporterName(staff.getStaffAuth().getStaffProfile().getFullName());
            }
            entity.setReporterRole(primaryRole(authentication));
            return;
        }

        if (principal instanceof CustomerPrincipal customer) {
            entity.setReporterType(ReporterType.CUSTOMER);
            entity.setReporterCustomerId(customer.getCustomerId());
            entity.setReporterName(customer.getName());
            entity.setReporterRole("CUSTOMER");
            if (entity.getReporterContact() == null) {
                entity.setReporterContact(customer.getPhone());
            }
            return;
        }

        // Endpoint yêu cầu đăng nhập nên nhánh này gần như không xảy ra;
        // giữ lại để không ghi bản ghi thiếu cột NOT NULL.
        entity.setReporterType(ReporterType.CUSTOMER);
        entity.setReporterName("Không xác định");
    }

    private static String primaryRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(role -> role.startsWith("ROLE_") ? role.substring("ROLE_".length()) : role)
                .findFirst()
                .orElse(null);
    }

    private static String resolveIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.isBlank()) return null;
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }
}
