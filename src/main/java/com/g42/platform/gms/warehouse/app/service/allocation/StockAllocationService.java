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
import com.g42.platform.gms.warehouse.domain.repository.StockIssueItemRepo;
import com.g42.platform.gms.warehouse.domain.repository.StockIssueRepo;
import com.g42.platform.gms.warehouse.domain.entity.StockIssueItem;
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
    private final StockIssueItemRepo stockIssueItemRepo;
    private final ServiceTicketRepo serviceTicketRepo;

    @Transactional
    public List<StockShortageInfo> reserve(Integer estimateId, Integer staffId) {
        List<EstimateItem> estimateItems = getCheckedEstimateItems(estimateId);
        List<StockShortageInfo> shortages = new ArrayList<>();

        for (EstimateItem estimateItem : estimateItems) {
            int requiredQuantity = estimateItem.getQuantity();

            StockAllocation existingAllocation = findFirstAllocationByEstimateItem(estimateItem.getId());
            if (existingAllocation != null && existingAllocation.getStatus() == AllocationStatus.COMMITTED) {
                continue;
            }

            Inventory inventory = getInventoryOrThrow(estimateItem.getWarehouseId(), estimateItem.getItemId());
            int availableQuantity = getAvailableQuantity(inventory);
            // reserveDelta là phần chênh lệch cần tăng/giảm reserved so với allocation hiện có.
            int reserveDelta = existingAllocation != null
                    ? requiredQuantity - existingAllocation.getQuantity()
                    : requiredQuantity;

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
                if (reserveDelta == 0) {
                    continue;
                }

                existingAllocation.setQuantity(requiredQuantity);
                allocationRepo.save(existingAllocation);

                adjustReservedQuantity(inventory, reserveDelta);
                logAdjustmentTransaction(
                        estimateItem.getWarehouseId(),
                        estimateItem.getItemId(),
                        reserveDelta,
                        inventory.getQuantity(),
                        "stock_allocation",
                        existingAllocation.getAllocationId(),
                        staffId
                );
                continue;
            }

            Integer serviceTicketId = getServiceTicketIdFromEstimate(estimateId);
            StockAllocation allocation = new StockAllocation();
            allocation.setServiceTicketId(serviceTicketId);
            allocation.setEstimateItemId(estimateItem.getId());
            allocation.setEstimateId(estimateId);
            allocation.setWarehouseId(estimateItem.getWarehouseId());
            allocation.setItemId(estimateItem.getItemId());
            allocation.setQuantity(requiredQuantity);
            allocation.setStatus(AllocationStatus.RESERVED);
            allocation.setCreatedBy(staffId);
            StockAllocation savedAllocation = allocationRepo.save(allocation);

            adjustReservedQuantity(inventory, requiredQuantity);
            logAdjustmentTransaction(
                    estimateItem.getWarehouseId(),
                    estimateItem.getItemId(),
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

        // true = chỉ lấy allocation chưa gán issueId để tránh tạo draft trùng cho cùng allocation.
        Map<Integer, List<CreateStockIssueRequest.IssueItemRequest>> issueItemsByWarehouse =
                buildIssueItemsByWarehouseFromAllocations(reserved, true);

        List<StockIssueResponse> createdIssues = createIssueDrafts(
                issueItemsByWarehouse,
                serviceTicketId,
                "Yeu cau xuat kho tu ServiceTicket #" + serviceTicketId,
                staffId
        );

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

            adjustReservedQuantity(inventory, -alloc.getQuantity());
            logAdjustmentTransaction(
                    alloc.getWarehouseId(),
                    alloc.getItemId(),
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
            // Luồng chính: đã có RESERVED thì tạo draft issue trực tiếp từ allocations.
            requestIssueDraft(serviceTicketId, staffId);
            return;
        }

        List<EstimateItem> estimateItems = getCheckedEstimateItems(estimateId);

        if (estimateItems.isEmpty()) {
            return;
        }

        Map<Integer, List<CreateStockIssueRequest.IssueItemRequest>> issueItemsByWarehouse =
                buildIssueItemsByWarehouseFromEstimateItems(estimateItems);

        if (issueItemsByWarehouse.isEmpty()) {
            return;
        }

        // Luồng fallback: chưa có allocation RESERVED thì dựng draft từ estimate items đã checked.
        createIssueDrafts(
                issueItemsByWarehouse,
                serviceTicketId,
                "Fallback yeu cau xuat kho theo Estimate #" + estimateId,
                staffId
        );
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
                int updatedReserved = Math.max(0, inventory.getReservedQuantity() - alloc.getQuantity());
                inventory.setReservedQuantity(updatedReserved);
                inventoryRepo.save(inventory);

                logAdjustmentTransaction(
                        alloc.getWarehouseId(),
                        alloc.getItemId(),
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
    public void cancelStockAllocation(Integer estimateItemId, Integer issueId, Integer staffId) {
        cancelStockAllocation(estimateItemId, issueId, null, staffId);
    }

    @Transactional
    public void cancelStockAllocation(Integer estimateItemId, Integer issueId, Integer issueItemId, Integer staffId) {
        if (estimateItemId == null && issueId == null && issueItemId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Phải cung cấp estimateItemId, issueId hoặc issueItemId");
        }

        if (issueItemId != null) {
            // Cancel allocation của 1 item cụ thể trong phiếu xuất (truyền issueItemId trực tiếp)
            cancelReservedByIssueItem(issueItemId, staffId);
        } else if (issueId != null && estimateItemId != null) {
            // FE truyền cả hai: tìm issue item của estimateItemId trong phiếu issueId → xóa item đó
            cancelReservedByEstimateItemInIssue(estimateItemId, issueId, staffId);
        } else if (issueId != null) {
            // Chỉ có issueId: cancel tất cả allocation của phiếu (cancel cả phiếu)
            cancelReservedWithIssue(issueId, staffId);
        } else {
            // Chỉ có estimateItemId và không có issueId
            cancelReservedWithoutIssue(estimateItemId, staffId);
        }
    }

    private void cancelReservedWithoutIssue(Integer estimateItemId, Integer staffId) {
        List<StockAllocation> allocations = allocationRepo.findByEstimateItemId(estimateItemId);

        if (allocations.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Không tìm thấy allocation cho estimateItemId=" + estimateItemId);
        }

        for (StockAllocation allocation : allocations) {
            if (allocation.getStatus() == AllocationStatus.COMMITTED) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Allocation id=" + allocation.getAllocationId() + " đã COMMITTED, không thể hủy giữ chỗ");
            }

            if (allocation.getStatus() == AllocationStatus.RELEASED) {
                continue;
            }

            // Nếu allocation đã gắn phiếu xuất → FE phải truyền issueId để xử lý đúng luồng
            if (allocation.getIssueId() != null) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Allocation id=" + allocation.getAllocationId()
                                + " đã gắn phiếu xuất issueId=" + allocation.getIssueId()
                                + ". Vui lòng truyền thêm issueId để cancel item trong phiếu");
            }

            Inventory inventory = inventoryRepo
                    .findByWarehouseAndItemWithLock(allocation.getWarehouseId(), allocation.getItemId())
                    .orElse(null);

            if (inventory != null) {
                int updatedReserved = Math.max(0, inventory.getReservedQuantity() - allocation.getQuantity());
                inventory.setReservedQuantity(updatedReserved);
                inventoryRepo.save(inventory);

                logAdjustmentTransaction(
                        allocation.getWarehouseId(),
                        allocation.getItemId(),
                        -allocation.getQuantity(),
                        inventory.getQuantity(),
                        "stock_allocation_cancel",
                        allocation.getAllocationId(),
                        staffId
                );
            }

            allocation.setStatus(AllocationStatus.RELEASED);
            allocationRepo.save(allocation);
        }
    }

    /**
     * Tìm issue item của estimateItemId trong phiếu issueId, sau đó xóa item đó khỏi phiếu.
     * Dùng khi FE truyền cả estimateItemId + issueId (cancel 1 item cụ thể trong phiếu).
     */
    private void cancelReservedByEstimateItemInIssue(Integer estimateItemId, Integer issueId, Integer staffId) {
        // Tìm allocation của estimateItemId đang gắn với issueId này
        List<StockAllocation> allocations = allocationRepo.findByEstimateItemId(estimateItemId)
                .stream()
                .filter(a -> issueId.equals(a.getIssueId()) && a.getStatus() == AllocationStatus.RESERVED)
                .toList();

        if (allocations.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Không tìm thấy allocation RESERVED cho estimateItemId=" + estimateItemId
                            + " trong phiếu issueId=" + issueId);
        }

        // Lấy itemId từ allocation để tìm issue item tương ứng
        Integer itemId = allocations.get(0).getItemId();

        List<StockIssueItem> issueItems = stockIssueItemRepo.findByIssueId(issueId)
                .stream()
                .filter(i -> itemId.equals(i.getItemId()))
                .toList();

        if (issueItems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Không tìm thấy issue item cho itemId=" + itemId + " trong phiếu issueId=" + issueId);
        }

        // Release tất cả allocation của estimateItemId này
        for (StockAllocation allocation : allocations) {
            inventoryRepo.findByWarehouseAndItemWithLock(allocation.getWarehouseId(), allocation.getItemId())
                    .ifPresent(inv -> {
                        inv.setReservedQuantity(Math.max(0, inv.getReservedQuantity() - allocation.getQuantity()));
                        inventoryRepo.save(inv);
                    });
            logAdjustmentTransaction(
                    allocation.getWarehouseId(), allocation.getItemId(),
                    -allocation.getQuantity(), 0,
                    "stock_allocation_cancel_item", allocation.getAllocationId(), staffId
            );
            allocation.setStatus(AllocationStatus.RELEASED);

            allocationRepo.save(allocation);
        }

        // Xóa issue item khỏi phiếu (xóa tất cả dòng của itemId này trong phiếu)
        for (StockIssueItem issueItem : issueItems) {
            stockIssueItemRepo.deleteById(issueItem.getIssueItemId());
        }

        // Nếu phiếu hết item → tự cancel phiếu
        List<StockIssueItem> remaining = stockIssueItemRepo.findByIssueId(issueId);
        if (remaining.isEmpty()) {
            stockIssueService.cancel(issueId, staffId);
        }
    }

    private void cancelReservedByIssueItem(Integer issueItemId, Integer staffId) {
        StockIssueItem issueItem = stockIssueItemRepo.findById(issueItemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy issue item id=" + issueItemId));

        Integer issueId = issueItem.getIssueId();
        Integer itemId = issueItem.getItemId();

        // Tìm allocation RESERVED của item này trong phiếu xuất
        List<StockAllocation> allocations = allocationRepo.findByIssueIdAndStatus(issueId, AllocationStatus.RESERVED)
                .stream()
                .filter(a -> itemId.equals(a.getItemId()))
                .toList();

        if (allocations.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Không tìm thấy allocation RESERVED cho issueItemId=" + issueItemId);
        }

        for (StockAllocation allocation : allocations) {
            Inventory inventory = inventoryRepo
                    .findByWarehouseAndItemWithLock(allocation.getWarehouseId(), allocation.getItemId())
                    .orElse(null);

            if (inventory != null) {
                int updatedReserved = Math.max(0, inventory.getReservedQuantity() - allocation.getQuantity());
                inventory.setReservedQuantity(updatedReserved);
                inventoryRepo.save(inventory);

                logAdjustmentTransaction(
                        allocation.getWarehouseId(),
                        allocation.getItemId(),
                        -allocation.getQuantity(),
                        inventory.getQuantity(),
                        "stock_allocation_cancel_item",
                        allocation.getAllocationId(),
                        staffId
                );
            }

            allocation.setStatus(AllocationStatus.RELEASED);

            allocationRepo.save(allocation);
        }

        // Xóa issue item khỏi phiếu
        stockIssueItemRepo.deleteById(issueItemId);

        // Nếu phiếu hết item → tự cancel phiếu
        List<com.g42.platform.gms.warehouse.domain.entity.StockIssueItem> remaining =
                stockIssueItemRepo.findByIssueId(issueId);
        if (remaining.isEmpty()) {
            stockIssueService.cancel(issueId, staffId);
        }
    }

    private void cancelReservedWithIssue(Integer issueId, Integer staffId) {
        List<StockAllocation> allocations = allocationRepo.findByIssueIdAndStatus(issueId, AllocationStatus.RESERVED);

        if (allocations.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Không tìm thấy allocation RESERVED cho issueId=" + issueId);
        }

        for (StockAllocation allocation : allocations) {
            Inventory inventory = inventoryRepo
                    .findByWarehouseAndItemWithLock(allocation.getWarehouseId(), allocation.getItemId())
                    .orElse(null);

            if (inventory != null) {
                // Giảm reserved quantity (allocation vẫn ở RESERVED)
                int updatedReserved = Math.max(0, inventory.getReservedQuantity() - allocation.getQuantity());
                inventory.setReservedQuantity(updatedReserved);
                inventoryRepo.save(inventory);

                logAdjustmentTransaction(
                        allocation.getWarehouseId(),
                        allocation.getItemId(),
                        -allocation.getQuantity(),
                        inventory.getQuantity(),
                        "stock_allocation_cancel_with_issue",
                        allocation.getAllocationId(),
                        staffId
                );
            }

            allocation.setStatus(AllocationStatus.RELEASED);
            allocationRepo.save(allocation);
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
            int available = getAvailableQuantity(inventory);
            if (available < delta) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Khong du ton kho kha dung de tang allocation");
            }
        }

        adjustReservedQuantity(inventory, delta);

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

    private StockAllocation findFirstAllocationByEstimateItem(Integer estimateItemId) {
        List<StockAllocation> allocations = allocationRepo.findByEstimateItemId(estimateItemId);
        return allocations.isEmpty() ? null : allocations.get(0);
    }

    private Inventory getInventoryOrThrow(Integer warehouseId, Integer itemId) {
        return inventoryRepo.findByWarehouseAndItemWithLock(warehouseId, itemId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "Khong tim thay ton kho cho itemId=" + itemId + " tai warehouseId=" + warehouseId
                ));
    }

    private int getAvailableQuantity(Inventory inventory) {
        return Math.max(0, inventory.getQuantity() - inventory.getReservedQuantity());
    }

    private void adjustReservedQuantity(Inventory inventory, int delta) {
        inventory.setReservedQuantity(inventory.getReservedQuantity() + delta);
        inventoryRepo.save(inventory);
    }

    private Map<Integer, List<CreateStockIssueRequest.IssueItemRequest>> buildIssueItemsByWarehouseFromAllocations(
            List<StockAllocation> allocations,
            boolean skipAssignedIssue
    ) {
        Map<Integer, List<CreateStockIssueRequest.IssueItemRequest>> result = new HashMap<>();
        for (StockAllocation allocation : allocations) {
            // skipAssignedIssue: bỏ qua allocation đã gắn issueId VÀ issue đó vẫn còn DRAFT
            // (tránh tạo trùng item cho cùng phiếu đang tồn tại)
            if (skipAssignedIssue && allocation.getIssueId() != null) {
                boolean issueStillActive = stockIssueRepo.findById(allocation.getIssueId())
                        .map(issue -> issue.getStatus() == com.g42.platform.gms.warehouse.domain.enums.StockIssueStatus.DRAFT)
                        .orElse(false);
                if (issueStillActive) {
                    continue;
                }
            }
            addIssueItem(result, allocation.getWarehouseId(), allocation.getItemId(), allocation.getQuantity());
        }
        return result;
    }

    private Map<Integer, List<CreateStockIssueRequest.IssueItemRequest>> buildIssueItemsByWarehouseFromEstimateItems(
            List<EstimateItem> estimateItems
    ) {
        Map<Integer, List<CreateStockIssueRequest.IssueItemRequest>> result = new HashMap<>();
        for (EstimateItem estimateItem : estimateItems) {
            addIssueItem(result, estimateItem.getWarehouseId(), estimateItem.getItemId(), estimateItem.getQuantity());
        }
        return result;
    }

    private void addIssueItem(
            Map<Integer, List<CreateStockIssueRequest.IssueItemRequest>> issueItemsByWarehouse,
            Integer warehouseId,
            Integer itemId,
            Integer quantity
    ) {
        CreateStockIssueRequest.IssueItemRequest item = new CreateStockIssueRequest.IssueItemRequest();
        item.setItemId(itemId);
        item.setQuantity(quantity);
        item.setDiscountRate(BigDecimal.ZERO);
        issueItemsByWarehouse.computeIfAbsent(warehouseId, key -> new ArrayList<>()).add(item);
    }

    private List<StockIssueResponse> createIssueDrafts(
            Map<Integer, List<CreateStockIssueRequest.IssueItemRequest>> issueItemsByWarehouse,
            Integer serviceTicketId,
            String issueReason,
            Integer staffId
    ) {
        List<StockIssueResponse> createdIssues = new ArrayList<>();
        for (Map.Entry<Integer, List<CreateStockIssueRequest.IssueItemRequest>> entry : issueItemsByWarehouse.entrySet()) {
            Integer warehouseId = entry.getKey();
            if (stockIssueRepo.existsDraftServiceTicketIssueInWarehouse(serviceTicketId, warehouseId)) {
                continue;
            }

            CreateStockIssueRequest issueRequest = new CreateStockIssueRequest();
            issueRequest.setWarehouseId(warehouseId);
            issueRequest.setIssueType(IssueType.SERVICE_TICKET);
            issueRequest.setIssueReason(issueReason);
            issueRequest.setServiceTicketId(serviceTicketId);
            issueRequest.setItems(entry.getValue());
            createdIssues.add(stockIssueService.create(issueRequest, staffId));
        }
        return createdIssues;
    }

    private void logAdjustmentTransaction(
            Integer warehouseId,
            Integer itemId,
            int qty,
            int balanceAfter,
            String refType,
            Integer refId,
            Integer staffId
    ) {
        logTransaction(
                warehouseId,
                itemId,
                InventoryTransactionType.ADJUSTMENT,
                qty,
                balanceAfter,
                refType,
                refId,
                staffId
        );
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
