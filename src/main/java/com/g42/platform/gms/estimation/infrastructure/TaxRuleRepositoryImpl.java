package com.g42.platform.gms.estimation.infrastructure;

import com.g42.platform.gms.estimation.domain.entity.TaxRule;
import com.g42.platform.gms.estimation.domain.repository.TaxRuleRepository;
import com.g42.platform.gms.estimation.infrastructure.entity.TaxRuleJpa;
import com.g42.platform.gms.estimation.infrastructure.mapper.TaxRuleJpaMapper;
import com.g42.platform.gms.estimation.infrastructure.repository.TaxRuleRepositoryJpa;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@AllArgsConstructor
public class TaxRuleRepositoryImpl implements TaxRuleRepository {
    private final TaxRuleRepositoryJpa taxRepositoryJpa;
    private final TaxRuleJpaMapper taxJpaMapper;

    @Override
    public TaxRule findById(Integer taxRuleId) {
        TaxRuleJpa taxRuleJpa = taxRepositoryJpa.findByTaxRuleId(taxRuleId);
        return taxJpaMapper.toDomain(taxRuleJpa);
    }

    @Override
    public List<TaxRule> getAllActiveTax() {
        List<TaxRuleJpa> taxRuleJpas = taxRepositoryJpa.findAllByIsActive((byte)1);
        return taxRuleJpas.stream().map(taxJpaMapper::toDomain).toList();
    }

    @Override
    public List<TaxRule> findAllByIds(List<Integer> taxRuleIds) {
        return taxRepositoryJpa.findAllById(taxRuleIds)
                .stream()
                .map(taxJpaMapper::toDomain)
                .toList();
    }
}

