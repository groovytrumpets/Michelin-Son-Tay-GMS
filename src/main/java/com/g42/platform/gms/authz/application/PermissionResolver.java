package com.g42.platform.gms.authz.application;

import com.g42.platform.gms.authz.infrastructure.repository.PermissionJpaRepo;
import com.g42.platform.gms.authz.infrastructure.repository.RolePermissionJpaRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Đổi danh sách vai trò của một nhân viên thành danh sách mã quyền.
 *
 * <p>StaffJwtFilter nạp lại nhân viên từ DB ở mỗi request, nên nếu resolver này
 * cũng truy vấn DB thì mỗi request phải cõng thêm một lượt đọc bảng nối. Bảng
 * role_permission rất nhỏ và gần như không đổi, nên giữ nguyên nó trong bộ nhớ
 * và chỉ dựng lại khi có người bấm lưu ở màn cấu hình.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionResolver {

    /** Vai trò này luôn có mọi quyền, kể cả quyền được thêm vào sau này. */
    public static final String SUPER_ROLE = "ADMIN";

    private final RolePermissionJpaRepo rolePermissionRepo;
    private final PermissionJpaRepo permissionRepo;

    /** Ảnh chụp bản đồ vai trò → quyền; null nghĩa là cần dựng lại. */
    private volatile Map<String, Set<String>> snapshot;
    private volatile Set<String> allPermissionCodes;

    /**
     * Gộp quyền của tất cả vai trò mà nhân viên đang giữ. Nhân viên kiêm nhiệm
     * nhiều vai trò thì được hợp của các bộ quyền, đúng như cách hasAnyRole cũ
     * hoạt động.
     */
    public Set<String> resolve(Collection<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> normalizedRoles = new HashSet<>();
        for (String roleCode : roleCodes) {
            String normalized = normalize(roleCode);
            if (!normalized.isEmpty()) {
                normalizedRoles.add(normalized);
            }
        }

        // ADMIN cố ý không có dòng nào trong role_permission. Nếu lưu ra bảng thì
        // mỗi lần seed thêm mã quyền mới, ADMIN sẽ thiếu đúng mã đó cho tới khi
        // có ai nhớ ra mà tick lại — kể cả mã ROLE_PERMISSION_EDIT dùng để vào
        // màn cấu hình. Suy ra trong code thì không bao giờ tự khoá mình.
        if (normalizedRoles.contains(SUPER_ROLE)) {
            return allPermissionCodes();
        }

        Map<String, Set<String>> map = snapshot();
        Set<String> granted = new LinkedHashSet<>();
        for (String roleCode : normalizedRoles) {
            Set<String> ofRole = map.get(roleCode);
            if (ofRole != null) {
                granted.addAll(ofRole);
            }
        }
        return granted;
    }

    public boolean isSuperRole(Collection<String> roleCodes) {
        if (roleCodes == null) return false;
        return roleCodes.stream().map(PermissionResolver::normalize).anyMatch(SUPER_ROLE::equals);
    }

    /** Gọi sau mỗi lần ghi role_permission để lần đọc kế tiếp dựng lại bản đồ. */
    public void invalidate() {
        snapshot = null;
        allPermissionCodes = null;
        log.info("Bộ nhớ đệm phân quyền đã được xoá, sẽ nạp lại ở request kế tiếp");
    }

    @Transactional(readOnly = true)
    public Set<String> allPermissionCodes() {
        Set<String> cached = allPermissionCodes;
        if (cached == null) {
            cached = new LinkedHashSet<>(permissionRepo.findAll().stream().map(p -> p.getCode()).toList());
            allPermissionCodes = cached;
        }
        return cached;
    }

    @Transactional(readOnly = true)
    protected Map<String, Set<String>> snapshot() {
        Map<String, Set<String>> cached = snapshot;
        if (cached != null) {
            return cached;
        }

        Map<String, Set<String>> built = new HashMap<>();
        for (Object[] row : rolePermissionRepo.findAllRoleCodeAndPermissionCode()) {
            String roleCode = normalize((String) row[0]);
            String permissionCode = (String) row[1];
            if (roleCode.isEmpty() || permissionCode == null) continue;
            built.computeIfAbsent(roleCode, k -> new LinkedHashSet<>()).add(permissionCode);
        }
        snapshot = built;
        return built;
    }

    /** Chấp nhận cả "ROLE_MANAGER" lẫn "manager" — FE và BE viết mỗi nơi một kiểu. */
    private static String normalize(String value) {
        if (value == null) return "";
        String raw = value.trim().toUpperCase();
        return raw.startsWith("ROLE_") ? raw.substring("ROLE_".length()) : raw;
    }
}
