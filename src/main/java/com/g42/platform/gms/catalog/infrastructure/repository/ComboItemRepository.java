package com.g42.platform.gms.catalog.repository;

import com.g42.platform.gms.booking.customer.infrastructure.entity.ComboItemJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComboItemRepository extends JpaRepository<ComboItemJpaEntity, Integer> {
    
    List<ComboItemJpaEntity> findByComboId(Integer comboId);
}
