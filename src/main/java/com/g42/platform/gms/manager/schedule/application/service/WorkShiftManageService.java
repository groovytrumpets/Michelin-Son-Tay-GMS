package com.g42.platform.gms.manager.schedule.application.service;

import com.g42.platform.gms.manager.schedule.api.dto.WorkShiftRequest;
import com.g42.platform.gms.manager.schedule.api.dto.WorkShiftResponse;
import com.g42.platform.gms.manager.schedule.domain.entity.WorkShift;
import com.g42.platform.gms.manager.schedule.domain.exception.ScheduleErrorCode;
import com.g42.platform.gms.manager.schedule.domain.exception.ScheduleException;
import com.g42.platform.gms.manager.schedule.domain.repository.WorkShiftRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkShiftManageService {

    private final WorkShiftRepo workShiftRepo;

    public List<WorkShiftResponse> getAllShifts() {
        return workShiftRepo.findAll().stream().map(this::toResponse).toList();
    }

    public WorkShiftResponse getShiftById(Integer shiftId) {
        return toResponse(findShift(shiftId));
    }

    @Transactional
    public WorkShiftResponse createShift(WorkShiftRequest request) {
        WorkShift shift = new WorkShift();
        shift.setShiftName(request.getShiftName());
        shift.setStartTime(request.getStartTime());
        shift.setEndTime(request.getEndTime());
        shift.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        shift.setCreatedAt(LocalDateTime.now());
        return toResponse(workShiftRepo.save(shift));
    }

    @Transactional
    public WorkShiftResponse updateShift(Integer shiftId, WorkShiftRequest request) {
        WorkShift shift = findShift(shiftId);
        shift.setShiftName(request.getShiftName());
        shift.setStartTime(request.getStartTime());
        shift.setEndTime(request.getEndTime());
        if (request.getIsActive() != null) shift.setIsActive(request.getIsActive());
        return toResponse(workShiftRepo.save(shift));
    }

    @Transactional
    public void deleteShift(Integer shiftId) {
        WorkShift shift = findShift(shiftId);
        shift.setIsActive(false);
        workShiftRepo.save(shift);
    }

    private WorkShift findShift(Integer shiftId) {
        return workShiftRepo.findById(shiftId)
                .orElseThrow(() -> new ScheduleException(ScheduleErrorCode.SHIFT_NOT_FOUND));
    }

    private WorkShiftResponse toResponse(WorkShift shift) {
        WorkShiftResponse r = new WorkShiftResponse();
        r.setShiftId(shift.getShiftId());
        r.setShiftName(shift.getShiftName());
        r.setStartTime(shift.getStartTime());
        r.setEndTime(shift.getEndTime());
        r.setIsActive(shift.getIsActive());
        r.setCreatedAt(shift.getCreatedAt());
        return r;
    }
}
