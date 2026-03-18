package com.g42.platform.gms.estimation.domain.repository;

import com.g42.platform.gms.estimation.domain.entity.TaxRule;
import com.g42.platform.gms.estimation.domain.entity.WorkCategory;

import java.util.List;

public interface TaxRuleRepository {
    TaxRule findById(Integer taxRuleId);

    List<TaxRule> getAllActiveTax();
}
