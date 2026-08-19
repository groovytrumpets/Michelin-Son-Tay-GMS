package com.g42.platform.gms.warehouse.infrastructure.repository;

import com.g42.platform.gms.warehouse.infrastructure.entity.ItemColorJpa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItemColorJpaRepo extends JpaRepository<ItemColorJpa, Integer> {
    List<ItemColorJpa> findByItemIdOrderByDisplayOrderAscItemColorIdAsc(Integer itemId);

    void deleteByItemId(Integer itemId);
}
