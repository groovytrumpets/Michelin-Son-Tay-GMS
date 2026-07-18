package com.g42.platform.gms.dashboard.infrastructure.repository;

import com.g42.platform.gms.dashboard.infrastructure.entity.KpiConfigJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface KpiConfigRepository extends JpaRepository<KpiConfigJpa, Integer> {
    Optional<KpiConfigJpa> findByRoleName(String roleName);
}
