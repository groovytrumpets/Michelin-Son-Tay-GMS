package com.g42.platform.gms.warehouse.infrastructure.repository.impl;

import com.g42.platform.gms.warehouse.domain.entity.ReturnEntry;
import com.g42.platform.gms.warehouse.domain.entity.ReturnEntryItem;
import com.g42.platform.gms.warehouse.domain.enums.ReturnEntryStatus;
import com.g42.platform.gms.warehouse.domain.enums.ReturnType;
import com.g42.platform.gms.warehouse.domain.repository.ReturnEntryRepo;
import com.g42.platform.gms.warehouse.infrastructure.entity.ReturnEntryItemJpa;
import com.g42.platform.gms.warehouse.infrastructure.entity.ReturnEntryJpa;
import com.g42.platform.gms.warehouse.infrastructure.repository.ReturnEntryItemJpaRepo;
import com.g42.platform.gms.warehouse.infrastructure.repository.ReturnEntryJpaRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ReturnEntryRepoImpl implements ReturnEntryRepo {

    private final ReturnEntryJpaRepo jpaRepo;
    private final ReturnEntryItemJpaRepo itemJpaRepo;

    @Override
    public Optional<ReturnEntry> findById(Integer returnId) {
        return jpaRepo.findById(returnId).map(this::toDomain);
    }

    @Override
    public List<ReturnEntry> findByWarehouseId(Integer warehouseId) {
        return jpaRepo.findByWarehouseIdOrderByCreatedAtDesc(warehouseId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Page<ReturnEntry> search(Integer warehouseId,
                                    ReturnEntryStatus status,
                                    ReturnType returnType,
                                    LocalDate fromDate,
                                    LocalDate toDate,
                                    String search,
                                    Pageable pageable) {
        String normalizedSearch = (search == null || search.isBlank()) ? null : search.trim();
        LocalDateTime fromDateTime = fromDate != null ? fromDate.atStartOfDay() : null;
        LocalDateTime toDateTime = toDate != null ? toDate.atTime(23, 59, 59) : null;
        return jpaRepo.search(warehouseId, status, returnType, fromDateTime, toDateTime, normalizedSearch, pageable)
                .map(this::toDomain);
    }

    @Override
    public ReturnEntry save(ReturnEntry entry) {
        ReturnEntryJpa saved = jpaRepo.save(toJpa(entry));
        return toDomain(saved);
    }

    @Override
    public boolean existsByCode(String returnCode) {
        return jpaRepo.existsByReturnCode(returnCode);
    }

    @Override
    public boolean existsActiveBySourceIssueItemId(Integer sourceIssueItemId) {
        Long count = jpaRepo.countActiveBySourceIssueItemId(sourceIssueItemId);
        return count != null && count > 0;
    }

    @Override
    public boolean existsActiveByAllocationId(Integer allocationId) {
        Long count = jpaRepo.countActiveByAllocationId(allocationId);
        return count != null && count > 0;
    }

    @Override
    public boolean existsAnyBySourceIssueItemId(Integer sourceIssueItemId) {
        Long count = jpaRepo.countAnyBySourceIssueItemId(sourceIssueItemId);
        return count != null && count > 0;
    }

    @Override
    public boolean existsAnyByAllocationId(Integer allocationId) {
        Long count = jpaRepo.countAnyByAllocationId(allocationId);
        return count != null && count > 0;
    }

    @Override
    public boolean existsAnyBySourceIssueItemIdExcludingReturnId(Integer sourceIssueItemId, Integer returnId) {
        Long count = jpaRepo.countAnyBySourceIssueItemIdExcludingReturnId(sourceIssueItemId, returnId);
        return count != null && count > 0;
    }

    @Override
    public boolean existsAnyByAllocationIdExcludingReturnId(Integer allocationId, Integer returnId) {
        Long count = jpaRepo.countAnyByAllocationIdExcludingReturnId(allocationId, returnId);
        return count != null && count > 0;
    }

    @Override
    public Optional<ReturnEntryItem> findItemById(Integer returnItemId) {
        return itemJpaRepo.findById(returnItemId).map(this::toDomainItem);
    }

    @Override
    public ReturnEntryItem saveItem(ReturnEntryItem item) {
        ReturnEntryItemJpa saved = itemJpaRepo.save(toJpaItem(item));
        return toDomainItem(saved);
    }

    private ReturnEntry toDomain(ReturnEntryJpa jpa) {
        ReturnEntry domain = new ReturnEntry();
        domain.setReturnId(jpa.getReturnId());
        domain.setReturnCode(jpa.getReturnCode());
        domain.setWarehouseId(jpa.getWarehouseId());
        domain.setReturnReason(jpa.getReturnReason());
        domain.setReturnType(jpa.getReturnType());
        domain.setSourceIssueId(jpa.getSourceIssueId());
        domain.setStatus(jpa.getStatus());
        domain.setConfirmedBy(jpa.getConfirmedBy());
        domain.setConfirmedAt(jpa.getConfirmedAt());
        domain.setCreatedBy(jpa.getCreatedBy());
        domain.setCreatedAt(jpa.getCreatedAt());
        domain.setUpdatedAt(jpa.getUpdatedAt());
        domain.setItems(new java.util.ArrayList<>(jpa.getItems().stream().map(this::toDomainItem).toList()));
        return domain;
    }

    private ReturnEntryJpa toJpa(ReturnEntry domain) {
        ReturnEntryJpa jpa = new ReturnEntryJpa();
        jpa.setReturnId(domain.getReturnId());
        jpa.setReturnCode(domain.getReturnCode());
        jpa.setWarehouseId(domain.getWarehouseId());
        jpa.setReturnReason(domain.getReturnReason());
        jpa.setReturnType(domain.getReturnType());
        jpa.setSourceIssueId(domain.getSourceIssueId());
        jpa.setStatus(domain.getStatus());
        jpa.setConfirmedBy(domain.getConfirmedBy());
        jpa.setConfirmedAt(domain.getConfirmedAt());
        jpa.setCreatedBy(domain.getCreatedBy());
        jpa.setCreatedAt(domain.getCreatedAt());
        jpa.setUpdatedAt(domain.getUpdatedAt());
        if (domain.getItems() != null) {
            jpa.setItems(new java.util.ArrayList<>(domain.getItems().stream().map(this::toJpaItem).toList()));
        }
        return jpa;
    }

    private ReturnEntryItem toDomainItem(ReturnEntryItemJpa jpa) {
        ReturnEntryItem item = new ReturnEntryItem();
        item.setReturnItemId(jpa.getReturnItemId());
        item.setReturnId(jpa.getReturnId());
        item.setItemId(jpa.getItemId());
        item.setAllocationId(jpa.getAllocationId());
        item.setSourceIssueItemId(jpa.getSourceIssueItemId());
        item.setEntryItemId(jpa.getEntryItemId());
        item.setQuantity(jpa.getQuantity());
        item.setConditionNote(jpa.getConditionNote());
        item.setExchangeItem(jpa.isExchangeItem());
        return item;
    }

    private ReturnEntryItemJpa toJpaItem(ReturnEntryItem domain) {
        ReturnEntryItemJpa item = new ReturnEntryItemJpa();
        item.setReturnItemId(domain.getReturnItemId());
        item.setReturnId(domain.getReturnId());
        item.setItemId(domain.getItemId());
        item.setAllocationId(domain.getAllocationId());
        item.setSourceIssueItemId(domain.getSourceIssueItemId());
        item.setEntryItemId(domain.getEntryItemId());
        item.setQuantity(domain.getQuantity());
        item.setConditionNote(domain.getConditionNote());
        item.setExchangeItem(domain.isExchangeItem());
        return item;
    }
}
