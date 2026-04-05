package com.g42.platform.gms.warehouse.infrastructure.repository;

import com.g42.platform.gms.warehouse.infrastructure.entity.StockIssueJpa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StockIssueJpaRepo extends JpaRepository<StockIssueJpa, Integer> {

    Optional<StockIssueJpa> findByIssueCode(String issueCode);

    List<StockIssueJpa> findByServiceTicketId(Integer serviceTicketId);

    boolean existsByIssueCode(String issueCode);
}
