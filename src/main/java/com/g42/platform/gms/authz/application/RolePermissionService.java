package com.g42.platform.gms.authz.application;

import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.authz.PermissionCodes;
import com.g42.platform.gms.authz.api.dto.MyPermissionsDto;
import com.g42.platform.gms.authz.api.dto.PermissionDto;
import com.g42.platform.gms.authz.api.dto.PermissionMatrixDto;
import com.g42.platform.gms.authz.api.dto.RoleWithPermissionsDto;
import com.g42.platform.gms.authz.infrastructure.entity.PermissionJpa;
import com.g42.platform.gms.authz.infrastructure.entity.RolePermissionJpa;
import com.g42.platform.gms.authz.infrastructure.repository.PermissionJpaRepo;
import com.g42.platform.gms.authz.infrastructure.repository.RolePermissionDefaultRepo;
import com.g42.platform.gms.authz.infrastructure.repository.RolePermissionJpaRepo;
import com.g42.platform.gms.staff.profile.infrastructure.entity.RoleJpa;
import com.g42.platform.gms.staff.profile.infrastructure.repository.RoleJpaRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class RolePermissionService {

    private static final Pattern ROLE_CODE_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_]{1,49}$");

    private final PermissionJpaRepo permissionRepo;
    private final RolePermissionJpaRepo rolePermissionRepo;
    private final RolePermissionDefaultRepo rolePermissionDefaultRepo;
    private final RoleJpaRepo roleRepo;
    private final PermissionResolver permissionResolver;

    // ------------------------------------------------------------------
    // Đọc
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PermissionMatrixDto getMatrix() {
        List<PermissionDto> permissions = permissionRepo.findAllByOrderBySortOrderAsc()
                .stream().map(this::toDto).toList();

        Map<Integer, List<String>> grantsByRole = new HashMap<>();
        for (RolePermissionJpa row : rolePermissionRepo.findAll()) {
            grantsByRole.computeIfAbsent(row.getRoleId(), k -> new ArrayList<>()).add(row.getPermissionCode());
        }

        Map<Integer, Integer> staffCounts = new HashMap<>();
        for (Object[] row : roleRepo.countStaffPerRole()) {
            staffCounts.put(((Number) row[0]).intValue(), ((Number) row[1]).intValue());
        }

        List<String> allCodes = permissions.stream().map(PermissionDto::getCode).toList();

        List<RoleWithPermissionsDto> roles = new ArrayList<>();
        for (RoleJpa role : roleRepo.findAllByOrderByIdAsc()) {
            // CUSTOMER là vai trò của phía khách, không liên quan màn quản trị.
            if ("CUSTOMER".equals(role.getRoleCode())) continue;

            boolean superRole = PermissionResolver.SUPER_ROLE.equals(role.getRoleCode());
            roles.add(new RoleWithPermissionsDto(
                    role.getId(),
                    role.getRoleCode(),
                    role.getRoleName(),
                    role.getDescription(),
                    Boolean.TRUE.equals(role.getIsSystem()),
                    superRole,
                    staffCounts.getOrDefault(role.getId(), 0),
                    superRole ? allCodes : grantsByRole.getOrDefault(role.getId(), List.of())
            ));
        }

        return new PermissionMatrixDto(permissions, roles);
    }

    @Transactional(readOnly = true)
    public MyPermissionsDto getMyPermissions() {
        StaffPrincipal principal = currentPrincipal();
        if (principal == null) {
            return new MyPermissionsDto(null, List.of(), List.of(), false);
        }
        List<String> roleCodes = principal.getRoleCodes();
        return new MyPermissionsDto(
                principal.getStaffId(),
                roleCodes,
                List.copyOf(permissionResolver.resolve(roleCodes)),
                permissionResolver.isSuperRole(roleCodes)
        );
    }

    // ------------------------------------------------------------------
    // Ghi
    // ------------------------------------------------------------------

    @Transactional
    public PermissionMatrixDto updateRolePermissions(Integer roleId, List<String> permissionCodes) {
        RoleJpa role = roleRepo.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy vai trò"));

        if (PermissionResolver.SUPER_ROLE.equals(role.getRoleCode())) {
            throw new IllegalArgumentException(
                    "Vai trò ADMIN luôn có toàn quyền và không cấu hình được. "
                            + "Muốn giới hạn một người thì gán cho họ vai trò khác.");
        }

        Set<String> requested = new LinkedHashSet<>(permissionCodes == null ? List.of() : permissionCodes);
        Set<String> known = permissionResolver.allPermissionCodes();
        requested.removeIf(code -> {
            boolean unknown = !known.contains(code);
            if (unknown) log.warn("Bỏ qua mã quyền không có trong danh mục: {}", code);
            return unknown;
        });

        guardAgainstSelfLockout(role, requested);

        rolePermissionRepo.deleteByRoleId(roleId);
        rolePermissionRepo.flush();
        rolePermissionRepo.saveAll(requested.stream()
                .map(code -> new RolePermissionJpa(roleId, code))
                .toList());

        permissionResolver.invalidate();
        log.info("Đã cập nhật quyền của vai trò {} ({} quyền)", role.getRoleCode(), requested.size());
        return getMatrix();
    }

    @Transactional
    public PermissionMatrixDto resetRoleToDefault(Integer roleId) {
        RoleJpa role = roleRepo.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy vai trò"));

        List<String> defaults = rolePermissionDefaultRepo.findPermissionCodesByRoleId(roleId);
        if (defaults.isEmpty() && !Boolean.TRUE.equals(role.getIsSystem())) {
            throw new IllegalArgumentException(
                    "Vai trò này do người dùng tự tạo nên không có bộ quyền mặc định để khôi phục.");
        }
        return updateRolePermissions(roleId, defaults);
    }

    @Transactional
    public PermissionMatrixDto createRole(String roleCode, String roleName, String description) {
        String code = roleCode == null ? "" : roleCode.trim().toUpperCase();
        if (!ROLE_CODE_PATTERN.matcher(code).matches()) {
            throw new IllegalArgumentException(
                    "Mã vai trò chỉ gồm chữ in hoa, số và dấu gạch dưới, bắt đầu bằng chữ cái.");
        }
        if (roleRepo.existsByRoleCode(code)) {
            throw new IllegalArgumentException("Mã vai trò '" + code + "' đã tồn tại.");
        }
        if (roleName == null || roleName.isBlank()) {
            throw new IllegalArgumentException("Tên vai trò không được để trống.");
        }

        RoleJpa role = new RoleJpa();
        role.setRoleCode(code);
        role.setRoleName(roleName.trim());
        role.setDescription(description);
        role.setIsSystem(Boolean.FALSE);
        roleRepo.save(role);

        // Vai trò mới bắt đầu với bộ quyền rỗng: an toàn hơn là chép của vai trò
        // khác rồi người tạo quên rà lại.
        permissionResolver.invalidate();
        return getMatrix();
    }

    @Transactional
    public PermissionMatrixDto updateRole(Integer roleId, String roleName, String description) {
        RoleJpa role = roleRepo.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy vai trò"));
        if (roleName != null && !roleName.isBlank()) {
            role.setRoleName(roleName.trim());
        }
        role.setDescription(description);
        roleRepo.save(role);
        return getMatrix();
    }

    @Transactional
    public PermissionMatrixDto deleteRole(Integer roleId) {
        RoleJpa role = roleRepo.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy vai trò"));

        if (Boolean.TRUE.equals(role.getIsSystem())) {
            throw new IllegalArgumentException(
                    "'" + role.getRoleName() + "' là vai trò gốc của hệ thống, không xoá được. "
                            + "Muốn thu hẹp thì bỏ bớt quyền của nó.");
        }

        Map<Integer, Integer> staffCounts = new HashMap<>();
        for (Object[] row : roleRepo.countStaffPerRole()) {
            staffCounts.put(((Number) row[0]).intValue(), ((Number) row[1]).intValue());
        }
        int assigned = staffCounts.getOrDefault(roleId, 0);
        if (assigned > 0) {
            throw new IllegalArgumentException(
                    "Còn " + assigned + " nhân viên đang giữ vai trò này. Gỡ vai trò khỏi họ trước đã.");
        }

        rolePermissionRepo.deleteByRoleId(roleId);
        roleRepo.delete(role);
        permissionResolver.invalidate();
        return getMatrix();
    }

    // ------------------------------------------------------------------

    /**
     * Chặn tình huống người đang cấu hình tự gỡ mất quyền vào chính màn cấu hình
     * và không còn ai khác sửa được ngoài ADMIN. ADMIN thì luôn vào được nên
     * không cần chặn.
     */
    private void guardAgainstSelfLockout(RoleJpa role, Set<String> requested) {
        StaffPrincipal principal = currentPrincipal();
        if (principal == null) return;

        List<String> myRoles = principal.getRoleCodes();
        if (permissionResolver.isSuperRole(myRoles)) return;
        if (!myRoles.contains(role.getRoleCode())) return;

        boolean losingEdit = !requested.contains(PermissionCodes.ROLE_PERMISSION_EDIT);
        if (losingEdit) {
            throw new IllegalArgumentException(
                    "Bạn đang dùng chính vai trò này. Bỏ quyền 'Vai trò & phân quyền — Sửa' "
                            + "sẽ khiến bạn không vào lại được màn này. Nhờ ADMIN đổi giúp nếu thực sự cần.");
        }
    }

    private StaffPrincipal currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) return null;
        Object principal = authentication.getPrincipal();
        return principal instanceof StaffPrincipal staffPrincipal ? staffPrincipal : null;
    }

    private PermissionDto toDto(PermissionJpa entity) {
        return new PermissionDto(
                entity.getCode(),
                entity.getModuleCode(),
                entity.getModuleLabel(),
                entity.getGroupLabel(),
                entity.getActionCode(),
                entity.getLabel(),
                entity.getDescription(),
                entity.getSortOrder()
        );
    }
}
