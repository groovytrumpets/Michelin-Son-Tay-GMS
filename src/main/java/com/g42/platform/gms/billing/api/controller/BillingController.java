package com.g42.platform.gms.billing.api.controller;

import com.g42.platform.gms.billing.api.dto.BillEstimateDto;
import com.g42.platform.gms.billing.api.dto.PaymentTransactionDto;
import com.g42.platform.gms.billing.api.dto.ServiceBillDto;
import com.g42.platform.gms.billing.app.service.BillingService;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment")
public class BillingController {
    @Autowired
    private BillingService billingService;
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<ServiceBillDto>> createBilling(@RequestBody ServiceBillDto serviceBillDto) {
        ServiceBillDto promotion = billingService.createNewBilling(serviceBillDto);
        return ResponseEntity.ok(ApiResponses.success(promotion));
    }
    @PostMapping("/create/payment")
    public ResponseEntity<ApiResponse<PaymentTransactionDto>> createPayment(@RequestBody PaymentTransactionDto dto) {
        PaymentTransactionDto savedDto = billingService.createNewPayment(dto);
        return ResponseEntity.ok(ApiResponses.success(savedDto));
    }
    @GetMapping("/{serviceTicketId}")
    public ResponseEntity<ApiResponse<BillEstimateDto>> getBillWithEstimate(@PathVariable Integer serviceTicketId) {
        BillEstimateDto savedDto = billingService.getBillWithEstimate(serviceTicketId);
        return ResponseEntity.ok(ApiResponses.success(savedDto));
    }


}
