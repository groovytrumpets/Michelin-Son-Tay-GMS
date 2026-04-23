package com.g42.platform.gms.estimation.infrastructure;

import com.g42.platform.gms.estimation.api.internal.TaxRuleInternalApi;
import com.g42.platform.gms.estimation.domain.entity.TaxRule;
import com.g42.platform.gms.estimation.infrastructure.entity.TaxRuleJpa;
import com.g42.platform.gms.estimation.infrastructure.mapper.TaxRuleJpaMapper;
import com.g42.platform.gms.estimation.infrastructure.repository.TaxRuleRepositoryJpa;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class TaxRuleInternalApiImpl implements TaxRuleInternalApi {
    @Autowired
    private TaxRuleRepositoryJpa taxRuleRepositoryJpa;
    @Autowired
    private TaxRuleJpaMapper taxRuleJpaMapper;

    @Override
    public Integer getTaxCodeFreeId(String free) {
        Integer code = null;
        TaxRuleJpa taxRuleJpa = taxRuleRepositoryJpa.findByTaxCode(free);
        if (taxRuleJpa == null) {
            return -1;
        }
        code = taxRuleJpa.getTaxRuleId();
        if (code == null) {
            return -1;
        }
        return code;
    }

    @Override
    public Integer createNewFreeTax() {
        TaxRuleJpa taxRule = new TaxRuleJpa();
        taxRule.setTaxCode("FREE");
        taxRule.setTaxName("Miễn thuế");
        taxRule.setTaxRate(BigDecimal.ZERO);
        taxRule.setEffectiveFrom(LocalDate.now());
        return taxRuleRepositoryJpa.save(taxRule).getTaxRuleId();
    }

    @Override
    public TaxRule getTaxRuleById(Integer taxRuleId) {
        return taxRuleJpaMapper.toDomain(taxRuleRepositoryJpa.findById(taxRuleId).get());
    }
}
