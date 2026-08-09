package com.g42.platform.gms.warehouse.infrastructure.repository;

import com.g42.platform.gms.warehouse.infrastructure.entity.CatalogItemCompatJpa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CatalogItemCompatJpaRepo extends JpaRepository<CatalogItemCompatJpa, Integer> {

    List<CatalogItemCompatJpa> findByItemId(Integer itemId);

    void deleteByItemId(Integer itemId);
}
