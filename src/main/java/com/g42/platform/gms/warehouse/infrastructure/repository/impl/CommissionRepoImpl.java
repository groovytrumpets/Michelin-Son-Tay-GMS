package com.g42.platform.gms.warehouse.infrastructure.repository.impl;

import com.g42.platform.gms.warehouse.domain.entity.CommissionConfig;
import com.g42.platform.gms.warehouse.domain.entity.CommissionRecord;
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
    public Optional<CommissionConfig> findActiveConfigByItem(Integer itemId) {
        return configJpaRepo.findByItemIdAndIsActiveTrue(itemId).map(this::toDomain);
    }

    @Override
    public CommissionRecord saveRecord(CommissionRecord record) {
        CommissionRecordJpa saved = recordJpaRepo.save(toJpa(record));
        return toDomain(saved);
    }

    @Override
    public List<CommissionRecord> findRecordsByStaffAndPeriod(Integer staffId, String periodMonth) {
        return recordJpaRepo.findByStaffIdAndPeriodMonth(staffId, periodMonth)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<CommissionRecord> findRecordsByPeriod(String periodMonth) {
        return recordJpaRepo.findByPeriodMonth(periodMonth)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public int sumQuantityByStaffAndItemAndPeriod(Integer staffId, Integer itemId, String periodMonth) {
        Integer result = recordJpaRepo.sumQuantityByStaffAndItemAndPeriod(staffId, itemId, periodMonth);
        return result != null ? result : 0;
    }

    private CommissionConfig toDomain(CommissionConfigJpa jpa) {
        CommissionConfig domain = new CommissionConfig();
        domain.setConfigId(jpa.getConfigId());
        domain.setItemId(jpa.getItemId());
        domain.setCommissionRate(jpa.getCommissionRate());
        domain.setCommissionQuantityThreshold(jpa.getCommissionQuantityThreshold());
        domain.setIsActive(jpa.getIsActive());
        domain.setCreatedBy(jpa.getCreatedBy());
        domain.setCreatedAt(jpa.getCreatedAt());
        return domain;
    }

    private CommissionRecord toDomain(CommissionRecordJpa jpa) {
        CommissionRecord domain = new CommissionRecord();
        domain.setRecordId(jpa.getRecordId());
        domain.setStaffId(jpa.getStaffId());
        domain.setItemId(jpa.getItemId());
        domain.setIssueId(jpa.getIssueId());
        domain.setQuantity(jpa.getQuantity());
        domain.setFinalPrice(jpa.getFinalPrice());
        domain.setCommissionRate(jpa.getCommissionRate());
        domain.setCommissionValue(jpa.getCommissionValue());
        domain.setPeriodMonth(jpa.getPeriodMonth());
        domain.setCreatedAt(jpa.getCreatedAt());
        return domain;
    }

    private CommissionRecordJpa toJpa(CommissionRecord domain) {
        CommissionRecordJpa jpa = new CommissionRecordJpa();
        jpa.setRecordId(domain.getRecordId());
        jpa.setStaffId(domain.getStaffId());
        jpa.setItemId(domain.getItemId());
        jpa.setIssueId(domain.getIssueId());
        jpa.setQuantity(domain.getQuantity());
        jpa.setFinalPrice(domain.getFinalPrice());
        jpa.setCommissionRate(domain.getCommissionRate());
        jpa.setCommissionValue(domain.getCommissionValue());
        jpa.setPeriodMonth(domain.getPeriodMonth());
        jpa.setCreatedAt(domain.getCreatedAt());
        return jpa;
    }
}
