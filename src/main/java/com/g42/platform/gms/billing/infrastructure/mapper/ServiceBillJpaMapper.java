package com.g42.platform.gms.billing.infrastructure.mapper;

import com.g42.platform.gms.billing.domain.entity.ServiceBill;
import com.g42.platform.gms.billing.infrastructure.entity.ServiceBillJpa;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ServiceBillJpaMapper {
    ServiceBill toDomain(ServiceBillJpa s);
    ServiceBillJpa toEntity(ServiceBill s);
}
