package com.g42.platform.gms.estimation.app.service;

import com.g42.platform.gms.estimation.api.dto.taxRule.TaxCreateDto;
import com.g42.platform.gms.estimation.api.dto.taxRule.TaxRuleDto;
import com.g42.platform.gms.estimation.api.mapper.TaxRuleDtoMapper;
import com.g42.platform.gms.estimation.domain.entity.TaxRule;
import com.g42.platform.gms.estimation.domain.repository.TaxRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaxRuleService {
    private final TaxRuleRepository taxRuleRepository;
    private final TaxRuleDtoMapper taxRuleDtoMapper;
    public List<TaxRuleDto> getAllActiveTaxRules() {
        List<TaxRule> taxRules = taxRuleRepository.getAllActiveTax();
        return taxRules.stream().map(taxRuleDtoMapper::toTaxRuleDto).toList();
    }

    public TaxRuleDto createNewActiveTaxRules(TaxCreateDto taxCreateDto) {
        TaxRule taxRule = new TaxRule();
        taxRule.setTaxName(taxCreateDto.getTaxName());
        taxRule.setTaxRate(taxCreateDto.getTaxRate());
        taxRule.setTaxCode(taxCreateDto.getTaxName().toUpperCase());
        taxRule.setIsActive((byte)1);
        taxRule.setEffectiveFrom(LocalDate.now());
        TaxRule saved = taxRuleRepository.save(taxRule);
        return taxRuleDtoMapper.toTaxRuleDto(saved);
    }
}
