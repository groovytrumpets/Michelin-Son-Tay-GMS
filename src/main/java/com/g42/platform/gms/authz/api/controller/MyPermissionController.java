package com.g42.platform.gms.authz.api.controller;

import com.g42.platform.gms.authz.api.dto.MyPermissionsDto;
import com.g42.platform.gms.authz.application.RolePermissionService;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * FE gọi endpoint này mỗi lần nạp app để biết được phép thấy những gì.
 *
 * <p>Cố ý KHÔNG nhét danh sách quyền vào JWT: quyền nằm trong token thì admin
 * sửa xong người dùng vẫn giữ quyền cũ tới lúc token hết hạn. Đọc theo request
 * thì sửa là ăn ngay.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/staff/me")
public class MyPermissionController {

    private final RolePermissionService rolePermissionService;

    @GetMapping("permissions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<MyPermissionsDto>> getMyPermissions() {
        return ResponseEntity.ok(ApiResponses.success(rolePermissionService.getMyPermissions()));
    }
}
