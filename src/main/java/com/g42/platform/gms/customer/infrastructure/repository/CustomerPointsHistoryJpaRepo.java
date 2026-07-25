package com.g42.platform.gms.customer.infrastructure.repository;

import com.g42.platform.gms.customer.infrastructure.entity.CustomerPointsHistoryJpa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerPointsHistoryJpaRepo extends JpaRepository<CustomerPointsHistoryJpa, Integer> {
    Page<CustomerPointsHistoryJpa> findByCustomerIdOrderByCreatedAtDesc(Integer customerId, Pageable pageable);
    long countByCustomerIdAndReason(Integer customerId, String reason);
}
