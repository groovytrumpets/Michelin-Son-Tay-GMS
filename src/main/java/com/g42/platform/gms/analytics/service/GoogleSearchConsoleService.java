package com.g42.platform.gms.analytics.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.g42.platform.gms.analytics.dto.SearchConsoleDailyPointDto;
import com.g42.platform.gms.analytics.dto.SearchConsoleOverviewResponse;
import com.g42.platform.gms.analytics.dto.SearchConsolePageDto;
import com.g42.platform.gms.analytics.dto.SearchConsoleQueryDto;
import com.g42.platform.gms.analytics.exception.GoogleAnalyticsErrorCode;
import com.g42.platform.gms.analytics.exception.GoogleAnalyticsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Gọi thẳng Google Search Console API (searchAnalytics.query, REST) để lấy số liệu
 * hiển thị tìm kiếm cho trang quản trị. Access token lấy qua GoogleOAuthService.
 * Tài liệu: https://developers.google.com/webmaster-tools/v1/searchanalytics/query
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleSearchConsoleService {

    private static final String QUERY_URL = "https://www.googleapis.com/webmasters/v3/sites/%s/searchAnalytics/query";

    private final GoogleOAuthService googleOAuthService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${google.analytics.search-console.site-url:}")
    private String siteUrl;

    public boolean isSiteConfigured() {
        return siteUrl != null && !siteUrl.isBlank();
    }

    private void requireSiteConfigured() {
        if (!isSiteConfigured()) {
            throw new GoogleAnalyticsException(GoogleAnalyticsErrorCode.SITE_NOT_CONFIGURED);
        }
    }

    public SearchConsoleOverviewResponse getOverview(String startDate, String endDate) {
        requireSiteConfigured();
        String accessToken = googleOAuthService.getValidAccessToken();

        ObjectNode totalsBody = objectMapper.createObjectNode();
        totalsBody.put("startDate", startDate);
        totalsBody.put("endDate", endDate);
        JsonNode totalsJson = query(accessToken, totalsBody);
        JsonNode totalsRow = totalsJson.path("rows").isArray() && totalsJson.path("rows").size() > 0
                ? totalsJson.path("rows").get(0)
                : null;

        ObjectNode seriesBody = objectMapper.createObjectNode();
        seriesBody.put("startDate", startDate);
        seriesBody.put("endDate", endDate);
        seriesBody.set("dimensions", stringArray("date"));
        seriesBody.put("rowLimit", 1000);
        JsonNode seriesJson = query(accessToken, seriesBody);

        List<SearchConsoleDailyPointDto> timeseries = new ArrayList<>();
        for (JsonNode row : seriesJson.path("rows")) {
            String date = row.path("keys").get(0).asText();
            timeseries.add(new SearchConsoleDailyPointDto(
                    date, clicks(row), impressions(row), ctr(row), position(row)));
        }

        return new SearchConsoleOverviewResponse(
                clicks(totalsRow), impressions(totalsRow), ctr(totalsRow), position(totalsRow), timeseries);
    }

    public List<SearchConsoleQueryDto> getTopQueries(String startDate, String endDate, int limit) {
        requireSiteConfigured();
        String accessToken = googleOAuthService.getValidAccessToken();

        ObjectNode body = objectMapper.createObjectNode();
        body.put("startDate", startDate);
        body.put("endDate", endDate);
        body.set("dimensions", stringArray("query"));
        body.put("rowLimit", limit);
        JsonNode json = query(accessToken, body);

        List<SearchConsoleQueryDto> result = new ArrayList<>();
        for (JsonNode row : json.path("rows")) {
            String queryText = row.path("keys").get(0).asText();
            result.add(new SearchConsoleQueryDto(queryText, clicks(row), impressions(row), ctr(row), position(row)));
        }
        return result;
    }

    public List<SearchConsolePageDto> getTopPages(String startDate, String endDate, int limit) {
        requireSiteConfigured();
        String accessToken = googleOAuthService.getValidAccessToken();

        ObjectNode body = objectMapper.createObjectNode();
        body.put("startDate", startDate);
        body.put("endDate", endDate);
        body.set("dimensions", stringArray("page"));
        body.put("rowLimit", limit);
        JsonNode json = query(accessToken, body);

        List<SearchConsolePageDto> result = new ArrayList<>();
        for (JsonNode row : json.path("rows")) {
            String page = row.path("keys").get(0).asText();
            result.add(new SearchConsolePageDto(page, clicks(row), impressions(row), ctr(row), position(row)));
        }
        return result;
    }

    private JsonNode query(String accessToken, ObjectNode body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        String encodedSiteUrl = URLEncoder.encode(siteUrl, StandardCharsets.UTF_8);
        // Dùng java.net.URI thay vì String url: RestTemplate re-encode chuỗi String truyền vào
        // (qua DefaultUriBuilderFactory), khiến encodedSiteUrl bị encode 2 lần (%3A -> %253A).
        // Truyền URI dựng sẵn thì RestTemplate dùng nguyên văn, không encode lại.
        URI uri = URI.create(String.format(QUERY_URL, encodedSiteUrl));
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    uri,
                    new HttpEntity<>(body.toString(), headers),
                    String.class);
            return objectMapper.readTree(response.getBody());
        } catch (RestClientException e) {
            log.error("[SearchConsole] Lỗi gọi searchAnalytics.query: {}", e.getMessage());
            throw new GoogleAnalyticsException(GoogleAnalyticsErrorCode.UPSTREAM_ERROR);
        } catch (Exception e) {
            log.error("[SearchConsole] Không đọc được phản hồi searchAnalytics.query: {}", e.getMessage());
            throw new GoogleAnalyticsException(GoogleAnalyticsErrorCode.UPSTREAM_ERROR);
        }
    }

    private ArrayNode stringArray(String... values) {
        ArrayNode array = objectMapper.createArrayNode();
        for (String value : values) {
            array.add(value);
        }
        return array;
    }

    private long clicks(JsonNode row) {
        return row == null ? 0L : row.path("clicks").asLong(0);
    }

    private long impressions(JsonNode row) {
        return row == null ? 0L : row.path("impressions").asLong(0);
    }

    private double ctr(JsonNode row) {
        return row == null ? 0d : row.path("ctr").asDouble(0);
    }

    private double position(JsonNode row) {
        return row == null ? 0d : row.path("position").asDouble(0);
    }
}
