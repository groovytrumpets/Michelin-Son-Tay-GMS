package com.g42.platform.gms.booking.customer.infrastructure.jpa;

import com.g42.platform.gms.booking.customer.infrastructure.entity.IpBlacklistJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IpBlacklistJpaRepository extends JpaRepository<IpBlacklistJpaEntity, Integer> {
    
    Optional<IpBlacklistJpaEntity> findByIpAddressAndIsActiveTrue(String ipAddress);
    
    boolean existsByIpAddressAndIsActiveTrue(String ipAddress);
}
