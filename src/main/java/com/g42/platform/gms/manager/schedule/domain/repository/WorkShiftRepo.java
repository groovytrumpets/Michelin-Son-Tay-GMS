package com.g42.platform.gms.manager.schedule.domain.repository;

import com.g42.platform.gms.manager.schedule.domain.entity.WorkShift;

import java.util.List;
import java.util.Optional;

public interface WorkShiftRepo {
    List<WorkShift> findAll();
    Optional<WorkShift> findById(Integer shiftId);
    WorkShift save(WorkShift workShift);
}
