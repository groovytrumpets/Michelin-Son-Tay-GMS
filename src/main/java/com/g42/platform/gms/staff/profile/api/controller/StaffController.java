package com.g42.platform.gms.staff.profile.api.controller;

import com.g42.platform.gms.auth.entity.StaffProfile;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.staff.profile.api.dto.StaffProfileDto;
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
                                                                                    @RequestParam(required = false) List<Integer> roleIds) {
        return ResponseEntity.ok(ApiResponses.success(staffService.getListOfAllStaffProfile(page, size, isActive, search, roleIds)));
    }
    @GetMapping("{staff-Id}")
    public ResponseEntity<ApiResponse<StaffProfileDto>> getStaffProfile(@PathVariable("staff-Id") Integer staffId) {
        return ResponseEntity.ok(ApiResponses.success(staffService.getStaffProfileById(staffId)));
    }
}
