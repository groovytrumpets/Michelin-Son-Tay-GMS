package com.g42.platform.gms.estimation.app.service;

import com.g42.platform.gms.estimation.infrastructure.entity.StockAllocationJpa;
import com.g42.platform.gms.estimation.infrastructure.repository.StaleStockHoldJpaRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Nhả hàng đang giữ cho những lịch hẹn sẽ không bao giờ tới nơi.
 *
 * Từ khi /create-booking giữ hàng ngay lúc chốt lịch, một lượt giữ hàng có thể
 * tồn tại mà không gắn phiếu dịch vụ nào (phiếu chỉ sinh lúc check-in). Nếu lịch
 * bị hủy hoặc khách không đến thì không có ai nhả số hàng đó ra — job này quét
 * và trả chúng về kho.
 *
 * Số ngày chờ cấu hình qua `gms.booking-hold.expiry-days` (mặc định 3), tính từ
 * ngày hẹn (với lịch đã qua) hoặc từ lúc giữ (với báo giá không gắn lịch nào).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingStockHoldSweeper {

    private final StaleStockHoldJpaRepo staleStockHoldJpaRepo;

    @Autowired
    @Qualifier("warehouseStockAllocationService")
    private com.g42.platform.gms.warehouse.app.service.allocation.StockAllocationService warehouseStockAllocationService;

    @Value("${gms.booking-hold.expiry-days:3}")
    private int expiryDays;

    @Scheduled(cron = "0 15 2 * * *")
    @Transactional
    public void releaseStaleBookingHolds() {
        if (expiryDays <= 0) {
            return;
        }
        LocalDate staleScheduledBefore = LocalDate.now().minusDays(expiryDays);
        Instant staleCreatedBefore = Instant.now().minus(expiryDays, ChronoUnit.DAYS);

        List<StockAllocationJpa> stale = staleStockHoldJpaRepo
                .findStaleHolds(staleScheduledBefore, staleCreatedBefore);
        if (stale.isEmpty()) {
            return;
        }

        List<Integer> allocationIds = stale.stream().map(StockAllocationJpa::getAllocationId).toList();
        // staffId null: job chạy nền, không có nhân viên nào đứng sau thao tác
        int released = warehouseStockAllocationService.releaseAllocations(allocationIds, null);
        if (released > 0) {
            log.info("Đã nhả {} lượt giữ hàng của lịch hẹn đã hủy/quá hạn", released);
        }
    }
}
