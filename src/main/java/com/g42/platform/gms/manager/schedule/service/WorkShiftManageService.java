package com.g42.platform.gms.manager.schedule.service;

import com.g42.platform.gms.auth.entity.StaffProfile;
import com.g42.platform.gms.auth.repository.StaffProfileRepo;
import com.g42.platform.gms.dashboard.infrastructure.entity.StaffScheduleJpa;
import com.g42.platform.gms.dashboard.infrastructure.entity.WorkShiftJpa;
import com.g42.platform.gms.manager.schedule.dto.*;
import com.g42.platform.gms.manager.schedule.repository.ManagerStaffScheduleRepository;
import com.g42.platform.gms.manager.schedule.repository.ManagerWorkShiftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkShiftManageService {

    private final ManagerWorkShiftRepository workShiftRepo;
    private final ManagerStaffScheduleRepository scheduleRepo;
    private final StaffProfileRepo staffProfileRepo;

    // ===== Work Shift CRUD =====

    public List<WorkShiftResponse> getAllShifts() {
        return workShiftRepo.findAll().stream().map(this::toShiftResponse).toList();
    }

    public WorkShiftResponse getShiftById(Integer shiftId) {
        return toShiftResponse(findShift(shiftId));
    }

    @Transactional
    public WorkShiftResponse createShift(WorkShiftRequest request) {
        WorkShiftJpa entity = new WorkShiftJpa();
        entity.setShiftName(request.getShiftName());
        entity.setStartTime(request.getStartTime());
        entity.setEndTime(request.getEndTime());
        entity.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        entity.setCreatedAt(LocalDateTime.now());
        return toShiftResponse(workShiftRepo.save(entity));
    }

    @Transactional
    public WorkShiftResponse updateShift(Integer shiftId, WorkShiftRequest request) {
        WorkShiftJpa entity = findShift(shiftId);
        entity.setShiftName(request.getShiftName());
        entity.setStartTime(request.getStartTime());
        entity.setEndTime(request.getEndTime());
        if (request.getIsActive() != null) entity.setIsActive(request.getIsActive());
        return toShiftResponse(workShiftRepo.save(entity));
    }

    @Transactional
    public void deleteShift(Integer shiftId) {
        WorkShiftJpa entity = findShift(shiftId);
        entity.setIsActive(false);
        workShiftRepo.save(entity);
    }

    // ===== Staff Schedule =====

    public List<StaffScheduleResponse> getSchedules(Integer staffId, LocalDate from, LocalDate to) {
        List<StaffScheduleJpa> schedules;
        if (staffId != null) {
            schedules = scheduleRepo.findByStaffIdAndWorkDateBetween(staffId, from, to);
        } else {
            schedules = scheduleRepo.findByWorkDateBetweenOrderByWorkDateAsc(from, to);
        }

        // Batch load staff profiles
        List<Integer> staffIds = schedules.stream().map(StaffScheduleJpa::getStaffId).distinct().toList();
        Map<Integer, StaffProfile> profileMap = staffProfileRepo.findAllById(staffIds)
                .stream().collect(Collectors.toMap(StaffProfile::getStaffId, p -> p));

        return schedules.stream().map(s -> toScheduleResponse(s, profileMap.get(s.getStaffId()))).toList();
    }

    @Transactional
    public StaffScheduleResponse createSchedule(StaffScheduleRequest request) {
        WorkShiftJpa shift = findShift(request.getShiftId());
        StaffProfile staff = staffProfileRepo.findById(request.getStaffId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên: " + request.getStaffId()));

        StaffScheduleJpa entity = new StaffScheduleJpa();
        entity.setStaffId(request.getStaffId());
        entity.setWorkDate(request.getWorkDate());
        entity.setShiftId(request.getShiftId());
        entity.setStatus("SCHEDULED");
        entity.setNotes(request.getNotes());
        entity.setCreatedAt(LocalDateTime.now());

        StaffScheduleJpa saved = scheduleRepo.save(entity);
        return toScheduleResponse(saved, staff);
    }

    @Transactional
    public StaffScheduleResponse updateSchedule(Integer scheduleId, StaffScheduleRequest request) {
        StaffScheduleJpa entity = findSchedule(scheduleId);
        WorkShiftJpa shift = findShift(request.getShiftId());
        StaffProfile staff = staffProfileRepo.findById(request.getStaffId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên: " + request.getStaffId()));

        entity.setStaffId(request.getStaffId());
        entity.setWorkDate(request.getWorkDate());
        entity.setShiftId(request.getShiftId());
        if (request.getNotes() != null) entity.setNotes(request.getNotes());

        return toScheduleResponse(scheduleRepo.save(entity), staff);
    }

    @Transactional
    public void deleteSchedule(Integer scheduleId) {
        StaffScheduleJpa entity = findSchedule(scheduleId);
        entity.setStatus("CANCELLED");
        scheduleRepo.save(entity);
    }

    // ===== Stats =====

    public WorkShiftStatsResponse getStats() {
        WorkShiftStatsResponse stats = new WorkShiftStatsResponse();
        stats.setTotalScheduledShifts(scheduleRepo.countTotalScheduled());
        stats.setActiveShiftsToday(scheduleRepo.countActiveByDate(LocalDate.now()));
        stats.setPendingSwapRequests(0L); // placeholder — swap feature TBD
        stats.setCompletedShifts(scheduleRepo.countCompleted());
        return stats;
    }

    // ===== Helpers =====

    private WorkShiftJpa findShift(Integer shiftId) {
        return workShiftRepo.findById(shiftId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ca làm việc: " + shiftId));
    }

    private StaffScheduleJpa findSchedule(Integer scheduleId) {
        return scheduleRepo.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch: " + scheduleId));
    }

    private WorkShiftResponse toShiftResponse(WorkShiftJpa e) {
        WorkShiftResponse r = new WorkShiftResponse();
        r.setShiftId(e.getShiftId());
        r.setShiftName(e.getShiftName());
        r.setStartTime(e.getStartTime());
        r.setEndTime(e.getEndTime());
        r.setIsActive(e.getIsActive());
        return r;
    }

    private StaffScheduleResponse toScheduleResponse(StaffScheduleJpa e, StaffProfile profile) {
        StaffScheduleResponse r = new StaffScheduleResponse();
        r.setScheduleId(e.getScheduleId());
        r.setStaffId(e.getStaffId());
        r.setWorkDate(e.getWorkDate());
        r.setStatus(e.getStatus());
        r.setNotes(e.getNotes());
        if (profile != null) {
            r.setStaffName(profile.getFullName());
            r.setPosition(profile.getPosition());
        }
        if (e.getShift() != null) {
            r.setShiftId(e.getShift().getShiftId());
            r.setShiftName(e.getShift().getShiftName());
            r.setStartTime(e.getShift().getStartTime());
            r.setEndTime(e.getShift().getEndTime());
        } else {
            r.setShiftId(e.getShiftId());
        }
        return r;
    }
}
