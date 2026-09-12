package com.g42.platform.gms.warehouse.api.controller;

import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.warehouse.api.dto.*;
import com.g42.platform.gms.warehouse.api.dto.request.CreateWarehouseRequest;
import com.g42.platform.gms.warehouse.api.dto.request.UpdateWarehouseRequest;
import com.g42.platform.gms.warehouse.app.service.catalog.CatalogItemService;
import com.g42.platform.gms.warehouse.app.service.catalog.WarehouseService;
import com.g42.platform.gms.warehouse.domain.entity.*;
import com.g42.platform.gms.warehouse.infrastructure.entity.ProductUnitJpa;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/warehouse")
public class WarehouseController {
    @Autowired
    private WarehouseService warehouseService;
    @Autowired
    private CatalogItemService catalogItemService;

    // ─── Catalog master data (giữ nguyên, không thêm auth để không breaking change) ───

    @GetMapping("/brand/all")
    public ResponseEntity<ApiResponse<List<BrandHintDto>>> getAllBrands() {
        return ResponseEntity.ok(ApiResponses.success(catalogItemService.getAllBrands()));
    }
    @GetMapping("/product-line/all")
    public ResponseEntity<ApiResponse<List<ProductLineDto>>> getAllProductLines() {
        return ResponseEntity.ok(ApiResponses.success(catalogItemService.getAllProductLines()));
    }
    @GetMapping("/specification/all")
    public ResponseEntity<ApiResponse<List<SpecificationDto>>> getAllSpecs() {
        return ResponseEntity.ok(ApiResponses.success(catalogItemService.getAllSpecs()));
    }
    @GetMapping("/specification/all/{CatalogItemId}")
    public ResponseEntity<ApiResponse<List<SpecificationDto>>> getAllSpecsById(@PathVariable Integer CatalogItemId) {
        return ResponseEntity.ok(ApiResponses.success(catalogItemService.getAllSpecsById(CatalogItemId)));
    }

    // ─── Màu (item_color): một phụ tùng có nhiều mã màu, hiển thị chấm tròn ở trang bán hàng ───

    @GetMapping("/item-color/{itemId}")
    public ResponseEntity<ApiResponse<List<ItemColorDto>>> getItemColors(@PathVariable Integer itemId) {
        return ResponseEntity.ok(ApiResponses.success(catalogItemService.getColorsByItemId(itemId)));
    }

