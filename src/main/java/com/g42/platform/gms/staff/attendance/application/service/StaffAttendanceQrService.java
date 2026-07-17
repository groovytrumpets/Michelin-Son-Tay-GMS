package com.g42.platform.gms.staff.attendance.application.service;

import com.g42.platform.gms.auth.entity.StaffProfile;
import com.g42.platform.gms.auth.repository.StaffProfileRepo;
import com.g42.platform.gms.manager.attendance.api.dto.AttendanceCheckinResponse;
import com.g42.platform.gms.manager.attendance.api.mapper.AttendanceDtoMapper;
import com.g42.platform.gms.manager.attendance.domain.entity.AttendanceCheckin;
import com.g42.platform.gms.manager.attendance.domain.exception.AttendanceErrorCode;
import com.g42.platform.gms.manager.attendance.domain.exception.AttendanceException;
import com.g42.platform.gms.manager.attendance.domain.repository.AttendanceCheckinRepo;
import com.g42.platform.gms.manager.attendancelocation.domain.entity.AttendanceLocation;
import com.g42.platform.gms.manager.attendancelocation.domain.repository.AttendanceLocationRepo;
import com.g42.platform.gms.manager.schedule.domain.entity.WorkShift;
import com.g42.platform.gms.manager.schedule.domain.repository.WorkShiftRepo;
import com.g42.platform.gms.staff.attendance.api.dto.QrAttendanceRequest;
import com.g42.platform.gms.staff.attendance.api.dto.QrStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StaffAttendanceQrService {

    private static final double EARTH_RADIUS_METERS = 6371000;
    private static final int LATE_THRESHOLD_MINUTES = 15;

    private final AttendanceLocationRepo locationRepo;
    private final AttendanceCheckinRepo checkinRepo;
    private final WorkShiftRepo workShiftRepo;
    private final StaffProfileRepo staffProfileRepo;
    private final AttendanceDtoMapper attendanceDtoMapper;

    public QrStatusResponse getStatus(Integer staffId, String qrToken) {
        AttendanceLocation location = findActiveLocation(qrToken);
        LocalDate today = LocalDate.now();
        List<AttendanceCheckin> todayRecords = checkinRepo.findByStaffAndDateRange(staffId, today, today);
        Optional<AttendanceCheckin> openRecord = todayRecords.stream()
                .filter(c -> c.getCheckOutTime() == null)
                .findFirst();

        QrStatusResponse response = new QrStatusResponse();
        response.setLocationId(location.getLocationId());
        response.setLocationName(location.getLocationName());
        response.setAddress(location.getAddress());
        response.setAlreadyCheckedIn(!todayRecords.isEmpty());
        response.setAlreadyCheckedOut(!todayRecords.isEmpty() && openRecord.isEmpty());
        openRecord.ifPresent(c -> response.setCheckinId(c.getCheckinId()));
        return response;
    }

    @Transactional
    public AttendanceCheckinResponse checkIn(Integer staffId, QrAttendanceRequest request) {
        AttendanceLocation location = findActiveLocation(request.getQrToken());
        double distance = assertWithinRadius(request, location);

        LocalDate today = LocalDate.now();
        boolean hasOpenRecord = checkinRepo.findByStaffAndDateRange(staffId, today, today).stream()
                .anyMatch(c -> c.getCheckOutTime() == null);
        if (hasOpenRecord) {
            throw new AttendanceException(AttendanceErrorCode.ALREADY_CHECKED_IN);
        }

        Integer shiftId = findCurrentShiftId();
        LocalTime now = LocalTime.now();
        String status = "PRESENT";
        if (shiftId != null) {
            WorkShift shift = workShiftRepo.findById(shiftId).orElse(null);
            if (shift != null && now.isAfter(shift.getStartTime().plusMinutes(LATE_THRESHOLD_MINUTES))) {
                status = "LATE";
            }
        }

        AttendanceCheckin checkin = new AttendanceCheckin();
        checkin.setStaffId(staffId);
        checkin.setAttendanceDate(today);
        checkin.setShiftId(shiftId);
        checkin.setCheckInTime(now);
        checkin.setStatus(status);
        checkin.setNotes("QR_GPS");
        checkin.setLocationId(location.getLocationId());
        checkin.setCheckInMethod("QR_GPS");
        checkin.setCheckInLat(request.getLatitude());
        checkin.setCheckInLng(request.getLongitude());
        checkin.setDistanceMeters(distance);
        checkin.setCreatedAt(LocalDateTime.now());

        AttendanceCheckin saved = checkinRepo.save(checkin);
        enrichNames(saved);
        return attendanceDtoMapper.toResponse(saved);
    }

    @Transactional
    public AttendanceCheckinResponse checkOut(Integer staffId, QrAttendanceRequest request) {
        AttendanceLocation location = findActiveLocation(request.getQrToken());
        assertWithinRadius(request, location);

        LocalDate today = LocalDate.now();
        AttendanceCheckin open = checkinRepo.findByStaffAndDateRange(staffId, today, today).stream()
                .filter(c -> c.getCheckOutTime() == null)
                .findFirst()
                .orElseThrow(() -> new AttendanceException(AttendanceErrorCode.NOT_CHECKED_IN));

        open.setCheckOutTime(LocalTime.now());
        AttendanceCheckin saved = checkinRepo.save(open);
        enrichNames(saved);
        return attendanceDtoMapper.toResponse(saved);
    }

    private double assertWithinRadius(QrAttendanceRequest request, AttendanceLocation location) {
        double distance = haversineMeters(request.getLatitude(), request.getLongitude(), location.getLatitude(), location.getLongitude());
        if (distance > location.getRadiusMeters()) {
            throw new AttendanceException(AttendanceErrorCode.OUT_OF_RADIUS, String.format(
                    "Bạn đang cách vị trí chấm công %.0fm, vượt quá %dm cho phép.",
                    distance, location.getRadiusMeters()));
        }
        return distance;
    }

    private AttendanceLocation findActiveLocation(String qrToken) {
        AttendanceLocation location = locationRepo.findByQrToken(qrToken)
                .orElseThrow(() -> new AttendanceException(AttendanceErrorCode.INVALID_QR_TOKEN));
        if (Boolean.FALSE.equals(location.getIsActive())) {
            throw new AttendanceException(AttendanceErrorCode.LOCATION_INACTIVE);
        }
        return location;
    }

    private Integer findCurrentShiftId() {
        LocalTime now = LocalTime.now();
        return workShiftRepo.findAll().stream()
                .filter(s -> Boolean.TRUE.equals(s.getIsActive()))
                .filter(s -> isTimeInsideShift(now, s.getStartTime(), s.getEndTime()))
                .map(WorkShift::getShiftId)
                .findFirst()
                .orElse(null);
    }

    private boolean isTimeInsideShift(LocalTime t, LocalTime start, LocalTime end) {
        if (!start.isAfter(end)) {
            return !t.isBefore(start) && !t.isAfter(end);
        }
        // Ca qua đêm (VD 22:00 - 06:00)
        return !t.isBefore(start) || !t.isAfter(end);
    }

    private void enrichNames(AttendanceCheckin checkin) {
        StaffProfile staff = staffProfileRepo.findById(checkin.getStaffId()).orElse(null);
        if (staff != null) {
            checkin.setStaffName(staff.getFullName());
        }
        if (checkin.getShiftId() != null) {
            workShiftRepo.findById(checkin.getShiftId()).ifPresent(s -> checkin.setShiftName(s.getShiftName()));
        }
    }

    private double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }
}
