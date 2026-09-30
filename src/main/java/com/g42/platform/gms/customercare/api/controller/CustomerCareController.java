package com.g42.platform.gms.customercare.api.controller;

import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.authz.PermissionCodes;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.customercare.api.dto.CareDtos.*;
import com.g42.platform.gms.customercare.application.service.CustomerCareService;
import com.g42.platform.gms.customercare.application.service.CustomerCareService.ListQuery;
import com.g42.platform.gms.systemlog.annotation.Auditable;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Gọi chăm sóc khách hàng (/customer-care-calls).
 * Khớp contract FE: src/services/customerCareService.js.
 */
@RestController
@RequestMapping("/api/admin/customer-care")
@RequiredArgsConstructor
public class CustomerCareController {

    private final CustomerCareService service;

    @GetMapping("/outcomes")
    @PreAuthorize("hasAuthority('" + PermissionCodes.CUSTOMER_CARE_VIEW + "')")
    public ResponseEntity<ApiResponse<List<OutcomeOption>>> outcomes() {
        return ResponseEntity.ok(ApiResponses.success(service.outcomes()));
    }

    /**
     * Danh sách cần gọi.
     *
     * @param tab    DUE (mặc định: đến lượt gọi) | ALL | một mã CareStatus
     * @param source HAS_LEGACY | HAS_SYSTEM | LEGACY_ONLY
     * @param flag   DUPLICATE | INVALID_PHONE | RETURNED | HAS_CARE_NOTE
     * @param sort   PRIORITY (mặc định) | SPEND_DESC | LAST_VISIT_DESC | LAST_VISIT_ASC | LAST_CALL_DESC
     *               | FOLLOW_UP_ASC | NAME_ASC
     */
    @GetMapping("/customers")
    @PreAuthorize("hasAuthority('" + PermissionCodes.CUSTOMER_CARE_VIEW + "')")
    public ResponseEntity<ApiResponse<CareListResponse>> list(
            @RequestParam(required = false) String tab,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer minDays,
            @RequestParam(required = false) Integer maxDays,
            @RequestParam(required = false) BigDecimal minSpend,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String flag,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "false") boolean includeNoHistory,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        ListQuery query = new ListQuery(tab, search, minDays, maxDays, minSpend, source, flag, sort,
                includeNoHistory, page, size);
        return ResponseEntity.ok(ApiResponses.success(service.list(query)));
    }

    @GetMapping("/customers/{customerId}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.CUSTOMER_CARE_VIEW + "')")
    public ResponseEntity<ApiResponse<CareCustomerDetail>> detail(@PathVariable Integer customerId) {
        return ResponseEntity.ok(ApiResponses.success(service.detail(customerId)));
    }

    @PostMapping("/customers/{customerId}/calls")
    @PreAuthorize("hasAuthority('" + PermissionCodes.CUSTOMER_CARE_CALL + "')")
    @Auditable(action = "CREATE", module = "CUSTOMER",
            description = "Ghi kết quả gọi chăm sóc khách", targetType = "CUSTOMER")
    public ResponseEntity<ApiResponse<CareCustomerDetail>> logCall(
            @AuthenticationPrincipal StaffPrincipal staff,
            @PathVariable Integer customerId,
            @RequestBody CallRequest request) {
        return ResponseEntity.ok(ApiResponses.success(
                service.logCall(customerId, request, staffId(staff))));
    }

    @DeleteMapping("/calls/{careCallId}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.CUSTOMER_CARE_CALL + "')")
    @Auditable(action = "DELETE", module = "CUSTOMER", severity = "WARNING",
            description = "Xoá cuộc gọi chăm sóc ghi nhầm", targetType = "CUSTOMER")
    public ResponseEntity<ApiResponse<CareCustomerDetail>> deleteCall(@PathVariable Integer careCallId) {
        return ResponseEntity.ok(ApiResponses.success(service.deleteCall(careCallId)));
    }

    @PutMapping("/customers/{customerId}/care-profile")
    @PreAuthorize("hasAuthority('" + PermissionCodes.CUSTOMER_CARE_CALL + "')")
    @Auditable(action = "UPDATE", module = "CUSTOMER",
            description = "Sửa ghi chú chăm sóc khách", targetType = "CUSTOMER")
    public ResponseEntity<ApiResponse<CareCustomerDetail>> saveCareProfile(
            @AuthenticationPrincipal StaffPrincipal staff,
            @PathVariable Integer customerId,
            @RequestBody CareProfileRequest request) {
        return ResponseEntity.ok(ApiResponses.success(
                service.saveCareProfile(customerId, request, staffId(staff))));
    }

    @PutMapping("/customers/{customerId}/do-not-contact")
    @PreAuthorize("hasAuthority('" + PermissionCodes.CUSTOMER_CARE_CALL + "')")
    @Auditable(action = "UPDATE", module = "CUSTOMER", severity = "WARNING",
            description = "Bật/tắt ngừng liên hệ khách", targetType = "CUSTOMER")
    public ResponseEntity<ApiResponse<CareCustomerDetail>> setDoNotContact(
            @PathVariable Integer customerId,
            @RequestBody DoNotContactRequest request) {
        boolean value = request != null && Boolean.TRUE.equals(request.getDoNotContact());
        return ResponseEntity.ok(ApiResponses.success(service.setDoNotContact(customerId, value)));
    }

    private static Integer staffId(StaffPrincipal staff) {
        return staff != null ? staff.getStaffId() : null;
    }
}
