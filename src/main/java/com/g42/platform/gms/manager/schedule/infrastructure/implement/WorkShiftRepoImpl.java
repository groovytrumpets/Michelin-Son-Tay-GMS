package com.g42.platform.gms.manager.schedule.infrastructure.implement;

import com.g42.platform.gms.manager.schedule.domain.entity.WorkShift;
import com.g42.platform.gms.manager.schedule.domain.repository.WorkShiftRepo;
import com.g42.platform.gms.manager.schedule.infrastructure.mapper.WorkShiftJpaMapper;
import com.g42.platform.gms.manager.schedule.infrastructure.repository.WorkShiftJpaRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class WorkShiftRepoImpl implements WorkShiftRepo {

    private final WorkShiftJpaRepo jpaRepo;
    private final WorkShiftJpaMapper mapper;

    @Override
    public List<WorkShift> findAll() {
        return jpaRepo.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<WorkShift> findById(Integer shiftId) {
        return jpaRepo.findById(shiftId).map(mapper::toDomain);
    }

    @Override
    public WorkShift save(WorkShift workShift) {
        return mapper.toDomain(jpaRepo.save(mapper.toJpa(workShift)));
    }
}
