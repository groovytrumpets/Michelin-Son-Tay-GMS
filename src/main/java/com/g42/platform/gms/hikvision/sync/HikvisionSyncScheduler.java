package com.g42.platform.gms.hikvision.sync;

import com.g42.platform.gms.hikvision.client.HikvisionApiClient;
import com.g42.platform.gms.hikvision.client.dto.HikvisionEvent;
import com.g42.platform.gms.hikvision.config.HikvisionProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled task that syncs attendance events from Hikvision by date.
 * Auto-scheduler runs daily at 00:05 to sync the previous day.
 * Manual trigger accepts a specific date.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HikvisionSyncScheduler {

    private final HikvisionApiClient apiClient;
    private final HikvisionEventProcessor eventProcessor;
    private final HikvisionProperties props;

    /** Runs every day at 00:05 to sync yesterday's attendance */
    @Scheduled(cron = "0 5 0 * * *")
    void runDailySync() {
        if (!props.isSyncEnabled()) {
            log.debug("HikvisionSyncScheduler: sync is disabled — skipping");
            return;
        }
        syncDate(LocalDate.now().minusDays(1));
    }

    /** Runs every 10 minutes during working hours (6:30–18:30) to keep today's data fresh */
    @Scheduled(cron = "0 */10 6-18 * * *")
    void runIntradaySync() {
        if (!props.isSyncEnabled()) return;
        syncDate(LocalDate.now());
    }

    /**
     * Manually trigger sync for a specific date.
     */
    public void syncDate(LocalDate date) {
        log.info("HikvisionSyncScheduler: starting sync for date={}", date);

        List<HikvisionEvent> events = apiClient.fetchEvents(date.atStartOfDay(), date.atTime(23, 59, 59));
        log.info("HikvisionSyncScheduler: fetched {} events for date={}", events.size(), date);

        if (!events.isEmpty()) {
            int recordsWritten = eventProcessor.processEvents(events);
            log.info("HikvisionSyncScheduler: sync complete — {} events processed, {} records written",
                    events.size(), recordsWritten);
        } else {
            log.info("HikvisionSyncScheduler: no events found for date={}", date);
        }
    }
}
