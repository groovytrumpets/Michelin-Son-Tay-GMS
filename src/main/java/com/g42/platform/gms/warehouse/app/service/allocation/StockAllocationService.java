package com.g42.platform.gms.warehouse.app.service.allocation;

import com.g42.platform.gms.estimation.domain.entity.Estimate;
import com.g42.platform.gms.estimation.domain.entity.EstimateItem;
import com.g42.platform.gms.estimation.domain.repository.EstimateItemRepository;
import com.g42.platform.gms.estimation.domain.repository.EstimateRepository;
import com.g42.platform.gms.service_ticket_management.domain.entity.ServiceTicket;
import com.g42.platform.gms.service_ticket_management.domain.enums.TicketStatus;
import com.g42.platform.gms.service_ticket_management.domain.repository.ServiceTicketRepo;
import com.g42.platform.gms.warehouse.api.dto.issue.CreateStockIssueRequest;
import com.g42.platform.gms.warehouse.api.dto.response.StockAllocationResult;
import com.g42.platform.gms.warehouse.api.dto.response.StockIssueResponse;
import com.g42.platform.gms.warehouse.app.service.dto.StockShortageInfo;
import com.g42.platform.gms.warehouse.app.service.issue.StockIssueService;
import com.g42.platform.gms.warehouse.domain.entity.Inventory;
import com.g42.platform.gms.warehouse.domain.entity.InventoryTransaction;
import com.g42.platform.gms.warehouse.domain.entity.StockAllocation;
import com.g42.platform.gms.warehouse.domain.enums.AllocationStatus;
import com.g42.platform.gms.warehouse.domain.enums.InventoryTransactionType;
import com.g42.platform.gms.warehouse.domain.enums.IssueType;
import com.g42.platform.gms.warehouse.domain.repository.InventoryRepo;
import com.g42.platform.gms.warehouse.domain.repository.InventoryTransactionRepo;
import com.g42.platform.gms.warehouse.domain.repository.StockAllocationRepo;
import com.g42.platform.gms.warehouse.domain.repository.StockIssueRepo;
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
    private final EstimateItemRepository estimateItemRepository;
    private final EstimateRepository estimateRepository;
    private final StockIssueService stockIssueService;
    private final StockIssueRepo stockIssueRepo;
    private final ServiceTicketRepo serviceTicketRepo;

    @Transactional
    public List<StockShortageInfo> reserve(Integer estimateId, Integer staffId) {
        List<EstimateItem> estimateItems = getCheckedEstimateItems(estimateId);

        List<StockShortageInfo> shortages = new ArrayList<>();

        for (EstimateItem estimateItem : estimateItems) {
            int requiredQuantity = estimateItem.getQuantity();

            List<StockAllocation> existingAllocations = allocationRepo.findByEstimateItemId(estimateItem.getId());
            StockAllocation existingAllocation = existingAllocations.isEmpty()
                ? null
                : existingAllocations.get(0);

            Inventory inventory = inventoryRepo
                    .findByWarehouseAndItemWithLock(estimateItem.getWarehouseId(), estimateItem.getItemId())
                    .orElse(null);

            int availableQuantity = inventory != null
                    ? Math.max(0, inventory.getQuantity() - inventory.getReservedQuantity())
                    : 0;

            // delta > 0: reserve thêm, delta < 0: giảm phần đang reserve.
            int reserveDelta = existingAllocation != null
                    ? requiredQuantity - existingAllocation.getQuantity()
                    : requiredQuantity;

            if (existingAllocation != null && existingAllocation.getStatus() == AllocationStatus.COMMITTED) {
                continue;
            }

            if (reserveDelta > 0 && availableQuantity < reserveDelta) {
                shortages.add(new StockShortageInfo(
                        estimateItem.getWarehouseId(),
                        estimateItem.getItemId(),
                        reserveDelta,
                        availableQuantity
                ));
                continue;
            }

            if (existingAllocation != null) {
                if (reserveDelta != 0) {
                    existingAllocation.setQuantity(requiredQuantity);
                    allocationRepo.save(existingAllocation);
                    inventory.setReservedQuantity(inventory.getReservedQuantity() + reserveDelta);
                    inventoryRepo.save(inventory);

                    logTransaction(
                            estimateItem.getWarehouseId(),
                            estimateItem.getItemId(),
                            InventoryTransactionType.ADJUSTMENT,
                            reserveDelta,
                            inventory.getQuantity(),
                            "stock_allocation",
                            existingAllocation.getAllocationId(),
                            staffId
                    );
                }
                continue;
            }

            Integer serviceTicketId = getServiceTicketIdFromEstimate(estimateId);
            StockAllocation allocation = new StockAllocation();
            allocation.setServiceTicketId(serviceTicketId);
            allocation.setEstimateItemId(estimateItem.getId());
            allocation.setWarehouseId(estimateItem.getWarehouseId());
            allocation.setItemId(estimateItem.getItemId());
            allocation.setQuantity(requiredQuantity);
            allocation.setStatus(AllocationStatus.RESERVED);
            allocation.setCreatedBy(staffId);
            StockAllocation savedAllocation = allocationRepo.save(allocation);

            inventory.setReservedQuantity(inventory.getReservedQuantity() + requiredQuantity);
            inventoryRepo.save(inventory);

            logTransaction(
                    estimateItem.getWarehouseId(),
                    estimateItem.getItemId(),
                    InventoryTransactionType.ADJUSTMENT,
                    requiredQuantity,
                    inventory.getQuantity(),
                    "stock_allocation",
                    savedAllocation.getAllocationId(),
                    staffId
            );
        }

        return shortages;
    }

    @Transactional
    public List<StockIssueResponse> requestIssueDraft(Integer serviceTicketId, Integer staffId) {
        List<StockAllocation> reserved = allocationRepo
                .findByTicketAndStatus(serviceTicketId, AllocationStatus.RESERVED);

        if (reserved.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Khong co stock allocation RESERVED cho service ticket nay");
        }

        Map<Integer, List<CreateStockIssueRequest.IssueItemRequest>> issueItemsByWarehouse = new HashMap<>();

        for (StockAllocation alloc : reserved) {
            // Chỉ tạo phiếu cho allocation chưa được gắn issue.
            if (alloc.getIssueId() != null) {
                continue;
            }

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

        if (createdIssues.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Khong co allocation RESERVED moi de tao phieu xuat kho");
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
        List<StockAllocation> reserved = allocationRepo
                .findByTicketAndStatus(serviceTicketId, AllocationStatus.RESERVED);

        for (StockAllocation alloc : reserved) {
            Inventory inventory = inventoryRepo
                    .findByWarehouseAndItemWithLock(alloc.getWarehouseId(), alloc.getItemId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                            "Khong tim thay ton kho cho itemId=" + alloc.getItemId()));

            if (inventory.getReservedQuantity() < alloc.getQuantity()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Reserved quantity khong hop le cho itemId=" + alloc.getItemId());
            }

            inventory.setReservedQuantity(inventory.getReservedQuantity() - alloc.getQuantity());
            inventoryRepo.save(inventory);

            logTransaction(
                    alloc.getWarehouseId(),
                    alloc.getItemId(),
                    InventoryTransactionType.ADJUSTMENT,
                    -alloc.getQuantity(),
                    inventory.getQuantity(),
                    "stock_allocation_commit",
                    alloc.getAllocationId(),
                    staffId
            );

            alloc.setStatus(AllocationStatus.COMMITTED);
            allocationRepo.save(alloc);
        }
    }

    @Transactional
    public void commitOnPaid(Integer serviceTicketId, Integer estimateId, Integer staffId) {
        if (stockIssueRepo.existsConfirmedServiceTicketIssue(serviceTicketId)) {
            return;
        }

        List<StockAllocation> reserved = allocationRepo
                .findByTicketAndStatus(serviceTicketId, AllocationStatus.RESERVED);

        if (!reserved.isEmpty()) {
            requestIssueDraft(serviceTicketId, staffId);
            return;
        }

        List<EstimateItem> estimateItems = getCheckedEstimateItems(estimateId);

        if (estimateItems.isEmpty()) {
            return;
        }

        Map<Integer, List<CreateStockIssueRequest.IssueItemRequest>> issueItemsByWarehouse = new HashMap<>();

        for (EstimateItem estimateItem : estimateItems) {
            CreateStockIssueRequest.IssueItemRequest item = new CreateStockIssueRequest.IssueItemRequest();
            item.setItemId(estimateItem.getItemId());
            item.setQuantity(estimateItem.getQuantity());
            item.setDiscountRate(BigDecimal.ZERO);
            issueItemsByWarehouse
                    .computeIfAbsent(estimateItem.getWarehouseId(), k -> new ArrayList<>())
                    .add(item);
        }

        if (issueItemsByWarehouse.isEmpty()) {
            return;
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
        List<StockAllocation> reserved = allocationRepo
                .findByTicketAndStatus(serviceTicketId, AllocationStatus.RESERVED);

        for (StockAllocation alloc : reserved) {
            Inventory inventory = inventoryRepo
                    .findByWarehouseAndItemWithLock(alloc.getWarehouseId(), alloc.getItemId())
                    .orElse(null);

            if (inventory != null) {
                inventory.setReservedQuantity(Math.max(0, inventory.getReservedQuantity() - alloc.getQuantity()));
                inventoryRepo.save(inventory);

                logTransaction(
                        alloc.getWarehouseId(),
                        alloc.getItemId(),
                        InventoryTransactionType.ADJUSTMENT,
                        -alloc.getQuantity(),
                        inventory.getQuantity(),
                        "stock_allocation_release",
                        alloc.getAllocationId(),
                        staffId
                );
            }

            alloc.setStatus(AllocationStatus.RELEASED);
            allocationRepo.save(alloc);
        }
    }

    @Transactional
    public StockAllocationResult updateAllocation(Integer allocationId, int newQuantity, Integer staffId) {
        StockAllocation allocation = allocationRepo.findById(allocationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Khong tim thay allocation id=" + allocationId));

        if (allocation.getStatus() == AllocationStatus.COMMITTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Khong the cap nhat allocation da commit");
        }

        Inventory inventory = inventoryRepo
                .findByWarehouseAndItemWithLock(allocation.getWarehouseId(), allocation.getItemId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Khong tim thay ton kho"));

        int delta = newQuantity - allocation.getQuantity();
        if (delta > 0) {
            int available = Math.max(0, inventory.getQuantity() - inventory.getReservedQuantity());
            if (available < delta) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Khong du ton kho kha dung de tang allocation");
            }
        }

        inventory.setReservedQuantity(inventory.getReservedQuantity() + delta);
        inventoryRepo.save(inventory);

        allocation.setQuantity(newQuantity);
        allocationRepo.save(allocation);

        return toResult(allocation);
    }

    private List<EstimateItem> getCheckedEstimateItems(Integer estimateId) {
        return estimateItemRepository.findByEstimateId(estimateId)
                .stream()
                .filter(i -> i.getWarehouseId() != null)
                .filter(i -> i.getItemId() != null)
                .filter(i -> i.getQuantity() != null && i.getQuantity() > 0)
                .filter(i -> !Boolean.TRUE.equals(i.getIsRemoved()))
                .filter(i -> Boolean.TRUE.equals(i.getIsChecked()))
                .toList();
    }

    private void logTransaction(
            Integer warehouseId,
            Integer itemId,
            InventoryTransactionType type,
            int qty,
            int balanceAfter,
            String refType,
            Integer refId,
            Integer staffId
    ) {
        InventoryTransaction tx = new InventoryTransaction();
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
        Estimate estimate = estimateRepository.findEstimateById(estimateId);
        return estimate != null ? estimate.getServiceTicketId() : null;
    }

    private StockAllocationResult toResult(StockAllocation a) {
        StockAllocationResult r = new StockAllocationResult();
        r.setAllocationId(a.getAllocationId());
        r.setServiceTicketId(a.getServiceTicketId());
        r.setIssueId(a.getIssueId());
        r.setEstimateItemId(a.getEstimateItemId());
        r.setWarehouseId(a.getWarehouseId());
        r.setItemId(a.getItemId());
        r.setQuantity(a.getQuantity());
        r.setStatus(a.getStatus());
        return r;
    }
}
