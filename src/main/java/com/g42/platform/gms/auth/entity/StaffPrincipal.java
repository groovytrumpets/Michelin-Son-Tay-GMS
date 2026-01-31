package com.g42.platform.gms.auth.entity;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class StaffPrincipal implements UserDetails {

    private Staffauth staffAuth;

    public StaffPrincipal(Staffauth staffAuth) {
        this.staffAuth = staffAuth;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton(new SimpleGrantedAuthority("STAFF"));
    }// thể hiện role của user

    @Override
    public String getPassword() {
        return staffAuth.getPasswordHash(); //truyền vào giá trị pass
    }

    @Override
    public String getUsername() {
        return staffAuth.getEmail(); //truyền vào giá trị username
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
