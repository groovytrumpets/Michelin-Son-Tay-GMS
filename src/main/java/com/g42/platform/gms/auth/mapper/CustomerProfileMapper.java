package com.g42.platform.gms.auth.mapper;

import com.g42.platform.gms.auth.dto.CustomerProfileResponse;
import com.g42.platform.gms.auth.entity.CustomerProfile;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CustomerProfileMapper {
    
    CustomerProfileResponse toResponse(CustomerProfile profile);
}
