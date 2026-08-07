package com.g42.platform.gms.customer.api.controller;

import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.customer.api.dto.CustomerGroupDto;
import com.g42.platform.gms.customer.application.service.CustomerGroupService;
import com.g42.platform.gms.systemlog.annotation.Auditable;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/customer-group")
public class CustomerGroupController {

    private final CustomerGroupService customerGroupService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CustomerGroupDto>>> list(
            @RequestParam(defaultValue = "true") boolean activeOnly) {
        return ResponseEntity.ok(ApiResponses.success(customerGroupService.findAll(activeOnly)));
    }

    @PostMapping
    @Auditable(action = "CREATE", module = "CUSTOMER", description = "Tạo nhóm khách hàng", targetType = "CUSTOMER_GROUP")
    public ResponseEntity<ApiResponse<CustomerGroupDto>> create(@RequestBody CustomerGroupDto dto) {
        return ResponseEntity.ok(ApiResponses.success(customerGroupService.create(dto)));
    }

    @PutMapping("/{groupId}")
    @Auditable(action = "UPDATE", module = "CUSTOMER", description = "Cập nhật nhóm khách hàng", targetType = "CUSTOMER_GROUP")
    public ResponseEntity<ApiResponse<CustomerGroupDto>> update(@PathVariable Integer groupId,
                                                                @RequestBody CustomerGroupDto dto) {
        return ResponseEntity.ok(ApiResponses.success(customerGroupService.update(groupId, dto)));
    }

    @DeleteMapping("/{groupId}")
    @Auditable(action = "DELETE", module = "CUSTOMER", severity = "WARNING", description = "Ngừng sử dụng nhóm khách hàng", targetType = "CUSTOMER_GROUP")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Integer groupId) {
        customerGroupService.delete(groupId);
        return ResponseEntity.ok(ApiResponses.successMessage("Đã ngừng sử dụng nhóm khách hàng"));
    }
}
