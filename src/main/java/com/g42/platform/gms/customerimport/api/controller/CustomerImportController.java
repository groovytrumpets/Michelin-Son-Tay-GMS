package com.g42.platform.gms.customerimport.api.controller;

import com.g42.platform.gms.authz.PermissionCodes;
import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.customerimport.api.dto.CustomerImportReport;
import com.g42.platform.gms.customerimport.api.dto.CustomerImportRequest;
import com.g42.platform.gms.customerimport.api.dto.ImportBatchDetailDto;
import com.g42.platform.gms.customerimport.api.dto.ImportBatchRowsDto;
import com.g42.platform.gms.customerimport.application.service.CustomerImportService;
import com.g42.platform.gms.customerimport.infrastructure.entity.ImportBatchJpa;
import com.g42.platform.gms.systemlog.annotation.Auditable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Nhập khách hàng, xe và lịch sử dịch vụ từ sổ Excel cũ.
 * Khớp contract FE: src/services/customerImportService.js.
 *
 * Chỉ quản lý và quản trị được gọi: một lần chạy tạo hàng loạt tài khoản khách và
 * ghi lịch sử, không phải thao tác hằng ngày của lễ tân.
 */
@RestController
@RequestMapping("/api/admin/customer-import")
@PreAuthorize("hasAuthority('" + PermissionCodes.CUSTOMER_IMPORT + "')")
public class CustomerImportController {

    @Autowired
    private CustomerImportService service;

    /** Chạy thử: không ghi gì, trả về đúng những gì bước ghi sẽ làm cùng lỗi từng dòng. */
    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<CustomerImportReport>> validate(@RequestBody CustomerImportRequest request) {
        return ResponseEntity.ok(ApiResponses.success(service.validate(request)));
    }

    @PostMapping("/commit")
    @Auditable(action = "CREATE", module = "CUSTOMER", severity = "WARNING",
            description = "Nhập khách hàng và lịch sử dịch vụ từ Excel", targetType = "CUSTOMER")
    public ResponseEntity<ApiResponse<CustomerImportReport>> commit(
            @AuthenticationPrincipal StaffPrincipal staffPrincipal,
            @RequestBody CustomerImportRequest request) {
        Integer staffId = staffPrincipal == null ? null : staffPrincipal.getStaffId();
        return ResponseEntity.ok(ApiResponses.success(service.commit(request, staffId)));
    }

    /** Lịch sử các lô đã nhập, để chọn lô cần hoàn tác. */
    @GetMapping("/batches")
    public ResponseEntity<ApiResponse<List<ImportBatchJpa>>> batches() {
        return ResponseEntity.ok(ApiResponses.success(service.listBatches()));
    }

    /**
     * Lô đó đưa vào hệ thống những khách nào, mỗi khách có xe gì và những lượt nào.
     * Dùng cho màn xem chi tiết lô, không phải để sửa.
     */
    @GetMapping("/batches/{batchId}/detail")
    public ResponseEntity<ApiResponse<ImportBatchDetailDto>> batchDetail(@PathVariable Integer batchId) {
        return ResponseEntity.ok(ApiResponses.success(service.batchDetail(batchId)));
    }

    /**
     * Nội dung một lô để mở lại lên bảng mà sửa. Trả nguyên văn các dòng đã gửi lần
     * trước; lô nhập từ trước khi có chỗ lưu nguyên văn thì dựng lại từ dữ liệu đã ghi
     * và bật cờ reconstructed.
     */
    @GetMapping("/batches/{batchId}/rows")
    public ResponseEntity<ApiResponse<ImportBatchRowsDto>> batchRows(@PathVariable Integer batchId) {
        return ResponseEntity.ok(ApiResponses.success(service.batchRows(batchId)));
    }

    /** Gỡ nguyên lô: xoá lượt dịch vụ, và chỉ xoá khách/xe do chính lô này tạo ra. */
    @DeleteMapping("/batches/{batchId}")
    @Auditable(action = "DELETE", module = "CUSTOMER", severity = "CRITICAL",
            description = "Hoàn tác lô nhập dữ liệu khách hàng", targetType = "CUSTOMER")
    public ResponseEntity<ApiResponse<CustomerImportReport>> rollback(@PathVariable Integer batchId) {
        return ResponseEntity.ok(ApiResponses.success(service.rollback(batchId)));
    }
}
