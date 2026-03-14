package com.g42.platform.gms.customer.api.controller;

import com.g42.platform.gms.booking_management.domain.enums.BookingEnum;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.customer.api.dto.CustomerCreateDto;
import com.g42.platform.gms.customer.api.dto.CustomerUpdateDto;
import com.g42.platform.gms.customer.application.service.CustomerService;
import com.g42.platform.gms.customer.domain.entity.CustomerProfile;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@AllArgsConstructor
@RequestMapping("/api/admin/customer/")
public class CustomerController {
    @Autowired
    CustomerService customerService;
    @PostMapping("create")
    public ResponseEntity<ApiResponse<CustomerCreateDto>> createCustomer(@RequestBody CustomerCreateDto customerDto) {
        return ResponseEntity.ok(ApiResponses.success(customerService.createNewCustomer(customerDto)));
    }
    @GetMapping("getAllCustomer")
    public ResponseEntity<ApiResponse<Page<CustomerProfile>>> getAllCustomerProfile(@RequestParam(defaultValue = "0") int page,
                                                                                    @RequestParam(defaultValue = "10") int size,
                                                                                    @RequestParam(required = false) LocalDate date,
                                                                                    @RequestParam(required = false) Boolean isGuest,
                                                                                    @RequestParam(required = false) String search) {
        return ResponseEntity.ok(ApiResponses.success(customerService.getListOfAllCustomerProfile(page, size, date, isGuest, search)));
    }
    @PutMapping("{customerId}/update")
    public ResponseEntity<ApiResponse<CustomerCreateDto>> updateProfile(@PathVariable Integer customerId,@RequestBody CustomerUpdateDto customerUpdateDto) {
        return ResponseEntity.ok(ApiResponses.success(customerService.updateCustomer(customerId, customerUpdateDto)));
    }
}
