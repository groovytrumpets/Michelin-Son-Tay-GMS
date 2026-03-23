package com.g42.platform.gms.billing.infrastructure.mapper;

import com.g42.platform.gms.billing.domain.entity.PaymentTransaction;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PaymentTransactionJpa {
    PaymentTransaction toDomain(PaymentTransactionJpa s);
}
