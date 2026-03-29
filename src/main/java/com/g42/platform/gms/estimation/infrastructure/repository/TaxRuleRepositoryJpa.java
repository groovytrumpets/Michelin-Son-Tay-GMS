package com.g42.platform.gms.estimation.infrastructure.repository;

import com.g42.platform.gms.estimation.infrastructure.entity.TaxRuleJpa;
import com.g42.platform.gms.estimation.infrastructure.entity.WorkCategoryJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface TaxRuleRepositoryJpa extends JpaRepository<TaxRuleJpa, Integer> {
    TaxRuleJpa findByTaxRuleId(Integer taxRuleId);

    List<TaxRuleJpa> findAllByIsActive(Byte isActive);

    boolean existsByTaxCode(String taxCode);

    TaxRuleJpa findByTaxCode(String taxCode);
}
