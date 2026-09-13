package com.g42.platform.gms.warehouse.api.controller.serial;

import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.warehouse.api.dto.serial.ItemSerialDto;
import com.g42.platform.gms.warehouse.api.dto.serial.RegisterSerialsRequest;
import com.g42.platform.gms.warehouse.app.service.serial.ItemSerialService;
import com.g42.platform.gms.warehouse.domain.enums.SerialStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/warehouse/serials")
@RequiredArgsConstructor
public class ItemSerialController {

    private final ItemSerialService itemSerialService;

    /** Serial của một sản phẩm, lọc theo kho và trạng thái (mặc định mọi trạng thái). */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ItemSerialDto>>> list(
            @RequestParam Integer itemId,
            @RequestParam(required = false) Integer warehouseId,
            @RequestParam(required = false) List<SerialStatus> status) {
        return ResponseEntity.ok(ApiResponses.success(itemSerialService.list(itemId, warehouseId, status)));
    }

    /** Serial đã bán ở một phiếu xuất. */
    @GetMapping("/by-issue/{issueId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ItemSerialDto>>> listByIssue(@PathVariable Integer issueId) {
        return ResponseEntity.ok(ApiResponses.success(itemSerialService.listByIssue(issueId)));
    }

    /** Khai báo serial cho hàng đã có trong một lô (tồn từ trước khi bật theo dõi serial). */
    @PostMapping("/register")
    @PreAuthorize("hasAnyRole('WAREHOUSE_KEEPER','WAREHOUSE_MANAGER','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<List<ItemSerialDto>>> register(
            @Valid @RequestBody RegisterSerialsRequest request,
            @AuthenticationPrincipal StaffPrincipal principal) {
        Integer staffId = principal != null ? principal.getStaffId() : null;
        return ResponseEntity.ok(ApiResponses.success(itemSerialService.registerForLot(request, staffId)));
    }
}
