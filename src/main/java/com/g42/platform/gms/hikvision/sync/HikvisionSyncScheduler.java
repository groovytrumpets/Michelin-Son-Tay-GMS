package com.g42.platform.gms.hikvision.sync;

import com.g42.platform.gms.hikvision.client.HikvisionApiClient;
import com.g42.platform.gms.hikvision.client.dto.HikvisionEvent;
import com.g42.platform.gms.hikvision.config.HikvisionProperties;
import com.g42.platform.gms.manager.attendance.infrastructure.repository.AttendanceCheckinJpaRepo;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Scheduled task that pulls attendance events from Hikvision device every 5 minutes.
 *
 * Sync_State: lastSyncTime is stored in-memory (AtomicReference).
 * On startup: initialized from MAX(created_at) WHERE notes='Auto sync from Hikvision'.
 * Fallback: now() - 1 hour if no records exist.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HikvisionSyncScheduler {

    private static final String AUTO_SYNC_NOTE = "Auto sync from Hikvision";

    private final HikvisionApiClient apiClient;
    private final HikvisionEventProcessor eventProcessor;
    private final HikvisionProperties props;
    private final AttendanceCheckinJpaRepo checkinRepo;

    private final AtomicReference<LocalDateTime> lastSyncTime = new AtomicReference<>();

    @PostConstruct
    void initLastSyncTime() {
        LocalDateTime initialTime = checkinRepo.findMaxCreatedAtByNotes(AUTO_SYNC_NOTE)
                .orElse(LocalDateTime.now().minusHours(1));
        lastSyncTime.set(initialTime);
        log.info("HikvisionSyncScheduler: initialized lastSyncTime={}, syncEnabled={}", initialTime, props.isSyncEnabled());
    }

    @Scheduled(fixedDelay = 300_000)
    void runSync() {
        if (!props.isSyncEnabled()) {
            log.debug("HikvisionSyncScheduler: sync is disabled — skipping");
            return;
        }

        LocalDateTime from = lastSyncTime.get();
        LocalDateTime to = LocalDateTime.now();

        log.info("HikvisionSyncScheduler: starting sync from={} to={}", from, to);

        List<HikvisionEvent> events = apiClient.fetchEvents(from, to);
        log.info("HikvisionSyncScheduler: fetched {} events", events.size());

        if (!events.isEmpty()) {
            int successCount = eventProcessor.processEvents(events);
            log.info("HikvisionSyncScheduler: sync complete — {} events processed, {} records written",
                    events.size(), successCount);
        } else {
            log.info("HikvisionSyncScheduler: no new events found");
        }

        lastSyncTime.set(to);
    }
}
