package com.g42.platform.gms.promotion.api.controller;

import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.promotion.api.dto.PromotionCreateDto;
import com.g42.platform.gms.promotion.app.service.PromotionService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

}
