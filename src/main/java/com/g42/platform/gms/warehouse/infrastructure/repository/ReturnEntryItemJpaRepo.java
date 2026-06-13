package com.g42.platform.gms.warehouse.infrastructure.repository;

import com.g42.platform.gms.warehouse.infrastructure.entity.ReturnEntryItemJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReturnEntryItemJpaRepo extends JpaRepository<ReturnEntryItemJpa, Integer> {

    List<ReturnEntryItemJpa> findByReturnId(Integer returnId);

    ReturnEntryItemJpa findByAllocationId(Integer allocationId);

    ReturnEntryItemJpa findTopByAllocationId(Integer allocationId);

    ReturnEntryItemJpa findTopByAllocationIdOrderByReturnItemIdDesc(Integer allocationId);

    /**
     * Đếm số lần gây lỗi theo nhân viên, phân theo nguyên nhân và loại phiếu xác nhận.
     * Chỉ tính các phiếu đã CONFIRMED (đã thực sự ảnh hưởng hàng tồn).
     */
    @Query("""
        SELECT rei.responsibleStaffId, rei.defectCause, COUNT(rei) as defectCount, SUM(rei.quantity) as defectQty
        FROM ReturnEntryItemJpa rei
        JOIN ReturnEntryJpa re ON re.returnId = rei.returnId
        WHERE rei.returnReason = 'DEFECTIVE'
          AND rei.responsibleStaffId IS NOT NULL
          AND re.status = 'CONFIRMED'
          AND (:from IS NULL OR re.confirmedAt >= :from)
          AND (:to IS NULL OR re.confirmedAt <= :to)
        GROUP BY rei.responsibleStaffId, rei.defectCause
        ORDER BY defectCount DESC
    """)
    List<Object[]> summarizeDefectsByStaff(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    /**
     * Lấy chi tiết từng lỗi: phiếu hoàn, sản phẩm, ngày xác nhận, giá trị — dùng để drill-down báo cáo.
     */
    @Query("""
        SELECT rei.returnItemId, rei.returnId, re.returnCode, re.confirmedAt,
               rei.itemId, rei.quantity, rei.defectCause, rei.conditionNote,
               rei.responsibleStaffId, rei.entryItemId
        FROM ReturnEntryItemJpa rei
        JOIN ReturnEntryJpa re ON re.returnId = rei.returnId
        WHERE rei.returnReason = 'DEFECTIVE'
          AND rei.responsibleStaffId = :staffId
          AND re.status = 'CONFIRMED'
          AND (:from IS NULL OR re.confirmedAt >= :from)
          AND (:to IS NULL OR re.confirmedAt <= :to)
        ORDER BY re.confirmedAt DESC
    """)
    List<Object[]> findDefectDetailsByStaff(
            @Param("staffId") Integer staffId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}
