package com.g42.platform.gms.authz.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/** Toàn bộ dữ liệu màn /role-permission-config cần để vẽ ma trận. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PermissionMatrixDto {
    private List<PermissionDto> permissions;
    private List<RoleWithPermissionsDto> roles;
}
