package com.g42.platform.gms.notification.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.authz.PermissionCodes;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.notification.application.zalo.ZaloOaApiClient;
import com.g42.platform.gms.notification.application.zalo.ZaloOaSettings;
import com.g42.platform.gms.notification.application.zalo.ZaloTokenService;
import com.g42.platform.gms.notification.application.zalo.ZaloWebhookEventLog;
import com.g42.platform.gms.notification.domain.ZnsTemplateType;
import com.g42.platform.gms.notification.infrastructure.ZaloNotificationSender;
import com.g42.platform.gms.notification.infrastructure.ZaloOAuthService;
import com.g42.platform.gms.notification.infrastructure.entity.ZaloOaConfig;
import com.g42.platform.gms.notification.infrastructure.entity.ZaloToken;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Màn /zalo-oa-config: xem trạng thái token, dán token, đổi cấu hình ứng dụng, xem OA/ZNS.
 *
 * Không bao giờ trả secret key hay token đầy đủ về FE — chỉ dạng che (6 ký tự đầu … 4 ký tự cuối).
 */
@RestController
@RequestMapping("/api/admin/zalo-oa")
@RequiredArgsConstructor
public class ZaloOaAdminController {

    private static final String VIEW = "hasAuthority('" + PermissionCodes.ZALO_OA_VIEW + "')";
    private static final String CONFIG = "hasAuthority('" + PermissionCodes.ZALO_OA_CONFIG + "')";

    private final ZaloTokenService tokenService;
    private final ZaloOaSettings settings;
    private final ZaloOaApiClient apiClient;
    private final ZaloOAuthService oauthService;
    private final ZaloNotificationSender sender;
    private final ZaloWebhookEventLog webhookEventLog;

    // ------------------------------------------------------------------ DTO

    public record TokenStatus(
            boolean present,
            /** ACTIVE | EXPIRING | EXPIRED | MISSING | UNKNOWN */
            String health,
            String accessTokenMasked,
            int accessTokenLength,
            boolean hasRefreshToken,
            String refreshTokenMasked,
            Instant expiresAt,
            Instant refreshTokenExpiresAt,
            Instant lastRefreshAt,
            String lastRefreshError,
            String source,
            Integer updatedByStaffId,
            Instant updatedAt,
            Instant createdAt) {}

    public record TemplateMapping(String type, String label, String params, String defaultTemplateId,
                                  String templateId, boolean custom) {}

    public record ConfigView(String appId, boolean appIdFromDb, boolean secretKeyConfigured, String secretKeyMasked,
                             boolean secretKeyFromDb, String redirectUri, boolean redirectUriFromDb,
                             List<TemplateMapping> templates, Instant updatedAt, Integer updatedByStaffId) {}

    public record StatusResponse(TokenStatus token, ConfigView config, String webhookUrl, String callbackUrlHint,
                                 List<ZaloWebhookEventLog.Event> recentWebhookEvents, Instant serverTime) {}

    public record ConfigRequest(String appId, String secretKey, String redirectUri, Map<String, String> templateMap) {}

    public record ManualTokenRequest(String accessToken, String refreshToken, Long expiresInSeconds) {}

    public record TestSendRequest(String type, String phone) {}

    // ------------------------------------------------------------------ đọc

    @GetMapping("/status")
    @PreAuthorize(VIEW)
    public ResponseEntity<ApiResponse<StatusResponse>> status() {
        String base = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        StatusResponse res = new StatusResponse(
                tokenStatus(tokenService.current()),
                configView(),
                base + "/api/webhook/zalo/events",
                base + "/zalo/callback",
                webhookEventLog.recent(),
                Instant.now());
        return ResponseEntity.ok(ApiResponses.success(res));
    }

    @GetMapping("/oa-info")
    @PreAuthorize(VIEW)
    public ResponseEntity<ApiResponse<JsonNode>> oaInfo() {
        return ResponseEntity.ok(ApiResponses.success(apiClient.getOaInfo()));
    }

    @GetMapping("/zns/quota")
    @PreAuthorize(VIEW)
    public ResponseEntity<ApiResponse<JsonNode>> znsQuota() {
        return ResponseEntity.ok(ApiResponses.success(apiClient.getZnsQuota()));
    }

    @GetMapping("/zns/templates")
    @PreAuthorize(VIEW)
    public ResponseEntity<ApiResponse<JsonNode>> znsTemplates(@RequestParam(defaultValue = "0") int offset,
                                                              @RequestParam(defaultValue = "100") int limit,
                                                              @RequestParam(required = false) Integer status) {
        return ResponseEntity.ok(ApiResponses.success(apiClient.listTemplates(offset, limit, status)));
    }

    @GetMapping("/zns/templates/{templateId}")
    @PreAuthorize(VIEW)
    public ResponseEntity<ApiResponse<JsonNode>> znsTemplate(@PathVariable String templateId) {
        return ResponseEntity.ok(ApiResponses.success(apiClient.templateInfo(templateId)));
    }

    @GetMapping("/zns/messages/{msgId}")
    @PreAuthorize(VIEW)
    public ResponseEntity<ApiResponse<JsonNode>> znsMessageStatus(@PathVariable String msgId) {
        return ResponseEntity.ok(ApiResponses.success(apiClient.messageStatus(msgId)));
    }

    // ------------------------------------------------------------------ ghi

