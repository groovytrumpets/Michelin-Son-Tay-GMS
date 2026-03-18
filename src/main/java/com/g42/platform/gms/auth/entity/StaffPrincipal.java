package com.g42.platform.gms.auth.entity;

import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
@Data
public class StaffPrincipal implements UserDetails {

    private StaffAuth staffAuth;

    public StaffPrincipal(StaffAuth staffAuth) {
        this.staffAuth = staffAuth;
    }

    public Integer getAuthId() {
        return staffAuth.getStaffAuthId();
    }

    public Integer getStaffId() {
        return staffAuth.getStaffProfile() != null ? staffAuth.getStaffProfile().getStaffId() : null;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Trả về tất cả roles của staff từ database
        // Cần có prefix "ROLE_" để @PreAuthorize("hasRole('TECHNICIAN')") hoạt động
        if (staffAuth.getStaffProfile() == null || staffAuth.getStaffProfile().getStaffRoles() == null) {
            return Collections.emptyList();
        }
        
        return staffAuth.getStaffProfile().getStaffRoles().stream()
            .map(staffRole -> new SimpleGrantedAuthority("ROLE_" + staffRole.getRole().getRoleCode()))
            .toList();
    }

    @Override
    public String getPassword() {
        // trả về password hash của staff
        return staffAuth.getPasswordHash();
    }

    @Override
    public String getUsername() {
        // trả về staff auth id dưới dạng string làm username
        return String.valueOf(staffAuth.getStaffAuthId());
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return (staffAuth.getStatus().equals("ACTIVE"));
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
