package com.g42.platform.gms.staff.profile.api.mapper;

import com.g42.platform.gms.staff.profile.api.dto.RoleDto;
import com.g42.platform.gms.staff.profile.api.dto.StaffProfileDto;
import com.g42.platform.gms.staff.profile.domain.entity.Role;
import com.g42.platform.gms.staff.profile.domain.entity.StaffProfile;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface StaffProfileDtoMapper {
    StaffProfileDto toStaffProfileDto(StaffProfile staffProfile);
    StaffProfile toStaffProfile(StaffProfileDto staffProfileDto);
    RoleDto toDto(Role role);
}
