package com.g42.platform.gms.authz.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Vai trò được cấp quyền nào. Đây là phần duy nhất admin sửa được ở
 * /role-permission-config; bảng {@code permission} thì cố định theo code.
 *
 * <p>ADMIN không có dòng nào ở đây — xem {@code PermissionResolver}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@IdClass(RolePermissionJpa.RolePermissionId.class)
@Table(name = "role_permission")
public class RolePermissionJpa {

    @Id
    @Column(name = "role_id", nullable = false)
    private Integer roleId;

    @Id
    @Column(name = "permission_code", nullable = false, length = 64)
    private String permissionCode;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RolePermissionId implements java.io.Serializable {
        private Integer roleId;
        private String permissionCode;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof RolePermissionId other)) return false;
            return java.util.Objects.equals(roleId, other.roleId)
                    && java.util.Objects.equals(permissionCode, other.permissionCode);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(roleId, permissionCode);
        }
    }
}
