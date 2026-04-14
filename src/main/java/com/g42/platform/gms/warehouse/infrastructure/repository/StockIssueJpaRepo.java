package com.g42.platform.gms.warehouse.infrastructure.repository;

import com.g42.platform.gms.warehouse.infrastructure.entity.StockIssueJpa;
import com.g42.platform.gms.warehouse.domain.enums.IssueType;
import com.g42.platform.gms.warehouse.domain.enums.StockIssueStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StockIssueJpaRepo extends JpaRepository<StockIssueJpa, Integer> {

    Optional<StockIssueJpa> findByIssueCode(String issueCode);

    List<StockIssueJpa> findByServiceTicketId(Integer serviceTicketId);

    List<StockIssueJpa> findByWarehouseIdOrderByCreatedAtDesc(Integer warehouseId);

    boolean existsByIssueCode(String issueCode);

    boolean existsByServiceTicketIdAndIssueTypeAndStatus(
            Integer serviceTicketId,
            IssueType issueType,
            StockIssueStatus status);
}
