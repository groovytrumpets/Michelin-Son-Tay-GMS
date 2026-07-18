package com.g42.platform.gms.customer.api.controller;

import com.g42.platform.gms.booking_management.domain.enums.BookingEnum;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.customer.api.dto.CustomerCreateDto;
import com.g42.platform.gms.customer.api.dto.CustomerUpdateDto;
import com.g42.platform.gms.customer.application.service.CustomerService;
import com.g42.platform.gms.customer.domain.entity.CustomerProfile;
import com.g42.platform.gms.systemlog.annotation.Auditable;
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
    @Auditable(action = "CREATE", module = "CUSTOMER", description = "Tạo hồ sơ khách hàng", targetType = "CUSTOMER")
    public ResponseEntity<ApiResponse<CustomerCreateDto>> createCustomer(@RequestBody CustomerCreateDto customerDto) {
        return ResponseEntity.ok(ApiResponses.success(customerService.createNewCustomer(customerDto)));
    }
    @GetMapping("getAllCustomer")
    public ResponseEntity<ApiResponse<Page<CustomerProfile>>> getAllCustomerProfile(@RequestParam(defaultValue = "0") int page,
                                                                                    @RequestParam(defaultValue = "10") int size,
                                                                                    @RequestParam(required = false) LocalDate date,
                                                                                    @RequestParam(required = false) Boolean isGuest,
                                                                                    @RequestParam(required = false) String search,
                                                                                    @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(ApiResponses.success(customerService.getListOfAllCustomerProfile(page, size, date, isGuest, search, status)));
    }
    @PutMapping("{customerId}/update")
    @Auditable(action = "UPDATE", module = "CUSTOMER", description = "Cập nhật hồ sơ khách hàng", targetType = "CUSTOMER")
    public ResponseEntity<ApiResponse<CustomerCreateDto>> updateProfile(@PathVariable Integer customerId,@RequestBody CustomerUpdateDto customerUpdateDto) {
        return ResponseEntity.ok(ApiResponses.success(customerService.updateCustomer(customerId, customerUpdateDto)));
    }
    @GetMapping("{customerId}")
    public ResponseEntity<ApiResponse<CustomerProfile>> getCustomerProfile(@PathVariable Integer customerId) {
        return ResponseEntity.ok(ApiResponses.success(customerService.findByCustomerId(customerId)));
    }
    @PutMapping("{customerId}/delete")
    @Auditable(action = "DELETE", module = "CUSTOMER", severity = "CRITICAL", description = "Xóa hồ sơ khách hàng", targetType = "CUSTOMER")
    public ResponseEntity<ApiResponse<CustomerProfile>> deleteProfile(@PathVariable Integer customerId) {
        return ResponseEntity.ok(ApiResponses.success(customerService.deleteCustomer(customerId)));
    }
    @PutMapping("{customerId}/locked")
    @Auditable(action = "UPDATE", module = "CUSTOMER", severity = "WARNING", description = "Khóa tài khoản khách hàng", targetType = "CUSTOMER")
    public ResponseEntity<ApiResponse<CustomerProfile>> lockedProfile(@PathVariable Integer customerId) {
        return ResponseEntity.ok(ApiResponses.success(customerService.lockedCustomer(customerId)));
    }
}
