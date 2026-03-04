package com.g42.platform.gms.customer.infrastructure.mapper;

import com.g42.platform.gms.customer.domain.entity.CustomerProfile;
import com.g42.platform.gms.customer.infrastructure.entity.CustomerProfileJpa;
import org.mapstruct.Mapper;
import org.springframework.data.domain.Page;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CustomerJpaMapper{
    CustomerProfile toDomain (CustomerProfileJpa customerProfileJpa);
    List<CustomerProfile> toDomainList (List<CustomerProfileJpa> customerProfileJpaList);
    CustomerProfileJpa toJpa (CustomerProfile customerProfile);
}
