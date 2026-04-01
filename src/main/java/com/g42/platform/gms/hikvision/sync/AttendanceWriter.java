package com.g42.platform.gms.hikvision.sync;

import com.g42.platform.gms.manager.attendance.infrastructure.entity.AttendanceCheckinJpa;
import com.g42.platform.gms.manager.attendance.infrastructure.repository.AttendanceCheckinJpaRepo;
import com.g42.platform.gms.manager.schedule.infrastructure.entity.WorkShiftJpa;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

/**
 * Writes or updates attendance check-in/check-out records from Hikvision events.
 *
 * Logic:
 * - No existing record → create new check-in, calculate PRESENT/LATE status
 * - Existing record with check_in but no check_out → update check_out if scanTime >= checkIn + 2h
 * - Existing record with both → skip (idempotent)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AttendanceWriter {

    private static final String AUTO_SYNC_NOTE = "Auto sync from Hikvision";
    private static final long MIN_CHECKOUT_HOURS = 2;

    private final AttendanceCheckinJpaRepo checkinRepo;

    /**
     * Write attendance record for a single scan event.
     * Each call runs in its own transaction so one failure doesn't affect the batch.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void write(Integer staffId, Integer shiftId, LocalDateTime scanDateTime, WorkShiftJpa shift) {
        LocalDate attendanceDate = scanDateTime.toLocalDate();
        LocalTime scanTime = scanDateTime.toLocalTime();

        try {
            Optional<AttendanceCheckinJpa> existing =
                    checkinRepo.findByStaffIdAndAttendanceDateAndShiftId(staffId, attendanceDate, shiftId);

            if (existing.isEmpty()) {
                // Create new check-in record
                createCheckIn(staffId, shiftId, attendanceDate, scanTime, shift);
            } else {
                AttendanceCheckinJpa record = existing.get();
                if (record.getCheckOutTime() == null) {
                    // Update check-out if enough time has passed
                    updateCheckOut(record, scanTime);
                } else {
                    log.info("AttendanceWriter: record already complete for staffId={}, date={}, shiftId={} — skipping",
                            staffId, attendanceDate, shiftId);
                }
            }
        } catch (DataIntegrityViolationException e) {
            log.info("AttendanceWriter: duplicate record detected for staffId={}, date={}, shiftId={} — skipping",
                    staffId, attendanceDate, shiftId);
        }
    }

    private void createCheckIn(Integer staffId, Integer shiftId, LocalDate date,
                                LocalTime checkInTime, WorkShiftJpa shift) {
        String status = checkInTime.isAfter(shift.getStartTime()) ? "LATE" : "PRESENT";

        AttendanceCheckinJpa record = new AttendanceCheckinJpa();
        record.setStaffId(staffId);
        record.setAttendanceDate(date);
        record.setShiftId(shiftId);
        record.setCheckInTime(checkInTime);
        record.setStatus(status);
        record.setNotes(AUTO_SYNC_NOTE);
        record.setCreatedAt(LocalDateTime.now());

        checkinRepo.save(record);
        log.debug("AttendanceWriter: CHECK_IN staffId={}, shiftId={}, time={}, status={}",
                staffId, shiftId, checkInTime, status);
    }

    private void updateCheckOut(AttendanceCheckinJpa record, LocalTime scanTime) {
        LocalTime checkInTime = record.getCheckInTime();
        long minutesSinceCheckIn = java.time.Duration.between(checkInTime, scanTime).toMinutes();

        if (minutesSinceCheckIn < MIN_CHECKOUT_HOURS * 60) {
            log.debug("AttendanceWriter: scan too close to check-in time ({}min) for staffId={} — skipping checkout",
                    minutesSinceCheckIn, record.getStaffId());
            return;
        }

        record.setCheckOutTime(scanTime);
        checkinRepo.save(record);
        log.debug("AttendanceWriter: CHECK_OUT staffId={}, shiftId={}, time={}",
                record.getStaffId(), record.getShiftId(), scanTime);
    }
}
