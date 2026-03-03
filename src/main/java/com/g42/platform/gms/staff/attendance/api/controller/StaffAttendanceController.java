package com.g42.platform.gms.staff.attendance.api.controller;

import com.g42.platform.gms.booking_management.api.dto.confirmed.BookedDetailResponse;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.staff.attendance.api.dto.StaffAttendanceRes;
import com.g42.platform.gms.staff.attendance.application.service.StaffAttendanceService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/staff/attendance")
public class StaffAttendanceController {
    private final StaffAttendanceService staffAttendanceService;

    @GetMapping("/{staffId}")
    public ResponseEntity<ApiResponse<List<StaffAttendanceRes>>> getAllAttendanceByStaffId(@PathVariable String staffId){

        List<StaffAttendanceRes> staffAttendanceRes = staffAttendanceService.getAllAttendanceByStaffId(Integer.parseInt(staffId));
        return ResponseEntity.ok(ApiResponses.success(staffAttendanceRes));
    }
}
