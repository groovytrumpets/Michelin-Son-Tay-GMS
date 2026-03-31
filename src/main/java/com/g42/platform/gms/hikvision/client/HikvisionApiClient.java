package com.g42.platform.gms.hikvision.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.g42.platform.gms.hikvision.client.dto.HikvisionEvent;
import com.g42.platform.gms.hikvision.client.dto.HikvisionEventResponse;
import com.g42.platform.gms.hikvision.config.HikvisionProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
     * within the given time window.
     */
    public List<HikvisionEvent> fetchEvents(LocalDateTime from, LocalDateTime to) {
        String startTime = URLEncoder.encode(from.format(HIKVISION_FORMATTER) + "+07:00", StandardCharsets.UTF_8);
        String endTime = URLEncoder.encode(to.format(HIKVISION_FORMATTER) + "+07:00", StandardCharsets.UTF_8);

        String url = String.format("http://%s:%d/ISAPI/AccessControl/AcsEvent?format=json" +
                        "&major=5&minor=38&startTime=%s&endTime=%s",
                props.getHost(), props.getPort(), startTime, endTime);

        HttpGet request = new HttpGet(url);

        try {
            return hikvisionHttpClient.execute(request, response -> {
                int statusCode = response.getCode();
                if (statusCode != 200) {
                    log.error("Hikvision API returned HTTP {}: {}", statusCode, url);
                    return Collections.emptyList();
                }

                String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                HikvisionEventResponse eventResponse = objectMapper.readValue(body, HikvisionEventResponse.class);

                if (eventResponse.getAcsEventCond() == null
                        || eventResponse.getAcsEventCond().getInfoList() == null) {
                    return Collections.emptyList();
                }

                List<HikvisionEvent> events = eventResponse.getAcsEventCond().getInfoList();
                log.debug("Fetched {} events from Hikvision device", events.size());
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
