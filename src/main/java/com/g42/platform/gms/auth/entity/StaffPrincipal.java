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

    public Long getAuthId() {
        return staffAuth.getStaffAuthId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // trả về role của staff, cần có prefix "ROLE_" để @PreAuthorize("hasRole('STAFF')") hoạt động
        return Collections.singleton(new SimpleGrantedAuthority("ROLE_STAFF"));
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
