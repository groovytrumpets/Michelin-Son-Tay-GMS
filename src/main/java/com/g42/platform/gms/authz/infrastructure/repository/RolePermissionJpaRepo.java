package com.g42.platform.gms.authz.infrastructure.repository;

import com.g42.platform.gms.authz.infrastructure.entity.RolePermissionJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RolePermissionJpaRepo extends JpaRepository<RolePermissionJpa, RolePermissionJpa.RolePermissionId> {

    List<RolePermissionJpa> findByRoleId(Integer roleId);

    @Modifying
    void deleteByRoleId(Integer roleId);

    /**
     * Nạp cả bảng một lần dưới dạng (role_code, permission_code) để
     * {@code PermissionResolver} dựng bản đồ trong bộ nhớ. Rẻ hơn nhiều so với
     * truy vấn theo từng vai trò ở mỗi request.
     */
    @Query("""
            SELECT r.roleCode, rp.permissionCode
            FROM RolePermissionJpa rp
            JOIN RoleJpa r ON r.id = rp.roleId
            """)
    List<Object[]> findAllRoleCodeAndPermissionCode();

    @Query("SELECT rp.permissionCode FROM RolePermissionJpa rp WHERE rp.roleId = :roleId")
    List<String> findPermissionCodesByRoleId(@Param("roleId") Integer roleId);
}
