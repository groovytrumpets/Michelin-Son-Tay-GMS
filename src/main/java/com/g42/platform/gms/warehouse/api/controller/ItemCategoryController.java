package com.g42.platform.gms.warehouse.api.controller;

import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.warehouse.api.dto.ItemCategoryAssignmentDto;
import com.g42.platform.gms.warehouse.api.dto.ItemCategoryDto;
import com.g42.platform.gms.warehouse.api.dto.request.AssignItemCategoryRequest;
import com.g42.platform.gms.warehouse.app.service.catalog.ItemCategoryService;
import com.g42.platform.gms.warehouse.domain.entity.ItemCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Cấu hình danh mục phụ tùng / dịch vụ — màn Hệ thống → Danh mục phụ tùng & dịch vụ.
 *
 * Đầu mục kiểm tra an toàn của phiếu dịch vụ là khái niệm khác hẳn và nằm ở
 * /api/admin/safety-category, đừng nhầm hai cái với nhau như bảng work_category cũ.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/item-category")
public class ItemCategoryController {

    private final ItemCategoryService itemCategoryService;

    /** Cả danh mục đã ẩn, để màn cấu hình bật lại được. */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ItemCategoryDto>>> getAll() {
        return ResponseEntity.ok(ApiResponses.success(itemCategoryService.getAllForConfig()));
    }

    /** Chỉ danh mục đang dùng, để đổ vào ô chọn ở các màn nhập liệu. */
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<ItemCategoryDto>>> getActive() {
        return ResponseEntity.ok(ApiResponses.success(itemCategoryService.getActive()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ItemCategoryDto>> create(@RequestBody ItemCategory request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponses.success(itemCategoryService.create(request)));
    }

    @PutMapping("/{categoryId}")
    public ResponseEntity<ApiResponse<ItemCategoryDto>> update(@PathVariable Integer categoryId,
                                                               @RequestBody ItemCategory request) {
        return ResponseEntity.ok(ApiResponses.success(itemCategoryService.update(categoryId, request)));
    }

    /** Ẩn danh mục nếu đang có hàng hóa dùng, xóa hẳn nếu chưa ai dùng. */
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Integer categoryId) {
        itemCategoryService.deactivate(categoryId);
        return ResponseEntity.ok(ApiResponses.success(null));
    }

    /**
     * Danh sách phụ tùng / dịch vụ kèm danh mục hiện tại, cho màn xếp danh mục hàng loạt.
     * Đặt {@code uncategorized=true} để lọc ra những món chưa được xếp danh mục nào.
     */
    @GetMapping("/items")
    public ResponseEntity<ApiResponse<Page<ItemCategoryAssignmentDto>>> searchItems(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String itemType,
            @RequestParam(required = false) Integer itemCategoryId,
            @RequestParam(required = false) Boolean uncategorized,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(ApiResponses.success(itemCategoryService.searchItemsForAssignment(
                search, itemType, itemCategoryId, uncategorized, isActive, page, size)));
    }

    /** Xếp nhiều món vào một danh mục; bỏ trống itemCategoryId là gỡ danh mục. */
    @PatchMapping("/assign")
    public ResponseEntity<ApiResponse<Integer>> assign(@RequestBody AssignItemCategoryRequest request) {
        int updated = itemCategoryService.assignCategory(request);
        return ResponseEntity.ok(ApiResponses.success(updated, "Đã cập nhật danh mục cho " + updated + " mục."));
    }
}
