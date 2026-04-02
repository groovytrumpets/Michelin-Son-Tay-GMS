package com.g42.platform.gms.hikvision.sync;

import com.g42.platform.gms.auth.entity.StaffProfile;
import com.g42.platform.gms.auth.repository.StaffProfileRepo;
import com.g42.platform.gms.hikvision.client.dto.HikvisionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Processes Hikvision attendance events.
 *
 * Logic mirrors the Hikvision device:
 * - First scan of the day = check-in
 * - Last scan of the day = check-out
 * - 1 record per staff per day
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HikvisionEventProcessor {

    private final StaffProfileRepo staffProfileRepo;
    private final AttendanceWriter attendanceWriter;

    public int processEvents(List<HikvisionEvent> events) {
        if (events == null || events.isEmpty()) return 0;

        int successCount = 0;
        for (HikvisionEvent event : events) {
            if (processEvent(event)) successCount++;
        }
        return successCount;
    }

    private boolean processEvent(HikvisionEvent event) {
        String employeeNo = event.getEmployeeNoString();
        if (employeeNo == null || employeeNo.isBlank()) return false;

        LocalDateTime scanDateTime;
        try {
            scanDateTime = OffsetDateTime.parse(event.getDateTime(),
                    DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDateTime();
        } catch (Exception e) {
            log.warn("HikvisionEventProcessor: cannot parse dateTime='{}' for employeeNo={} — skipping",
                    event.getDateTime(), employeeNo);
            return false;
        }

        Optional<StaffProfile> staffOpt = staffProfileRepo.findByEmployeeNo(employeeNo);
        if (staffOpt.isEmpty()) {
            log.warn("HikvisionEventProcessor: no staff found for employeeNo={} — skipping", employeeNo);
            return false;
        }

        attendanceWriter.write(staffOpt.get().getStaffId(), scanDateTime);
        return true;
    }
}
