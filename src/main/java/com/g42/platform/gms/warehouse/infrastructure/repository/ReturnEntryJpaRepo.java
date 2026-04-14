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
}
