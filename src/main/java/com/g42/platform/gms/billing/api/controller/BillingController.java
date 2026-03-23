package com.g42.platform.gms.billing.api.controller;

import com.g42.platform.gms.billing.api.dto.ServiceBillDto;
import com.g42.platform.gms.billing.app.service.BillingService;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

}
