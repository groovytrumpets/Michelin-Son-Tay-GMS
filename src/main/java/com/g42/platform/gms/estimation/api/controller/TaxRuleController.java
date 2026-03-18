package com.g42.platform.gms.estimation.api.controller;

import com.g42.platform.gms.common.dto.ApiResponses;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping("/api/service-ticket/tax-rule/")
public class TaxRuleController {
//    @GetMapping("/tax-rules")
//    public ResponseEntity<ApiResponse<List<TaxRuleDto>>> getAllTaxRules() {
//        return ResponseEntity.ok(ApiResponses.success(taxRuleService.getAllActiveTaxRules()));
//    }
}
