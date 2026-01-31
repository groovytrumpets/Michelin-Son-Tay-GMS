package com.g42.platform.gms.auth.mapper;

import com.g42.platform.gms.auth.dto.StaffAuthDto;
import com.g42.platform.gms.auth.entity.Staffauth;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface StaffAuthMapper {
    StaffAuthDto toDto(Staffauth staffAuth);
}
