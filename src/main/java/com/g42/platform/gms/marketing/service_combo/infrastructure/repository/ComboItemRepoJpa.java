package com.g42.platform.gms.marketing.service_combo.infrastructure.repository;

import com.g42.platform.gms.marketing.service_combo.domain.entity.ComboItem;
import com.g42.platform.gms.marketing.service_combo.infrastructure.entity.ComboItemJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ComboItemRepoJpa extends JpaRepository<ComboItemJpa,Integer> {
}
