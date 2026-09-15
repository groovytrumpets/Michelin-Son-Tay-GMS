package com.g42.platform.gms.analytics.controller;

import com.g42.platform.gms.authz.PermissionCodes;
import com.g42.platform.gms.analytics.dto.GoogleConnectionStatusResponse;
import com.g42.platform.gms.analytics.entity.GoogleOAuthToken;
import com.g42.platform.gms.analytics.service.GoogleAnalyticsDataService;
import com.g42.platform.gms.analytics.service.GoogleOAuthService;
import com.g42.platform.gms.analytics.service.GoogleSearchConsoleService;
import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

/**
 * Kết nối OAuth2 với Google để đọc số liệu Google Analytics (GA4) + Search Console.
 * /connect và /disconnect/status yêu cầu quyền quản lý; /callback là địa chỉ Google tự
 * redirect trình duyệt về nên phải công khai (đã thêm vào SecurityConfig#permitAll),
 * an toàn nhờ tham số state đối chiếu ở GoogleOAuthService.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/analytics/google")
@RequiredArgsConstructor
public class GoogleOAuthController {

    private final GoogleOAuthService googleOAuthService;
    private final GoogleAnalyticsDataService gaDataService;
    private final GoogleSearchConsoleService searchConsoleService;

    @GetMapping("/connect")
    @PreAuthorize("hasAuthority('" + PermissionCodes.GOOGLE_INSIGHTS_CONFIG + "')")
    public ResponseEntity<ApiResponse<Map<String, String>>> connect(@AuthenticationPrincipal StaffPrincipal principal) {
        String url = googleOAuthService.buildAuthorizeUrl(principal.getStaffId());
        return ResponseEntity.ok(ApiResponses.success(Map.of("authorizeUrl", url)));
    }

    @GetMapping("/callback")
    public void callback(@RequestParam String code, @RequestParam String state, HttpServletResponse response) throws IOException {
        String redirectBase = googleOAuthService.getFrontendRedirectUrl();
        try {
            googleOAuthService.handleCallback(code, state);
            response.sendRedirect(redirectBase + "?google_connected=1");
        } catch (Exception e) {
            log.error("[GoogleOAuth] Callback thất bại: {}", e.getMessage());
            response.sendRedirect(redirectBase + "?google_connected=0");
        }
    }

    @PostMapping("/disconnect")
    @PreAuthorize("hasAuthority('" + PermissionCodes.GOOGLE_INSIGHTS_CONFIG + "')")
    public ResponseEntity<ApiResponse<Void>> disconnect() {
        googleOAuthService.disconnect();
        return ResponseEntity.ok(ApiResponses.successMessage("Đã ngắt kết nối tài khoản Google"));
    }

    @GetMapping("/status")
    @PreAuthorize("hasAuthority('" + PermissionCodes.GOOGLE_INSIGHTS_VIEW + "')")
    public ResponseEntity<ApiResponse<GoogleConnectionStatusResponse>> status() {
        GoogleOAuthToken token = googleOAuthService.getCurrentToken();
        boolean connected = token != null && "CONNECTED".equals(token.getStatus());
        GoogleConnectionStatusResponse dto = new GoogleConnectionStatusResponse(
                googleOAuthService.isConfigured(),
                connected,
                connected ? token.getConnectedEmail() : null,
                gaDataService.isPropertyConfigured(),
                searchConsoleService.isSiteConfigured(),
                token != null && token.getUpdatedAt() != null ? token.getUpdatedAt().toString() : null);
        return ResponseEntity.ok(ApiResponses.success(dto));
    }
}
