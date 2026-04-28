package com.g42.platform.gms.warehouse.api.controller.allocation;

import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.warehouse.api.dto.response.StockIssueResponse;
import com.g42.platform.gms.warehouse.app.service.allocation.StockAllocationService;
import com.g42.platform.gms.warehouse.app.service.dto.StockShortageInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/warehouse/allocations")
@RequiredArgsConstructor
public class StockAllocationController {

    private final StockAllocationService stockAllocationService;

    @PostMapping("/reserve")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<StockShortageInfo>>> reserve(
            @RequestParam Integer estimateId,
            @AuthenticationPrincipal StaffPrincipal principal) {
        // TEMP DISABLED: Estimate module is current owner of reserve/update reserved_quantity.
        // Keep endpoint blocked to avoid accidental double-reserve from warehouse side.
        // Old logic (keep for future re-enable):
        // return ResponseEntity.ok(ApiResponses.success(
        //         stockAllocationService.reserve(estimateId, principal.getStaffId())));
        throw new ResponseStatusException(HttpStatus.CONFLICT,
            "Warehouse reserve endpoint is temporarily disabled. Use estimate allocation flow.");
    }

    @PostMapping("/{ticketId}/request-issue")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<StockIssueResponse>>> requestIssue(
            @PathVariable Integer ticketId,
            @AuthenticationPrincipal StaffPrincipal principal) {
        return ResponseEntity.ok(ApiResponses.success(
                stockAllocationService.requestIssueDraft(ticketId, principal.getStaffId())));
    }

    @PostMapping("/{ticketId}/commit")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<StockIssueResponse>>> commitAlias(
            @PathVariable Integer ticketId,
            @AuthenticationPrincipal StaffPrincipal principal) {
        return ResponseEntity.ok(ApiResponses.success(
                stockAllocationService.requestIssueDraft(ticketId, principal.getStaffId())));
    }

    @PostMapping("/{ticketId}/release")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> release(
            @PathVariable Integer ticketId,
            @AuthenticationPrincipal StaffPrincipal principal) {
        stockAllocationService.release(ticketId, principal.getStaffId());
        return ResponseEntity.ok(ApiResponses.success(null));
    }
}
