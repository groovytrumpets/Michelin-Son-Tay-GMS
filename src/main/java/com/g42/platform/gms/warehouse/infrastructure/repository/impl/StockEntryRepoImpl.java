package com.g42.platform.gms.warehouse.infrastructure.repository.impl;

import com.g42.platform.gms.warehouse.domain.entity.StockEntry;
import com.g42.platform.gms.warehouse.domain.entity.StockEntryItem;
import com.g42.platform.gms.warehouse.domain.enums.StockEntryStatus;
import com.g42.platform.gms.warehouse.domain.repository.StockEntryRepo;
import com.g42.platform.gms.warehouse.infrastructure.entity.StockEntryItemJpa;
import com.g42.platform.gms.warehouse.infrastructure.entity.StockEntryJpa;
import com.g42.platform.gms.warehouse.infrastructure.repository.StockEntryItemJpaRepo;
import com.g42.platform.gms.warehouse.infrastructure.repository.StockEntryJpaRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class StockEntryRepoImpl implements StockEntryRepo {

    /*
     * Infrastructure adapter: chuyển đổi giữa JPA entities (StockEntryJpa,
     * StockEntryItemJpa) và domain entities (StockEntry, StockEntryItem).
     *
     * Ghi chú quan trọng:
     * - `save(...)` sử dụng CascadeType để persist items cùng với StockEntryJpa.
     * - Các thao tác decrease/increase remainingQuantity được delegate tới
     *   `StockEntryItemJpaRepo` và thực hiện bằng `@Modifying` UPDATE SQL để
     *   tránh Hibernate overwrite khi có cập nhật cạnh tranh (giảm risk của
     *   optimistic lock trong các thao tác confirm xuất kho).
     * - `findFifoLots(...)` trả về các lô còn hàng theo thứ tự cũ nhất trước
     *   (entryItemId ASC) — đảm bảo tính nhất quán khi service tính FIFO.
     *
     * Các service sử dụng lớp này: `StockIssueService` (tạo draft + confirm),
     * `StockEntryService` (quản lý nhập kho), và các tool export/import.
     */

    private final StockEntryJpaRepo jpaRepo;
    private final StockEntryItemJpaRepo itemJpaRepo;

    // ── mappers ──────────────────────────────────────────────────────────────

    private StockEntry toDomain(StockEntryJpa jpa) {
        return StockEntry.builder()
                .entryId(jpa.getEntryId())
                .entryCode(jpa.getEntryCode())
                .warehouseId(jpa.getWarehouseId())
                .supplierName(jpa.getSupplierName())
                .entryDate(jpa.getEntryDate())
                .status(jpa.getStatus())
                .notes(jpa.getNotes())
                .confirmedBy(jpa.getConfirmedBy())
                .confirmedAt(jpa.getConfirmedAt())
                .createdBy(jpa.getCreatedBy())
                .createdAt(jpa.getCreatedAt())
                .updatedAt(jpa.getUpdatedAt())
                .items(jpa.getItems() != null
                        ? jpa.getItems().stream().map(this::toDomainItem).toList()
                        : new ArrayList<>())
                .build();
    }

    private StockEntryJpa toJpa(StockEntry domain) {
        StockEntryJpa jpa = new StockEntryJpa();
        jpa.setEntryId(domain.getEntryId());
        jpa.setEntryCode(domain.getEntryCode());
        jpa.setWarehouseId(domain.getWarehouseId());
        jpa.setSupplierName(domain.getSupplierName());
        jpa.setEntryDate(domain.getEntryDate());
        jpa.setStatus(domain.getStatus() != null ? domain.getStatus() : StockEntryStatus.DRAFT);
        jpa.setNotes(domain.getNotes());
        jpa.setConfirmedBy(domain.getConfirmedBy());
        jpa.setConfirmedAt(domain.getConfirmedAt());
        jpa.setCreatedBy(domain.getCreatedBy());
        jpa.setCreatedAt(domain.getCreatedAt());
        jpa.setUpdatedAt(domain.getUpdatedAt());
        return jpa;
    }

    private StockEntryItem toDomainItem(StockEntryItemJpa jpa) {
        return StockEntryItem.builder()
                .entryItemId(jpa.getEntryItemId())
                .entryId(jpa.getEntryId())
                .itemId(jpa.getItemId())
                .quantity(jpa.getQuantity())
                .importPrice(jpa.getImportPrice())
                .markupMultiplier(jpa.getMarkupMultiplier())
                .remainingQuantity(jpa.getRemainingQuantity())
                .notes(jpa.getNotes())
                .build();
    }

    private StockEntryItemJpa toJpaItem(StockEntryItem domain) {
        StockEntryItemJpa jpa = new StockEntryItemJpa();
        jpa.setEntryItemId(domain.getEntryItemId());
        jpa.setEntryId(domain.getEntryId());
        jpa.setItemId(domain.getItemId());
        jpa.setQuantity(domain.getQuantity());
        jpa.setImportPrice(domain.getImportPrice());
        jpa.setMarkupMultiplier(domain.getMarkupMultiplier() != null
                ? domain.getMarkupMultiplier() : BigDecimal.ONE);
        jpa.setRemainingQuantity(domain.getRemainingQuantity() != null
                ? domain.getRemainingQuantity() : 0);
        jpa.setNotes(domain.getNotes());
        return jpa;
    }

    // ── StockEntryRepo impl ──────────────────────────────────────────────────

    @Override
    public Optional<StockEntry> findById(Integer entryId) {
        return jpaRepo.findById(entryId).map(this::toDomain);
    }

    @Override
    public List<StockEntry> findByWarehouseId(Integer warehouseId) {
        return jpaRepo.findByWarehouseIdOrderByCreatedAtDesc(warehouseId)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<StockEntry> findByWarehouseIdAndStatus(Integer warehouseId, StockEntryStatus status) {
        return jpaRepo.findByWarehouseIdAndStatusOrderByCreatedAtDesc(warehouseId, status)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public Page<StockEntry> search(Integer warehouseId,
                                   StockEntryStatus status,
                                   LocalDate fromDate,
                                   LocalDate toDate,
                                   String search,
                                   Pageable pageable) {
        String normalizedSearch = (search == null || search.isBlank()) ? null : search.trim();
        return jpaRepo.search(warehouseId, status, fromDate, toDate, normalizedSearch, pageable)
                .map(this::toDomain);
    }

    @Override
    public StockEntry save(StockEntry entry) {
        StockEntryJpa jpa = toJpa(entry);

        // Đồng bộ items vào JPA entity trước khi save để CascadeType.ALL persist
        // cả header lẫn chi tiết trong cùng một lần ghi.
        if (entry.getItems() != null) {
            jpa.getItems().clear();
            for (StockEntryItem item : entry.getItems()) {
                StockEntryItemJpa itemJpa = toJpaItem(item);
                if (itemJpa.getEntryId() == null) {
                    itemJpa.setEntryId(entry.getEntryId());
                }
                jpa.getItems().add(itemJpa);
            }
        }

        return toDomain(jpaRepo.save(jpa));
    }

    @Override
    public boolean existsByCode(String entryCode) {
        return jpaRepo.existsByEntryCode(entryCode);
    }

    @Override
    public List<StockEntryItem> findFifoLots(Integer warehouseId, Integer itemId) {
        // FIFO: lô cũ nhất trước để service confirm xuất kho theo thứ tự nhập.
        return itemJpaRepo.findFifoLots(warehouseId, itemId)
                .stream().map(this::toDomainItem).toList();
    }

    @Override
    public Optional<StockEntryItem> findLatestLot(Integer warehouseId, Integer itemId) {
        return itemJpaRepo.findLatestLot(warehouseId, itemId)
                .stream().findFirst().map(this::toDomainItem);
    }

    @Override
    public StockEntryItem saveItem(StockEntryItem item) {
        return toDomainItem(itemJpaRepo.save(toJpaItem(item)));
    }

    @Override
    public Optional<StockEntryItem> findItemById(Integer entryItemId) {
        return itemJpaRepo.findById(entryItemId).map(this::toDomainItem);
    }

    @Override
    public int decreaseRemainingQuantity(Integer entryItemId, int qty) {
        // Delegate về UPDATE query để giảm remainingQuantity an toàn khi confirm.
        return itemJpaRepo.decreaseRemainingQuantity(entryItemId, qty);
    }

    @Override
    public int increaseRemainingQuantity(Integer entryItemId, int qty) {
        // Delegate về UPDATE query cho luồng hoàn trả / cộng lại số lượng lô.
        return itemJpaRepo.increaseRemainingQuantity(entryItemId, qty);
    }

    @Override
    public BigDecimal findLatesFallBackPrice(Integer itemId, Integer warehouseId) {
        return jpaRepo.findLatesFallBackPrice(itemId, warehouseId).orElse(null);
    }

    @Override
    public Optional<StockEntry> findByEntryCode(String entryCode) {
        return jpaRepo.findByEntryCode(entryCode).map(this::toDomain);
    }

    @Override
    public List<StockEntryItem> findActiveLotsByWarehouse(Integer warehouseId) {
        return itemJpaRepo.findActiveLotsByWarehouse(warehouseId)
                .stream().map(this::toDomainItem).toList();
    }

    @Override
    public void invalidateSyncLotsByWarehouse(Integer warehouseId) {
        itemJpaRepo.invalidateSyncLotsByWarehouse(warehouseId);
    }

    @Override
    public Optional<StockEntry> findEntryById(Integer entryId) {
        return jpaRepo.findById(entryId).map(this::toDomain);
    }
}
