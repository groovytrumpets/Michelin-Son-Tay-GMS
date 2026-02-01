package com.g42.platform.gms.auth.mapper;

import com.g42.platform.gms.auth.dto.StaffAuthDto;
import com.g42.platform.gms.auth.entity.StaffAuth;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface StaffAuthMapper {
    StaffAuthDto toDto(StaffAuth staffAuth);
}
