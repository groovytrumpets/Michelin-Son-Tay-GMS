package com.g42.platform.gms.warehouse.domain.repository;

import com.g42.platform.gms.warehouse.infrastructure.entity.CommissionConfigJpa;
import com.g42.platform.gms.warehouse.infrastructure.entity.CommissionRecordJpa;

import java.util.List;
import java.util.Optional;

public interface CommissionRepo {

    Optional<CommissionConfigJpa> findActiveConfigByItem(Integer itemId);

    CommissionRecordJpa saveRecord(CommissionRecordJpa record);

    List<CommissionRecordJpa> findRecordsByStaffAndPeriod(Integer staffId, String periodMonth);

    List<CommissionRecordJpa> findRecordsByPeriod(String periodMonth);

    int sumQuantityByStaffAndItemAndPeriod(Integer staffId, Integer itemId, String periodMonth);
}
