package com.g42.platform.gms.hikvision.sync;

import com.g42.platform.gms.auth.entity.StaffProfile;
import com.g42.platform.gms.auth.repository.StaffProfileRepo;
import com.g42.platform.gms.hikvision.client.dto.HikvisionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Processes Hikvision attendance events.
 * Groups events by (staffId, date) then calls AttendanceWriter.writeDay().
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HikvisionEventProcessor {

    private final StaffProfileRepo staffProfileRepo;
    private final AttendanceWriter attendanceWriter;

    public int processEvents(List<HikvisionEvent> events) {
        if (events == null || events.isEmpty()) return 0;

        // Parse và sort tăng dần
        record ScanEntry(Integer staffId, LocalDate date, LocalTime time) {}

        List<ScanEntry> entries = new ArrayList<>();
        for (HikvisionEvent event : events) {
            String employeeNo = event.getEmployeeNoString();
            if (employeeNo == null || employeeNo.isBlank()) continue;

            LocalDateTime scanDateTime;
            try {
                scanDateTime = OffsetDateTime.parse(event.getDateTime(),
                        DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDateTime();
            } catch (Exception e) {
                log.warn("HikvisionEventProcessor: cannot parse dateTime='{}' — skipping", event.getDateTime());
                continue;
            }

            Optional<StaffProfile> staffOpt = staffProfileRepo.findByEmployeeNo(employeeNo);
            if (staffOpt.isEmpty()) {
                log.warn("HikvisionEventProcessor: no staff for employeeNo={} — skipping", employeeNo);
                continue;
            }

            entries.add(new ScanEntry(
                    staffOpt.get().getStaffId(),
                    scanDateTime.toLocalDate(),
                    scanDateTime.toLocalTime()
            ));
        }

        if (entries.isEmpty()) return 0;

        // Group by (staffId, date)
        Map<String, List<ScanEntry>> grouped = entries.stream()
                .collect(Collectors.groupingBy(e -> e.staffId() + "_" + e.date()));

        int processedDays = 0;
        for (Map.Entry<String, List<ScanEntry>> group : grouped.entrySet()) {
            List<ScanEntry> dayEntries = group.getValue();
            Integer staffId = dayEntries.get(0).staffId();
            LocalDate date = dayEntries.get(0).date();

            // Sort scan times tăng dần
            List<LocalTime> scanTimes = dayEntries.stream()
                    .map(ScanEntry::time)
                    .sorted()
                    .collect(Collectors.toList());

            log.info("HikvisionEventProcessor: staffId={} date={} scans={}", staffId, date, scanTimes);
            attendanceWriter.writeDay(staffId, date, scanTimes);
            processedDays++;
        }

        return processedDays;
    }
}
