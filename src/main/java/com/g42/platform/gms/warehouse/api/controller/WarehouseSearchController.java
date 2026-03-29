package com.g42.platform.gms.warehouse.api.controller;

import com.g42.platform.gms.booking_management.api.dto.confirmed.BookedRespond;
import com.g42.platform.gms.booking_management.domain.enums.BookingEnum;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.warehouse.api.dto.CatalogItemDto;
import com.g42.platform.gms.warehouse.api.dto.CatalogSummaryDto;
import com.g42.platform.gms.warehouse.api.dto.SpecificationDto;
import com.g42.platform.gms.warehouse.api.mapper.CatalogDtoMapper;
import com.g42.platform.gms.warehouse.app.service.CatalogItemService;
import com.g42.platform.gms.warehouse.app.service.WarehouseService;
import com.g42.platform.gms.warehouse.domain.entity.CatalogItem;
import com.g42.platform.gms.warehouse.domain.enums.CatalogItemType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/warehouse/search")
public class WarehouseSearchController {
    @Autowired
    private WarehouseService warehouseService;
    @Autowired
    private CatalogItemService catalogItemService;
    @Autowired
    private CatalogDtoMapper catalogDtoMapper;
    @GetMapping("/catalog-items")
    public ResponseEntity<ApiResponse<Page<CatalogSummaryDto>>> getAllItems(@RequestParam(defaultValue = "0") int page,
                                                                            @RequestParam(defaultValue = "10") int size,
                                                                            @RequestParam(required = false) String search,
                                                                            @RequestParam(required = false) CatalogItemType itemType,
                                                                            @RequestParam(required = false) Boolean isActive,
                                                                            @RequestParam(required = false) Integer brand,
                                                                            @RequestParam(required = false) Integer productLine,
                                                                            @RequestParam(required = false) String categoryCode,
                                                                            @RequestParam(required = false) BigDecimal minPrice,
                                                                            @RequestParam(required = false) BigDecimal maxPrice,
                                                                            @RequestParam(required = false) String sortBy
                                                                            ){
        Page<CatalogSummaryDto> apiResponse = warehouseService.getListItems
                (page,size,itemType,isActive,search,brand,productLine,categoryCode,minPrice,maxPrice,sortBy);
        return ResponseEntity.ok(ApiResponses.success(apiResponse));
    }
    @GetMapping("/catalog-items/detail/{catalogItemId}")
    public ResponseEntity<ApiResponse<CatalogItemDto>> getCatalogDetailById(@PathVariable Integer catalogItemId) {
        CatalogItem catalogItem = catalogItemService.getCatalogDetailById(catalogItemId);
        return ResponseEntity.ok(ApiResponses.success(catalogDtoMapper.toDto(catalogItem)));
    }
}
