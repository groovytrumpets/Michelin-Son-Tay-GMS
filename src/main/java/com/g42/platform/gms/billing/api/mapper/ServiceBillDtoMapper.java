package com.g42.platform.gms.billing.api.mapper;

import com.g42.platform.gms.billing.api.dto.ServiceBillDto;
import com.g42.platform.gms.billing.domain.entity.ServiceBill;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ServiceBillDtoMapper {
    ServiceBillDto mapToDto(ServiceBill serviceBill);
    ServiceBill mapToEntity(ServiceBillDto serviceBillDto);
}
