package com.g42.platform.gms.itemreview.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ItemReviewJpaRepo extends JpaRepository<ItemReviewJpa, Integer> {
    Optional<ItemReviewJpa> findByCatalogItemIdAndCustomerId(Integer catalogItemId, Integer customerId);
}
