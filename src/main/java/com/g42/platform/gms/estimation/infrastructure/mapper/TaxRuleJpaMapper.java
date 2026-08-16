package com.g42.platform.gms.estimation.infrastructure.mapper;

import com.g42.platform.gms.estimation.domain.entity.TaxRule;
import com.g42.platform.gms.estimation.infrastructure.entity.TaxRuleJpa;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TaxRuleJpaMapper {
    TaxRule toDomain (TaxRuleJpa taxRule);

    TaxRuleJpa toJpa(TaxRule taxRule);
}
