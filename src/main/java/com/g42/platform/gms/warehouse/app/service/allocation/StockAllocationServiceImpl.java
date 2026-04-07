package com.g42.platform.gms.warehouse.app.service.allocation;

import com.g42.platform.gms.estimation.infrastructure.entity.EstimateItemJpa;
import com.g42.platform.gms.estimation.infrastructure.entity.EstimateJpa;
import com.g42.platform.gms.estimation.infrastructure.repository.EstimateItemRepositoryJpa;
import com.g42.platform.gms.estimation.infrastructure.repository.EstimateRepositoryJpa;
import com.g42.platform.gms.warehouse.api.dto.issue.CreateStockIssueRequest;
import com.g42.platform.gms.warehouse.api.dto.response.StockAllocationResult;
import com.g42.platform.gms.warehouse.app.service.dto.StockShortageInfo;
import com.g42.platform.gms.warehouse.domain.enums.AllocationStatus;
import com.g42.platform.gms.warehouse.domain.enums.InventoryTransactionType;
import com.g42.platform.gms.warehouse.domain.enums.IssueType;
import com.g42.platform.gms.warehouse.domain.repository.InventoryRepo;
import com.g42.platform.gms.warehouse.domain.repository.InventoryTransactionRepo;
import com.g42.platform.gms.warehouse.app.service.issue.StockIssueService;
import com.g42.platform.gms.warehouse.domain.repository.StockAllocationRepo;
import com.g42.platform.gms.warehouse.infrastructure.entity.InventoryJpa;
import com.g42.platform.gms.warehouse.infrastructure.entity.InventoryTransactionJpa;
import com.g42.platform.gms.warehouse.infrastructure.entity.StockAllocationJpa;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockAllocationServiceImpl implements StockAllocationService {

    private final StockAllocationRepo allocationRepo;
    private final InventoryRepo inventoryRepo;
    private final InventoryTransactionRepo transactionRepo;
    private final EstimateItemRepositoryJpa estimateItemRepositoryJpa;
    private final EstimateRepositoryJpa estimateRepositoryJpa;
    private final StockIssueService stockIssueService;

    @Override
    @Transactional
    public List<StockShortageInfo> reserve(Integer estimateId, Integer staffId) {
        List<EstimateItemJpa> estimateItems = estimateItemRepositoryJpa.findByEstimateId(estimateId);
        List<StockShortageInfo> shortages = new ArrayList<>();

        for (EstimateItemJpa ei : estimateItems) {
            if (ei.getWarehouseId() == null || ei.getItemId() == null || ei.getQuantity() == null) {
                continue;
            }

            InventoryJpa inv = inventoryRepo
                    .findByWarehouseAndItemWithLock(ei.getWarehouseId(), ei.getItemId())
                    .orElse(null);

            int available = inv != null
                    ? Math.max(0, inv.getQuantity() - inv.getReservedQuantity())
                    : 0;

            if (available < ei.getQuantity()) {
                shortages.add(new StockShortageInfo(
                        ei.getWarehouseId(), ei.getItemId(), ei.getQuantity(), available));
                continue;
            }

            StockAllocationJpa allocation = new StockAllocationJpa();
            allocation.setServiceTicketId(getServiceTicketIdFromEstimate(estimateId));
            allocation.setEstimateItemId(ei.getId());
            allocation.setWarehouseId(ei.getWarehouseId());
            allocation.setItemId(ei.getItemId());
            allocation.setQuantity(ei.getQuantity());
            allocation.setStatus(AllocationStatus.RESERVED);
            allocation.setCreatedBy(staffId);
            allocationRepo.save(allocation);

            inv.setReservedQuantity(inv.getReservedQuantity() + ei.getQuantity());
            inventoryRepo.save(inv);

            logTransaction(ei.getWarehouseId(), ei.getItemId(),
                    InventoryTransactionType.ADJUSTMENT, ei.getQuantity(),
                    inv.getQuantity(), "stock_allocation", allocation.getAllocationId(), staffId);
        }

        return shortages;
    }

    @Override
    @Transactional
    public void commit(Integer serviceTicketId, Integer staffId) {
        List<StockAllocationJpa> reserved = allocationRepo
                .findByTicketAndStatus(serviceTicketId, AllocationStatus.RESERVED);

        if (reserved.isEmpty()) return;

        List<CreateStockIssueRequest.IssueItemRequest> issueItems = new ArrayList<>();
        Integer warehouseId = reserved.get(0).getWarehouseId();

        for (StockAllocationJpa alloc : reserved) {
            InventoryJpa inv = inventoryRepo
                    .findByWarehouseAndItemWithLock(alloc.getWarehouseId(), alloc.getItemId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                            "Khong tim thay ton kho cho itemId=" + alloc.getItemId()));

            int newQty = inv.getQuantity() - alloc.getQuantity();
            inv.setQuantity(newQty);
            inv.setReservedQuantity(Math.max(0, inv.getReservedQuantity() - alloc.getQuantity()));
            inventoryRepo.save(inv);

            logTransaction(alloc.getWarehouseId(), alloc.getItemId(),
                    InventoryTransactionType.OUT, alloc.getQuantity(),
                    newQty, "stock_allocation_commit", alloc.getAllocationId(), staffId);

            alloc.setStatus(AllocationStatus.COMMITTED);
            allocationRepo.save(alloc);

            CreateStockIssueRequest.IssueItemRequest item = new CreateStockIssueRequest.IssueItemRequest();
            item.setItemId(alloc.getItemId());
            item.setQuantity(alloc.getQuantity());
            item.setDiscountRate(BigDecimal.ZERO);
            issueItems.add(item);
        }

        CreateStockIssueRequest issueRequest = new CreateStockIssueRequest();
        issueRequest.setWarehouseId(warehouseId);
        issueRequest.setIssueType(IssueType.CUSTOMER_1);
        issueRequest.setIssueReason("Tu dong xuat kho tu ServiceTicket #" + serviceTicketId);
        issueRequest.setServiceTicketId(serviceTicketId);
        issueRequest.setItems(issueItems);

        var issueResp = stockIssueService.create(issueRequest, staffId);
        stockIssueService.confirm(issueResp.getIssueId(), staffId);
    }

    @Override
    @Transactional
    public void release(Integer serviceTicketId, Integer staffId) {
        List<StockAllocationJpa> reserved = allocationRepo
                .findByTicketAndStatus(serviceTicketId, AllocationStatus.RESERVED);

        for (StockAllocationJpa alloc : reserved) {
            InventoryJpa inv = inventoryRepo
                    .findByWarehouseAndItemWithLock(alloc.getWarehouseId(), alloc.getItemId())
                    .orElse(null);

            if (inv != null) {
                inv.setReservedQuantity(Math.max(0, inv.getReservedQuantity() - alloc.getQuantity()));
                inventoryRepo.save(inv);

                logTransaction(alloc.getWarehouseId(), alloc.getItemId(),
                        InventoryTransactionType.ADJUSTMENT, -alloc.getQuantity(),
                        inv.getQuantity(), "stock_allocation_release", alloc.getAllocationId(), staffId);
            }

            alloc.setStatus(AllocationStatus.RELEASED);
            allocationRepo.save(alloc);
        }
    }

    @Override
    @Transactional
    public StockAllocationResult updateAllocation(Integer allocationId, int newQuantity, Integer staffId) {
        StockAllocationJpa alloc = allocationRepo.findById(allocationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Khong tim thay allocation id=" + allocationId));

        if (alloc.getStatus() == AllocationStatus.COMMITTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Khong the cap nhat allocation da commit");
        }

        InventoryJpa inv = inventoryRepo
                .findByWarehouseAndItemWithLock(alloc.getWarehouseId(), alloc.getItemId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Khong tim thay ton kho"));

        int delta = newQuantity - alloc.getQuantity();
        if (delta > 0) {
            int available = Math.max(0, inv.getQuantity() - inv.getReservedQuantity());
            if (available < delta) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Khong du ton kho kha dung de tang allocation");
            }
        }

        inv.setReservedQuantity(inv.getReservedQuantity() + delta);
        inventoryRepo.save(inv);

        alloc.setQuantity(newQuantity);
        allocationRepo.save(alloc);

        return toResult(alloc);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void logTransaction(Integer warehouseId, Integer itemId,
                                 InventoryTransactionType type, int qty,
                                 int balanceAfter, String refType, Integer refId, Integer staffId) {
        InventoryTransactionJpa tx = new InventoryTransactionJpa();
        tx.setWarehouseId(warehouseId);
        tx.setItemId(itemId);
        tx.setTransactionType(type);
        tx.setQuantity(qty);
        tx.setBalanceAfter(balanceAfter);
        tx.setReferenceType(refType);
        tx.setReferenceId(refId);
        tx.setCreatedById(staffId);
        tx.setCreatedAt(Instant.now());
        transactionRepo.save(tx);
    }

    private Integer getServiceTicketIdFromEstimate(Integer estimateId) {
        return estimateRepositoryJpa.findById(estimateId)
                .map(EstimateJpa::getServiceTicketId)
                .orElse(null);
    }

    private StockAllocationResult toResult(StockAllocationJpa a) {
        StockAllocationResult r = new StockAllocationResult();
        r.setAllocationId(a.getAllocationId());
        r.setServiceTicketId(a.getServiceTicketId());
        r.setEstimateItemId(a.getEstimateItemId());
        r.setWarehouseId(a.getWarehouseId());
        r.setItemId(a.getItemId());
        r.setQuantity(a.getQuantity());
        r.setStatus(a.getStatus());
        return r;
    }
}
