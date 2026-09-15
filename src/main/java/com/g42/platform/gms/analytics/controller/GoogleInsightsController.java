package com.g42.platform.gms.analytics.controller;

import com.g42.platform.gms.authz.PermissionCodes;
import com.g42.platform.gms.analytics.dto.*;
import com.g42.platform.gms.analytics.service.GoogleAnalyticsDataService;
import com.g42.platform.gms.analytics.service.GoogleSearchConsoleService;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Số liệu Google Analytics (GA4) + Google Search Console cho trang quản trị
 * /google-insights. Yêu cầu đã kết nối qua GoogleOAuthController#connect trước.
 */
@RestController
@RequestMapping("/api/admin/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('" + PermissionCodes.GOOGLE_INSIGHTS_VIEW + "')")
public class GoogleInsightsController {

    private final GoogleAnalyticsDataService gaDataService;
    private final GoogleSearchConsoleService searchConsoleService;

    private String defaultStart() {
        return LocalDate.now().minusDays(27).toString();
    }

    private String defaultEnd() {
        return LocalDate.now().toString();
    }

    @GetMapping("/ga4/overview")
    public ResponseEntity<ApiResponse<Ga4OverviewResponse>> ga4Overview(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Ga4OverviewResponse result = gaDataService.getOverview(
                startDate != null ? startDate : defaultStart(),
                endDate != null ? endDate : defaultEnd());
        return ResponseEntity.ok(ApiResponses.success(result));
    }

    @GetMapping("/ga4/top-pages")
    public ResponseEntity<ApiResponse<List<Ga4TopPageDto>>> ga4TopPages(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "10") int limit) {
        List<Ga4TopPageDto> result = gaDataService.getTopPages(
                startDate != null ? startDate : defaultStart(),
                endDate != null ? endDate : defaultEnd(),
                limit);
        return ResponseEntity.ok(ApiResponses.success(result));
    }

    @GetMapping("/ga4/traffic-sources")
    public ResponseEntity<ApiResponse<List<Ga4TrafficSourceDto>>> ga4TrafficSources(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        List<Ga4TrafficSourceDto> result = gaDataService.getTrafficSources(
                startDate != null ? startDate : defaultStart(),
                endDate != null ? endDate : defaultEnd());
        return ResponseEntity.ok(ApiResponses.success(result));
    }

    @GetMapping("/search-console/overview")
    public ResponseEntity<ApiResponse<SearchConsoleOverviewResponse>> searchConsoleOverview(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        SearchConsoleOverviewResponse result = searchConsoleService.getOverview(
                startDate != null ? startDate : defaultStart(),
                endDate != null ? endDate : defaultEnd());
        return ResponseEntity.ok(ApiResponses.success(result));
    }

    @GetMapping("/search-console/top-queries")
    public ResponseEntity<ApiResponse<List<SearchConsoleQueryDto>>> searchConsoleTopQueries(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "10") int limit) {
        List<SearchConsoleQueryDto> result = searchConsoleService.getTopQueries(
                startDate != null ? startDate : defaultStart(),
                endDate != null ? endDate : defaultEnd(),
                limit);
        return ResponseEntity.ok(ApiResponses.success(result));
    }

    @GetMapping("/search-console/top-pages")
    public ResponseEntity<ApiResponse<List<SearchConsolePageDto>>> searchConsoleTopPages(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "10") int limit) {
        List<SearchConsolePageDto> result = searchConsoleService.getTopPages(
                startDate != null ? startDate : defaultStart(),
                endDate != null ? endDate : defaultEnd(),
                limit);
        return ResponseEntity.ok(ApiResponses.success(result));
    }
}
