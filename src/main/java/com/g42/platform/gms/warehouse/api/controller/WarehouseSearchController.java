package com.g42.platform.gms.warehouse.api.controller;

import com.g42.platform.gms.booking_management.api.dto.confirmed.BookedRespond;
import com.g42.platform.gms.booking_management.domain.enums.BookingEnum;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.warehouse.api.dto.CatalogSummaryDto;
import com.g42.platform.gms.warehouse.app.service.CatalogItemService;
import com.g42.platform.gms.warehouse.app.service.WarehouseService;
import com.g42.platform.gms.warehouse.domain.enums.CatalogItemType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/warehouse/search")
public class WarehouseSearchController {
    @Autowired
    private WarehouseService warehouseService;
    @Autowired
    private CatalogItemService catalogItemService;
    @GetMapping("/catalog-items")
    public ResponseEntity<ApiResponse<Page<CatalogSummaryDto>>> getAllItems(@RequestParam(defaultValue = "0") int page,
                                                                            @RequestParam(defaultValue = "10") int size,
                                                                            @RequestParam(required = false) String search,
                                                                            @RequestParam(required = false) CatalogItemType itemType,
                                                                            @RequestParam(required = false) Boolean isActive,
                                                                            @RequestParam(required = false) Integer brandId,
                                                                            @RequestParam(required = false) Integer productLineId,
                                                                            @RequestParam(required = false) Integer categoryId,
                                                                            @RequestParam(required = false) BigDecimal minPrice,
                                                                            @RequestParam(required = false) BigDecimal maxPrice,
                                                                            @RequestParam(required = false) String sortBy
                                                                            ){
        Page<CatalogSummaryDto> apiResponse = warehouseService.getListItems
                (page,size,itemType,isActive,search,brandId,productLineId,categoryId,minPrice,maxPrice,sortBy);
        return ResponseEntity.ok(ApiResponses.success(apiResponse));
    }
}
