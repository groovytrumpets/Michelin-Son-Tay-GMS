package com.g42.platform.gms.catalog.api.controller;

import com.g42.platform.gms.catalog.api.dto.CatalogItemResponse;
import com.g42.platform.gms.catalog.application.service.CatalogItemService;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
public class CatalogItemController {
    
    private final CatalogItemService catalogItemService;
    
    @GetMapping("/items")
    public ResponseEntity<ApiResponse<List<CatalogItemResponse>>> getAllActiveItems() {
        List<CatalogItemResponse> items = catalogItemService.getAllActiveItems();
        return ResponseEntity.ok(ApiResponses.success(items));
    }
    
    @GetMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<CatalogItemResponse>> getItemById(@PathVariable Integer itemId) {
        CatalogItemResponse item = catalogItemService.getItemById(itemId);
        return ResponseEntity.ok(ApiResponses.success(item));
    }
}
