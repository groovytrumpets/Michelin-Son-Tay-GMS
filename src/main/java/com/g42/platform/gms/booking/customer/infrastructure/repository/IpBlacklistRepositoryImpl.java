package com.g42.platform.gms.booking.customer.infrastructure.repository;

import com.g42.platform.gms.auth.entity.StaffProfile;
import com.g42.platform.gms.auth.repository.StaffProfileRepo;
import com.g42.platform.gms.booking.customer.domain.entity.IpBlacklist;
import com.g42.platform.gms.booking.customer.domain.repository.IpBlacklistRepository;
import com.g42.platform.gms.booking.customer.infrastructure.entity.IpBlacklistJpaEntity;
import com.g42.platform.gms.booking.customer.infrastructure.jpa.IpBlacklistJpaRepository;
import com.g42.platform.gms.booking.customer.infrastructure.mapper.IpBlacklistMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class IpBlacklistRepositoryImpl implements IpBlacklistRepository {
    
    private final IpBlacklistJpaRepository jpaRepository;
    private final IpBlacklistMapper mapper;
    private final StaffProfileRepo staffProfileRepo;
    
    @Override
    public IpBlacklist save(IpBlacklist domain) {
        IpBlacklistJpaEntity jpa = mapper.toJpa(domain);
        
        if (domain.getBlockedBy() != null) {
            StaffProfile staff = staffProfileRepo.findById(domain.getBlockedBy())
                .orElse(null);
            jpa.setBlockedBy(staff);
        }
        
        IpBlacklistJpaEntity saved = jpaRepository.save(jpa);
        IpBlacklist result = mapper.toDomain(saved);
        result.initializeDefaults();
        return result;
    }
    
    @Override
    public Optional<IpBlacklist> findByIpAddressAndIsActiveTrue(String ipAddress) {
        return jpaRepository.findByIpAddressAndIsActiveTrue(ipAddress)
            .map(mapper::toDomain)
            .map(blacklist -> {
                blacklist.initializeDefaults();
                return blacklist;
            });
    }
    
    @Override
    public boolean existsByIpAddressAndIsActiveTrue(String ipAddress) {
        return jpaRepository.existsByIpAddressAndIsActiveTrue(ipAddress);
    }
}
