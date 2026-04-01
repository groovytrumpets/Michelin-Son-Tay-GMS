package com.g42.platform.gms.estimation.api.internal;

import com.g42.platform.gms.estimation.domain.entity.TaxRule;

public interface TaxRuleInternalApi{
    Integer getTaxCodeFreeId(String free);

    Integer createNewFreeTax();

    TaxRule getTaxRuleById(Integer taxRuleId);
}