    @PutMapping("/config")
    @PreAuthorize(CONFIG)
    public ResponseEntity<ApiResponse<ConfigView>> updateConfig(@RequestBody ConfigRequest req,
                                                                @AuthenticationPrincipal StaffPrincipal principal) {
        settings.update(req.appId(), req.secretKey(), req.redirectUri(), req.templateMap(), principal.getStaffId());
        return ResponseEntity.ok(ApiResponses.success(configView(), "Đã lưu cấu hình Zalo OA"));
    }

    @PutMapping("/token")
    @PreAuthorize(CONFIG)
    public ResponseEntity<ApiResponse<TokenStatus>> saveToken(@RequestBody ManualTokenRequest req,
                                                              @AuthenticationPrincipal StaffPrincipal principal) {
        ZaloToken t = tokenService.saveManual(req.accessToken(), req.refreshToken(), req.expiresInSeconds(),
                principal.getStaffId());
        return ResponseEntity.ok(ApiResponses.success(tokenStatus(t), "Đã lưu token"));
    }

    @PostMapping("/token/refresh")
    @PreAuthorize(CONFIG)
    public ResponseEntity<ApiResponse<TokenStatus>> refreshToken(@AuthenticationPrincipal StaffPrincipal principal) {
        ZaloToken t = tokenService.refresh(principal.getStaffId());
        return ResponseEntity.ok(ApiResponses.success(tokenStatus(t), "Đã làm mới access token"));
    }

    @GetMapping("/oauth/authorize-url")
    @PreAuthorize(CONFIG)
    public ResponseEntity<ApiResponse<Map<String, String>>> authorizeUrl(@RequestParam(required = false) String returnUrl,
                                                                         @AuthenticationPrincipal StaffPrincipal principal) {
        String url = oauthService.buildAuthorizeUrl(principal.getStaffId(), returnUrl);
        return ResponseEntity.ok(ApiResponses.success(Map.of("authorizeUrl", url)));
    }

    @PostMapping("/zns/test")
    @PreAuthorize(CONFIG)
    public ResponseEntity<ApiResponse<ZaloNotificationSender.ZnsSendResult>> testSend(@RequestBody TestSendRequest req) {
        ZnsTemplateType type;
        try {
            type = ZnsTemplateType.valueOf(req.type());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Loại tin không hợp lệ.");
        }
        String phone = req.phone() == null ? "" : req.phone().replaceAll("[\\s.\\-]", "");
        if (!phone.matches("(\\+?84|0)\\d{9,10}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số điện thoại không hợp lệ.");
        }
        ZaloNotificationSender.ZnsSendResult r = sender.sendTest(type, phone);
        return ResponseEntity.ok(ApiResponses.success(r, r.ok() ? "Zalo đã nhận tin" : "Zalo từ chối tin"));
    }

    // ------------------------------------------------------------------ dựng DTO

    private TokenStatus tokenStatus(ZaloToken t) {
        if (t == null || t.getAccessToken() == null || t.getAccessToken().isBlank()) {
            return new TokenStatus(false, "MISSING", null, 0, false, null, null, null, null, null, null, null, null, null);
        }
        Instant now = Instant.now();
        String health;
        if (t.getExpiresAt() == null) health = "UNKNOWN";
        else if (t.getExpiresAt().isBefore(now)) health = "EXPIRED";
        else if (t.getExpiresAt().isBefore(now.plusSeconds(3 * 3600))) health = "EXPIRING";
        else health = "ACTIVE";
        boolean hasRefresh = t.getRefreshToken() != null && !t.getRefreshToken().isBlank();
        return new TokenStatus(true, health, mask(t.getAccessToken()), t.getAccessToken().length(),
                hasRefresh, hasRefresh ? mask(t.getRefreshToken()) : null,
                t.getExpiresAt(), t.getRefreshTokenExpiresAt(), t.getLastRefreshAt(), t.getLastRefreshError(),
                t.getSource(), t.getUpdatedByStaffId(), t.getUpdatedAt(), t.getCreatedAt());
    }

    private ConfigView configView() {
        ZaloOaConfig row = settings.row();
        Map<String, String> custom = settings.customTemplateMap();
        List<TemplateMapping> templates = new ArrayList<>();
        Map<ZnsTemplateType, String> effective = settings.effectiveTemplateMap();
        for (Map.Entry<ZnsTemplateType, String> e : effective.entrySet()) {
            ZnsTemplateType type = e.getKey();
            templates.add(new TemplateMapping(type.name(), type.getLabel(), type.getParams(),
                    type.getDefaultTemplateId(), e.getValue(), custom.containsKey(type.name())));
        }
        String secret = settings.secretKey();
        return new ConfigView(settings.appId(), settings.appIdFromDb(),
                !secret.isBlank(), secret.isBlank() ? null : maskSecret(secret), settings.secretKeyFromDb(),
                settings.redirectUri(), settings.redirectUriFromDb(), templates,
                row.getUpdatedAt(), row.getUpdatedByStaffId());
    }

    private static String mask(String s) {
        if (s == null) return null;
        if (s.length() <= 12) return "••••";
        return s.substring(0, 6) + "…" + s.substring(s.length() - 4);
    }

    private static String maskSecret(String s) {
        return s.length() <= 4 ? "••••" : "••••••" + s.substring(s.length() - 3);
    }
}
