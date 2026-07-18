package com.g42.platform.gms.auth.listener;

import com.g42.platform.gms.auth.entity.StaffAuth;
import com.g42.platform.gms.auth.entity.StaffProfile;
import com.g42.platform.gms.auth.repository.StaffAuthRepo;
import com.g42.platform.gms.auth.repository.StaffProfileRepo;
import com.g42.platform.gms.systemlog.service.AuditRecord;
import com.g42.platform.gms.systemlog.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthFailureListener {
    private final StaffAuthRepo staffAuthRepo;
    private final StaffProfileRepo staffProfileRepo;
    private final AuditService auditService;
    private static final int MAX_LOGIN_ATTEMPTS = 10;
    @EventListener
    public void onStaffLoginFailure(AbstractAuthenticationFailureEvent event) {

        Object principal = event.getAuthentication().getPrincipal();
        if (!(principal instanceof String identifier)) {
            return;
        }

        StaffAuth staffAuth = null;
        if (identifier.contains("@")) {
        staffAuth = staffAuthRepo.searchByEmail(identifier);
        }else {
            //System.err.println("ID: "+identifier);
            StaffProfile staffProfile = staffProfileRepo.searchByPhone(identifier);
            if (staffProfile == null) {
                auditLoginFailure(identifier, false);
                return;
            }else staffAuth = staffProfile.getStaffauth();

        }
        if (staffAuth == null) {
            auditLoginFailure(identifier, false);
            return;
        }

            staffAuth.setFailedLoginCount(staffAuth.getFailedLoginCount() + 1);
        System.err.println("Staff Login Failure, FAILED LOGIN ATTEMPT: " + staffAuth.getFailedLoginCount());
            boolean locked = staffAuth.getFailedLoginCount() >= MAX_LOGIN_ATTEMPTS;
            if (locked) {
                staffAuth.setStatus("LOCKED");
            }
                staffAuthRepo.save(staffAuth);
        auditLoginFailure(identifier, locked);

    }

    private void auditLoginFailure(String identifier, boolean locked) {
        try {
            AuditRecord.AuditRecordBuilder builder = AuditRecord.builder()
                    .action("LOGIN_FAILED")
                    .module("AUTH")
                    .severity(locked ? "CRITICAL" : "WARNING")
                    .actorName(identifier)
                    .description(locked
                            ? "Đăng nhập thất bại — tài khoản đã bị khóa do vượt quá số lần thử"
                            : "Đăng nhập thất bại");
            auditService.fillRequestInfo(builder);
            auditService.record(builder.build());
        } catch (Exception ignored) {
        }
    }

}
