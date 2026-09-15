package com.g42.platform.gms.staff.profile.api.controller;

import com.g42.platform.gms.auth.entity.StaffProfile;
import com.g42.platform.gms.authz.PermissionCodes;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.staff.profile.api.dto.RoleDto;
import com.g42.platform.gms.staff.profile.api.dto.StaffCreateDto;
import com.g42.platform.gms.staff.profile.api.dto.StaffProfileDto;
import com.g42.platform.gms.staff.profile.api.dto.StaffUpdateDto;
import com.g42.platform.gms.staff.profile.app.service.StaffService;
import com.g42.platform.gms.systemlog.annotation.Auditable;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/admin/staff/")
public class StaffController {
    @Autowired
    StaffService staffService;
    @GetMapping("all-staff")
    @PreAuthorize("hasAuthority('" + PermissionCodes.STAFF_VIEW + "')")
    public ResponseEntity<ApiResponse<Page<StaffProfileDto>>> getAllCustomerProfile(@RequestParam(defaultValue = "0") int page,
                                                                                    @RequestParam(defaultValue = "10") int size,
                                                                                    @RequestParam(required = false) Boolean isActive,
                                                                                    @RequestParam(required = false) String search,
                                                                                    @RequestParam(required = false) List<Integer> roleIds,
                                                                                    @RequestParam(required = false) String status) {
        return ResponseEntity.ok(ApiResponses.success(staffService.getListOfAllStaffProfile(page, size, isActive, search, roleIds, status)));
    }
    @GetMapping("{staff-Id}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.STAFF_VIEW + "')")
    public ResponseEntity<ApiResponse<StaffProfileDto>> getStaffProfile(@PathVariable("staff-Id") Integer staffId) {
        return ResponseEntity.ok(ApiResponses.success(staffService.getStaffProfileById(staffId)));
    }
    // Cố ý chỉ cần đăng nhập: danh sách vai trò còn dùng để hiển thị nhãn ở
    // nhiều màn khác (điều phối phiếu, xếp ca), không riêng màn quản lý nhân sự.
    @GetMapping("all-roles")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<RoleDto>>> getAllRoles() {
        return ResponseEntity.ok(ApiResponses.success(staffService.getListOfRoles()));
    }
    @PostMapping("create")
    @PreAuthorize("hasAuthority('" + PermissionCodes.STAFF_CREATE + "')")
    @Auditable(action = "CREATE", module = "STAFF", description = "Tạo hồ sơ nhân viên", targetType = "STAFF")
    public ResponseEntity<ApiResponse<StaffProfileDto>> createStaffProfile(@RequestBody StaffCreateDto staffProfileDto) {
        return ResponseEntity.ok(ApiResponses.success(staffService.createStaff(staffProfileDto)));
    }
    @PutMapping("{staffId}/update")
    @PreAuthorize("hasAuthority('" + PermissionCodes.STAFF_EDIT + "')")
    @Auditable(action = "UPDATE", module = "STAFF", description = "Cập nhật hồ sơ nhân viên", targetType = "STAFF")
    public ResponseEntity<ApiResponse<StaffProfileDto>> updateStaffProfile(@PathVariable Integer staffId,
                                                                           @RequestBody StaffUpdateDto staffProfileDto) {
        return ResponseEntity.ok(ApiResponses.success(staffService.updateStaff(staffId, staffProfileDto)));
    }
    @PutMapping("{staffId}/delete")
    @PreAuthorize("hasAuthority('" + PermissionCodes.STAFF_DELETE + "')")
    @Auditable(action = "DELETE", module = "STAFF", severity = "CRITICAL", description = "Xóa hồ sơ nhân viên", targetType = "STAFF")
    public ResponseEntity<ApiResponse<StaffProfileDto>> deleteStaffProfile(@PathVariable Integer staffId) {
        return ResponseEntity.ok(ApiResponses.success(staffService.deleteStaff(staffId)));
    }
    @PutMapping("{staffId}/lock")
    @PreAuthorize("hasAuthority('" + PermissionCodes.STAFF_EDIT + "')")
    @Auditable(action = "PERMISSION", module = "STAFF", severity = "WARNING", description = "Khóa tài khoản nhân viên", targetType = "STAFF")
    public ResponseEntity<ApiResponse<StaffProfileDto>> lockStaffProfile(@PathVariable Integer staffId) {
        return ResponseEntity.ok(ApiResponses.success(staffService.lockStaff(staffId)));
    }
}
