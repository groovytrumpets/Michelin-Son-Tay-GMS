package com.g42.platform.gms.warehouse.app.service.allocation;

import com.g42.platform.gms.estimation.infrastructure.entity.EstimateItemJpa;
import com.g42.platform.gms.estimation.infrastructure.entity.EstimateJpa;
import com.g42.platform.gms.estimation.infrastructure.repository.EstimateItemRepositoryJpa;
import com.g42.platform.gms.estimation.infrastructure.repository.EstimateRepositoryJpa;
import com.g42.platform.gms.service_ticket_management.domain.entity.ServiceTicket;
import com.g42.platform.gms.service_ticket_management.domain.enums.TicketStatus;
import com.g42.platform.gms.service_ticket_management.domain.repository.ServiceTicketRepo;
import com.g42.platform.gms.warehouse.api.dto.issue.CreateStockIssueRequest;
import com.g42.platform.gms.warehouse.api.dto.response.StockAllocationResult;
import com.g42.platform.gms.warehouse.api.dto.response.StockIssueResponse;
import com.g42.platform.gms.warehouse.app.service.dto.StockShortageInfo;
import com.g42.platform.gms.warehouse.domain.enums.AllocationStatus;
import com.g42.platform.gms.warehouse.domain.enums.InventoryTransactionType;
import com.g42.platform.gms.warehouse.domain.enums.IssueType;
import com.g42.platform.gms.warehouse.domain.repository.InventoryRepo;
import com.g42.platform.gms.warehouse.domain.repository.InventoryTransactionRepo;
import com.g42.platform.gms.warehouse.app.service.issue.StockIssueService;
import com.g42.platform.gms.warehouse.domain.repository.StockAllocationRepo;
import com.g42.platform.gms.warehouse.domain.repository.StockIssueRepo;
import com.g42.platform.gms.warehouse.domain.entity.Inventory;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service("warehouseStockAllocationService")
@RequiredArgsConstructor
public class StockAllocationService {

    private final StockAllocationRepo allocationRepo;
    private final InventoryRepo inventoryRepo;
    private final InventoryTransactionRepo transactionRepo;
    private final EstimateItemRepositoryJpa estimateItemRepositoryJpa;
    private final EstimateRepositoryJpa estimateRepositoryJpa;
    private final StockIssueService stockIssueService;
    private final StockIssueRepo stockIssueRepo;
    private final ServiceTicketRepo serviceTicketRepo;
    @Transactional
    public List<StockShortageInfo> reserve(Integer estimateId, Integer staffId) {
        List<EstimateItemJpa> estimateItems = estimateItemRepositoryJpa.findByEstimateId(estimateId)
                .stream()
                .filter(i -> i.getWarehouseId() != null)
                .filter(i -> i.getItemId() != null)
                .filter(i -> i.getQuantity() != null && i.getQuantity() > 0)
                .filter(i -> !Boolean.TRUE.equals(i.getIsRemoved()))
                .filter(i -> Boolean.TRUE.equals(i.getIsChecked()))
                .toList();
        List<StockShortageInfo> shortages = new ArrayList<>();

        for (EstimateItemJpa ei : estimateItems) {
            int alreadyReservedOrCommitted = allocationRepo.findByEstimateItemId(ei.getId()).stream()
                    .filter(a -> a.getStatus() == AllocationStatus.RESERVED || a.getStatus() == AllocationStatus.COMMITTED)
                    .mapToInt(StockAllocationJpa::getQuantity)
                    .sum();

            int required = ei.getQuantity();
            int deltaToReserve = required - alreadyReservedOrCommitted;
            if (deltaToReserve <= 0) {
                continue;
            }

            Inventory inv = inventoryRepo
                    .findByWarehouseAndItemWithLock(ei.getWarehouseId(), ei.getItemId())
                    .orElse(null);

            int available = inv != null
                    ? Math.max(0, inv.getQuantity() - inv.getReservedQuantity())
                    : 0;

                if (available < deltaToReserve) {
                shortages.add(new StockShortageInfo(
                    ei.getWarehouseId(), ei.getItemId(), deltaToReserve, available));
                continue;
            }

            StockAllocationJpa allocation = new StockAllocationJpa();
            allocation.setServiceTicketId(getServiceTicketIdFromEstimate(estimateId));
            allocation.setEstimateItemId(ei.getId());
            allocation.setWarehouseId(ei.getWarehouseId());
            allocation.setItemId(ei.getItemId());
                allocation.setQuantity(deltaToReserve);
            allocation.setStatus(AllocationStatus.RESERVED);
            allocation.setCreatedBy(staffId);
            allocationRepo.save(allocation);

                inv.setReservedQuantity(inv.getReservedQuantity() + deltaToReserve);
            inventoryRepo.save(inv);

            logTransaction(ei.getWarehouseId(), ei.getItemId(),
                    InventoryTransactionType.ADJUSTMENT, deltaToReserve,
                    inv.getQuantity(), "stock_allocation", allocation.getAllocationId(), staffId);
        }

        return shortages;
    }
    @Transactional
        public List<StockIssueResponse> requestIssueDraft(Integer serviceTicketId, Integer staffId) {
        if (stockIssueRepo.existsDraftServiceTicketIssue(serviceTicketId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Service ticket da co phieu xuat DRAFT");
        }

        List<StockAllocationJpa> reserved = allocationRepo
                .findByTicketAndStatus(serviceTicketId, AllocationStatus.RESERVED);

        if (reserved.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "Khong co stock allocation RESERVED cho service ticket nay");
        }

        Map<Integer, List<CreateStockIssueRequest.IssueItemRequest>> issueItemsByWarehouse = new HashMap<>();

        for (StockAllocationJpa alloc : reserved) {
            CreateStockIssueRequest.IssueItemRequest item = new CreateStockIssueRequest.IssueItemRequest();
            item.setItemId(alloc.getItemId());
            item.setQuantity(alloc.getQuantity());
            item.setDiscountRate(BigDecimal.ZERO);

            issueItemsByWarehouse
                    .computeIfAbsent(alloc.getWarehouseId(), k -> new ArrayList<>())
                    .add(item);
        }

        List<StockIssueResponse> createdIssues = new ArrayList<>();
        for (Map.Entry<Integer, List<CreateStockIssueRequest.IssueItemRequest>> entry : issueItemsByWarehouse.entrySet()) {
            CreateStockIssueRequest issueRequest = new CreateStockIssueRequest();
            issueRequest.setWarehouseId(entry.getKey());
            issueRequest.setIssueType(IssueType.SERVICE_TICKET);
            issueRequest.setIssueReason("Yeu cau xuat kho tu ServiceTicket #" + serviceTicketId);
            issueRequest.setServiceTicketId(serviceTicketId);
            issueRequest.setItems(entry.getValue());

            createdIssues.add(stockIssueService.create(issueRequest, staffId));
        }

        ServiceTicket ticket = serviceTicketRepo.findByServiceTicketId(serviceTicketId);
        if (ticket != null && ticket.getTicketStatus() == TicketStatus.ESTIMATED) {
            ticket.setTicketStatus(TicketStatus.PENDING);
            serviceTicketRepo.save(ticket);
        }

        return createdIssues;
    }