    @PutMapping("/item-color/{itemId}")
    public ResponseEntity<ApiResponse<List<ItemColorDto>>> replaceItemColors(
            @PathVariable Integer itemId, @RequestBody List<ItemColorDto> colors) {
        return ResponseEntity.ok(ApiResponses.success(catalogItemService.replaceItemColors(itemId, colors)));
    }
    @GetMapping("/spec-attribute/all")
    public ResponseEntity<ApiResponse<List<SpecAttributeDto>>> getAllSpecAttributes() {
        return ResponseEntity.ok(ApiResponses.success(catalogItemService.getAllSpecAttributes()));
    }
    @GetMapping("/spec-attribute/{attributeId}")
    public ResponseEntity<ApiResponse<SpecAttributeDto>> getSpecsAttributeById(@PathVariable Integer attributeId) {
        return ResponseEntity.ok(ApiResponses.success(catalogItemService.getSpecsAttributeById(attributeId)));
    }
    /** "item-categoy" là lỗi gõ có từ đầu; giữ lại vì client cũ vẫn đang gọi đường dẫn đó. */
    @GetMapping({"/item-category/all", "/item-categoy/all"})
    public ResponseEntity<ApiResponse<List<ItemCategoryDto>>> getAllItemCategory() {
        return ResponseEntity.ok(ApiResponses.success(catalogItemService.getAllItemCategory()));
    }
    @PostMapping("/brand/create")
    public ResponseEntity<ApiResponse<Brand>> createBrand(@RequestBody Brand brand) {
        return ResponseEntity.ok(ApiResponses.success(catalogItemService.createNewBrand(brand)));
    }
    @PostMapping("/catalog-item/create")
    public ResponseEntity<ApiResponse<CatalogItemDto>> createCatalog(@RequestBody CatalogCreateDto createDto) {
        return ResponseEntity.ok(ApiResponses.success(catalogItemService.createNewCatalog(createDto)));
    }
    @PutMapping("/catalog-item/update/{itemId}")
    public ResponseEntity<ApiResponse<CatalogItemDto>> updateCatalog(
            @RequestBody CatalogCreateDto updateDto,
            @PathVariable Integer itemId,
            @AuthenticationPrincipal com.g42.platform.gms.auth.entity.StaffPrincipal principal) {
        Integer staffId = principal != null ? principal.getStaffId() : null;
        return ResponseEntity.ok(ApiResponses.success(catalogItemService.updateCatalog(updateDto, itemId, staffId)));
    }
    @DeleteMapping("/catalog-item/{itemId}")
    @PreAuthorize("hasAnyRole('WAREHOUSE_MANAGER','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivateCatalogItem(@PathVariable Integer itemId) {
        catalogItemService.deactivateCatalogItem(itemId);
        return ResponseEntity.ok(ApiResponses.success(null));
    }
    @PostMapping("/catalog-item/{itemId}/activate")
    @PreAuthorize("hasAnyRole('WAREHOUSE_MANAGER','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<Void>> activateCatalogItem(@PathVariable Integer itemId) {
        catalogItemService.activateCatalogItem(itemId);
        return ResponseEntity.ok(ApiResponses.success(null));
    }
    @PostMapping("/product-line/create")
    public ResponseEntity<ApiResponse<ProductLine>> createProductLine(@RequestBody ProductLine productLine) {
        return ResponseEntity.ok(ApiResponses.success(catalogItemService.saveProductLine(productLine)));
    }
    @PostMapping({"/item-category/create", "/itemCategory/create"})
    public ResponseEntity<ApiResponse<ItemCategoryDto>> createItemCategory(@RequestBody ItemCategory itemCategory) {
        return ResponseEntity.ok(ApiResponses.success(catalogItemService.saveItemCate(itemCategory)));
    }
    @DeleteMapping("/brand/{brandId}")
    public ResponseEntity<ApiResponse<Void>> deleteBrand(@PathVariable Integer brandId) {
        catalogItemService.deleteBrand(brandId);
        return ResponseEntity.ok(ApiResponses.success(null));
    }
    @DeleteMapping("/product-line/{productLineId}")
    public ResponseEntity<ApiResponse<Void>> deleteProductLine(@PathVariable Integer productLineId) {
        catalogItemService.deleteProductLine(productLineId);
        return ResponseEntity.ok(ApiResponses.success(null));
    }
    @DeleteMapping("/item-category/{categoryId}")
    public ResponseEntity<ApiResponse<Void>> deleteItemCategory(@PathVariable Integer categoryId) {
        catalogItemService.deleteItemCategory(categoryId);
        return ResponseEntity.ok(ApiResponses.success(null));
    }
    @GetMapping("/units")
    public ResponseEntity<ApiResponse<List<ProductUnitJpa>>> getAllUnits() {
        return ResponseEntity.ok(ApiResponses.success(catalogItemService.getAllProductUnits()));
    }
    @PostMapping("/units")
    public ResponseEntity<ApiResponse<ProductUnitJpa>> createUnit(@RequestParam String unitName) {
        return ResponseEntity.ok(ApiResponses.success(catalogItemService.createProductUnit(unitName)));
    }
    @DeleteMapping("/units/{unitId}")
    public ResponseEntity<ApiResponse<Void>> deleteUnit(@PathVariable Integer unitId) {
        catalogItemService.deleteProductUnit(unitId);
        return ResponseEntity.ok(ApiResponses.success(null));
    }
    @PostMapping("/specs/create")
    public ResponseEntity<ApiResponse<Specification>> createSpec(@RequestBody Specification specification) {
        return ResponseEntity.ok(ApiResponses.success(catalogItemService.saveSpecs(specification)));
    }
    @PostMapping("/specs-attribute/create")
    public ResponseEntity<ApiResponse<SpecAttribute>> createSpecAttribute(@RequestBody SpecAttribute specAttribute) {
        return ResponseEntity.ok(ApiResponses.success(catalogItemService.saveSpecAttribute(specAttribute)));
    }

