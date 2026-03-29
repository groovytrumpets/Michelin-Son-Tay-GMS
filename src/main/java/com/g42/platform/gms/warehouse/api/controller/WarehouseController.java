package com.g42.platform.gms.warehouse.api.controller;

import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.warehouse.api.dto.*;
import com.g42.platform.gms.warehouse.app.service.CatalogItemService;
import com.g42.platform.gms.warehouse.app.service.WarehouseService;
import com.g42.platform.gms.warehouse.domain.entity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/warehouse")
public class WarehouseController {
    @Autowired
    private WarehouseService warehouseService;
    @Autowired
    private CatalogItemService catalogItemService;
    @GetMapping("/brand/all")
    public ResponseEntity<ApiResponse<List<BrandHintDto>>> getAllBrands() {
        List<BrandHintDto> promotionCreateDtoList = catalogItemService.getAllBrands();
        return ResponseEntity.ok(ApiResponses.success(promotionCreateDtoList));
    }
    @GetMapping("/product-line/all")
    public ResponseEntity<ApiResponse<List<ProductLineDto>>> getAllProductLines() {
        List<ProductLineDto> promotionCreateDtoList = catalogItemService.getAllProductLines();
        return ResponseEntity.ok(ApiResponses.success(promotionCreateDtoList));
    }
    @GetMapping("/specification/all")
    public ResponseEntity<ApiResponse<List<SpecificationDto>>> getAllSpecs() {
        List<SpecificationDto> promotionCreateDtoList = catalogItemService.getAllSpecs();
        return ResponseEntity.ok(ApiResponses.success(promotionCreateDtoList));
    }
    @GetMapping("/specification/all/{CatalogItemId}")
    public ResponseEntity<ApiResponse<List<SpecificationDto>>> getAllSpecsById(@PathVariable Integer CatalogItemId) {
        List<SpecificationDto> promotionCreateDtoList = catalogItemService.getAllSpecsById(CatalogItemId);
        return ResponseEntity.ok(ApiResponses.success(promotionCreateDtoList));
    }
    @GetMapping("/spec-attribute/all")
    public ResponseEntity<ApiResponse<List<SpecAttributeDto>>> getAllSpecAttributes() {
        List<SpecAttributeDto> promotionCreateDtoList = catalogItemService.getAllSpecAttributes();
        return ResponseEntity.ok(ApiResponses.success(promotionCreateDtoList));
    }
    @GetMapping("/spec-attribute/{attributeId}")
    public ResponseEntity<ApiResponse<SpecAttributeDto>> getSpecsAttributeById(@PathVariable Integer attributeId) {
        SpecAttributeDto specAttributeDto = catalogItemService.getSpecsAttributeById(attributeId);
        return ResponseEntity.ok(ApiResponses.success(specAttributeDto));
    }
    @GetMapping("/item-categoy/all")
    public ResponseEntity<ApiResponse<List<ItemCategoryHintDto>>> getAllItemCategory() {
        List<ItemCategoryHintDto> promotionCreateDtoList = catalogItemService.getAllItemCategory();
        return ResponseEntity.ok(ApiResponses.success(promotionCreateDtoList));
    }
    @PostMapping("/brand/create")
    public ResponseEntity<ApiResponse<Brand>> createBrand(@RequestBody Brand brand) {
        return ResponseEntity.ok(ApiResponses.success(catalogItemService.createNewBrand(brand)));
    }
    @PostMapping("/catalog-item/create")
    public ResponseEntity<ApiResponse<CatalogItemDto>> createCatalog(@RequestBody CatalogCreateDto createDto) {
        return ResponseEntity.ok(ApiResponses.success(catalogItemService.createNewCatalog(createDto)));
    }
    @PostMapping("/product-line/create")
    public ResponseEntity<ApiResponse<ProductLine>> createProductLine(@RequestBody ProductLine productLine) {
        return ResponseEntity.ok(ApiResponses.success(catalogItemService.saveProductLine(productLine)));
    }
    @PostMapping("/itemCategory/create")
    public ResponseEntity<ApiResponse<ItemCategory>> createItemCategory(@RequestBody ItemCategoryReqDto itemCategory) {
        return ResponseEntity.ok(ApiResponses.success(catalogItemService.saveItemCate(itemCategory)));
    }
    @PostMapping("/specs/create")
    public ResponseEntity<ApiResponse<Specification>> createSpec(@RequestBody Specification specification) {
        return ResponseEntity.ok(ApiResponses.success(catalogItemService.saveSpecs(specification)));
    }
    @PostMapping("/specs-attribute/create")
    public ResponseEntity<ApiResponse<SpecAttribute>> createSpecAttribute(@RequestBody SpecAttribute specAttribute) {
        return ResponseEntity.ok(ApiResponses.success(catalogItemService.saveSpecAttribute(specAttribute)));
    }

}
