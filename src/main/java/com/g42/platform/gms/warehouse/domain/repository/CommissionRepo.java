package com.g42.platform.gms.warehouse.domain.repository;

import com.g42.platform.gms.warehouse.domain.entity.CommissionConfig;
import com.g42.platform.gms.warehouse.domain.entity.CommissionRecord;

import java.util.List;
import java.util.Optional;

public interface CommissionRepo {

    Optional<CommissionConfig> findActiveConfigByItem(Integer itemId);

    CommissionRecord saveRecord(CommissionRecord record);

    List<CommissionRecord> findRecordsByStaffAndPeriod(Integer staffId, String periodMonth);

    List<CommissionRecord> findRecordsByPeriod(String periodMonth);

    int sumQuantityByStaffAndItemAndPeriod(Integer staffId, Integer itemId, String periodMonth);
}
