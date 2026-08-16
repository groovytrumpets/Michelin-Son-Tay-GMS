package com.g42.platform.gms.service_ticket_management.api.controller;

import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.service_ticket_management.api.dto.safety.CreateWorkCategoryRequest;
import com.g42.platform.gms.service_ticket_management.api.dto.safety.WorkCategoryResponse;
import com.g42.platform.gms.service_ticket_management.application.service.SafetyCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Cấu hình đầu mục kiểm tra an toàn — màn Hệ thống → Đầu mục kiểm tra an toàn.
 *
 * Danh mục phụ tùng / dịch vụ là khái niệm khác và nằm ở /api/admin/item-category.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/safety-category")
public class SafetyCategoryController {

    private final SafetyCategoryService safetyCategoryService;

    /** Cả đầu mục đã ẩn, để màn cấu hình bật lại được. */
    @GetMapping
    public ResponseEntity<ApiResponse<List<WorkCategoryResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponses.success(safetyCategoryService.getAllForConfig()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WorkCategoryResponse>> create(
            @Valid @RequestBody CreateWorkCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponses.success(safetyCategoryService.create(request)));
    }

    @PutMapping("/{categoryId}")
    public ResponseEntity<ApiResponse<WorkCategoryResponse>> update(@PathVariable Integer categoryId,
                                                                    @RequestBody CreateWorkCategoryRequest request) {
        return ResponseEntity.ok(ApiResponses.success(safetyCategoryService.update(categoryId, request)));
    }

    /** Ẩn đầu mục nếu phiếu cũ đang dùng, xóa hẳn nếu chưa ai dùng. */
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Integer categoryId) {
        safetyCategoryService.deactivate(categoryId);
        return ResponseEntity.ok(ApiResponses.success(null));
    }
}
