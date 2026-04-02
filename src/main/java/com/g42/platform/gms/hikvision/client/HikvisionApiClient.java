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
import java.util.ArrayList;
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
    private static final int PAGE_SIZE = 50;

    public List<HikvisionEvent> fetchEvents(LocalDateTime from, LocalDateTime to) {
        String url = String.format("https://%s:%d/ISAPI/AccessControl/AcsEvent?format=json",
                props.getHost(), props.getPort());
        String startTime = from.format(HIKVISION_FORMATTER) + "+07:00";
        String endTime   = to.format(HIKVISION_FORMATTER)   + "+07:00";

        List<HikvisionEvent> allEvents = new ArrayList<>();
        int position = 0;
        boolean hasMore = true;

        while (hasMore) {
            final int pos = position;
            String reqBody = String.format(
                "{\"AcsEventCond\":{\"searchID\":\"1\",\"searchResultPosition\":%d,\"maxResults\":%d,\"major\":0,\"minor\":0,\"startTime\":\"%s\",\"endTime\":\"%s\"}}",
                pos, PAGE_SIZE, startTime, endTime);

            HttpPost request = new HttpPost(url);
            request.setEntity(new StringEntity(reqBody, ContentType.APPLICATION_JSON));

            try {
                HikvisionEventResponse.AcsEvent page = hikvisionHttpClient.execute(request, response -> {
                    if (response.getCode() != 200) {
                        log.error("Hikvision HTTP {} at pos {}", response.getCode(), pos);
                        return null;
                    }
                    String rb = EntityUtils.toString(response.getEntity(), java.nio.charset.StandardCharsets.UTF_8);
                    return objectMapper.readValue(rb, HikvisionEventResponse.class).getAcsEvent();
                });

                if (page == null || page.getInfoList() == null || page.getInfoList().isEmpty()) break;

                log.info("Hikvision page: pos={}, inPage={}, total={}, status={}",
                        pos, page.getInfoList().size(), page.getTotalMatches(), page.getResponseStatusStrg());

                page.getInfoList().forEach(e -> {
                    if (e.getEmployeeNoString() != null && !e.getEmployeeNoString().isBlank()) {
                        log.info("  employee event: no={}, major={}, minor={}, name={}, time={}",
                                e.getEmployeeNoString(), e.getMajor(), e.getMinor(), e.getName(), e.getDateTime());
                    }
                });

                page.getInfoList().stream()
                        .filter(e -> e.getMajor() != null && e.getMajor() == 5
                                && e.getMinor() != null && (e.getMinor() == 75 || e.getMinor() == 76 || e.getMinor() == 77 || e.getMinor() == 79)
                                && e.getEmployeeNoString() != null && !e.getEmployeeNoString().isBlank())
                        .forEach(allEvents::add);

                int fetched = page.getInfoList().size();
                position += fetched;
                hasMore = "MORE".equalsIgnoreCase(page.getResponseStatusStrg()) && fetched == PAGE_SIZE;

            } catch (Exception e) {
                log.error("Hikvision fetch error at pos {}: {}", pos, e.getMessage(), e);
                break;
            }
        }

        log.info("Hikvision fetchEvents done: collected={}", allEvents.size());
        return allEvents;
    }
}
