package com.g42.platform.gms.hikvision.sync;

import com.g42.platform.gms.auth.entity.StaffProfile;
import com.g42.platform.gms.auth.repository.StaffProfileRepo;
import com.g42.platform.gms.hikvision.client.dto.HikvisionEvent;
import com.g42.platform.gms.manager.schedule.infrastructure.entity.WorkShiftJpa;
import com.g42.platform.gms.manager.schedule.infrastructure.repository.WorkShiftJpaRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Processes Hikvision attendance events:
 * 1. Debounce check (60s per employee)
 * 2. Lookup staff by employee_no
 * 3. Resolve shift from scan time
 * 4. Write attendance record
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HikvisionEventProcessor {

    private static final long DEBOUNCE_SECONDS = 60;

    private final StaffProfileRepo staffProfileRepo;
    private final WorkShiftJpaRepo workShiftJpaRepo;
    private final ShiftResolver shiftResolver;
    private final AttendanceWriter attendanceWriter;

    // In-memory debounce map: employeeNoString -> last processed time
    private final Map<String, LocalDateTime> debounceMap = new ConcurrentHashMap<>();

    /**
     * Process a batch of Hikvision events.
     *
     * @return number of records successfully written
     */
    public int processEvents(List<HikvisionEvent> events) {
        if (events == null || events.isEmpty()) {
            return 0;
        }

        List<WorkShiftJpa> activeShifts = workShiftJpaRepo.findAll().stream()
                .filter(s -> Boolean.TRUE.equals(s.getIsActive()))
                .toList();

        if (activeShifts.isEmpty()) {
            log.warn("HikvisionEventProcessor: no active shifts found — skipping all {} events", events.size());
            return 0;
        }

        int successCount = 0;
        for (HikvisionEvent event : events) {
            if (processEvent(event, activeShifts)) {
                successCount++;
            }
        }
        return successCount;
    }

    private boolean processEvent(HikvisionEvent event, List<WorkShiftJpa> activeShifts) {
        String employeeNo = event.getEmployeeNoString();
        if (employeeNo == null || employeeNo.isBlank()) {
            log.warn("HikvisionEventProcessor: event has null/blank employeeNoString — skipping");
            return false;
        }

        // Parse scan time
        LocalDateTime scanDateTime;
        try {
            scanDateTime = OffsetDateTime.parse(event.getDateTime(),
                    DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDateTime();
        } catch (Exception e) {
            log.warn("HikvisionEventProcessor: cannot parse dateTime='{}' for employeeNo={} — skipping",
                    event.getDateTime(), employeeNo);
            return false;
        }

        // Debounce check
        if (isDebounced(employeeNo, scanDateTime)) {
            log.debug("HikvisionEventProcessor: debounced event for employeeNo={} at {}", employeeNo, scanDateTime);
            return false;
        }

        // Lookup staff by employee_no
        Optional<StaffProfile> staffOpt = staffProfileRepo.findByEmployeeNo(employeeNo);
        if (staffOpt.isEmpty()) {
            log.warn("HikvisionEventProcessor: no staff found for employeeNo={} at {} — skipping",
                    employeeNo, scanDateTime);
            return false;
        }

        // Resolve shift
        Optional<WorkShiftJpa> shiftOpt = shiftResolver.resolve(scanDateTime.toLocalTime(), activeShifts);
        if (shiftOpt.isEmpty()) {
            log.warn("HikvisionEventProcessor: no shift resolved for employeeNo={} at {} — skipping",
                    employeeNo, scanDateTime);
            return false;
        }

        // Write attendance
        StaffProfile staff = staffOpt.get();
        WorkShiftJpa shift = shiftOpt.get();
        attendanceWriter.write(staff.getStaffId(), shift.getShiftId(), scanDateTime, shift);

        // Update debounce map after successful processing
        debounceMap.put(employeeNo, scanDateTime);
        return true;
    }

    private boolean isDebounced(String employeeNo, LocalDateTime scanDateTime) {
        LocalDateTime lastProcessed = debounceMap.get(employeeNo);
        if (lastProcessed == null) {
            return false;
        }
        long secondsSinceLast = java.time.Duration.between(lastProcessed, scanDateTime).toSeconds();
        return secondsSinceLast < DEBOUNCE_SECONDS;
    }
}
