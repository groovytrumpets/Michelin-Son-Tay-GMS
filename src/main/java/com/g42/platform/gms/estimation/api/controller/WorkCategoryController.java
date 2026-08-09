package com.g42.platform.gms.estimation.api.controller;

import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.estimation.api.dto.WorkCataDto;
import com.g42.platform.gms.estimation.api.dto.request.WorkCategoryReqDto;
import com.g42.platform.gms.estimation.app.service.WorkCategoryService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Cấu hình danh mục Hạng mục công việc, phục vụ màn Hệ thống → Hạng mục công việc.
 */
@RestController
@AllArgsConstructor
@RequestMapping("/api/admin/work-category")
public class WorkCategoryController {

    private final WorkCategoryService workCategoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<WorkCataDto>>> getAll() {
        return ResponseEntity.ok(ApiResponses.success(workCategoryService.getAllForConfig()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WorkCataDto>> create(@RequestBody WorkCategoryReqDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponses.success(workCategoryService.create(request)));
    }

    @PutMapping("/{categoryId}")
    public ResponseEntity<ApiResponse<WorkCataDto>> update(@PathVariable Integer categoryId,
                                                           @RequestBody WorkCategoryReqDto request) {
        return ResponseEntity.ok(ApiResponses.success(workCategoryService.update(categoryId, request)));
    }

    /** Ẩn hạng mục; phiếu báo giá cũ vẫn giữ nguyên tham chiếu. */
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<ApiResponse<WorkCataDto>> deactivate(@PathVariable Integer categoryId) {
        return ResponseEntity.ok(ApiResponses.success(workCategoryService.deactivate(categoryId)));
    }
}
