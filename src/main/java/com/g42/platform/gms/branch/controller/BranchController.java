package com.g42.platform.gms.branch.controller;

import com.g42.platform.gms.authz.PermissionCodes;
import com.g42.platform.gms.branch.dto.BranchDto;
import com.g42.platform.gms.branch.service.BranchService;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.systemlog.annotation.Auditable;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Danh mục xưởng.
 *
 * <ul>
 *   <li>{@code GET /api/public/branches} — xưởng đang hoạt động, cho form khách đặt lịch.</li>
 *   <li>{@code GET /api/branches} — như trên, cho nút chọn xưởng trên thanh đầu trang nhân viên.</li>
 *   <li>{@code /api/admin/branches} — màn /branch-config, dùng quyền danh mục (MASTER_DATA_*)
 *       như các màn cấu hình danh mục khác.</li>
 * </ul>
 */
@RestController
@RequiredArgsConstructor
public class BranchController {

    private final BranchService branchService;

    @GetMapping("/api/public/branches")
    public ResponseEntity<ApiResponse<List<BranchDto>>> listPublic() {
        return ResponseEntity.ok(ApiResponses.success(branchService.listActive()));
    }

    @GetMapping("/api/branches")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<BranchDto>>> listActive() {
        return ResponseEntity.ok(ApiResponses.success(branchService.listActive()));
    }

    @GetMapping("/api/admin/branches")
    @PreAuthorize("hasAuthority('" + PermissionCodes.MASTER_DATA_VIEW + "')")
    public ResponseEntity<ApiResponse<List<BranchDto>>> listAll() {
        return ResponseEntity.ok(ApiResponses.success(branchService.listAll()));
    }

    @PostMapping("/api/admin/branches")
    @PreAuthorize("hasAuthority('" + PermissionCodes.MASTER_DATA_EDIT + "')")
    @Auditable(action = "CREATE", module = "BRANCH", description = "Thêm xưởng", targetType = "BRANCH")
    public ResponseEntity<ApiResponse<BranchDto>> create(@RequestBody BranchDto dto) {
        return ResponseEntity.ok(ApiResponses.success(branchService.create(dto)));
    }

    @PutMapping("/api/admin/branches/{branchId}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.MASTER_DATA_EDIT + "')")
    @Auditable(action = "UPDATE", module = "BRANCH", description = "Sửa xưởng", targetType = "BRANCH")
    public ResponseEntity<ApiResponse<BranchDto>> update(@PathVariable Integer branchId, @RequestBody BranchDto dto) {
        return ResponseEntity.ok(ApiResponses.success(branchService.update(branchId, dto)));
    }

    @PutMapping("/api/admin/branches/{branchId}/default")
    @PreAuthorize("hasAuthority('" + PermissionCodes.MASTER_DATA_EDIT + "')")
    @Auditable(action = "UPDATE", module = "BRANCH", description = "Đặt xưởng mặc định", targetType = "BRANCH")
    public ResponseEntity<ApiResponse<BranchDto>> setDefault(@PathVariable Integer branchId) {
        return ResponseEntity.ok(ApiResponses.success(branchService.setDefault(branchId)));
    }

    @DeleteMapping("/api/admin/branches/{branchId}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.MASTER_DATA_EDIT + "')")
    @Auditable(action = "DELETE", module = "BRANCH", description = "Xoá xưởng", targetType = "BRANCH")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Integer branchId) {
        branchService.delete(branchId);
        return ResponseEntity.ok(ApiResponses.success(null));
    }
}
