package com.g42.platform.gms.warehouse.infrastructure.repository;

import com.g42.platform.gms.warehouse.domain.enums.IssueType;
import com.g42.platform.gms.warehouse.infrastructure.entity.DiscountConfigJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DiscountConfigJpaRepo extends JpaRepository<DiscountConfigJpa, Integer> {

    @Query("""
        SELECT d FROM DiscountConfigJpa d
        WHERE d.isActive = true
        AND (d.itemId = :itemId OR d.itemId IS NULL)
        AND (d.issueType = :issueType OR d.issueType IS NULL)
        ORDER BY d.itemId DESC NULLS LAST, d.issueType DESC NULLS LAST
    """)
    List<DiscountConfigJpa> findActiveByItemIdAndIssueType(
            @Param("itemId") Integer itemId,
            @Param("issueType") IssueType issueType);
}
