package com.g42.platform.gms.marketing.service_combo.api.controller;

import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.marketing.service_combo.api.dto.ComboCreateDto;
import com.g42.platform.gms.marketing.service_combo.api.dto.ComboResDto;
import com.g42.platform.gms.marketing.service_combo.app.service.ComboItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/service/combo")
@RequiredArgsConstructor
public class ComboItemController {
    @Autowired
    private ComboItemService comboItemService;
    @GetMapping("/{catalogId}")
    public ResponseEntity<ApiResponse<List<ComboResDto>>> getComboByCatalogId(@PathVariable String catalogId){
    List<ComboResDto> apiResponse = comboItemService.getListItemByCombo(catalogId);
        return ResponseEntity.ok(ApiResponses.success(apiResponse));
    }
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<List<ComboCreateDto>>> createComboByCatalogId(@RequestBody List<ComboCreateDto> comboCreateDtos){
        List<ComboCreateDto> apiResponse = comboItemService.createListItemByCatalogId(comboCreateDtos);
        return ResponseEntity.ok(ApiResponses.success(apiResponse));
    }
//    @PutMapping("/update/{catalogId}")
//    public ResponseEntity<ApiResponse<List<ComboCreateDto>>> updateComboByCatalogId(@PathVariable String catalogId,@RequestBody List<ComboCreateDto> comboCreateDtos){
//        List<ComboCreateDto> apiResponse = comboItemService.updateListItemByCatalogId(comboCreateDtos,catalogId);
//        return ResponseEntity.ok(ApiResponses.success(apiResponse));
//    }

}
