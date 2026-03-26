package com.g42.platform.gms.warehouse.api.controller;

import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.warehouse.api.dto.*;
import com.g42.platform.gms.warehouse.app.service.CatalogItemService;
import com.g42.platform.gms.warehouse.app.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    @GetMapping("/specification/all")
    public ResponseEntity<ApiResponse<List<SpecAttributeDto>>> getAllSpecAttributes() {
        List<SpecAttributeDto> promotionCreateDtoList = catalogItemService.getAllSpecAttributes();
        return ResponseEntity.ok(ApiResponses.success(promotionCreateDtoList));
    }
}
