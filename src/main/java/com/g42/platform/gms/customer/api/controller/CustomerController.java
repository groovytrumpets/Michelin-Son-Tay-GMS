package com.g42.platform.gms.customer.api.controller;

import com.g42.platform.gms.booking_management.api.dto.CustomerDto;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.customer.api.dto.CustomerCreateDto;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@RequestMapping("/api/customer/")
public class CustomerController {
//    @PostMapping("create")
//    public ResponseEntity<ApiResponse<CustomerCreateDto>> createCustomer(@RequestBody CustomerCreateDto customerDto) {
//
//        return ResponseEntity.ok(ApiResponses.success(bookingService.confirmBookingRequest(requestId)));
//    }
}
