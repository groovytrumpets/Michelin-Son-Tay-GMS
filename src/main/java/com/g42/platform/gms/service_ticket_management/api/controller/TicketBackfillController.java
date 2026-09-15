package com.g42.platform.gms.service_ticket_management.api.controller;

import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.service_ticket_management.api.dto.backfill.BackfillCreateRequest;
import com.g42.platform.gms.service_ticket_management.api.dto.backfill.BackfillDuplicateDto;
import com.g42.platform.gms.service_ticket_management.api.dto.backfill.BackfillParentDto;
import com.g42.platform.gms.service_ticket_management.api.dto.backfill.BackfillPolicyDto;
import com.g42.platform.gms.service_ticket_management.api.dto.backfill.BackfillReviewRequest;
import com.g42.platform.gms.service_ticket_management.api.dto.backfill.BackfillStaffStatDto;
import com.g42.platform.gms.service_ticket_management.api.dto.backfill.BackfillTicketDto;
import com.g42.platform.gms.service_ticket_management.application.service.TicketBackfillService;
import com.g42.platform.gms.systemlog.annotation.Auditable;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Nhập bù phiếu của ngày trước (phiếu bị miss / thiếu dòng) — xem TicketBackfillService.
 *
 * Nhân viên: POST (nhập bù, giữ hàng, chờ duyệt), GET danh sách phiếu mình nhập.
 * Quản lý/Admin: xem tất cả, thống kê theo nhân viên, duyệt (xuất kho + ghi thu) hoặc từ chối (nhả hàng).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/service-ticket/backfill")
public class TicketBackfillController {

    private final TicketBackfillService backfillService;

    @PostMapping
    @Auditable(action = "CREATE", module = "SERVICE_TICKET", severity = "WARNING",
            description = "Nhập bù phiếu ngày trước (chờ duyệt)", targetType = "SERVICE_TICKET")
    public ResponseEntity<ApiResponse<BackfillTicketDto>> create(
            @RequestBody BackfillCreateRequest request,
            @AuthenticationPrincipal StaffPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponses.success(backfillService.create(request, principal)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<BackfillTicketDto>>> list(
            @RequestParam(required = false) String reviewStatus,
            @RequestParam(required = false) Integer createdBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal StaffPrincipal principal) {
        return ResponseEntity.ok(ApiResponses.success(
                backfillService.list(reviewStatus, createdBy, from, to, page, size, principal)));
    }

    @GetMapping("/policy")
    public ResponseEntity<ApiResponse<BackfillPolicyDto>> policy(@AuthenticationPrincipal StaffPrincipal principal) {
        return ResponseEntity.ok(ApiResponses.success(backfillService.policy(principal)));
    }

    @GetMapping("/parent")
    public ResponseEntity<ApiResponse<BackfillParentDto>> findParent(
            @RequestParam String code,
            @AuthenticationPrincipal StaffPrincipal principal) {
        return ResponseEntity.ok(ApiResponses.success(backfillService.findParent(code, principal)));
    }

    @GetMapping("/same-day")
    public ResponseEntity<ApiResponse<List<BackfillDuplicateDto>>> sameDay(
            @RequestParam Integer customerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal StaffPrincipal principal) {
        return ResponseEntity.ok(ApiResponses.success(backfillService.sameDayTickets(customerId, date, principal)));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<BackfillStaffStatDto>>> stats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal StaffPrincipal principal) {
        return ResponseEntity.ok(ApiResponses.success(backfillService.stats(from, to, principal)));
    }

    @GetMapping("/{serviceTicketId}")
    public ResponseEntity<ApiResponse<BackfillTicketDto>> detail(
            @PathVariable Integer serviceTicketId,
            @AuthenticationPrincipal StaffPrincipal principal) {
        return ResponseEntity.ok(ApiResponses.success(backfillService.detail(serviceTicketId, principal)));
    }

    @PostMapping("/{serviceTicketId}/approve")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Auditable(action = "UPDATE", module = "SERVICE_TICKET", severity = "CRITICAL",
            description = "Duyệt phiếu nhập bù (xuất kho + ghi thanh toán)", targetType = "SERVICE_TICKET")
    public ResponseEntity<ApiResponse<BackfillTicketDto>> approve(
            @PathVariable Integer serviceTicketId,
            @RequestBody(required = false) BackfillReviewRequest request,
            @AuthenticationPrincipal StaffPrincipal principal) {
        String note = request == null ? null : request.getNote();
        return ResponseEntity.ok(ApiResponses.success(backfillService.approve(serviceTicketId, note, principal)));
    }

    @PostMapping("/{serviceTicketId}/reject")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Auditable(action = "UPDATE", module = "SERVICE_TICKET", severity = "WARNING",
            description = "Từ chối phiếu nhập bù (nhả hàng, huỷ phiếu)", targetType = "SERVICE_TICKET")
    public ResponseEntity<ApiResponse<BackfillTicketDto>> reject(
            @PathVariable Integer serviceTicketId,
            @RequestBody BackfillReviewRequest request,
            @AuthenticationPrincipal StaffPrincipal principal) {
        return ResponseEntity.ok(ApiResponses.success(
                backfillService.reject(serviceTicketId, request == null ? null : request.getNote(), principal)));
    }
}
