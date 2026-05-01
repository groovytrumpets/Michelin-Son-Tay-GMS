package com.g42.platform.gms.warehouse.infrastructure.repository;

import com.g42.platform.gms.warehouse.domain.enums.ReturnEntryStatus;
import com.g42.platform.gms.warehouse.domain.enums.ReturnType;
import com.g42.platform.gms.warehouse.infrastructure.entity.ReturnEntryJpa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReturnEntryJpaRepo extends JpaRepository<ReturnEntryJpa, Integer> {

    Optional<ReturnEntryJpa> findByReturnCode(String returnCode);

    List<ReturnEntryJpa> findByWarehouseIdOrderByCreatedAtDesc(Integer warehouseId);

    @Query("""
        select r from ReturnEntryJpa r
        where r.warehouseId = :warehouseId
            and (:status is null or r.status = :status)
            and (:returnType is null or r.returnType = :returnType)
            and (:fromDateTime is null or r.createdAt >= :fromDateTime)
            and (:toDateTime is null or r.createdAt <= :toDateTime)
            and (
                :search is null
                or lower(r.returnCode) like lower(concat('%', :search, '%'))
                or lower(r.returnReason) like lower(concat('%', :search, '%'))
                or str(r.sourceIssueId) like concat('%', :search, '%')
            )
        """)
    Page<ReturnEntryJpa> search(
            @Param("warehouseId") Integer warehouseId,
            @Param("status") ReturnEntryStatus status,
            @Param("returnType") ReturnType returnType,
            @Param("fromDateTime") java.time.LocalDateTime fromDateTime,
            @Param("toDateTime") java.time.LocalDateTime toDateTime,
            @Param("search") String search,
            Pageable pageable);

    boolean existsByReturnCode(String returnCode);

    @Query(value = """
                select count(*)
                from return_entry_item ri
                join return_entry r on r.return_id = ri.return_id
                where ri.source_issue_item_id = :sourceIssueItemId
                    and r.status in ('SUBMITTED', 'CONFIRMED')
                """, nativeQuery = true)
    Long countActiveBySourceIssueItemId(@Param("sourceIssueItemId") Integer sourceIssueItemId);

    @Query(value = """
                select count(*)
                from return_entry_item ri
                join return_entry r on r.return_id = ri.return_id
                where ri.allocation_id = :allocationId
                    and r.status in ('SUBMITTED', 'CONFIRMED')
                """, nativeQuery = true)
    Long countActiveByAllocationId(@Param("allocationId") Integer allocationId);

    @Query(value = """
            select count(*)
            from return_entry_item ri
            where ri.source_issue_item_id = :sourceIssueItemId
            """, nativeQuery = true)
    Long countAnyBySourceIssueItemId(@Param("sourceIssueItemId") Integer sourceIssueItemId);

    @Query(value = """
            select coalesce(sum(ri.quantity), 0)
            from return_entry_item ri
            join return_entry r on r.return_id = ri.return_id
            where ri.allocation_id = :allocationId
              and r.status in ('SUBMITTED', 'CONFIRMED')
            """, nativeQuery = true)
    Long sumActiveReturnedQuantityByAllocationId(@Param("allocationId") Integer allocationId);

        @Query(value = """
            select count(*)
            from return_entry_item ri
            where ri.allocation_id = :allocationId
            """, nativeQuery = true)
        Long countAnyByAllocationId(@Param("allocationId") Integer allocationId);

    @Query(value = """
            select count(*)
            from return_entry_item ri
            where ri.source_issue_item_id = :sourceIssueItemId
              and ri.return_id <> :returnId
            """, nativeQuery = true)
    Long countAnyBySourceIssueItemIdExcludingReturnId(@Param("sourceIssueItemId") Integer sourceIssueItemId,
                                                      @Param("returnId") Integer returnId);

        @Query(value = """
                        select count(*)
                        from return_entry_item ri
                        where ri.allocation_id = :allocationId
                            and ri.return_id <> :returnId
                        """, nativeQuery = true)
        Long countAnyByAllocationIdExcludingReturnId(@Param("allocationId") Integer allocationId,
                                                                                                 @Param("returnId") Integer returnId);
}
