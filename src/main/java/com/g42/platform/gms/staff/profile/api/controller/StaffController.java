package com.g42.platform.gms.staff.profile.api.controller;

import com.g42.platform.gms.auth.entity.StaffProfile;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.staff.profile.api.dto.RoleDto;
import com.g42.platform.gms.staff.profile.api.dto.StaffCreateDto;
import com.g42.platform.gms.staff.profile.api.dto.StaffProfileDto;
import com.g42.platform.gms.staff.profile.api.dto.StaffUpdateDto;
import com.g42.platform.gms.staff.profile.app.service.StaffService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<ApiResponse<Page<StaffProfileDto>>> getAllCustomerProfile(@RequestParam(defaultValue = "0") int page,
                                                                                    @RequestParam(defaultValue = "10") int size,
                                                                                    @RequestParam(required = false) Boolean isActive,
                                                                                    @RequestParam(required = false) String search,
                                                                                    @RequestParam(required = false) List<Integer> roleIds,
                                                                                    @RequestParam(required = false) String status) {
        return ResponseEntity.ok(ApiResponses.success(staffService.getListOfAllStaffProfile(page, size, isActive, search, roleIds, status)));
    }
    @GetMapping("{staff-Id}")
    public ResponseEntity<ApiResponse<StaffProfileDto>> getStaffProfile(@PathVariable("staff-Id") Integer staffId) {
        return ResponseEntity.ok(ApiResponses.success(staffService.getStaffProfileById(staffId)));
    }
    @GetMapping("all-roles")
    public ResponseEntity<ApiResponse<List<RoleDto>>> getAllRoles() {
        return ResponseEntity.ok(ApiResponses.success(staffService.getListOfRoles()));
    }
    @PostMapping("create")
    public ResponseEntity<ApiResponse<StaffProfileDto>> createStaffProfile(@RequestBody StaffCreateDto staffProfileDto) {
        return ResponseEntity.ok(ApiResponses.success(staffService.createStaff(staffProfileDto)));
    }
    @PutMapping("{staffId}/update")
    public ResponseEntity<ApiResponse<StaffProfileDto>> updateStaffProfile(@PathVariable Integer staffId,
                                                                           @RequestBody StaffUpdateDto staffProfileDto) {
        return ResponseEntity.ok(ApiResponses.success(staffService.updateStaff(staffId, staffProfileDto)));
    }
}
