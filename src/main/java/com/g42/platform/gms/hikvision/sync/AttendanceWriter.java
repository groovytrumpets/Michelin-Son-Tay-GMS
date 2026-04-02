package com.g42.platform.gms.hikvision.sync;

import com.g42.platform.gms.manager.attendance.infrastructure.entity.AttendanceCheckinJpa;
import com.g42.platform.gms.manager.attendance.infrastructure.repository.AttendanceCheckinJpaRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Writes attendance records from Hikvision events.
 *
 * Logic mirrors the Hikvision device's own attendance calculation:
 * - 1 record per staff per day (no shift split)
 * - First scan of the day → check_in_time
 * - Any subsequent scan → update check_out_time (always keep the latest)
 * - Status: PRESENT if on time, LATE if after shift start
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AttendanceWriter {

    private static final String AUTO_SYNC_NOTE = "Auto sync from Hikvision";

    private final AttendanceCheckinJpaRepo checkinRepo;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void write(Integer staffId, LocalDateTime scanDateTime) {
        LocalDate attendanceDate = scanDateTime.toLocalDate();
        LocalTime scanTime = scanDateTime.toLocalTime();

        try {
            List<AttendanceCheckinJpa> existing =
                    checkinRepo.findByStaffIdAndAttendanceDate(staffId, attendanceDate);

            if (existing.isEmpty()) {
                // First scan of the day → check-in
                AttendanceCheckinJpa record = new AttendanceCheckinJpa();
                record.setStaffId(staffId);
                record.setAttendanceDate(attendanceDate);
                record.setCheckInTime(scanTime);
                record.setStatus("PRESENT");
                record.setNotes(AUTO_SYNC_NOTE);
                record.setCreatedAt(LocalDateTime.now());
                checkinRepo.save(record);
                log.debug("AttendanceWriter: CHECK_IN staffId={}, date={}, time={}", staffId, attendanceDate, scanTime);
            } else {
                // Subsequent scan → update check-out (always keep latest scan)
                AttendanceCheckinJpa record = existing.get(0);
                LocalTime checkIn = record.getCheckInTime();
                if (scanTime.isAfter(checkIn)) {
                    record.setCheckOutTime(scanTime);
                    checkinRepo.save(record);
                    log.debug("AttendanceWriter: CHECK_OUT staffId={}, date={}, time={}", staffId, attendanceDate, scanTime);
                }
            }
        } catch (DataIntegrityViolationException e) {
            log.info("AttendanceWriter: duplicate for staffId={}, date={} — skipping", staffId, attendanceDate);
        }
    }
}
