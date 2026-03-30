package com.g42.platform.gms.estimation.api.controller;

import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.estimation.api.dto.taxRule.TaxCreateDto;
import com.g42.platform.gms.estimation.api.dto.taxRule.TaxRuleDto;
import com.g42.platform.gms.estimation.app.service.TaxRuleService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/service-ticket/tax-rule")
public class TaxRuleController {
    private final TaxRuleService taxRuleService;
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<TaxRuleDto>>> getAllTaxRules() {
        return ResponseEntity.ok(ApiResponses.success(taxRuleService.getAllActiveTaxRules()));
    }
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<TaxRuleDto>> createTaxRules(@RequestBody TaxCreateDto taxCreateDto) {
        return ResponseEntity.ok(ApiResponses.success(taxRuleService.createNewActiveTaxRules(taxCreateDto)));
    }

}
