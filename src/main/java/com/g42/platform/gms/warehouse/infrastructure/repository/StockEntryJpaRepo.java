package com.g42.platform.gms.warehouse.infrastructure.repository;

import com.g42.platform.gms.warehouse.domain.enums.StockEntryStatus;
import com.g42.platform.gms.warehouse.infrastructure.entity.StockEntryJpa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StockEntryJpaRepo extends JpaRepository<StockEntryJpa, Integer> {

    Optional<StockEntryJpa> findByEntryCode(String entryCode);

    boolean existsByEntryCode(String entryCode);

    List<StockEntryJpa> findByWarehouseIdOrderByCreatedAtDesc(Integer warehouseId);

    List<StockEntryJpa> findByWarehouseIdAndStatusOrderByCreatedAtDesc(Integer warehouseId, StockEntryStatus status);

        @Query("""
        select e from StockEntryJpa e
        where e.warehouseId = :warehouseId
            and (:status is null or e.status = :status)
            and (:fromDate is null or e.entryDate >= :fromDate)
            and (:toDate is null or e.entryDate <= :toDate)
            and (
                :search is null
                or lower(e.entryCode) like lower(concat('%', :search, '%'))
                or lower(e.supplierName) like lower(concat('%', :search, '%'))
            )
        """)
        Page<StockEntryJpa> search(
                        @Param("warehouseId") Integer warehouseId,
                        @Param("status") StockEntryStatus status,
                        @Param("fromDate") LocalDate fromDate,
                        @Param("toDate") LocalDate toDate,
                        @Param("search") String search,
                        Pageable pageable);

    @Query("""
    select se.importPrice*se.markupMultiplier from StockEntryItemJpa se join StockEntryJpa s on se.entryId = s.entryId
        where s.warehouseId=:warehouseId and se.itemId=:itemId 
            and se.remainingQuantity> 0
                and s.status = 'CONFIRMED'
                order by s.createdAt asc limit 1
    """)
    Optional<BigDecimal> findLatesFallBackPrice(Integer itemId, Integer warehouseId);
}
