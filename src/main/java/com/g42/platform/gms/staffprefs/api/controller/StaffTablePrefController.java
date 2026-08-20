package com.g42.platform.gms.staffprefs.api.controller;

import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.staffprefs.api.dto.StaffTablePrefRequest;
import com.g42.platform.gms.staffprefs.application.service.StaffTablePrefService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Cấp 2 (đồng bộ đa thiết bị) cho cấu hình bảng của nhân viên đang đăng nhập —
 * cột ẩn/hiện, thứ tự, độ rộng. Khớp contract FE: src/services/tablePrefsService.js.
 * Đây là tính năng phụ trợ (best-effort) — FE tự bỏ qua khi lỗi, không chặn UI.
 */
@RestController
@RequestMapping("/api/staff/table-prefs")
public class StaffTablePrefController {

    @Autowired
    private StaffTablePrefService service;

    @GetMapping("/{tableKey}")
    public ResponseEntity<ApiResponse<Object>> get(
            @AuthenticationPrincipal StaffPrincipal staffPrincipal,
            @PathVariable String tableKey) {
        return ResponseEntity.ok(ApiResponses.success(
                service.get(staffPrincipal.getStaffId(), tableKey)));
    }

    @PutMapping("/{tableKey}")
    public ResponseEntity<ApiResponse<Boolean>> save(
            @AuthenticationPrincipal StaffPrincipal staffPrincipal,
            @PathVariable String tableKey,
            @RequestBody StaffTablePrefRequest request) {
        service.save(staffPrincipal.getStaffId(), tableKey, request.getPrefs());
        return ResponseEntity.ok(ApiResponses.success(true));
    }
}
