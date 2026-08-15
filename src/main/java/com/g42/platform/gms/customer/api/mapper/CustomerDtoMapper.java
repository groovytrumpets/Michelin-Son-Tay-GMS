package com.g42.platform.gms.customer.api.mapper;


import com.g42.platform.gms.booking_management.api.dto.CustomerDto;
import com.g42.platform.gms.customer.api.dto.CustomerCreateDto;
import com.g42.platform.gms.customer.domain.entity.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface CustomerDtoMapper {
    @Mapping(source = "customerProfile.customerId", target = "customerId")
    @Mapping(source = "customerProfile.fullName", target = "fullName")
    @Mapping(source = "customerProfile.phone", target = "phone")
    @Mapping(source = "customerProfile.email", target = "email")
    @Mapping(source = "customerProfile.gender", target = "gender")
    @Mapping(source = "customerProfile.avatar", target = "avatar")
    @Mapping(source = "customerProfile.dob", target = "dob", dateFormat = "yyyy-MM-dd")
    @Mapping(source = "customerProfile.customerType", target = "customerType")
    @Mapping(source = "customerProfile.isDealer", target = "isDealer")
    @Mapping(source = "customerProfile.isCompany", target = "isCompany")
    @Mapping(source = "customerProfile.companyName", target = "companyName")
    @Mapping(source = "customerProfile.notificationChannel", target = "notificationChannel")
    @Mapping(source = "customerProfile.customerCode", target = "customerCode")
    @Mapping(source = "customerProfile.taxCode", target = "taxCode")
    @Mapping(source = "customerProfile.provinceId", target = "provinceId")
    @Mapping(source = "customerProfile.provinceName", target = "provinceName")
    @Mapping(source = "customerProfile.districtId", target = "districtId")
    @Mapping(source = "customerProfile.districtName", target = "districtName")
    @Mapping(source = "customerProfile.wardId", target = "wardId")
    @Mapping(source = "customerProfile.wardName", target = "wardName")
    @Mapping(source = "customerProfile.address", target = "address")
    @Mapping(source = "customerProfile.identityCard", target = "identityCard")
    @Mapping(source = "customerProfile.idIssueDate", target = "idIssueDate")
    @Mapping(source = "customerProfile.idIssuePlace", target = "idIssuePlace")
    @Mapping(source = "customerProfile.customerGroupId", target = "customerGroupId")
    @Mapping(source = "customerProfile.note", target = "note")
    @Mapping(source = "customerProfile.representativeName", target = "representativeName")
    @Mapping(source = "customerProfile.repIdentityCard", target = "repIdentityCard")
    @Mapping(source = "customerProfile.position", target = "position")
    @Mapping(source = "customerProfile.contractNumber", target = "contractNumber")
    @Mapping(source = "customerProfile.contractDate", target = "contractDate")
    @Mapping(source = "customerProfile.bankAccountInfo", target = "bankAccountInfo")
    @Mapping(source = "customerProfile.latitude", target = "latitude")
    @Mapping(source = "customerProfile.longitude", target = "longitude")
    @Mapping(source = "customerProfile.contactName", target = "contactName")
    @Mapping(source = "customerProfile.contactPhone", target = "contactPhone")
    @Mapping(source = "customerProfile.contactEmail", target = "contactEmail")
    @Mapping(source = "customerProfile.contactAddress", target = "contactAddress")
    CustomerCreateDto toCusCreateDto(CustomerProfile customerProfile, CustomerAuth customerAuth);

    CustomerDto toCustomerDto(com.g42.platform.gms.auth.entity.CustomerProfile customerProfile);
    // Force MapStruct recompilation - updated mapping
}
