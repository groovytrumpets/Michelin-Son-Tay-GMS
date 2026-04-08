package com.g42.platform.gms.warehouse.infrastructure.repository.impl;

import com.g42.platform.gms.warehouse.domain.repository.CommissionRepo;
import com.g42.platform.gms.warehouse.infrastructure.entity.CommissionConfigJpa;
import com.g42.platform.gms.warehouse.infrastructure.entity.CommissionRecordJpa;
import com.g42.platform.gms.warehouse.infrastructure.repository.CommissionConfigJpaRepo;
import com.g42.platform.gms.warehouse.infrastructure.repository.CommissionRecordJpaRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CommissionRepoImpl implements CommissionRepo {

    private final CommissionConfigJpaRepo configJpaRepo;
    private final CommissionRecordJpaRepo recordJpaRepo;

    @Override
    public Optional<CommissionConfigJpa> findActiveConfigByItem(Integer itemId) {
        return configJpaRepo.findByItemIdAndIsActiveTrue(itemId);
    }

    @Override
    public CommissionRecordJpa saveRecord(CommissionRecordJpa record) {
        return recordJpaRepo.save(record);
    }

    @Override
    public List<CommissionRecordJpa> findRecordsByStaffAndPeriod(Integer staffId, String periodMonth) {
        return recordJpaRepo.findByStaffIdAndPeriodMonth(staffId, periodMonth);
    }

    @Override
    public List<CommissionRecordJpa> findRecordsByPeriod(String periodMonth) {
        return recordJpaRepo.findByPeriodMonth(periodMonth);
    }

    @Override
    public int sumQuantityByStaffAndItemAndPeriod(Integer staffId, Integer itemId, String periodMonth) {
        Integer result = recordJpaRepo.sumQuantityByStaffAndItemAndPeriod(staffId, itemId, periodMonth);
        return result != null ? result : 0;
    }
}
