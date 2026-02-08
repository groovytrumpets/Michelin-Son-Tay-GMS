package com.g42.platform.gms.booking.customer.repository;

import com.g42.platform.gms.booking.customer.entity.IpBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IpBlacklistRepository extends JpaRepository<IpBlacklist, Integer> {
    
    Optional<IpBlacklist> findByIpAddressAndIsActiveTrue(String ipAddress);
    
    boolean existsByIpAddressAndIsActiveTrue(String ipAddress);
}