    // ─── Warehouse CRUD ───────────────────────────────────────────────────────

    /**
     * Danh sách tất cả kho.
     * WAREHOUSE_KEEPER / WAREHOUSE_MANAGER / MANAGER / ADMIN đều được xem.
     */
    @GetMapping("/warehouse/all")
    @PreAuthorize("hasAnyRole('WAREHOUSE_KEEPER','WAREHOUSE_MANAGER','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<List<WarehouseDto>>> getAllWarehouse(
            @RequestParam(required = false) Boolean isActive) {
        return ResponseEntity.ok(ApiResponses.success(warehouseService.listWarehouses(isActive)));
    }

    /** Chi tiết 1 kho. */
    @GetMapping("/warehouse/{id}")
    @PreAuthorize("hasAnyRole('WAREHOUSE_KEEPER','WAREHOUSE_MANAGER','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<WarehouseDto>> getWarehouse(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponses.success(warehouseService.getWarehouse(id)));
    }

    /**
     * Tạo kho mới (MASTER / BRANCH / DEFECTIVE).
     * Chỉ WAREHOUSE_MANAGER / MANAGER / ADMIN.
     */
    @PostMapping("/warehouse")
    @PreAuthorize("hasAnyRole('WAREHOUSE_MANAGER','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<WarehouseDto>> createWarehouse(
            @Valid @RequestBody CreateWarehouseRequest request) {
        return ResponseEntity.ok(ApiResponses.success(warehouseService.createWarehouse(request)));
    }

    /**
     * Cập nhật tên / địa chỉ / manager của kho.
     * Chỉ WAREHOUSE_MANAGER / MANAGER / ADMIN.
     */
    @PutMapping("/warehouse/{id}")
    @PreAuthorize("hasAnyRole('WAREHOUSE_MANAGER','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<WarehouseDto>> updateWarehouse(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateWarehouseRequest request) {
        return ResponseEntity.ok(ApiResponses.success(warehouseService.updateWarehouse(id, request)));
    }

    /**
     * Bật kho (isActive = true).
     * Chỉ WAREHOUSE_MANAGER / MANAGER / ADMIN.
     */
    @PostMapping("/warehouse/{id}/activate")
    @PreAuthorize("hasAnyRole('WAREHOUSE_MANAGER','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<WarehouseDto>> activateWarehouse(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponses.success(warehouseService.setWarehouseActive(id, true)));
    }

    /**
     * Tắt kho (isActive = false).
     * Chỉ WAREHOUSE_MANAGER / MANAGER / ADMIN.
     */
    @PostMapping("/warehouse/{id}/deactivate")
    @PreAuthorize("hasAnyRole('WAREHOUSE_MANAGER','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<WarehouseDto>> deactivateWarehouse(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponses.success(warehouseService.setWarehouseActive(id, false)));
    }

    // ─── Legacy defective endpoint (giữ nguyên để không breaking change) ─────

    @PostMapping("/warehouse/defective/create/{branchWarehouseId}")
    @PreAuthorize("hasAnyRole('WAREHOUSE_MANAGER','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<String>> createDefectiveWarehouse(@PathVariable Integer branchWarehouseId) {
        try {
            warehouseService.createDefectiveWarehouse(branchWarehouseId);
            return ResponseEntity.ok(ApiResponses.success("Tạo kho hàng lỗi thành công cho warehouse ID: " + branchWarehouseId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponses.error("ERROR", e.getMessage()));
        }
    }
}
