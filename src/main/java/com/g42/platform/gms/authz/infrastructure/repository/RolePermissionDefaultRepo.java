package com.g42.platform.gms.authz.infrastructure.repository;

import com.g42.platform.gms.authz.infrastructure.entity.RolePermissionJpa;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Chỉ đọc bảng role_permission_default — ảnh chụp bộ quyền mặc định lúc cài đặt,
 * phục vụ nút "Khôi phục mặc định". Không khai báo entity riêng vì bảng này
 * không bao giờ được ghi lúc chạy.
 */
public interface RolePermissionDefaultRepo extends Repository<RolePermissionJpa, String> {

    @Query(value = "SELECT permission_code FROM role_permission_default WHERE role_id = :roleId",
            nativeQuery = true)
    List<String> findPermissionCodesByRoleId(@Param("roleId") Integer roleId);
}
