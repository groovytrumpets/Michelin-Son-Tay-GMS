package com.g42.platform.gms.notification.infrastructure;

import com.g42.platform.gms.notification.application.zalo.ZaloTokenService;
import com.g42.platform.gms.notification.infrastructure.entity.ZaloToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Tự làm mới access token Zalo OA (sống 25 giờ).
 *
 * Trước đây chạy cố định mỗi 20 giờ tính từ lúc khởi động — nghĩa là mỗi lần restart BE
 * (devtools restart liên tục khi dev) lại đổi token một lần, còn BE tắt quá 25 giờ thì token
 * chết mà không ai biết. Giờ cứ 30 phút kiểm tra một lần và chỉ đổi khi token sắp hết hạn
 * (còn dưới 3 giờ) hoặc không rõ hạn.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ZaloTokenAutoRefresher {

    private static final Duration REFRESH_BEFORE = Duration.ofHours(3);

    private final ZaloTokenService tokenService;

    @Scheduled(initialDelay = 120_000, fixedDelay = 1_800_000)
    public void refreshZaloTokenAutomatically() {
        ZaloToken current = tokenService.current();
        if (current == null || current.getRefreshToken() == null || current.getRefreshToken().isBlank()) {
            return;
        }
        Instant expiresAt = current.getExpiresAt();
        if (expiresAt != null && expiresAt.isAfter(Instant.now().plus(REFRESH_BEFORE))) {
            return;
        }
        try {
            tokenService.refresh(null);
        } catch (Exception e) {
            // Lỗi đã được ghi vào zalo_token.last_refresh_error để hiện ở /zalo-oa-config.
            log.warn("Zalo: tự làm mới token thất bại: {}", e.getMessage());
        }
    }
}
