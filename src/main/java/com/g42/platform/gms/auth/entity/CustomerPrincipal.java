package com.g42.platform.gms.auth.entity;

import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Data
public class CustomerPrincipal implements UserDetails {
    
    private Integer customerId;
    private String phone;
    private String name; // fullName từ token
    
    public CustomerPrincipal(Integer customerId, String phone, String name) {
        this.customerId = customerId;
        this.phone = phone;
        this.name = name;
    }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
    }
    
    @Override
    public String getPassword() {
        return ""; // Customer không dùng password
    }
    
    @Override
    public String getUsername() {
        return phone; // Username = phone
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return true;
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
