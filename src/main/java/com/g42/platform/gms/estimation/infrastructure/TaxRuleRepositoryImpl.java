package com.g42.platform.gms.estimation.infrastructure;

import com.g42.platform.gms.estimation.domain.entity.TaxRule;
import com.g42.platform.gms.estimation.domain.entity.WorkCategory;
import com.g42.platform.gms.estimation.domain.repository.TaxRuleRepository;
import com.g42.platform.gms.estimation.domain.repository.WorkCategoryRepository;
import com.g42.platform.gms.estimation.infrastructure.entity.TaxRuleJpa;
import com.g42.platform.gms.estimation.infrastructure.entity.WorkCategoryJpa;
import com.g42.platform.gms.estimation.infrastructure.mapper.TaxRuleJpaMapper;
import com.g42.platform.gms.estimation.infrastructure.mapper.WorkCategoryJpaMapper;
import com.g42.platform.gms.estimation.infrastructure.repository.TaxRuleRepositoryJpa;
import com.g42.platform.gms.estimation.infrastructure.repository.WorkCategoryRepositoryJpa;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@AllArgsConstructor
public class TaxRuleRepositoryImpl implements TaxRuleRepository {
    private final TaxRuleRepositoryJpa workCategoryRepositoryJpa;
    private final TaxRuleJpaMapper workCategoryJpaMapper;

    @Override
    public TaxRule findById(Integer taxRuleId) {
        TaxRuleJpa taxRuleJpa = workCategoryRepositoryJpa.findByTaxRuleId(taxRuleId);
        return workCategoryJpaMapper.toDomain(taxRuleJpa);
    }
}

