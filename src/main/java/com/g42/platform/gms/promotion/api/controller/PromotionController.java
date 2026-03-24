package com.g42.platform.gms.promotion.api.controller;

import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.promotion.api.dto.PromotionCreateDto;
import com.g42.platform.gms.promotion.app.service.PromotionService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/promotion")
public class PromotionController {
    @Autowired
    PromotionService promotionService;
    @PostMapping("/admin/create")
    public ResponseEntity<ApiResponse<PromotionCreateDto>> createPromotion(@RequestBody PromotionCreateDto promotionCreateDto) {
        PromotionCreateDto promotion = promotionService.createNewPromotion(promotionCreateDto);
        return ResponseEntity.ok(ApiResponses.success(promotion));
    }
    @GetMapping("/admin/all")
    public ResponseEntity<ApiResponse<List<PromotionCreateDto>>> getAllPromotions() {
        List<PromotionCreateDto> promotionCreateDtoList = promotionService.getAllPromotion();
        return ResponseEntity.ok(ApiResponses.success(promotionCreateDtoList));
    }
    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<PromotionCreateDto>>> getAllAvailablePromotions() {
        List<PromotionCreateDto> promotionCreateDtoList = promotionService.getAllAvailablePromotion();
        return ResponseEntity.ok(ApiResponses.success(promotionCreateDtoList));
    }
    @GetMapping("/")
    public ResponseEntity<ApiResponse<PromotionCreateDto>> getPromotionByCode(@RequestParam String code) {
        PromotionCreateDto promotionCreateDtoList = promotionService.getPromotionByCode(code);
        return ResponseEntity.ok(ApiResponses.success(promotionCreateDtoList));
    }

}
