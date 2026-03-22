package com.g42.platform.gms.estimation.domain.repository;

import com.g42.platform.gms.estimation.domain.entity.TaxRule;
import com.g42.platform.gms.estimation.domain.entity.WorkCategory;

import java.util.List;
import java.util.Map;

public interface TaxRuleRepository {
    TaxRule findById(Integer taxRuleId);

    List<TaxRule> getAllActiveTax();

    List<TaxRule> findAllByIds(List<Integer> taxRuleIds);
}
