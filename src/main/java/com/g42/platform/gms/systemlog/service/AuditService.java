package com.g42.platform.gms.systemlog.service;

import com.g42.platform.gms.auth.entity.CustomerPrincipal;
import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.systemlog.entity.SystemLogJpa;
import com.g42.platform.gms.systemlog.repository.SystemLogJpaRepo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final SystemLogJpaRepo systemLogJpaRepo;

    /**
     * Ghi bản ghi audit trên thread riêng. Không bao giờ ném exception ra ngoài
     * — audit lỗi không được phép làm hỏng nghiệp vụ chính.
     */
    @Async("auditExecutor")
    public void record(AuditRecord cmd) {
        try {
            SystemLogJpa row = new SystemLogJpa();
            row.setActorStaffId(cmd.getActorStaffId());
            row.setActorName(truncate(cmd.getActorName(), 150));
            row.setActorRole(truncate(cmd.getActorRole(), 50));
            row.setAction(cmd.getAction());
            row.setModule(cmd.getModule());
            row.setSeverity(cmd.getSeverity() == null || cmd.getSeverity().isBlank() ? "INFO" : cmd.getSeverity());
            row.setDescription(truncate(cmd.getDescription(), 1000));
            row.setTargetType(truncate(cmd.getTargetType(), 60));
            row.setTargetId(truncate(cmd.getTargetId(), 60));
            row.setIpAddress(truncate(cmd.getIpAddress(), 64));
            row.setUserAgent(truncate(cmd.getUserAgent(), 300));
            systemLogJpaRepo.save(row);
        } catch (Exception e) {
            log.warn("Audit write failed ({} / {}): {}", cmd.getModule(), cmd.getAction(), e.getMessage());
        }
    }

    /** Builder đã điền sẵn actor (từ SecurityContext) + ip/user-agent (từ request hiện tại). */
    public AuditRecord.AuditRecordBuilder currentContext() {
        AuditRecord.AuditRecordBuilder builder = AuditRecord.builder();
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof StaffPrincipal principal) {
                builder.actorStaffId(principal.getStaffId());
                if (principal.getStaffAuth() != null && principal.getStaffAuth().getStaffProfile() != null) {
                    builder.actorName(principal.getStaffAuth().getStaffProfile().getFullName());
                }
                principal.getAuthorities().stream().findFirst()
                        .map(a -> a.getAuthority().replaceFirst("^ROLE_", ""))
                        .ifPresent(builder::actorRole);
            } else if (auth != null && auth.getPrincipal() instanceof CustomerPrincipal customer) {
                builder.actorName(customer.getName());
                builder.actorRole("CUSTOMER");
            }
        } catch (Exception ignored) {
        }
        fillRequestInfo(builder);
        return builder;
    }

    /** Điền ip/user-agent từ request hiện tại (dùng được cả khi chưa đăng nhập). */
    public void fillRequestInfo(AuditRecord.AuditRecordBuilder builder) {
        try {
            if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
                HttpServletRequest request = attrs.getRequest();
                String forwarded = request.getHeader("X-Forwarded-For");
                String ip = (forwarded != null && !forwarded.isBlank())
                        ? forwarded.split(",")[0].trim()
                        : request.getRemoteAddr();
                builder.ipAddress(ip);
                builder.userAgent(request.getHeader("User-Agent"));
            }
        } catch (Exception ignored) {
        }
    }

    private static String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }
}
