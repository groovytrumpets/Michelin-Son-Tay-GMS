package com.g42.platform.gms.service_ticket_management.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Nhả hàng của các phiếu bán linh kiện giữ quá lâu mà không thanh toán.
 *
 * Không có job này thì mỗi phiếu bị bỏ dở (nhân viên lưu báo giá rồi đóng máy)
 * sẽ khoá tồn kho vĩnh viễn: hàng vẫn nằm trong kho nhưng không ai bán được nữa.
 *
 * Số ngày giữ cấu hình qua `gms.parts-sale.hold-expiry-days` (mặc định 3).
 * Chạy 1 lần/ngày lúc 2h sáng — lúc garage đóng cửa nên không đụng vào phiếu
 * nhân viên đang thao tác dở trong giờ làm.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PartsSaleHoldExpiryScheduler {

    private final PartsSaleService partsSaleService;

    @Value("${gms.parts-sale.hold-expiry-days:3}")
    private int holdExpiryDays;

    @Scheduled(cron = "0 0 2 * * *")
    public void releaseExpiredHolds() {
        if (holdExpiryDays <= 0) {
            return;
        }
        LocalDateTime expiredBefore = LocalDateTime.now().minusDays(holdExpiryDays);
        int released = partsSaleService.releaseExpiredHolds(expiredBefore);
        if (released > 0) {
            log.info("Đã nhả hàng của {} phiếu bán linh kiện giữ quá {} ngày", released, holdExpiryDays);
        }
    }
}
