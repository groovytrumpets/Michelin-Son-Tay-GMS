package com.g42.platform.gms.billing.infrastructure.mapper;

import com.g42.platform.gms.billing.domain.entity.PaymentTransaction;
import com.g42.platform.gms.billing.infrastructure.entity.PaymentTransactionJpa;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PaymentTransactionJpaMapper {
    PaymentTransaction toDomain(PaymentTransactionJpa s);
    PaymentTransactionJpa toJpa (PaymentTransaction s);
}
