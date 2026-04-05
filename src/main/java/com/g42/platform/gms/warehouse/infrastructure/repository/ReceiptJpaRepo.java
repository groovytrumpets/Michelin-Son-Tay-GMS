package com.g42.platform.gms.warehouse.infrastructure.repository;

import com.g42.platform.gms.warehouse.infrastructure.entity.ReceiptJpa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReceiptJpaRepo extends JpaRepository<ReceiptJpa, Integer> {

    Optional<ReceiptJpa> findByIssueId(Integer issueId);

    Optional<ReceiptJpa> findByReceiptCode(String receiptCode);
}
