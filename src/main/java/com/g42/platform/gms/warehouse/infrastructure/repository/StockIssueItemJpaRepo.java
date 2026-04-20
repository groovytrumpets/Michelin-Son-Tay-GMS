package com.g42.platform.gms.warehouse.infrastructure.repository;

import com.g42.platform.gms.warehouse.infrastructure.entity.StockIssueItemJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockIssueItemJpaRepo extends JpaRepository<StockIssueItemJpa, Integer> {

    @Modifying
    @Query("DELETE FROM StockIssueItemJpa i WHERE i.issueId = :issueId")
    void deleteByIssueId(@Param("issueId") Integer issueId);

    List<StockIssueItemJpa> findByIssueId(Integer issueId);
}
