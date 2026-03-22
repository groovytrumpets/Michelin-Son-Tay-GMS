package com.g42.platform.gms.manager.attendance.application.service;

import com.g42.platform.gms.auth.entity.StaffProfile;
import com.g42.platform.gms.auth.repository.StaffProfileRepo;
import com.g42.platform.gms.manager.attendance.api.dto.AttendanceCheckinResponse;
import com.g42.platform.gms.manager.attendance.api.dto.CheckinRequest;
import com.g42.platform.gms.manager.attendance.api.dto.CheckoutRequest;
import com.g42.platform.gms.manager.attendance.api.dto.TodaySummaryResponse;
import com.g42.platform.gms.manager.attendance.api.mapper.AttendanceDtoMapper;
import com.g42.platform.gms.manager.attendance.domain.entity.AttendanceCheckin;
import com.g42.platform.gms.manager.attendance.domain.exception.AttendanceErrorCode;
import com.g42.platform.gms.manager.attendance.domain.exception.AttendanceException;
import com.g42.platform.gms.manager.attendance.domain.repository.AttendanceCheckinRepo;
import com.g42.platform.gms.manager.schedule.domain.entity.WorkShift;
import com.g42.platform.gms.manager.schedule.domain.repository.WorkShiftRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceManageService {

    private final AttendanceCheckinRepo checkinRepo;
    private final WorkShiftRepo workShiftRepo;
    private final StaffProfileRepo staffProfileRepo;
    private final AttendanceDtoMapper dtoMapper;

    public List<AttendanceCheckinResponse> getAttendance(Integer staffId, LocalDate from, LocalDate to) {
        List<AttendanceCheckin> list = staffId != null
                ? checkinRepo.findByStaffAndDateRange(staffId, from, to)
                : checkinRepo.findByDateRange(from, to);
        return enrich(list);
    }

    public List<AttendanceCheckinResponse> getAttendanceByDate(LocalDate date) {
        return enrich(checkinRepo.findByDate(date));
    }

    @Transactional
    public AttendanceCheckinResponse checkIn(CheckinRequest request) {
        LocalDate date = request.getAttendanceDate() != null ? request.getAttendanceDate() : LocalDate.now();
        Integer shiftId = request.getShiftId();

        // Validate shift exists
        WorkShift shift = workShiftRepo.findById(shiftId)
                .orElseThrow(() -> new AttendanceException(AttendanceErrorCode.SHIFT_NOT_FOUND));

        // Validate staff exists
        StaffProfile staff = staffProfileRepo.findById(request.getStaffId())
                .orElseThrow(() -> new AttendanceException(AttendanceErrorCode.STAFF_NOT_FOUND));

        // Check duplicate
        checkinRepo.findByStaffAndDateAndShift(request.getStaffId(), date, shiftId)
                .ifPresent(existing -> { throw new AttendanceException(AttendanceErrorCode.ALREADY_CHECKED_IN); });

        AttendanceCheckin checkin = new AttendanceCheckin();
        checkin.setStaffId(request.getStaffId());
        checkin.setAttendanceDate(date);
        checkin.setShiftId(shiftId);
        checkin.setCheckInTime(request.getCheckInTime() != null ? request.getCheckInTime() : LocalTime.now());
        checkin.setStatus("PRESENT");
        checkin.setNotes(request.getNotes());
        checkin.setCreatedAt(LocalDateTime.now());

        AttendanceCheckin saved = checkinRepo.save(checkin);
        saved.setStaffName(staff.getFullName());
        saved.setShiftName(shift.getShiftName());
        return dtoMapper.toResponse(saved);
    }

    @Transactional
    public AttendanceCheckinResponse checkOut(Integer checkinId, CheckoutRequest request) {
        AttendanceCheckin checkin = checkinRepo.findById(checkinId)
                .orElseThrow(() -> new AttendanceException(AttendanceErrorCode.CHECKIN_NOT_FOUND));

        if (checkin.getCheckInTime() == null) {
            throw new AttendanceException(AttendanceErrorCode.NOT_CHECKED_IN);
        }

        checkin.setCheckOutTime(request.getCheckOutTime() != null ? request.getCheckOutTime() : LocalTime.now());
        if (request.getNotes() != null) checkin.setNotes(request.getNotes());

        AttendanceCheckin saved = checkinRepo.save(checkin);
        return enrichSingle(saved);
    }

    @Transactional
    public void deleteCheckin(Integer checkinId) {
        checkinRepo.findById(checkinId)
                .orElseThrow(() -> new AttendanceException(AttendanceErrorCode.CHECKIN_NOT_FOUND));
        checkinRepo.deleteById(checkinId);
    }

    public TodaySummaryResponse getTodaySummary(LocalDate date) {
        LocalDate target = date != null ? date : LocalDate.now();
        List<AttendanceCheckin> checkins = checkinRepo.findByDate(target);

        List<StaffProfile> allStaff = staffProfileRepo.findAll();

        Map<Integer, AttendanceCheckin> checkinByStaff = checkins.stream()
                .collect(Collectors.toMap(AttendanceCheckin::getStaffId, c -> c, (a, b) -> a));

        Map<Integer, String> shiftNames = workShiftRepo.findAll().stream()
                .collect(Collectors.toMap(WorkShift::getShiftId, WorkShift::getShiftName));

        List<TodaySummaryResponse.StaffAttendanceStatus> staffList = allStaff.stream().map(staff -> {
            AttendanceCheckin c = checkinByStaff.get(staff.getStaffId());
            return TodaySummaryResponse.StaffAttendanceStatus.builder()
                    .staffId(staff.getStaffId())
                    .fullName(staff.getFullName())
                    .position(staff.getPosition())
                    .avatar(staff.getAvatar())
                    .hasCheckedIn(c != null)
                    .checkinId(c != null ? c.getCheckinId() : null)
                    .shiftId(c != null ? c.getShiftId() : null)
                    .shiftName(c != null ? shiftNames.getOrDefault(c.getShiftId(), "") : null)
                    .checkInTime(c != null ? c.getCheckInTime() : null)
                    .checkOutTime(c != null ? c.getCheckOutTime() : null)
                    .status(c != null ? c.getStatus() : null)
                    .build();
        }).toList();

        long checkedInCount = staffList.stream().filter(TodaySummaryResponse.StaffAttendanceStatus::isHasCheckedIn).count();

        return TodaySummaryResponse.builder()
                .date(target)
                .totalStaff(allStaff.size())
                .checkedIn((int) checkedInCount)
                .notCheckedIn(allStaff.size() - (int) checkedInCount)
                .staffList(staffList)
                .build();
    }

    // ===== Helpers =====

    private List<AttendanceCheckinResponse> enrich(List<AttendanceCheckin> list) {
        List<Integer> staffIds = list.stream().map(AttendanceCheckin::getStaffId).distinct().toList();
        List<Integer> shiftIds = list.stream().map(AttendanceCheckin::getShiftId).filter(id -> id != null).distinct().toList();

        Map<Integer, String> staffNames = staffProfileRepo.findAllById(staffIds)
                .stream().collect(Collectors.toMap(StaffProfile::getStaffId, StaffProfile::getFullName));

        Map<Integer, String> shiftNames = workShiftRepo.findAll()
                .stream().filter(s -> shiftIds.contains(s.getShiftId()))
                .collect(Collectors.toMap(WorkShift::getShiftId, WorkShift::getShiftName));

        return list.stream().map(c -> {
            c.setStaffName(staffNames.getOrDefault(c.getStaffId(), ""));
            c.setShiftName(shiftNames.getOrDefault(c.getShiftId(), ""));
            return dtoMapper.toResponse(c);
        }).toList();
    }

    private AttendanceCheckinResponse enrichSingle(AttendanceCheckin checkin) {
        staffProfileRepo.findById(checkin.getStaffId())
                .ifPresent(p -> checkin.setStaffName(p.getFullName()));
        if (checkin.getShiftId() != null) {
            workShiftRepo.findById(checkin.getShiftId())
                    .ifPresent(s -> checkin.setShiftName(s.getShiftName()));
        }
        return dtoMapper.toResponse(checkin);
    }
}
