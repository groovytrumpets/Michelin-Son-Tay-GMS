package com.g42.platform.gms.warehouse.infrastructure.repository;

import com.g42.platform.gms.warehouse.infrastructure.entity.StockIssueJpa;
import com.g42.platform.gms.warehouse.domain.enums.IssueType;
import com.g42.platform.gms.warehouse.domain.enums.StockIssueStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StockIssueJpaRepo extends JpaRepository<StockIssueJpa, Integer> {

    Optional<StockIssueJpa> findByIssueCode(String issueCode);

    List<StockIssueJpa> findByServiceTicketId(Integer serviceTicketId);

    List<StockIssueJpa> findByWarehouseIdOrderByCreatedAtDesc(Integer warehouseId);

        @Query("""
        select i from StockIssueJpa i
        where i.warehouseId = :warehouseId
            and (:status is null or i.status = :status)
            and (:issueType is null or i.issueType = :issueType)
            and (:fromDateTime is null or i.createdAt >= :fromDateTime)
            and (:toDateTime is null or i.createdAt <= :toDateTime)
            and (
                :search is null
                or lower(i.issueCode) like lower(concat('%', :search, '%'))
                or lower(i.issueReason) like lower(concat('%', :search, '%'))
                or str(i.serviceTicketId) like concat('%', :search, '%')
            )
        """)
        Page<StockIssueJpa> search(
                        @Param("warehouseId") Integer warehouseId,
                        @Param("status") StockIssueStatus status,
                        @Param("issueType") IssueType issueType,
                        @Param("fromDateTime") java.time.LocalDateTime fromDateTime,
                        @Param("toDateTime") java.time.LocalDateTime toDateTime,
                        @Param("search") String search,
                        Pageable pageable);

    boolean existsByIssueCode(String issueCode);

    boolean existsByServiceTicketIdAndIssueTypeAndStatus(
            Integer serviceTicketId,
            IssueType issueType,
            StockIssueStatus status);

        boolean existsByServiceTicketIdAndWarehouseIdAndIssueTypeAndStatus(
            Integer serviceTicketId,
            Integer warehouseId,
            IssueType issueType,
            StockIssueStatus status);
}
