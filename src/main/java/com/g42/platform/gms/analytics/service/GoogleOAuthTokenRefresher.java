package com.g42.platform.gms.analytics.service;

import com.g42.platform.gms.analytics.entity.GoogleOAuthToken;
import com.g42.platform.gms.analytics.repository.GoogleOAuthTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.g42.platform.gms.analytics.exception.GoogleAnalyticsException;

import java.time.Instant;

/**
 * Chủ động làm mới access_token Google trước khi hết hạn, tránh dashboard bị lỗi
 * NOT_CONNECTED giả (token còn refresh_token hợp lệ nhưng access_token vừa hết hạn).
 * access_token GA4/Search Console thường sống 1 giờ — kiểm tra mỗi 15 phút là đủ.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleOAuthTokenRefresher {

    private final GoogleOAuthTokenRepository tokenRepository;
    private final GoogleOAuthService googleOAuthService;

    /**
     * KHÔNG bọc @Transactional ở đây: refreshAccessToken() đã tự mở giao dịch riêng. Bọc thêm
     * một giao dịch ngoài thì khi làm mới thất bại, giao dịch chung bị đánh dấu rollback-only,
     * lệnh catch bên dưới nuốt mất lỗi và Spring ném UnexpectedRollbackException lúc commit —
     * biến một cảnh báo thành cả vệt stack trace ERROR mỗi 15 phút.
     */
    @Scheduled(fixedRate = 15 * 60 * 1000)
    public void refreshIfNeeded() {
        GoogleOAuthToken token = tokenRepository.findTopByOrderByIdAsc().orElse(null);
        if (token == null || !"CONNECTED".equals(token.getStatus()) || token.getRefreshToken() == null) {
            return;
        }
        if (token.getTokenExpiresAt() != null && token.getTokenExpiresAt().isAfter(Instant.now().plusSeconds(600))) {
            return;
        }
        try {
            googleOAuthService.refreshAccessToken(token);
            log.info("[GoogleOAuth] Đã tự làm mới access_token định kỳ");
        } catch (GoogleAnalyticsException e) {
            // RECONNECT_REQUIRED: GoogleOAuthService đã chuyển token sang trạng thái cần kết nối
            // lại nên lần chạy sau sẽ tự bỏ qua, không lặp lại cảnh báo này nữa.
            log.warn("[GoogleOAuth] Tự làm mới access_token thất bại: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("[GoogleOAuth] Tự làm mới access_token thất bại: {}", e.getMessage());
        }
    }
}
