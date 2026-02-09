package com.g42.platform.gms.booking.customer.infrastructure.mapper;

import com.g42.platform.gms.booking.customer.domain.entity.IpBlacklist;
import com.g42.platform.gms.booking.customer.infrastructure.entity.IpBlacklistJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class IpBlacklistMapper {
    
    public IpBlacklist toDomain(IpBlacklistJpaEntity jpa) {
        if (jpa == null) {
            return null;
        }
        
        IpBlacklist domain = new IpBlacklist();
        domain.setBlacklistId(jpa.getBlacklistId());
        domain.setIpAddress(jpa.getIpAddress());
        domain.setReason(jpa.getReason());
        
        Integer blockedBy = null;
        if (jpa.getBlockedBy() != null) {
            blockedBy = (int) jpa.getBlockedBy().getStaffId();
        }
        domain.setBlockedBy(blockedBy);
        
        domain.setBlockedAt(jpa.getBlockedAt());
        domain.setIsActive(jpa.getIsActive());
        
        return domain;
    }
    
    public IpBlacklistJpaEntity toJpa(IpBlacklist domain) {
        if (domain == null) {
            return null;
        }
        
        IpBlacklistJpaEntity jpa = new IpBlacklistJpaEntity();
        jpa.setBlacklistId(domain.getBlacklistId());
        jpa.setIpAddress(domain.getIpAddress());
        jpa.setReason(domain.getReason());
        jpa.setBlockedAt(domain.getBlockedAt());
        jpa.setIsActive(domain.getIsActive());
        
        return jpa;
    }
}
