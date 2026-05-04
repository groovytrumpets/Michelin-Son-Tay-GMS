package com.g42.platform.gms.warehouse.infrastructure.repository;

import com.g42.platform.gms.warehouse.infrastructure.entity.InventoryJpa;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository cho Inventory.
 *
 * Cơ chế PESSIMISTIC_WRITE lock (SELECT FOR UPDATE):
 * - Khi service cần cập nhật inventory.quantity hoặc inventory.reservedQuantity,
 *   phải sử dụng findByWarehouseIdAndItemIdWithLock(...) để khóa row trong database.
 * - Khi row bị khóa, các transaction khác sẽ phải chờ đợi lock được release
 *   (sau khi commit transaction hiện tại).
 * - Điều này tránh race condition:
 *   * Nếu 2 request confirm() cùng lúc, request 1 lock row → request 2 chờ
 *   * Request 1 hoàn thành: quantity -= 100, commit → release lock
 *   * Request 2 tiếp tục: lấy quantity mới (sau trừ), quantity -= 50, commit
 *   * Kết quả: đúng, quantity -= 150 (chứ không phải -= 100 rồi tính lại)
 *
 * Ghi chú:
 * - Lock chỉ cần thiết KHI SẼ GHI DỮ LIỆU (findByWarehouseAndItemWithLock).
 * - Khi chỉ ĐỌC (không sửa), dùng findByWarehouseAndItem(...) để tránh lock thừa.
 * - Lock tự động release khi transaction commit/rollback.
 */
public interface InventoryJpaRepo extends JpaRepository<InventoryJpa, Integer> {

    /** Đọc inventory mà không khóa (dùng khi chỉ lấy thông tin, không sửa) */
    Optional<InventoryJpa> findByWarehouseIdAndItemId(Integer warehouseId, Integer itemId);

    /**
     * SELECT FOR UPDATE — khóa row trước khi cập nhật.
     * Dùng khi service sẽ tính lại quantity/reservedQuantity và save ngay.
     *
     * Ví dụ luồng:
     * 1. InventoryRepoImpl.findByWarehouseAndItemWithLock(...) → SELECT FOR UPDATE
     * 2. Service logic: inv.setReservedQuantity(inv.getReservedQuantity() + delta)
     * 3. InventoryRepoImpl.save(...) → UPDATE ... SET ...
     * 4. @Transactional commit → release lock
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM InventoryJpa i WHERE i.warehouseId = :warehouseId AND i.itemId = :itemId")
    Optional<InventoryJpa> findByWarehouseIdAndItemIdWithLock(
            @Param("warehouseId") Integer warehouseId,
            @Param("itemId") Integer itemId);

    List<InventoryJpa> findByWarehouseId(Integer warehouseId);

    @Query("SELECT i FROM InventoryJpa i WHERE i.warehouseId = :warehouseId AND (i.quantity - i.reservedQuantity) <= i.minStockLevel")
    List<InventoryJpa> findLowStockByWarehouse(@Param("warehouseId") Integer warehouseId);

    InventoryJpa getInventoryJpaByWarehouseIdAndItemId(Integer warehouseId, Integer itemId);
}
