package com.g42.platform.gms.warehouse.infrastructure.repository.impl;

import com.g42.platform.gms.warehouse.domain.entity.InventoryTransaction;
import com.g42.platform.gms.warehouse.domain.repository.InventoryTransactionRepo;
import com.g42.platform.gms.warehouse.infrastructure.entity.InventoryTransactionJpa;
import com.g42.platform.gms.warehouse.infrastructure.repository.InventoryTransactionJpaRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * Infrastructure Adapter: implements InventoryTransactionRepo (domain port).
 *
 * Bảng inventory_transaction là audit log — chỉ INSERT, không bao giờ UPDATE/DELETE.
 * Mỗi lần tồn kho thay đổi (nhập/xuất/trả) đều ghi 1 bản ghi vào đây.
 *
 * Cấu trúc 1 bản ghi audit:
 *   - warehouseId + itemId: sản phẩm nào trong kho nào
 *   - transactionType: IN (nhập) / OUT (xuất) / RETURN (trả)
 *   - quantity: số lượng thay đổi trong lần này
 *   - balanceAfter: tồn kho SAU khi thay đổi (snapshot để tra cứu nhanh)
 *   - referenceType + referenceId: chứng từ gốc (stock_entry / stock_issue / return_entry)
 *   - createdById + createdAt: ai làm, lúc nào
 *
 * Luồng ghi audit khi confirm nhập kho:
 *   StockEntryService.confirm()
 *     → increaseInventory(warehouseId, item, entryId, staffId)
 *       → inventoryRepo.save(inventory)  ← cập nhật tồn kho
 *       → saveInventoryTransaction(...)  ← ghi audit log
 *         → transactionRepo.save(tx)
 *           → InventoryTransactionRepoImpl.save(tx)
 *             → toJpa(tx) → InventoryTransactionJpa
 *             → jpaRepo.save(jpa)
 *             → SQL: INSERT INTO inventory_transaction (...)
 *             → toDomain(saved) → trả về với transactionId đã được DB sinh ra
 */
@Repository
@RequiredArgsConstructor
public class InventoryTransactionRepoImpl implements InventoryTransactionRepo {

    private final InventoryTransactionJpaRepo jpaRepo;

    /**
     * Ghi 1 bản ghi audit log.
     * SQL: INSERT INTO inventory_transaction (warehouse_id, item_id, transaction_type,
     *        quantity, balance_after, reference_type, reference_id, created_by_id, created_at)
     *      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
     * Trả về: InventoryTransaction với transactionId đã được DB sinh ra (auto-increment).
     */
    @Override
    public InventoryTransaction save(InventoryTransaction transaction) {
        InventoryTransactionJpa saved = jpaRepo.save(toJpa(transaction));
        return toDomain(saved);
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    /** InventoryTransaction domain → InventoryTransactionJpa */
    private InventoryTransactionJpa toJpa(InventoryTransaction domain) {
        InventoryTransactionJpa jpa = new InventoryTransactionJpa();
        jpa.setTransactionId(domain.getTransactionId());
        jpa.setWarehouseId(domain.getWarehouseId());
        jpa.setItemId(domain.getItemId());
        jpa.setEntryItemId(domain.getEntryItemId());     // ID lô hàng (nếu có)
        jpa.setTransactionType(domain.getTransactionType()); // IN / OUT / RETURN
        jpa.setQuantity(domain.getQuantity());           // số lượng thay đổi
        jpa.setBalanceAfter(domain.getBalanceAfter());   // tồn kho sau thay đổi
        jpa.setReferenceType(domain.getReferenceType()); // "stock_entry" / "stock_issue" / ...
        jpa.setReferenceId(domain.getReferenceId());     // ID chứng từ gốc
        jpa.setNotes(domain.getNotes());
        jpa.setCreatedById(domain.getCreatedById());
        jpa.setCreatedAt(domain.getCreatedAt());
        return jpa;
    }

    /** InventoryTransactionJpa → InventoryTransaction domain */
    private InventoryTransaction toDomain(InventoryTransactionJpa jpa) {
        InventoryTransaction domain = new InventoryTransaction();
        domain.setTransactionId(jpa.getTransactionId());
        domain.setWarehouseId(jpa.getWarehouseId());
        domain.setItemId(jpa.getItemId());
        domain.setEntryItemId(jpa.getEntryItemId());
        domain.setTransactionType(jpa.getTransactionType());
        domain.setQuantity(jpa.getQuantity());
        domain.setBalanceAfter(jpa.getBalanceAfter());
        domain.setReferenceType(jpa.getReferenceType());
        domain.setReferenceId(jpa.getReferenceId());
        domain.setNotes(jpa.getNotes());
        domain.setCreatedById(jpa.getCreatedById());
        domain.setCreatedAt(jpa.getCreatedAt());
        return domain;
    }
}
