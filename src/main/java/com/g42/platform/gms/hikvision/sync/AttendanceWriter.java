package com.g42.platform.gms.hikvision.sync;

import com.g42.platform.gms.manager.attendance.infrastructure.entity.AttendanceCheckinJpa;
import com.g42.platform.gms.manager.attendance.infrastructure.repository.AttendanceCheckinJpaRepo;
import com.g42.platform.gms.manager.schedule.infrastructure.entity.WorkShiftJpa;
import com.g42.platform.gms.manager.schedule.infrastructure.repository.WorkShiftJpaRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class AttendanceWriter {

    private static final String AUTO_SYNC_NOTE = "Auto sync from Hikvision";
    private static final int BOUNDARY_MINUTES = 30;
    private static final int LATE_GRACE_MINUTES = 15;

    private final AttendanceCheckinJpaRepo checkinRepo;
    private final WorkShiftJpaRepo workShiftRepo;

    @Transactional
    public void writeDay(Integer staffId, LocalDate date, List<LocalTime> scanTimes) {
        if (scanTimes == null || scanTimes.isEmpty()) return;

        List<WorkShiftJpa> shifts = workShiftRepo.findAll().stream()
                .filter(s -> Boolean.TRUE.equals(s.getIsActive()))
                .sorted(Comparator.comparing(WorkShiftJpa::getStartTime))
                .collect(Collectors.toList());

        if (shifts.isEmpty()) return;

        Map<Integer, List<LocalTime>> scansByShift = new LinkedHashMap<>();
        for (WorkShiftJpa shift : shifts) {
            scansByShift.put(shift.getShiftId(), new ArrayList<>());
        }
        for (LocalTime scan : scanTimes) {
            WorkShiftJpa assigned = assignToShift(scan, shifts);
            if (assigned != null) {
                scansByShift.get(assigned.getShiftId()).add(scan);
            }
        }

        List<AttendanceCheckinJpa> existing = checkinRepo.findByStaffIdAndAttendanceDate(staffId, date);
        // Map Hikvision records by shiftId
        Map<Integer, AttendanceCheckinJpa> hikvisionByShift = existing.stream()
                .filter(r -> AUTO_SYNC_NOTE.equals(r.getNotes()) && r.getShiftId() != null)
                .collect(Collectors.toMap(AttendanceCheckinJpa::getShiftId, r -> r, (a, b) -> a));
        // Set of shiftIds already handled by manual/edited records
        Set<Integer> manualShiftIds = existing.stream()
                .filter(r -> !AUTO_SYNC_NOTE.equals(r.getNotes()) && r.getShiftId() != null)
                .map(AttendanceCheckinJpa::getShiftId)
                .collect(Collectors.toSet());

        for (WorkShiftJpa shift : shifts) {
            List<LocalTime> scans = scansByShift.get(shift.getShiftId());
            if (scans.isEmpty()) continue;

            // Skip if this shift already has a manual/edited record
            if (manualShiftIds.contains(shift.getShiftId())) {
                log.debug("AttendanceWriter: skip staffId={} shift={} - already edited by manager",
                        staffId, shift.getShiftName());
                continue;
            }

            LocalTime checkIn = scans.get(0);
            LocalTime checkOut;
            if (scans.size() == 1) {
                checkOut = checkIn.isAfter(shift.getEndTime()) ? checkIn : shift.getEndTime();
            } else {
                checkOut = scans.get(scans.size() - 1);
            }

            LocalTime lateThreshold = shift.getStartTime().plusMinutes(LATE_GRACE_MINUTES);
            String status = checkIn.isAfter(lateThreshold) ? "LATE" : "PRESENT";

            AttendanceCheckinJpa record = hikvisionByShift.get(shift.getShiftId());
            if (record == null) {
                record = new AttendanceCheckinJpa();
                record.setStaffId(staffId);
                record.setAttendanceDate(date);
                record.setShiftId(shift.getShiftId());
                record.setNotes(AUTO_SYNC_NOTE);
                record.setCreatedAt(LocalDateTime.now());
            }
            record.setCheckInTime(checkIn);
            record.setCheckOutTime(checkOut);
            record.setStatus(status);
            checkinRepo.save(record);

            log.debug("AttendanceWriter: staffId={} date={} shift={} in={} out={} status={}",
                    staffId, date, shift.getShiftName(), checkIn, checkOut, status);
        }
    }

    private WorkShiftJpa assignToShift(LocalTime scan, List<WorkShiftJpa> shifts) {
        for (int i = shifts.size() - 1; i >= 0; i--) {
            LocalTime boundary = (i == 0)
                    ? shifts.get(0).getStartTime().minusMinutes(60)
                    : shifts.get(i).getStartTime().minusMinutes(BOUNDARY_MINUTES);
            if (!scan.isBefore(boundary)) {
                return shifts.get(i);
            }
        }
        return shifts.get(0);
    }
}
