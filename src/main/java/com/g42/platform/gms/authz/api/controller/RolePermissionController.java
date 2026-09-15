package com.g42.platform.gms.authz.api.controller;

import com.g42.platform.gms.authz.PermissionCodes;
import com.g42.platform.gms.authz.api.dto.PermissionMatrixDto;
import com.g42.platform.gms.authz.api.dto.RoleUpsertRequest;
import com.g42.platform.gms.authz.api.dto.UpdateRolePermissionsRequest;
import com.g42.platform.gms.authz.application.RolePermissionService;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.systemlog.annotation.Auditable;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Màn /role-permission-config. Mọi thay đổi ở đây đều được ghi nhật ký hệ thống
 * — đổi quyền là loại thay đổi mà sau này hay phải truy "ai bật cái này".
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/authz")
public class RolePermissionController {

    private final RolePermissionService rolePermissionService;

    @GetMapping("matrix")
    @PreAuthorize("hasAuthority('" + PermissionCodes.ROLE_PERMISSION_VIEW + "')")
    public ResponseEntity<ApiResponse<PermissionMatrixDto>> getMatrix() {
        return ResponseEntity.ok(ApiResponses.success(rolePermissionService.getMatrix()));
    }

    @PutMapping("roles/{roleId}/permissions")
    @PreAuthorize("hasAuthority('" + PermissionCodes.ROLE_PERMISSION_EDIT + "')")
    @Auditable(action = "PERMISSION", module = "ROLE_PERMISSION", severity = "CRITICAL",
            description = "Cập nhật bộ quyền của vai trò", targetType = "ROLE")
    public ResponseEntity<ApiResponse<PermissionMatrixDto>> updatePermissions(
            @PathVariable Integer roleId,
            @RequestBody UpdateRolePermissionsRequest request) {
        return ResponseEntity.ok(ApiResponses.success(
                rolePermissionService.updateRolePermissions(roleId, request.getPermissionCodes()),
                "Đã cập nhật phân quyền"));
    }

    @PostMapping("roles/{roleId}/reset-default")
    @PreAuthorize("hasAuthority('" + PermissionCodes.ROLE_PERMISSION_EDIT + "')")
    @Auditable(action = "PERMISSION", module = "ROLE_PERMISSION", severity = "WARNING",
            description = "Khôi phục bộ quyền mặc định của vai trò", targetType = "ROLE")
    public ResponseEntity<ApiResponse<PermissionMatrixDto>> resetToDefault(@PathVariable Integer roleId) {
        return ResponseEntity.ok(ApiResponses.success(
                rolePermissionService.resetRoleToDefault(roleId),
                "Đã khôi phục bộ quyền mặc định"));
    }

    @PostMapping("roles")
    @PreAuthorize("hasAuthority('" + PermissionCodes.ROLE_PERMISSION_EDIT + "')")
    @Auditable(action = "CREATE", module = "ROLE_PERMISSION", severity = "WARNING",
            description = "Tạo vai trò mới", targetType = "ROLE")
    public ResponseEntity<ApiResponse<PermissionMatrixDto>> createRole(@RequestBody RoleUpsertRequest request) {
        return ResponseEntity.ok(ApiResponses.success(
                rolePermissionService.createRole(request.getRoleCode(), request.getRoleName(), request.getDescription()),
                "Đã tạo vai trò"));
    }

    @PutMapping("roles/{roleId}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.ROLE_PERMISSION_EDIT + "')")
    @Auditable(action = "UPDATE", module = "ROLE_PERMISSION",
            description = "Sửa thông tin vai trò", targetType = "ROLE")
    public ResponseEntity<ApiResponse<PermissionMatrixDto>> updateRole(
            @PathVariable Integer roleId,
            @RequestBody RoleUpsertRequest request) {
        return ResponseEntity.ok(ApiResponses.success(
                rolePermissionService.updateRole(roleId, request.getRoleName(), request.getDescription()),
                "Đã cập nhật vai trò"));
    }

    @DeleteMapping("roles/{roleId}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.ROLE_PERMISSION_EDIT + "')")
    @Auditable(action = "DELETE", module = "ROLE_PERMISSION", severity = "CRITICAL",
            description = "Xoá vai trò", targetType = "ROLE")
    public ResponseEntity<ApiResponse<PermissionMatrixDto>> deleteRole(@PathVariable Integer roleId) {
        return ResponseEntity.ok(ApiResponses.success(
                rolePermissionService.deleteRole(roleId),
                "Đã xoá vai trò"));
    }
}
