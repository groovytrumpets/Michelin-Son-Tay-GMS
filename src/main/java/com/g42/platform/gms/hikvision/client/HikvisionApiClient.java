package com.g42.platform.gms.hikvision.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.g42.platform.gms.hikvision.client.dto.HikvisionEvent;
import com.g42.platform.gms.hikvision.client.dto.HikvisionEventResponse;
import com.g42.platform.gms.hikvision.config.HikvisionProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class HikvisionApiClient {

    private final CloseableHttpClient hikvisionHttpClient;
    private final HikvisionProperties props;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter HIKVISION_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /**
     * Fetch authentication events (major=5, minor=38) from Hikvision device
     * within the given time window using POST with JSON body.
     */
    public List<HikvisionEvent> fetchEvents(LocalDateTime from, LocalDateTime to) {
        String url = String.format("https://%s:%d/ISAPI/AccessControl/AcsEvent?format=json",
                props.getHost(), props.getPort());

        String startTime = from.format(HIKVISION_FORMATTER) + "+07:00";
        String endTime = to.format(HIKVISION_FORMATTER) + "+07:00";

        // Hikvision requires POST with JSON body for event search
        // Note: major/minor filtering in body is not supported by all devices
        // We fetch all events and filter client-side
        String requestBody = String.format("""
                {
                  "AcsEventCond": {
                    "searchID": "1",
                    "searchResultPosition": 0,
                    "maxResults": 100,
                    "major": 0,
                    "minor": 0,
                    "startTime": "%s",
                    "endTime": "%s"
                  }
                }
                """, startTime, endTime);

        HttpPost request = new HttpPost(url);
        request.setEntity(new StringEntity(requestBody, ContentType.APPLICATION_JSON));

        try {
            return hikvisionHttpClient.execute(request, response -> {
                int statusCode = response.getCode();
                if (statusCode != 200) {
                    log.error("Hikvision API returned HTTP {}: {}", statusCode, url);
                    return Collections.emptyList();
                }

                String body = EntityUtils.toString(response.getEntity(), java.nio.charset.StandardCharsets.UTF_8);
                HikvisionEventResponse eventResponse = objectMapper.readValue(body, HikvisionEventResponse.class);

                if (eventResponse.getAcsEvent() == null
                        || eventResponse.getAcsEvent().getInfoList() == null) {
                    return Collections.emptyList();
                }

                List<HikvisionEvent> events = eventResponse.getAcsEvent().getInfoList().stream()
                        .filter(e -> e.getMajor() != null && e.getMajor() == 5
                                && e.getMinor() != null && e.getMinor() == 38)
                        .toList();
                log.debug("Fetched {} attendance events from Hikvision device", events.size());
                return events;
            });
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("timeout")) {
                log.warn("Hikvision device connection timeout: {}", e.getMessage());
            } else {
                log.error("Error fetching events from Hikvision device: {}", e.getMessage(), e);
            }
            return Collections.emptyList();
        }
    }
}
