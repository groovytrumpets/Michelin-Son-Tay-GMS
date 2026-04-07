package com.g42.platform.gms.manager.attendance.application.service;

import com.g42.platform.gms.manager.attendance.api.dto.StaffShiftAttendanceResponse;
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
import java.util.Comparator;
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
    private final com.g42.platform.gms.hikvision.sync.HikvisionSyncScheduler hikvisionSyncScheduler;
    private final com.g42.platform.gms.hikvision.config.HikvisionProperties hikvisionProperties;

    public List<AttendanceCheckinResponse> getAttendance(Integer staffId, LocalDate from, LocalDate to) {
        List<AttendanceCheckin> list = staffId != null
                ? checkinRepo.findByStaffAndDateRange(staffId, from, to)
                : checkinRepo.findByDateRange(from, to);
        return enrich(list);
    }

    public List<AttendanceCheckinResponse> getAttendanceByDate(LocalDate date) {
        // Auto-sync from Hikvision if date is today or yesterday and sync is enabled
        if (hikvisionProperties.isSyncEnabled()) {
            LocalDate today = LocalDate.now();
            if (!date.isBefore(today.minusDays(1))) {
                try {
                    hikvisionSyncScheduler.syncDate(date);
                } catch (Exception e) {
                    // Log but don't fail — return DB data even if sync fails
                }
            }
        }
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

        LocalTime checkInTime = request.getCheckInTime() != null ? request.getCheckInTime() : LocalTime.now();
        LocalTime lateThreshold = shift.getStartTime().plusMinutes(15);
        String status = checkInTime.isAfter(lateThreshold) ? "LATE" : "PRESENT";
        String notes = (request.getNotes() != null && !request.getNotes().isBlank()) ? request.getNotes() : "Manual";

        AttendanceCheckin checkin = new AttendanceCheckin();
        checkin.setStaffId(request.getStaffId());
        checkin.setAttendanceDate(date);
        checkin.setShiftId(shiftId);
        checkin.setCheckInTime(checkInTime);
        checkin.setStatus(status);
        checkin.setNotes(notes);
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

        AttendanceCheckin saved = checkinRepo.save(checkin);
        return enrichSingle(saved);
    }

    @Transactional
    public AttendanceCheckinResponse updateAttendance(Integer checkinId, com.g42.platform.gms.manager.attendance.api.dto.UpdateAttendanceRequest request) {
        AttendanceCheckin checkin = checkinRepo.findById(checkinId)
                .orElseThrow(() -> new AttendanceException(AttendanceErrorCode.CHECKIN_NOT_FOUND));

        if (request.getCheckInTime() != null) {
            checkin.setCheckInTime(request.getCheckInTime());
            // Recalculate status based on new check-in time
            if (checkin.getShiftId() != null) {
                workShiftRepo.findById(checkin.getShiftId()).ifPresent(shift -> {
                    LocalTime lateThreshold = shift.getStartTime().plusMinutes(15);
                    checkin.setStatus(request.getCheckInTime().isAfter(lateThreshold) ? "LATE" : "PRESENT");
                });
            }
        }
        if (request.getCheckOutTime() != null) {
            checkin.setCheckOutTime(request.getCheckOutTime());
        }

        // Validate checkIn < checkOut if both present
        LocalTime finalCheckIn = checkin.getCheckInTime();
        LocalTime finalCheckOut = checkin.getCheckOutTime();
        if (finalCheckIn != null && finalCheckOut != null && !finalCheckIn.isBefore(finalCheckOut)) {
            throw new AttendanceException(AttendanceErrorCode.INVALID_TIME_RANGE);
        }

        String notes = (request.getNotes() != null && !request.getNotes().isBlank())
                ? request.getNotes()
                : "Edited by manager";
        checkin.setNotes(notes);

        return enrichSingle(checkinRepo.save(checkin));
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

        Map<Integer, List<AttendanceCheckin>> checkinsByStaff = checkins.stream()
                .collect(Collectors.groupingBy(AttendanceCheckin::getStaffId));

        Map<Integer, String> shiftNames = workShiftRepo.findAll().stream()
                .collect(Collectors.toMap(WorkShift::getShiftId, WorkShift::getShiftName));

        List<TodaySummaryResponse.StaffAttendanceStatus> staffList = allStaff.stream().map(staff -> {
            List<AttendanceCheckin> staffCheckins = checkinsByStaff.getOrDefault(staff.getStaffId(), List.of());
            List<TodaySummaryResponse.ShiftCheckin> shiftCheckins = staffCheckins.stream()
                    .map(c -> TodaySummaryResponse.ShiftCheckin.builder()
                            .checkinId(c.getCheckinId())
                            .shiftId(c.getShiftId())
                            .shiftName(shiftNames.getOrDefault(c.getShiftId(), ""))
                            .checkInTime(c.getCheckInTime())
                            .checkOutTime(c.getCheckOutTime())
                            .status(c.getStatus())
                            .build())
                    .toList();
            return TodaySummaryResponse.StaffAttendanceStatus.builder()
                    .staffId(staff.getStaffId())
                    .fullName(staff.getFullName())
                    .position(staff.getPosition())
                    .avatar(staff.getAvatar())
                    .hasCheckedIn(!staffCheckins.isEmpty())
                    .checkins(shiftCheckins)
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

    /**
     * Lấy danh sách tất cả ca trong ngày cho 1 nhân viên.
     * - Record thủ công (shift_id != null): map trực tiếp theo shift_id
     * - Record Hikvision (shift_id = null): map check_in_time vào ca phù hợp.
     *   Nếu check_out_time vượt qua shiftEnd của ca check-in → tạo virtual record
     *   cho ca tiếp theo với check_in = shiftStart của ca đó, check_out = thực tế.
     */
    @Transactional(readOnly = true)
    public StaffShiftAttendanceResponse getStaffShiftAttendance(Integer staffId, LocalDate date) {
        LocalDate target = date != null ? date : LocalDate.now();

        StaffProfile staff = staffProfileRepo.findById(staffId)
                .orElseThrow(() -> new AttendanceException(AttendanceErrorCode.STAFF_NOT_FOUND));

        List<WorkShift> allShifts = workShiftRepo.findAll().stream()
                .filter(s -> Boolean.TRUE.equals(s.getIsActive()))
                .sorted(Comparator.comparing(WorkShift::getStartTime))
                .toList();

        List<AttendanceCheckin> checkins = checkinRepo.findByStaffAndDateRange(staffId, target, target);

        // Tất cả records: manual (shift_id != null, notes != Hikvision) và Hikvision (notes = Hikvision)
        Map<Integer, AttendanceCheckin> manualByShift = checkins.stream()
                .filter(c -> c.getShiftId() != null && (c.getNotes() == null || !c.getNotes().contains("Hikvision")))
                .collect(Collectors.toMap(AttendanceCheckin::getShiftId, c -> c, (a, b) -> a));

        Map<Integer, AttendanceCheckin> hikvisionByShift = checkins.stream()
                .filter(c -> c.getShiftId() != null && c.getNotes() != null && c.getNotes().contains("Hikvision"))
                .collect(Collectors.toMap(AttendanceCheckin::getShiftId, c -> c, (a, b) -> a));

        List<StaffShiftAttendanceResponse.ShiftStatus> shifts = allShifts.stream()
                .map(shift -> {
                    // Ưu tiên thủ công, sau đó Hikvision
                    AttendanceCheckin checkin = manualByShift.getOrDefault(shift.getShiftId(),
                            hikvisionByShift.get(shift.getShiftId()));
                    String source = checkin == null ? null
                            : (checkin.getNotes() != null && checkin.getNotes().contains("Hikvision") ? "HIKVISION" : "MANUAL");
                    return StaffShiftAttendanceResponse.ShiftStatus.builder()
                            .shiftId(shift.getShiftId())
                            .shiftName(shift.getShiftName())
                            .shiftStart(shift.getStartTime())
                            .shiftEnd(shift.getEndTime())
                            .checkinId(checkin != null ? checkin.getCheckinId() : null)
                            .checkInTime(checkin != null ? checkin.getCheckInTime() : null)
                            .checkOutTime(checkin != null ? checkin.getCheckOutTime() : null)
                            .status(checkin != null ? checkin.getStatus() : null)
                            .source(source)
                            .build();
                })
                .toList();

        return StaffShiftAttendanceResponse.builder()
                .staffId(staff.getStaffId())
                .fullName(staff.getFullName())
                .position(staff.getPosition())
                .avatar(staff.getAvatar())
                .date(target)
                .shifts(shifts)
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
