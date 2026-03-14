package com.g42.platform.gms.staff.profile.api.controller;

import com.g42.platform.gms.auth.entity.StaffProfile;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.staff.profile.app.service.StaffService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@AllArgsConstructor
@RequestMapping("/api/admin/staff/")
public class StaffController {
    @Autowired
    StaffService staffService;
    @GetMapping("all-staff")
    public ResponseEntity<ApiResponse<Page<StaffProfile>>> getAllCustomerProfile(@RequestParam(defaultValue = "0") int page,
                                                                                 @RequestParam(defaultValue = "10") int size,
                                                                                 @RequestParam(required = false) LocalDate date,
                                                                                 @RequestParam(required = false) String isGuest,
                                                                                 @RequestParam(required = false) String search) {
        return ResponseEntity.ok(ApiResponses.success(staffService.getListOfAllStaffProfile(page, size, date, isGuest, search)));
    }
}
