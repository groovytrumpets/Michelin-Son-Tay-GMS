package com.g42.platform.gms.warehouse.infrastructure.repository.impl;

import com.g42.platform.gms.warehouse.domain.entity.StockIssueItem;
import com.g42.platform.gms.warehouse.domain.repository.StockIssueItemRepo;
import com.g42.platform.gms.warehouse.infrastructure.entity.StockIssueItemJpa;
import com.g42.platform.gms.warehouse.infrastructure.repository.StockIssueItemJpaRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class StockIssueItemRepoImpl implements StockIssueItemRepo {

    private final StockIssueItemJpaRepo jpaRepo;

    @Override
    public List<StockIssueItem> saveAll(List<StockIssueItem> items) {
        return jpaRepo.saveAll(items.stream().map(this::toJpa).toList())
                .stream().map(this::toDomain).toList();
    }

    @Override
    public void deleteByIssueId(Integer issueId) {
        jpaRepo.deleteByIssueId(issueId);
    }

    @Override
    public Optional<StockIssueItem> findById(Integer issueItemId) {
        return jpaRepo.findById(issueItemId).map(this::toDomain);
    }

    @Override
    public StockIssueItem save(StockIssueItem item) {
        return toDomain(jpaRepo.save(toJpa(item)));
    }

    @Override
    public List<StockIssueItem> findByIssueId(Integer issueId) {
        return jpaRepo.findByIssueId(issueId).stream().map(this::toDomain).toList();
    }

    // ── mappers ──────────────────────────────────────────────────────────────

    private StockIssueItem toDomain(StockIssueItemJpa jpa) {
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

    private StockIssueItemJpa toJpa(StockIssueItem domain) {
        StockIssueItemJpa jpa = new StockIssueItemJpa();
        jpa.setIssueItemId(domain.getIssueItemId());
        jpa.setIssueId(domain.getIssueId());
        jpa.setItemId(domain.getItemId());
        jpa.setEntryItemId(domain.getEntryItemId());
        jpa.setQuantity(domain.getQuantity());
        jpa.setExportPrice(domain.getExportPrice());
        jpa.setImportPrice(domain.getImportPrice());
        jpa.setDiscountRate(domain.getDiscountRate() != null ? domain.getDiscountRate() : BigDecimal.ZERO);
        jpa.setFinalPrice(domain.getFinalPrice());
        return jpa;
    }
}
