package com.g42.platform.gms.analytics.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.g42.platform.gms.analytics.dto.Ga4DailyPointDto;
import com.g42.platform.gms.analytics.dto.Ga4OverviewResponse;
import com.g42.platform.gms.analytics.dto.Ga4TopPageDto;
import com.g42.platform.gms.analytics.dto.Ga4TrafficSourceDto;
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

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Gọi thẳng GA4 Data API (REST, không dùng google-api-client) để lấy số liệu Google
 * Analytics cho trang quản trị. Access token lấy qua GoogleOAuthService (tự refresh).
 * Tài liệu: https://developers.google.com/analytics/devguides/reporting/data/v1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleAnalyticsDataService {

    private static final String RUN_REPORT_URL = "https://analyticsdata.googleapis.com/v1beta/properties/%s:runReport";
    private static final DateTimeFormatter GA4_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final GoogleOAuthService googleOAuthService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${google.analytics.ga4.property-id:}")
    private String propertyId;

    public boolean isPropertyConfigured() {
        return propertyId != null && !propertyId.isBlank();
    }

    private void requirePropertyConfigured() {
        if (!isPropertyConfigured()) {
            throw new GoogleAnalyticsException(GoogleAnalyticsErrorCode.PROPERTY_NOT_CONFIGURED);
        }
    }

    public Ga4OverviewResponse getOverview(String startDate, String endDate) {
        requirePropertyConfigured();
        String accessToken = googleOAuthService.getValidAccessToken();

        ObjectNode totalsBody = objectMapper.createObjectNode();
        totalsBody.set("dateRanges", dateRangeArray(startDate, endDate));
        totalsBody.set("metrics", metricArray("activeUsers", "newUsers", "sessions", "screenPageViews",
                "averageSessionDuration", "bounceRate", "engagementRate"));
        JsonNode totalsJson = runReport(accessToken, totalsBody);
        JsonNode totalsRow = totalsJson.path("rows").isArray() && totalsJson.path("rows").size() > 0
                ? totalsJson.path("rows").get(0)
                : null;

        long activeUsers = metricLong(totalsRow, 0);
        long newUsers = metricLong(totalsRow, 1);
        long sessions = metricLong(totalsRow, 2);
        long pageViews = metricLong(totalsRow, 3);
        double avgSessionDuration = metricDouble(totalsRow, 4);
        double bounceRate = metricDouble(totalsRow, 5);
        double engagementRate = metricDouble(totalsRow, 6);

        ObjectNode seriesBody = objectMapper.createObjectNode();
        seriesBody.set("dateRanges", dateRangeArray(startDate, endDate));
        seriesBody.set("dimensions", dimensionArray("date"));
        seriesBody.set("metrics", metricArray("activeUsers", "sessions", "screenPageViews"));
        seriesBody.set("orderBys", dateOrderAsc());
        JsonNode seriesJson = runReport(accessToken, seriesBody);

        List<Ga4DailyPointDto> timeseries = new ArrayList<>();
        for (JsonNode row : seriesJson.path("rows")) {
            String rawDate = row.path("dimensionValues").get(0).path("value").asText();
            timeseries.add(new Ga4DailyPointDto(
                    formatGa4Date(rawDate),
                    metricLong(row, 0),
                    metricLong(row, 1),
                    metricLong(row, 2)));
        }

        return new Ga4OverviewResponse(activeUsers, newUsers, sessions, pageViews, avgSessionDuration, bounceRate, engagementRate, timeseries);
    }

    public List<Ga4TopPageDto> getTopPages(String startDate, String endDate, int limit) {
        requirePropertyConfigured();
        String accessToken = googleOAuthService.getValidAccessToken();

        ObjectNode body = objectMapper.createObjectNode();
        body.set("dateRanges", dateRangeArray(startDate, endDate));
        body.set("dimensions", dimensionArray("pagePath", "pageTitle"));
        body.set("metrics", metricArray("screenPageViews", "activeUsers"));
        body.set("orderBys", metricOrderDesc("screenPageViews"));
        body.put("limit", limit);

        JsonNode json = runReport(accessToken, body);
        List<Ga4TopPageDto> result = new ArrayList<>();
        for (JsonNode row : json.path("rows")) {
            String pagePath = row.path("dimensionValues").get(0).path("value").asText();
            String pageTitle = row.path("dimensionValues").get(1).path("value").asText();
            result.add(new Ga4TopPageDto(pagePath, pageTitle, metricLong(row, 0), metricLong(row, 1)));
        }
        return result;
    }

    public List<Ga4TrafficSourceDto> getTrafficSources(String startDate, String endDate) {
        requirePropertyConfigured();
        String accessToken = googleOAuthService.getValidAccessToken();

        ObjectNode body = objectMapper.createObjectNode();
        body.set("dateRanges", dateRangeArray(startDate, endDate));
        body.set("dimensions", dimensionArray("sessionDefaultChannelGroup"));
        body.set("metrics", metricArray("sessions", "activeUsers"));
        body.set("orderBys", metricOrderDesc("sessions"));
        body.put("limit", 10);

        JsonNode json = runReport(accessToken, body);
        List<Ga4TrafficSourceDto> result = new ArrayList<>();
        for (JsonNode row : json.path("rows")) {
            String channel = row.path("dimensionValues").get(0).path("value").asText();
            result.add(new Ga4TrafficSourceDto(channel, metricLong(row, 0), metricLong(row, 1)));
        }
        return result;
    }

    private JsonNode runReport(String accessToken, ObjectNode body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    String.format(RUN_REPORT_URL, propertyId),
                    new HttpEntity<>(body.toString(), headers),
                    String.class);
            return objectMapper.readTree(response.getBody());
        } catch (RestClientException e) {
            log.error("[GA4] Lỗi gọi runReport: {}", e.getMessage());
            throw new GoogleAnalyticsException(GoogleAnalyticsErrorCode.UPSTREAM_ERROR);
        } catch (Exception e) {
            log.error("[GA4] Không đọc được phản hồi runReport: {}", e.getMessage());
            throw new GoogleAnalyticsException(GoogleAnalyticsErrorCode.UPSTREAM_ERROR);
        }
    }

    private ArrayNode dateRangeArray(String startDate, String endDate) {
        ArrayNode array = objectMapper.createArrayNode();
        ObjectNode range = array.addObject();
        range.put("startDate", startDate);
        range.put("endDate", endDate);
        return array;
    }

    private ArrayNode dimensionArray(String... names) {
        ArrayNode array = objectMapper.createArrayNode();
        for (String name : names) {
            array.addObject().put("name", name);
        }
        return array;
    }

    private ArrayNode metricArray(String... names) {
        ArrayNode array = objectMapper.createArrayNode();
        for (String name : names) {
            array.addObject().put("name", name);
        }
        return array;
    }

    private ArrayNode dateOrderAsc() {
        ArrayNode array = objectMapper.createArrayNode();
        ObjectNode order = array.addObject();
        order.putObject("dimension").put("dimensionName", "date");
        order.put("desc", false);
        return array;
    }

    private ArrayNode metricOrderDesc(String metricName) {
        ArrayNode array = objectMapper.createArrayNode();
        ObjectNode order = array.addObject();
        order.putObject("metric").put("metricName", metricName);
        order.put("desc", true);
        return array;
    }

    private long metricLong(JsonNode row, int index) {
        if (row == null) return 0L;
        JsonNode value = row.path("metricValues").path(index).path("value");
        return value.isMissingNode() ? 0L : (long) Double.parseDouble(value.asText("0"));
    }

    private double metricDouble(JsonNode row, int index) {
        if (row == null) return 0d;
        JsonNode value = row.path("metricValues").path(index).path("value");
        return value.isMissingNode() ? 0d : Double.parseDouble(value.asText("0"));
    }

    private String formatGa4Date(String rawDate) {
        try {
            return java.time.LocalDate.parse(rawDate, GA4_DATE).toString();
        } catch (Exception e) {
            return rawDate;
        }
    }
}
