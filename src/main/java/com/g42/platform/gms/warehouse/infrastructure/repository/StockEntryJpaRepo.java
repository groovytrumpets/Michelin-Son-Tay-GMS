package com.g42.platform.gms.warehouse.infrastructure.repository;

import com.g42.platform.gms.warehouse.domain.enums.StockEntryStatus;
import com.g42.platform.gms.warehouse.infrastructure.entity.StockEntryJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface StockEntryJpaRepo extends JpaRepository<StockEntryJpa, Integer> {

    Optional<StockEntryJpa> findByEntryCode(String entryCode);

    boolean existsByEntryCode(String entryCode);

    List<StockEntryJpa> findByWarehouseIdOrderByCreatedAtDesc(Integer warehouseId);

    List<StockEntryJpa> findByWarehouseIdAndStatusOrderByCreatedAtDesc(Integer warehouseId, StockEntryStatus status);
    @Query("""
    select se.importPrice*se.markupMultiplier from StockEntryItemJpa se join StockEntryJpa s on se.entryId = s.entryId
        where s.warehouseId=:warehouseId and se.itemId=:itemId order by s.createdAt desc
    """)
    BigDecimal findLatesFallBackPrice(Integer itemId, Integer warehouseId);
}
