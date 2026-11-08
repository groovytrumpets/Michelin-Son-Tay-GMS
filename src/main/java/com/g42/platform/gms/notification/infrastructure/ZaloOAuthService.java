package com.g42.platform.gms.notification.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.g42.platform.gms.notification.application.zalo.ZaloOaSettings;
import com.g42.platform.gms.notification.application.zalo.ZaloTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Luồng OAuth v4 (có PKCE) để OA cấp quyền cho ứng dụng — nút "Kết nối lại OA" ở /zalo-oa-config.
 *
 * Bản cũ không gửi tham số state lên Zalo nên callback (bắt buộc @RequestParam state) không
 * bao giờ chạy được, gọi nhầm endpoint token của người dùng (/v4/access_token) thay vì của OA,
 * và không gửi secret_key — token trong DB thực tế đều được chèn tay.
 *
 * Phiên chờ (state -> code_verifier) giữ trong bộ nhớ 15 phút: restart BE giữa chừng thì chỉ
 * cần bấm kết nối lại, không đáng thêm bảng.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ZaloOAuthService {

    private static final long PENDING_TTL_SECONDS = 900;

    private final ZaloOaSettings settings;
    private final ZaloTokenService tokenService;

    private final Map<String, Pending> pending = new ConcurrentHashMap<>();

    private record Pending(String verifier, String returnUrl, Integer staffId, Instant createdAt) {}

    /** Kết quả callback: returnUrl null nếu luồng bắt đầu từ /zalo/login (không có màn FE nào chờ). */
    public record CallbackResult(boolean success, String message, String returnUrl) {}

    public String buildAuthorizeUrl(Integer staffId, String returnUrl) {
        String appId = settings.appId();
        if (appId.isBlank() || settings.secretKey().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chưa cấu hình App ID / Secret key của Zalo.");
        }
        purgeExpired();
        String verifier = PKCE.generateVerifier();
        String challenge = PKCE.generateChallenge(verifier);
        String state = UUID.randomUUID().toString().replace("-", "");
        pending.put(state, new Pending(verifier, safeReturnUrl(returnUrl), staffId, Instant.now()));

        return "https://oauth.zaloapp.com/v4/oa/permission"
                + "?app_id=" + enc(appId)
                + "&redirect_uri=" + enc(settings.redirectUri())
                + "&code_challenge=" + enc(challenge)
                + "&code_challenge_method=S256"
                + "&state=" + enc(state);
    }

    public CallbackResult handleCallback(String code, String state) {
        Pending p = state == null ? null : pending.remove(state);
        if (p == null || p.createdAt().isBefore(Instant.now().minusSeconds(PENDING_TTL_SECONDS))) {
            return new CallbackResult(false, "Phiên kết nối không hợp lệ hoặc đã hết hạn, hãy bấm kết nối lại.",
                    p == null ? null : p.returnUrl());
        }
        if (code == null || code.isBlank()) {
            return new CallbackResult(false, "Zalo không trả mã cấp quyền (có thể bạn đã bấm từ chối).", p.returnUrl());
        }
        try {
            JsonNode json = tokenService.exchangeAuthorizationCode(code, p.verifier());
            tokenService.saveFromOAuth(
                    json.get("access_token").asText(),
                    json.path("refresh_token").asText(null),
                    json.path("expires_in").asLong(0),
                    p.staffId());
            log.info("Zalo: kết nối OA qua OAuth thành công (staff {})", p.staffId());
            return new CallbackResult(true, "Đã kết nối Zalo OA.", p.returnUrl());
        } catch (Exception e) {
            log.warn("Zalo: OAuth callback thất bại: {}", e.getMessage());
            return new CallbackResult(false, e.getMessage(), p.returnUrl());
        }
    }

    /**
     * Chỉ quay về tên miền của mình — không thì callback thành chỗ chuyển hướng mở cho bất kỳ ai
     * (open redirect).
     */
    private static String safeReturnUrl(String returnUrl) {
        if (returnUrl == null || returnUrl.isBlank()) return null;
        try {
            URI uri = URI.create(returnUrl);
            String host = uri.getHost();
            if (host == null) return null;
            boolean ok = host.equals("localhost") || host.endsWith(".localhost") || host.equals("127.0.0.1")
                    || host.equals("sontaygarage.vn") || host.endsWith(".sontaygarage.vn");
            return ok ? returnUrl : null;
        } catch (Exception e) {
            return null;
        }
    }

    private void purgeExpired() {
        Instant cutoff = Instant.now().minusSeconds(PENDING_TTL_SECONDS);
        pending.entrySet().removeIf(e -> e.getValue().createdAt().isBefore(cutoff));
    }

    private static String enc(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }
}
