package com.g42.platform.gms.warehouse.infrastructure.repository;

import com.g42.platform.gms.warehouse.infrastructure.entity.StockIssueItemJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockIssueItemJpaRepo extends JpaRepository<StockIssueItemJpa, Integer> {

    @Modifying
    @Query("DELETE FROM StockIssueItemJpa i WHERE i.issueId = :issueId")
    void deleteByIssueId(@Param("issueId") Integer issueId);

    List<StockIssueItemJpa> findByIssueId(Integer issueId);

    /**
     * Tổng hợp số lượng / doanh thu / chi phí / lãi gộp theo item, chỉ tính
     * các dòng thuộc phiếu xuất kho CONFIRMED, lọc theo warehouse (tùy chọn)
     * và khoảng thời gian confirmed_at (tùy chọn).
     *
     * Loại trừ các dòng placeholder (entry_item_id = 0) mà
     * {@code StockIssueService#buildFifoItems} chèn khi xuất kho vượt tồn —
     * các dòng này có quantity > 0 nhưng import_price/export_price/final_price
     * đều = 0, nếu tính vào sẽ làm sai lệch doanh thu và biên lợi nhuận.
     */
    @Query(value = """
            SELECT i.item_id AS itemId,
                   CAST(SUM(i.quantity) AS SIGNED) AS totalQuantity,
                   SUM(i.final_price * i.quantity) AS totalRevenue,
                   SUM(i.import_price * i.quantity) AS totalCost,
                   SUM(i.gross_profit) AS totalGrossProfit
            FROM stock_issue_item i
            JOIN stock_issue s ON s.issue_id = i.issue_id
            WHERE s.status = 'CONFIRMED'
              AND i.entry_item_id IS NOT NULL AND i.entry_item_id != 0
              AND (:warehouseId IS NULL OR s.warehouse_id = :warehouseId)
              AND (:fromDateTime IS NULL OR s.confirmed_at >= :fromDateTime)
              AND (:toDateTime IS NULL OR s.confirmed_at <= :toDateTime)
            GROUP BY i.item_id
            """, nativeQuery = true)
    List<ItemProfitAggregateProjection> aggregateProfitByItem(
            @Param("warehouseId") Integer warehouseId,
            @Param("fromDateTime") LocalDateTime fromDateTime,
            @Param("toDateTime") LocalDateTime toDateTime);

    interface ItemProfitAggregateProjection {
        Integer getItemId();
        Integer getTotalQuantity();
        BigDecimal getTotalRevenue();
        BigDecimal getTotalCost();
        BigDecimal getTotalGrossProfit();
    }
}
