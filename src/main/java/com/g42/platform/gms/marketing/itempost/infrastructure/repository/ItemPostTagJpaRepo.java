package com.g42.platform.gms.marketing.itempost.infrastructure.repository;

import com.g42.platform.gms.marketing.itempost.infrastructure.entity.ItemPostTagJpa;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ItemPostTagJpaRepo extends JpaRepository<ItemPostTagJpa, Integer> {

    Optional<ItemPostTagJpa> findBySlug(String slug);

    List<ItemPostTagJpa> findByOrderByUsageCountDescNameAsc(Pageable pageable);
}
