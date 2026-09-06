package com.g42.platform.gms.analytics.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.g42.platform.gms.analytics.entity.GoogleOAuthToken;
import com.g42.platform.gms.analytics.exception.GoogleAnalyticsErrorCode;
import com.g42.platform.gms.analytics.exception.GoogleAnalyticsException;
import com.g42.platform.gms.analytics.repository.GoogleOAuthTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

/**
 * Quản lý kết nối OAuth2 (authorization-code, offline access) với tài khoản Google dùng để
 * đọc số liệu Google Analytics (GA4) + Google Search Console trên trang quản trị.
 *
 * Đây LÀ MỘT client OAuth riêng, KHÔNG dùng chung cơ chế "Đăng nhập bằng Google" của nhân
 * viên (xem SecurityConfig#oauth2Login) — client đó chỉ xin scope openid/email/profile và
 * không có access_type=offline nên không có refresh_token để gọi API định kỳ.
 *
 * Toàn hệ thống chỉ có một kết nối dùng chung (bảng google_oauth_token chỉ có 1 dòng), giống
 * cách notification/ZaloOAuthService quản lý token Zalo OA.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleOAuthService {

    private static final String AUTHORIZE_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
    private static final String USERINFO_ENDPOINT = "https://www.googleapis.com/oauth2/v2/userinfo";
    private static final String SCOPES = "https://www.googleapis.com/auth/analytics.readonly "
            + "https://www.googleapis.com/auth/webmasters.readonly openid email";

    private final GoogleOAuthTokenRepository tokenRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${google.analytics.oauth.client-id:}")
    private String clientId;

    @Value("${google.analytics.oauth.client-secret:}")
    private String clientSecret;

    @Value("${google.analytics.oauth.redirect-uri:}")
    private String redirectUri;

    @Value("${google.analytics.frontend-redirect-url:}")
    private String frontendRedirectUrl;

    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank()
                && clientSecret != null && !clientSecret.isBlank()
                && redirectUri != null && !redirectUri.isBlank();
    }

    public String getFrontendRedirectUrl() {
        return frontendRedirectUrl == null || frontendRedirectUrl.isBlank()
                ? "/google-insights"
                : frontendRedirectUrl;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    @Transactional
    public String buildAuthorizeUrl(Integer staffId) {
        if (!isConfigured()) {
            throw new GoogleAnalyticsException(GoogleAnalyticsErrorCode.NOT_CONFIGURED);
        }

        String state = UUID.randomUUID().toString();

        GoogleOAuthToken token = tokenRepository.findTopByOrderByIdAsc().orElseGet(GoogleOAuthToken::new);
        token.setState(state);
        token.setStatus("PENDING");
        token.setConnectedByStaffId(staffId);
        tokenRepository.save(token);

        return AUTHORIZE_ENDPOINT
                + "?client_id=" + encode(clientId)
                + "&redirect_uri=" + encode(redirectUri)
                + "&response_type=code"
                + "&scope=" + encode(SCOPES)
                + "&access_type=offline"
                + "&prompt=consent"
                + "&include_granted_scopes=true"
                + "&state=" + encode(state);
    }

    @Transactional
    public void handleCallback(String code, String state) {
        GoogleOAuthToken token = tokenRepository.findByState(state)
                .orElseThrow(() -> new GoogleAnalyticsException(GoogleAnalyticsErrorCode.INVALID_STATE));

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", code);
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("redirect_uri", redirectUri);
        body.add("grant_type", "authorization_code");

        JsonNode json = postForm(TOKEN_ENDPOINT, body);

        String accessToken = json.path("access_token").asText(null);
        String refreshToken = json.path("refresh_token").asText(null);
        long expiresIn = json.path("expires_in").asLong(3600);

        if (accessToken == null) {
            log.error("[GoogleOAuth] Không nhận được access_token từ Google: {}", json);
            throw new GoogleAnalyticsException(GoogleAnalyticsErrorCode.UPSTREAM_ERROR);
        }

        token.setAccessToken(accessToken);
        // Google chỉ trả refresh_token ở lần cấp quyền đầu tiên (prompt=consent) — giữ lại
        // refresh_token cũ nếu lần này không có, tránh mất khả năng tự làm mới token.
        if (refreshToken != null && !refreshToken.isBlank()) {
            token.setRefreshToken(refreshToken);
        }
        token.setTokenExpiresAt(Instant.now().plusSeconds(expiresIn));
        token.setScope(json.path("scope").asText(null));
        token.setStatus("CONNECTED");
        token.setState(null);
        token.setConnectedEmail(fetchEmail(accessToken));
        tokenRepository.save(token);

        log.info("[GoogleOAuth] Kết nối Google Analytics/Search Console thành công cho {}", token.getConnectedEmail());
    }

    private String fetchEmail(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            ResponseEntity<String> response = restTemplate.exchange(
                    USERINFO_ENDPOINT, org.springframework.http.HttpMethod.GET,
                    new HttpEntity<>(headers), String.class);
            JsonNode json = objectMapper.readTree(response.getBody());
            return json.path("email").asText(null);
        } catch (Exception e) {
            log.warn("[GoogleOAuth] Không lấy được email tài khoản Google: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Trả về access_token còn hạn dùng, tự refresh nếu sắp hết hạn (dưới 5 phút).
     * Dùng trước MỌI lượt gọi GA4 Data API / Search Console API.
     */
    @Transactional
    public String getValidAccessToken() {
        GoogleOAuthToken token = tokenRepository.findTopByOrderByIdAsc()
                .filter(t -> "CONNECTED".equals(t.getStatus()))
                .orElseThrow(() -> new GoogleAnalyticsException(GoogleAnalyticsErrorCode.NOT_CONNECTED));

        if (token.getTokenExpiresAt() == null || token.getTokenExpiresAt().isBefore(Instant.now().plusSeconds(300))) {
            refreshAccessToken(token);
        }
        return token.getAccessToken();
    }

    @Transactional
    public void refreshAccessToken(GoogleOAuthToken token) {
        if (token.getRefreshToken() == null || token.getRefreshToken().isBlank()) {
            throw new GoogleAnalyticsException(GoogleAnalyticsErrorCode.NOT_CONNECTED, "Thiếu refresh_token, cần kết nối lại");
        }

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("refresh_token", token.getRefreshToken());
        body.add("grant_type", "refresh_token");

        JsonNode json = postForm(TOKEN_ENDPOINT, body);

        String accessToken = json.path("access_token").asText(null);
        if (accessToken == null) {
            log.error("[GoogleOAuth] Làm mới token thất bại: {}", json);
            throw new GoogleAnalyticsException(GoogleAnalyticsErrorCode.UPSTREAM_ERROR);
        }
        long expiresIn = json.path("expires_in").asLong(3600);

        token.setAccessToken(accessToken);
        token.setTokenExpiresAt(Instant.now().plusSeconds(expiresIn));
        tokenRepository.save(token);
    }

    @Transactional
    public void disconnect() {
        tokenRepository.findTopByOrderByIdAsc().ifPresent(token -> {
            token.setStatus("DISCONNECTED");
            token.setAccessToken(null);
            token.setRefreshToken(null);
            token.setTokenExpiresAt(null);
            token.setState(null);
            tokenRepository.save(token);
        });
    }

    public GoogleOAuthToken getCurrentToken() {
        return tokenRepository.findTopByOrderByIdAsc().orElse(null);
    }

    private JsonNode postForm(String url, MultiValueMap<String, String> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);
            return objectMapper.readTree(response.getBody());
        } catch (RestClientException e) {
            log.error("[GoogleOAuth] Lỗi gọi {}: {}", url, e.getMessage());
            throw new GoogleAnalyticsException(GoogleAnalyticsErrorCode.UPSTREAM_ERROR);
        } catch (Exception e) {
            log.error("[GoogleOAuth] Không đọc được phản hồi từ {}: {}", url, e.getMessage());
            throw new GoogleAnalyticsException(GoogleAnalyticsErrorCode.UPSTREAM_ERROR);
        }
    }
}
