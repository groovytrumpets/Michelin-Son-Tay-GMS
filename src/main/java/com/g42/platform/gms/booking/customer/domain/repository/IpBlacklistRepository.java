package com.g42.platform.gms.booking.customer.domain.repository;

import com.g42.platform.gms.booking.customer.domain.entity.IpBlacklist;

import java.util.Optional;

public interface IpBlacklistRepository {
    IpBlacklist save(IpBlacklist ipBlacklist);
    Optional<IpBlacklist> findByIpAddressAndIsActiveTrue(String ipAddress);
    boolean existsByIpAddressAndIsActiveTrue(String ipAddress);
}
