package com.g42.platform.gms.auth.entity;

import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Data
public class StaffPrincipal implements UserDetails {

    private StaffAuth staffAuth;

    /**
     * Mã quyền suy ra từ các vai trò của nhân viên (PermissionResolver).
     * Null nghĩa là chưa ai nạp — principal dựng ở nhánh code cũ vẫn chạy được,
     * chỉ là lúc đó chỉ có ROLE_*; xem getAuthorities().
     */
    private Set<String> permissionCodes;

    public StaffPrincipal(StaffAuth staffAuth) {
        this.staffAuth = staffAuth;
    }

    public StaffPrincipal(StaffAuth staffAuth, Set<String> permissionCodes) {
        this.staffAuth = staffAuth;
        this.permissionCodes = permissionCodes;
    }

    public Integer getAuthId() {
        return staffAuth.getStaffAuthId();
    }

    public Integer getStaffId() {
        return staffAuth.getStaffProfile() != null ? staffAuth.getStaffProfile().getStaffId() : null;
    }

    /** Mã vai trò không có tiền tố ROLE_, dùng để tra quyền. */
    public List<String> getRoleCodes() {
        if (staffAuth.getStaffProfile() == null || staffAuth.getStaffProfile().getStaffRoles() == null) {
            return Collections.emptyList();
        }
        return staffAuth.getStaffProfile().getStaffRoles().stream()
                .map(staffRole -> staffRole.getRole().getRoleCode())
                .toList();
    }

    /**
     * Trả về hai loại authority cùng lúc:
     *   - ROLE_<mã vai trò> — phía nhân viên đã chuyển hết sang mã quyền, nhưng
     *     vẫn giữ vì code nghiệp vụ tra vai trò theo tên (TicketAssignmentService,
     *     CheckInService tìm "ai là KTV", "ai là cố vấn"), và vì phía khách hàng
     *     còn dùng hasRole('CUSTOMER').
     *   - <MÃ QUYỀN>        cho các @PreAuthorize("hasAuthority(...)").
     *
     * Quy tắc khi thêm endpoint mới cho nhân viên: chặn bằng hasAuthority với một
     * hằng số trong PermissionCodes, đừng quay lại hasRole — hasRole nằm ngoài
     * tầm với của màn /role-permission-config.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();

        for (String roleCode : getRoleCodes()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + roleCode));
        }

        if (permissionCodes != null) {
            for (String permissionCode : permissionCodes) {
                authorities.add(new SimpleGrantedAuthority(permissionCode));
            }
        }

        return authorities;
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
