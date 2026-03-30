package com.g42.platform.gms.customer.infrastructure.mapper;

import com.g42.platform.gms.customer.domain.entity.CustomerAuth;
import com.g42.platform.gms.customer.domain.entity.CustomerProfile;
import com.g42.platform.gms.customer.infrastructure.entity.CustomerAuthJpa;
import com.g42.platform.gms.customer.infrastructure.entity.CustomerProfileJpa;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CustomerAuthJpaMapper {
    CustomerAuthJpa toDomain (CustomerAuth customerProfile);
    CustomerAuth toJpa (CustomerAuthJpa customerProfile);
}
