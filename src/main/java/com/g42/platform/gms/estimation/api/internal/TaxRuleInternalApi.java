package com.g42.platform.gms.estimation.api.internal;

public interface TaxRuleInternalApi{
    Integer getTaxCodeFreeId(String free);

    Integer createNewFreeTax();
}
