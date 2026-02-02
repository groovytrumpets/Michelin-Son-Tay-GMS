package com.g42.platform.gms.auth.listener;

import com.g42.platform.gms.auth.entity.StaffAuth;
import com.g42.platform.gms.auth.entity.StaffProfile;
import com.g42.platform.gms.auth.repository.StaffAuthRepo;
import com.g42.platform.gms.auth.repository.StaffProfileRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthFailureListener {
    private final StaffAuthRepo staffAuthRepo;
    private final StaffProfileRepo staffProfileRepo;
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
                return;
            }else staffAuth = staffProfile.getStaffauth();

        }
        if (staffAuth == null) {
            return;
        }

            staffAuth.setFailedLoginCount(staffAuth.getFailedLoginCount() + 1);
        System.err.println("Staff Login Failure, FAILED LOGIN ATTEMPT: " + staffAuth.getFailedLoginCount());
            if (staffAuth.getFailedLoginCount() >= MAX_LOGIN_ATTEMPTS) {
                staffAuth.setStatus("LOCKED");
            }
                staffAuthRepo.save(staffAuth);

    }

}
