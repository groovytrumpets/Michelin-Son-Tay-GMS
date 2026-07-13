package com.g42.platform.gms.service_ticket_management.api.controller;

import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.service_ticket_management.api.dto.parts_sale.PartsSaleCreateDto;
import com.g42.platform.gms.service_ticket_management.api.dto.parts_sale.PartsSaleTicketDto;
import com.g42.platform.gms.service_ticket_management.application.service.PartsSaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API bán linh kiện cho đại lý/garage khác — phiếu dịch vụ rút gọn 1 bước.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/service-ticket/parts-sale")
public class PartsSaleController {

    private final PartsSaleService partsSaleService;

    @PostMapping
    public ResponseEntity<ApiResponse<PartsSaleTicketDto>> createPartsSale(
            @RequestBody PartsSaleCreateDto request,
            @AuthenticationPrincipal StaffPrincipal principal) {
        PartsSaleTicketDto result = partsSaleService.createPartsSale(request, principal.getStaffId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponses.success(result));
    }
}
