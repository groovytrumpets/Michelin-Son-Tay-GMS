package com.g42.platform.gms.authz.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoleWithPermissionsDto {
    private Integer roleId;
    private String roleCode;
    private String roleName;
    private String description;

    /** Vai trò gốc — không cho đổi mã hay xoá vì code nghiệp vụ tra theo mã này. */
    private boolean system;

    /** ADMIN: toàn quyền suy ra trong code, ma trận hiện xám và không sửa được. */
    private boolean superRole;

    private Integer staffCount;

    private List<String> permissionCodes;
}
