package com.g42.platform.gms.customermerge.api.controller;

import com.g42.platform.gms.authz.PermissionCodes;
import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.customermerge.api.dto.CustomerCompareDto;
import com.g42.platform.gms.customermerge.api.dto.CustomerMergeRequest;
import com.g42.platform.gms.customermerge.api.dto.DuplicateGroupDto;
import com.g42.platform.gms.customermerge.api.dto.MergeLogDto;
import com.g42.platform.gms.customermerge.api.dto.MergeResultDto;
import com.g42.platform.gms.customermerge.api.dto.VehicleMergeRequest;
import com.g42.platform.gms.customermerge.application.service.CustomerMergeService;
import com.g42.platform.gms.systemlog.annotation.Auditable;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Gộp hồ sơ khách hàng trùng (màn /customer-merge). Khớp contract FE:
 * src/services/customerMergeService.js.
 *
 * Lễ tân được dùng vì chính lễ tân là người hay lỡ tạo trùng khách và cần tự sửa được.
 */
@RestController
@RequestMapping("/api/admin/customer-merge")
@PreAuthorize("hasAuthority('" + PermissionCodes.CUSTOMER_MERGE + "')")
@RequiredArgsConstructor
public class CustomerMergeController {

    private final CustomerMergeService service;

    /** Các nhóm hồ sơ nghi trùng, gom theo biển số xe. */
    @GetMapping("/duplicates")
    public ResponseEntity<ApiResponse<List<DuplicateGroupDto>>> duplicates() {
        return ResponseEntity.ok(ApiResponses.success(service.listDuplicates()));
    }

    /** So sánh từng trường của 2+ hồ sơ; hồ sơ đầu tiên trong danh sách là hồ sơ dự kiến giữ lại. */
    @GetMapping("/compare")
    public ResponseEntity<ApiResponse<CustomerCompareDto>> compare(@RequestParam List<Integer> customerIds) {
        return ResponseEntity.ok(ApiResponses.success(service.compare(customerIds)));
    }

    @PostMapping
    @Auditable(action = "DELETE", module = "CUSTOMER", severity = "CRITICAL",
            description = "Gộp hồ sơ khách hàng trùng", targetType = "CUSTOMER")
    public ResponseEntity<ApiResponse<MergeResultDto>> merge(@AuthenticationPrincipal StaffPrincipal staff,
                                                             @RequestBody CustomerMergeRequest request) {
        return ResponseEntity.ok(ApiResponses.success(service.merge(request, staffId(staff))));
    }

    /** Khác người nhưng trùng biển số: chỉ gộp các dòng xe về một chủ. */
    @PostMapping("/vehicles")
    @Auditable(action = "UPDATE", module = "CUSTOMER", severity = "WARNING",
            description = "Gộp các dòng xe trùng biển số", targetType = "VEHICLE")
    public ResponseEntity<ApiResponse<MergeResultDto>> mergeVehicles(@AuthenticationPrincipal StaffPrincipal staff,
                                                                     @RequestBody VehicleMergeRequest request) {
        return ResponseEntity.ok(ApiResponses.success(service.mergeVehicles(request, staffId(staff))));
    }

    /** Xác nhận một nhóm gợi ý không phải cùng một khách — không hiện lại nữa. */
    @PostMapping("/dismiss")
    public ResponseEntity<ApiResponse<Void>> dismiss(@AuthenticationPrincipal StaffPrincipal staff,
                                                     @RequestBody Map<String, String> body) {
        service.dismiss(body.get("groupKey"), body.get("note"), staffId(staff));
        return ResponseEntity.ok(ApiResponses.successMessage("Đã bỏ qua nhóm này"));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<MergeLogDto>>> history(@RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(ApiResponses.success(service.history(limit)));
    }

    private static Integer staffId(StaffPrincipal staff) {
        return staff == null ? null : staff.getStaffId();
    }
}