    @Transactional
    public void commitReservedAfterIssueConfirmed(Integer serviceTicketId, Integer staffId) {
        List<StockAllocationJpa> reserved = allocationRepo
                .findByTicketAndStatus(serviceTicketId, AllocationStatus.RESERVED);

        for (StockAllocationJpa alloc : reserved) {
            Inventory inv = inventoryRepo
                    .findByWarehouseAndItemWithLock(alloc.getWarehouseId(), alloc.getItemId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                            "Khong tim thay ton kho cho itemId=" + alloc.getItemId()));

            if (inv.getReservedQuantity() < alloc.getQuantity()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Reserved quantity khong hop le cho itemId=" + alloc.getItemId());
            }

            inv.setReservedQuantity(inv.getReservedQuantity() - alloc.getQuantity());
            inventoryRepo.save(inv);

            logTransaction(alloc.getWarehouseId(), alloc.getItemId(),
                    InventoryTransactionType.ADJUSTMENT, -alloc.getQuantity(),
                    inv.getQuantity(), "stock_allocation_commit", alloc.getAllocationId(), staffId);

            alloc.setStatus(AllocationStatus.COMMITTED);
            allocationRepo.save(alloc);
        }
    }

    @Transactional
    public void commitOnPaid(Integer serviceTicketId, Integer estimateId, Integer staffId) {
        if (stockIssueRepo.existsConfirmedServiceTicketIssue(serviceTicketId)) {
            return;
        }

        List<StockAllocationJpa> reserved = allocationRepo
                .findByTicketAndStatus(serviceTicketId, AllocationStatus.RESERVED);

        if (!reserved.isEmpty()) {
            requestIssueDraft(serviceTicketId, staffId);
            return;
        }

        List<EstimateItemJpa> estimateItems = estimateItemRepositoryJpa.findByEstimateId(estimateId)
                .stream()
                .filter(i -> i.getWarehouseId() != null)
                .filter(i -> i.getItemId() != null)
                .filter(i -> i.getQuantity() != null && i.getQuantity() > 0)
                .filter(i -> !Boolean.TRUE.equals(i.getIsRemoved()))
                .filter(i -> Boolean.TRUE.equals(i.getIsChecked()))
                .toList();

        if (estimateItems.isEmpty()) {
            return;
        }

        Map<Integer, List<CreateStockIssueRequest.IssueItemRequest>> issueItemsByWarehouse = new HashMap<>();

        for (EstimateItemJpa estimateItem : estimateItems) {
            CreateStockIssueRequest.IssueItemRequest item = new CreateStockIssueRequest.IssueItemRequest();
            item.setItemId(estimateItem.getItemId());
            item.setQuantity(estimateItem.getQuantity());
            item.setDiscountRate(BigDecimal.ZERO);
            issueItemsByWarehouse
                    .computeIfAbsent(estimateItem.getWarehouseId(), k -> new ArrayList<>())
                    .add(item);
        }

        for (Map.Entry<Integer, List<CreateStockIssueRequest.IssueItemRequest>> entry : issueItemsByWarehouse.entrySet()) {
            CreateStockIssueRequest issueRequest = new CreateStockIssueRequest();
            issueRequest.setWarehouseId(entry.getKey());
            issueRequest.setIssueType(IssueType.SERVICE_TICKET);
            issueRequest.setIssueReason("Fallback yeu cau xuat kho theo Estimate #" + estimateId);
            issueRequest.setServiceTicketId(serviceTicketId);
            issueRequest.setItems(entry.getValue());

            stockIssueService.create(issueRequest, staffId);
        }
    }
    @Transactional
    public void release(Integer serviceTicketId, Integer staffId) {
        List<StockAllocationJpa> reserved = allocationRepo
                .findByTicketAndStatus(serviceTicketId, AllocationStatus.RESERVED);

        for (StockAllocationJpa alloc : reserved) {
            Inventory inv = inventoryRepo
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
    @Transactional
    public StockAllocationResult updateAllocation(Integer allocationId, int newQuantity, Integer staffId) {
        StockAllocationJpa alloc = allocationRepo.findById(allocationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Khong tim thay allocation id=" + allocationId));

        if (alloc.getStatus() == AllocationStatus.COMMITTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Khong the cap nhat allocation da commit");
        }

        Inventory inv = inventoryRepo
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
