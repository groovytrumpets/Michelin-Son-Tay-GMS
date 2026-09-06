package com.g42.platform.gms.analytics.service;

import com.g42.platform.gms.analytics.entity.GoogleOAuthToken;
import com.g42.platform.gms.analytics.repository.GoogleOAuthTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

    @Scheduled(fixedRate = 15 * 60 * 1000)
    @Transactional
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
        } catch (Exception e) {
            log.warn("[GoogleOAuth] Tự làm mới access_token thất bại: {}", e.getMessage());
        }
    }
}
