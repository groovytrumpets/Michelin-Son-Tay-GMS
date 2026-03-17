package com.g42.platform.gms.staff.profile.infrastructure.mapper;

import com.g42.platform.gms.staff.profile.domain.entity.Role;
import com.g42.platform.gms.staff.profile.domain.entity.StaffProfile;
import com.g42.platform.gms.staff.profile.infrastructure.entity.RoleJpa;
import com.g42.platform.gms.staff.profile.infrastructure.entity.StaffProfileJpa;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StaffProfileJpaMapper {
    @Mapping(source = "staffAuth.email", target = "email")
    @Mapping(source = "staffAuth.status", target = "status")
    StaffProfile toDomain(StaffProfileJpa domain);
    Role toRoleDomain(RoleJpa domain);
}
