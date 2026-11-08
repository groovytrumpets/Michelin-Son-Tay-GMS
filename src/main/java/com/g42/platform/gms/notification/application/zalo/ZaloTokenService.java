package com.g42.platform.gms.notification.application.zalo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.g42.platform.gms.notification.infrastructure.entity.ZaloToken;
import com.g42.platform.gms.notification.infrastructure.repository.ZaloTokenRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Giữ token gửi tin của Zalo OA: đọc token đang dùng, dán tay, đổi refresh token lấy token mới,
 * nhận token từ luồng OAuth.
 *
 * Bất biến: chỉ có tối đa 1 dòng zalo_token.state = "active". Trước đây các hàm
 * getZaloTokenByState("active") sẽ ném IncorrectResultSize nếu lỡ có 2 dòng — giờ mọi chỗ đọc
 * token đi qua {@link #current()} và mọi chỗ ghi đều ghi đè lên đúng dòng đó.
 *
 * Refresh token của Zalo chỉ dùng được MỘT lần (mỗi lần đổi nhận refresh token mới), nên
 * {@link #refresh} là synchronized để job tự làm mới và nút "Làm mới ngay" không giẫm lên nhau.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ZaloTokenService {

    public static final String ACTIVE = "active";
    public static final String SOURCE_MANUAL = "MANUAL";
    public static final String SOURCE_OAUTH = "OAUTH";

    static final String OA_TOKEN_URL = "https://oauth.zaloapp.com/v4/oa/access_token";
    /** Zalo: access token OA sống 25 giờ. */
    static final long DEFAULT_ACCESS_TTL_SECONDS = 90_000;
    /** Zalo: refresh token sống 3 tháng. */
    static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(90);

    private final ZaloTokenRepo zaloTokenRepo;
    private final ZaloOaSettings settings;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    /** Token đang dùng; null nếu chưa có. Lỡ có nhiều dòng active thì lấy dòng mới nhất. */
    public ZaloToken current() {
        List<ZaloToken> actives = zaloTokenRepo.findByStateOrderByIdDesc(ACTIVE);
        return actives.isEmpty() ? null : actives.get(0);
    }

    /** Access token để gọi API; null nếu chưa có (bên gửi tin tự chuyển sang email). */
    public String currentAccessToken() {
        ZaloToken t = current();
        return t == null || t.getAccessToken() == null || t.getAccessToken().isBlank() ? null : t.getAccessToken();
    }

    /** Dán tay token lấy từ công cụ API Explorer của Zalo. */
    public synchronized ZaloToken saveManual(String accessToken, String refreshToken,
                                             Long expiresInSeconds, Integer staffId) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thiếu access token.");
        }
        long ttl = expiresInSeconds == null || expiresInSeconds <= 0 ? DEFAULT_ACCESS_TTL_SECONDS : expiresInSeconds;
        ZaloToken t = activeRowForWrite();
        t.setAccessToken(accessToken.trim());
        if (refreshToken != null && !refreshToken.isBlank()) {
            t.setRefreshToken(refreshToken.trim());
            t.setRefreshTokenExpiresAt(Instant.now().plus(REFRESH_TOKEN_TTL));
        }
        t.setExpiresAt(Instant.now().plusSeconds(ttl));
        t.setSource(SOURCE_MANUAL);
        t.setUpdatedByStaffId(staffId);
        t.setLastRefreshError(null);
        return save(t);
    }

    /** Lưu token nhận được từ luồng OAuth (nút "Kết nối lại OA"). */
    public synchronized ZaloToken saveFromOAuth(String accessToken, String refreshToken,
                                                long expiresInSeconds, Integer staffId) {
        ZaloToken t = activeRowForWrite();
        t.setAccessToken(accessToken);
        t.setRefreshToken(refreshToken);
        t.setExpiresAt(Instant.now().plusSeconds(expiresInSeconds > 0 ? expiresInSeconds : DEFAULT_ACCESS_TTL_SECONDS));
        t.setRefreshTokenExpiresAt(Instant.now().plus(REFRESH_TOKEN_TTL));
        t.setSource(SOURCE_OAUTH);
        t.setUpdatedByStaffId(staffId);
        t.setLastRefreshError(null);
        return save(t);
    }

    /**
     * Đổi refresh token lấy access token mới.
     *
     * @param staffId null khi job tự chạy
     * @return token sau khi làm mới
     * @throws ResponseStatusException 400 kèm thông báo của Zalo nếu thất bại (lỗi cũng được ghi vào
     *         last_refresh_error để màn cấu hình hiện ra)
     */
    public synchronized ZaloToken refresh(Integer staffId) {
        ZaloToken t = current();
        if (t == null || t.getRefreshToken() == null || t.getRefreshToken().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Chưa có refresh token. Dán token mới hoặc bấm \"Kết nối lại OA\".");
        }
        String appId = settings.appId();
        String secretKey = settings.secretKey();
        if (appId.isBlank() || secretKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chưa cấu hình App ID / Secret key.");
        }

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("app_id", appId);
        body.add("grant_type", "refresh_token");
        body.add("refresh_token", t.getRefreshToken());

        t.setLastRefreshAt(Instant.now());
        try {
            JsonNode json = postTokenEndpoint(body, secretKey);
            String error = tokenError(json);
            if (error != null) {
                t.setLastRefreshError(truncate(error));
                save(t);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Zalo từ chối làm mới token: " + error);
            }
            t.setAccessToken(json.get("access_token").asText());
            if (json.hasNonNull("refresh_token")) {
                t.setRefreshToken(json.get("refresh_token").asText());
                t.setRefreshTokenExpiresAt(Instant.now().plus(REFRESH_TOKEN_TTL));
            }
            t.setExpiresAt(Instant.now().plusSeconds(json.path("expires_in").asLong(DEFAULT_ACCESS_TTL_SECONDS)));
            t.setLastRefreshError(null);
            if (staffId != null) t.setUpdatedByStaffId(staffId);
            log.info("Zalo: làm mới access token thành công, hết hạn lúc {}", t.getExpiresAt());
            return save(t);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            t.setLastRefreshError(truncate("Không gọi được Zalo: " + e.getMessage()));
            save(t);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Không gọi được máy chủ Zalo: " + e.getMessage());
        }
    }

    /** Đổi authorization code (luồng OAuth v4 có PKCE) lấy token. */
    public JsonNode exchangeAuthorizationCode(String code, String codeVerifier) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("app_id", settings.appId());
        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add("code_verifier", codeVerifier);
        JsonNode json = postTokenEndpoint(body, settings.secretKey());
        String error = tokenError(json);
        if (error != null) {
            throw new IllegalStateException("Zalo từ chối cấp token: " + error);
        }
        return json;
    }

    private JsonNode postTokenEndpoint(MultiValueMap<String, String> body, String secretKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("secret_key", secretKey);
        try {
            ResponseEntity<String> res = restTemplate.postForEntity(OA_TOKEN_URL, new HttpEntity<>(body, headers), String.class);
            return objectMapper.readTree(res.getBody() == null ? "{}" : res.getBody());
        } catch (RestClientResponseException e) {
            try {
                return objectMapper.readTree(e.getResponseBodyAsString());
            } catch (Exception ignored) {
                throw new IllegalStateException("HTTP " + e.getStatusCode().value());
            }
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    /** Zalo trả HTTP 200 cả khi lỗi; lỗi nằm ở trường error (khác 0) + error_description. */
    private static String tokenError(JsonNode json) {
        if (json != null && json.hasNonNull("access_token")) return null;
        if (json == null) return "phản hồi rỗng";
        String desc = json.path("error_description").asText(json.path("error_reason").asText(json.path("message").asText("")));
        String code = json.path("error").asText("");
        return (desc.isBlank() ? "không có access_token trong phản hồi" : desc) + (code.isBlank() ? "" : " (mã " + code + ")");
    }

    /** Dòng active để ghi đè; dọn luôn các dòng active thừa. */
    private ZaloToken activeRowForWrite() {
        List<ZaloToken> actives = zaloTokenRepo.findByStateOrderByIdDesc(ACTIVE);
        if (actives.isEmpty()) {
            ZaloToken t = new ZaloToken();
            t.setState(ACTIVE);
            t.setCreatedAt(Instant.now());
            return t;
        }
        for (int i = 1; i < actives.size(); i++) {
            ZaloToken extra = actives.get(i);
            extra.setState("inactive");
            zaloTokenRepo.save(extra);
        }
        return actives.get(0);
    }

    private ZaloToken save(ZaloToken t) {
        t.setUpdatedAt(Instant.now());
        return zaloTokenRepo.save(t);
    }

    private static String truncate(String s) {
        return s == null || s.length() <= 500 ? s : s.substring(0, 500);
    }
}
