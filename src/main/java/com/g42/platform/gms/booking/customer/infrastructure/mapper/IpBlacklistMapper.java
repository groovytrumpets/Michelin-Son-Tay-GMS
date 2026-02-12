package com.g42.platform.gms.booking.customer.infrastructure.mapper;

import com.g42.platform.gms.booking.customer.domain.entity.IpBlacklist;
import com.g42.platform.gms.booking.customer.infrastructure.entity.IpBlacklistJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface IpBlacklistMapper {

    @Mapping(target = "blockedBy",
            expression = "java(jpa.getBlockedBy() != null ? (int) jpa.getBlockedBy().getStaffId() : null)")
    IpBlacklist toDomain(IpBlacklistJpaEntity jpa);

    @Mapping(target = "blockedBy", ignore = true)
    IpBlacklistJpaEntity toJpa(IpBlacklist domain);
}
