package com.g42.platform.gms.customer.api.mapper;


import com.g42.platform.gms.customer.api.dto.CustomerCreateDto;
import com.g42.platform.gms.customer.domain.entity.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface CustomerDtoMapper {
    @Mapping(source = "customerProfile.fullName", target = "fullName")
    @Mapping(source = "customerProfile.phone", target = "phone")
    @Mapping(source = "customerProfile.email", target = "email")
    @Mapping(source = "customerProfile.gender", target = "gender")
    @Mapping(source = "customerProfile.avatar", target = "avatar")
    @Mapping(source = "customerProfile.dob", target = "dob", dateFormat = "yyyy-MM-dd")
    CustomerCreateDto toCusCreateDto(CustomerProfile customerProfile, CustomerAuth customerAuth);
}
