package com.g42.platform.gms.warehouse.infrastructure.repository.impl;

import com.g42.platform.gms.warehouse.domain.entity.StockIssue;
import com.g42.platform.gms.warehouse.domain.entity.StockIssueItem;
import com.g42.platform.gms.warehouse.domain.enums.IssueType;
import com.g42.platform.gms.warehouse.domain.enums.StockIssueStatus;
import com.g42.platform.gms.warehouse.domain.repository.StockIssueRepo;
import com.g42.platform.gms.warehouse.infrastructure.entity.StockIssueItemJpa;
import com.g42.platform.gms.warehouse.infrastructure.entity.StockIssueJpa;
import com.g42.platform.gms.warehouse.infrastructure.repository.StockIssueJpaRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class StockIssueRepoImpl implements StockIssueRepo {

    private final StockIssueJpaRepo jpaRepo;

    @Override
    public Optional<StockIssue> findById(Integer issueId) {
        return jpaRepo.findById(issueId).map(this::toDomain);
    }

    @Override
    public List<StockIssue> findByWarehouseId(Integer warehouseId) {
        return jpaRepo.findByWarehouseIdOrderByCreatedAtDesc(warehouseId)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public StockIssue save(StockIssue issue) {
        return toDomain(jpaRepo.save(toJpa(issue)));
    }

    @Override
    public boolean existsByCode(String issueCode) {
        return jpaRepo.existsByIssueCode(issueCode);
    }

    @Override
    public boolean existsConfirmedServiceTicketIssue(Integer serviceTicketId) {
        return jpaRepo.existsByServiceTicketIdAndIssueTypeAndStatus(
                serviceTicketId,
                IssueType.SERVICE_TICKET,
                StockIssueStatus.CONFIRMED);
    }

    // ── mappers ──────────────────────────────────────────────────────────────

    private StockIssue toDomain(StockIssueJpa jpa) {
        return StockIssue.builder()
                .issueId(jpa.getIssueId())
                .issueCode(jpa.getIssueCode())
                .warehouseId(jpa.getWarehouseId())
                .issueType(jpa.getIssueType())
                .issueReason(jpa.getIssueReason())
                .serviceTicketId(jpa.getServiceTicketId())
                .discountRate(jpa.getDiscountRate())
                .status(jpa.getStatus())
                .confirmedBy(jpa.getConfirmedBy())
                .confirmedAt(jpa.getConfirmedAt())
                .createdBy(jpa.getCreatedBy())
                .createdAt(jpa.getCreatedAt())
                .updatedAt(jpa.getUpdatedAt())
                .items(jpa.getItems() != null
                        ? jpa.getItems().stream().map(this::toDomainItem).toList()
                        : new ArrayList<>())
                .build();
    }

    private StockIssueJpa toJpa(StockIssue domain) {
        StockIssueJpa jpa = new StockIssueJpa();
        jpa.setIssueId(domain.getIssueId());
        jpa.setIssueCode(domain.getIssueCode());
        jpa.setWarehouseId(domain.getWarehouseId());
        jpa.setIssueType(domain.getIssueType());
        jpa.setIssueReason(domain.getIssueReason());
        jpa.setServiceTicketId(domain.getServiceTicketId());
        jpa.setDiscountRate(domain.getDiscountRate() != null ? domain.getDiscountRate() : BigDecimal.ZERO);
        jpa.setStatus(domain.getStatus() != null ? domain.getStatus()
                : com.g42.platform.gms.warehouse.domain.enums.StockIssueStatus.DRAFT);
        jpa.setConfirmedBy(domain.getConfirmedBy());
        jpa.setConfirmedAt(domain.getConfirmedAt());
        jpa.setCreatedBy(domain.getCreatedBy());
        return jpa;
    }

    private StockIssueItem toDomainItem(StockIssueItemJpa jpa) {
        return StockIssueItem.builder()
                .issueItemId(jpa.getIssueItemId())
                .issueId(jpa.getIssueId())
                .itemId(jpa.getItemId())
                .entryItemId(jpa.getEntryItemId())
                .quantity(jpa.getQuantity())
                .exportPrice(jpa.getExportPrice())
                .importPrice(jpa.getImportPrice())
                .discountRate(jpa.getDiscountRate())
                .finalPrice(jpa.getFinalPrice())
                .grossProfit(jpa.getGrossProfit())
                .build();
    }
}
