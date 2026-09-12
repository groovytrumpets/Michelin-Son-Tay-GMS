package com.g42.platform.gms.estimation.infrastructure.repository;

import com.g42.platform.gms.estimation.infrastructure.entity.StockAllocationJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Truy vấn các lượt giữ hàng "mồ côi" — giữ từ lúc đặt lịch nên chưa gắn phiếu
 * dịch vụ nào (service_ticket_id IS NULL) và sẽ không bao giờ được gắn nữa.
 *
 * Ba trường hợp:
 * 1. Lịch hẹn đã hủy / khách không đến  → nhả ngay ở lần quét kế tiếp
 * 2. Lịch hẹn đã qua ngày hẹn quá lâu   → khách không tới nữa, nhả hàng
 * 3. Báo giá không gắn lịch hẹn nào và giữ quá lâu → nhả hàng
 */
@Repository
public interface StaleStockHoldJpaRepo extends JpaRepository<StockAllocationJpa, Integer> {

    @Query("""
            select a from StockAllocationJpa a
            where a.serviceTicketId is null
              and a.status = 'RESERVED'
              and (
                    exists (
                        select 1 from BookingJpaEntity b
                        where b.estimateId = a.estimateId
                          and (b.status in (com.g42.platform.gms.booking.customer.domain.enums.BookingStatus.CANCELLED,
                                            com.g42.platform.gms.booking.customer.domain.enums.BookingStatus.NOT_ARRIVED)
                               or b.scheduledDate < :staleScheduledBefore)
                    )
                 or (
                        not exists (select 1 from BookingJpaEntity b2 where b2.estimateId = a.estimateId)
                        and a.createdAt < :staleCreatedBefore
                    )
              )
            """)
    List<StockAllocationJpa> findStaleHolds(@Param("staleScheduledBefore") LocalDate staleScheduledBefore,
                                            @Param("staleCreatedBefore") Instant staleCreatedBefore);

    /**
     * Cac bao gia dang giu hang ma chua gan phieu dich vu — dung de danh dau
     * "Dang giu hang" cho danh sach lich hen trong mot truy van duy nhat.
     */
    @Query("""
            select distinct a.estimateId from StockAllocationJpa a
            where a.estimateId in :estimateIds
              and a.status = 'RESERVED'
              and a.serviceTicketId is null
            """)
    List<Integer> findEstimateIdsWithActiveHold(@Param("estimateIds") List<Integer> estimateIds);
}
