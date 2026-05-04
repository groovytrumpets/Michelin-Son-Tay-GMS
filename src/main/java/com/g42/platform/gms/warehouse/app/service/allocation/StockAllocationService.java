package com.g42.platform.gms.warehouse.app.service.allocation;

import com.g42.platform.gms.estimation.api.internal.EstimateInternalApi;
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
import com.g42.platform.gms.warehouse.domain.entity.StockEntryItem;
import com.g42.platform.gms.warehouse.domain.repository.StockEntryRepo;
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

    /*
     * Mô tả các dependency (repo/service) chính và vai trò của chúng trong service này:
     * - allocationRepo: quản lý entity StockAllocation (tạo, cập nhật, tìm kiếm allocations).
     * - inventoryRepo: đọc và cập nhật entity Inventory; nhiều chỗ gọi
     *   findByWarehouseAndItemWithLock(...) để khóa hàng/row trước khi chỉnh sửa.
     * - transactionRepo: ghi InventoryTransaction để audit mọi thay đổi tồn kho (reserve/commit/adjust).
     * - estimateItemRepository / estimateRepository / estimateInternalApi: lấy thông tin Estimate/EstimateItem
     *   để biết yêu cầu xuất hàng, quantities, và mapping tới ServiceTicket.
     * - stockIssueService / stockIssueRepo / stockIssueItemRepo: tạo hoặc gắn allocations vào StockIssue (draft),
     *   và đọc/ghi các phiếu xuất hàng liên quan.
     * - stockEntryRepo: dùng để truy vấn các lô (entry items) khi cần tính toán FIFO hoặc kiểm tra lô tồn.
     * - serviceTicketRepo: cập nhật trạng thái ServiceTicket khi cần (ví dụ chuyển ESTIMATED → PENDING).
     *
     * Ghi chú quan trọng (concurrency / invariants):
     * - Mọi thao tác thay đổi số lượng thực tế (inventory.quantity, inventory.reservedQuantity,
     *   stockEntry.remainingQuantity) cần gọi các phương thức có lock hoặc thực hiện trong cùng một
     *   transaction (@Transactional) để tránh race condition.
     * - Allocation có trạng thái RESERVED/COMMITTED: không ghi đè allocation đã COMMITTED.
     */

    private final StockAllocationRepo allocationRepo;
    private final InventoryRepo inventoryRepo;
    private final InventoryTransactionRepo transactionRepo;
    private final EstimateItemRepository estimateItemRepository;
    private final EstimateRepository estimateRepository;
    private final StockIssueService stockIssueService;
    private final StockIssueRepo stockIssueRepo;
    private final StockIssueItemRepo stockIssueItemRepo;
    private final ServiceTicketRepo serviceTicketRepo;
    private final StockEntryRepo stockEntryRepo;
    private final EstimateInternalApi estimateInternalApi;

    /**
     * Giữ chỗ (reserve) stock dựa trên estimate items đã check.
     * Cơ chế:
     * - Nếu allocation tồn tại: cập nhật quantity (có thể tăng/giảm)
     * - Nếu chưa có allocation: tạo mới với trạng thái RESERVED
     * - Nếu không đủ tồn kho: thêm vào danh sách shortages để báo thiếu hàng
     *
     * @param estimateId - ID của báo giá
     * @param staffId - ID nhân viên thực hiện
     * @return List các item bị thiếu hàng (nếu có)
     */
    @Transactional
    public List<StockShortageInfo> reserve(Integer estimateId, Integer staffId) {
        // Lấy tất cả estimate items đã được kiểm tra (isChecked=true, isRemoved=false)
        List<EstimateItem> estimateItems = getCheckedEstimateItems(estimateId);
        List<StockShortageInfo> shortages = new ArrayList<>();

        for (EstimateItem estimateItem : estimateItems) {
            int requiredQty = estimateItem.getQuantity();

            // Kiểm tra allocation đã tồn tại
            StockAllocation existingAlloc = allocationRepo.findByEstimateItemId(estimateItem.getId())
                    .stream().findFirst().orElse(null);

            // Nếu allocation đã COMMITTED → skip (không sửa allocation đã commit)
            if (existingAlloc != null && existingAlloc.getStatus() == AllocationStatus.COMMITTED) {
                continue;
            }

            // Lấy tồn kho hiện tại (có lock để tránh race condition)
            Inventory inv = inventoryRepo.findByWarehouseAndItemWithLock(
                    estimateItem.getWarehouseId(), estimateItem.getItemId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                            "Không tìm thấy tồn kho cho itemId=" + estimateItem.getItemId()));

            // Tính số lượng khả dụng (tồn kho - giữ chỗ)
            int availableQty = Math.max(0, inv.getQuantity() - inv.getReservedQuantity());

            // Tính delta: phần chênh lệch giữa yêu cầu và allocation cũ
            int delta = existingAlloc != null ? requiredQty - existingAlloc.getQuantity() : requiredQty;

            // Nếu cần thêm hàng nhưng không đủ → báo thiếu
            if (delta > 0 && availableQty < delta) {
                shortages.add(new StockShortageInfo(
                        estimateItem.getWarehouseId(),
                        estimateItem.getItemId(),
                        delta,
                        availableQty
                ));
                continue;
            }

            // Cập nhật allocation cũ hoặc tạo mới
            if (existingAlloc != null) {
                if (delta == 0) continue;  // Không thay đổi → skip

                // Cập nhật quantity
                existingAlloc.setQuantity(requiredQty);
                allocationRepo.save(existingAlloc);

                // Cập nhật reserved quantity trong inventory
                inv.setReservedQuantity(inv.getReservedQuantity() + delta);
                inventoryRepo.save(inv);

                // Ghi log transaction
                logTransaction(estimateItem.getWarehouseId(), estimateItem.getItemId(),
                        delta, inv.getQuantity(), "stock_allocation", 
                        existingAlloc.getAllocationId(), staffId);
            } else {
                // Tạo allocation mới
                StockAllocation newAlloc = new StockAllocation();
                newAlloc.setServiceTicketId(getServiceTicketIdFromEstimate(estimateId));
                newAlloc.setEstimateItemId(estimateItem.getId());
                newAlloc.setEstimateId(estimateId);
                newAlloc.setWarehouseId(estimateItem.getWarehouseId());
                newAlloc.setItemId(estimateItem.getItemId());
                newAlloc.setQuantity(requiredQty);
                newAlloc.setStatus(AllocationStatus.RESERVED);
                newAlloc.setCreatedBy(staffId);

                StockAllocation saved = allocationRepo.save(newAlloc);

                // Cập nhật reserved quantity
                inv.setReservedQuantity(inv.getReservedQuantity() + requiredQty);
                inventoryRepo.save(inv);

                // Ghi log transaction
                logTransaction(estimateItem.getWarehouseId(), estimateItem.getItemId(),
                        requiredQty, inv.getQuantity(), "stock_allocation",
                        saved.getAllocationId(), staffId);
            }
        }

        return shortages;
    }

    /**
     * Tạo draft phiếu xuất kho từ các allocation RESERVED.
     * Luồng: allocation (RESERVED) → issue (DRAFT).
     * Dùng khi service ticket đã giữ chỗ hàng, bây giờ cần xuất hàng lên kho.
     *
     * Quy trình:
     * 1. Lấy tất cả allocation RESERVED của service ticket (chưa gắn issueId)
     * 2. Nhóm allocation theo warehouse (1 phiếu xuất/warehouse)
     * 3. Gọi StockIssueService.create() để tạo draft items từ allocations
     * 4. Gắn allocation.issueId = phiếu xuất vừa tạo
     * 5. Cập nhật trạng thái ServiceTicket: ESTIMATED → PENDING (đang chờ xuất kho)
     *
     * @param serviceTicketId - ID service ticket cần xuất hàng
     * @param staffId - ID nhân viên thực hiện
     * @return List phiếu xuất đã tạo (có thể nhiều phiếu nếu nhiều warehouse)
     */
    @Transactional
    public List<StockIssueResponse> requestIssueDraft(Integer serviceTicketId, Integer staffId) {
        // Lấy tất cả allocation RESERVED cho service ticket này
        List<StockAllocation> reserved = allocationRepo
                .findByTicketAndStatus(serviceTicketId, AllocationStatus.RESERVED);

        if (reserved.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Không có stock allocation ở trạng thái RESERVED cho service ticket này");
        }

        // Nhóm allocation theo kho (warehouse)
        // true = chỉ lấy allocation chưa gắn issueId (tránh tạo draft trùng)
        Map<Integer, List<CreateStockIssueRequest.IssueItemRequest>> itemsByWarehouse =
                buildIssueItemsByWarehouseFromAllocations(reserved, true);

        // Tạo draft phiếu (mỗi kho 1 phiếu)
        List<StockIssueResponse> createdIssues = createIssueDrafts(
                itemsByWarehouse,
                serviceTicketId,
                "Yêu cầu xuất kho từ Service Ticket #" + serviceTicketId,
                staffId
        );

        if (createdIssues.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Không có allocation RESERVED mới để tạo phiếu xuất kho");
        }

        // Cập nhật trạng thái ticket từ ESTIMATED → PENDING (đang chờ xuất kho)
        ServiceTicket ticket = serviceTicketRepo.findByServiceTicketId(serviceTicketId);
        if (ticket != null && ticket.getTicketStatus() == TicketStatus.ESTIMATED) {
            ticket.setTicketStatus(TicketStatus.PENDING);
            serviceTicketRepo.save(ticket);
        }

        return createdIssues;
    }

    /**
     * Xác nhận (commit) allocation RESERVED sau khi phiếu xuất được xác nhận.
     * Luồng: issue (CONFIRMED) → allocation (COMMITTED).
     * Chuyển trạng thái từ RESERVED → COMMITTED và giảm reserved_quantity.
     *
     * Quy trình:
     * 1. Lấy tất cả allocation RESERVED của service ticket
     * 2. Với mỗi allocation:
     *    - Giảm inventory.reservedQuantity (vì đã xuất ra ngoài, không còn giữ chỗ)
     *    - Ghi audit log transaction
     *    - Chuyển allocation RESERVED → COMMITTED (xác nhận đã xuất)
     *
     * Ghi chú:
     * - Phải được gọi NGAY SAU khi StockIssueService.confirm() thành công
     * - reserved_quantity giảm vì hàng đã xuất (không còn "chỗ dự trữ")
     *
     * @param serviceTicketId - ID service ticket
     * @param staffId - ID nhân viên xác nhận
     */
    @Transactional
    public void commitReservedAfterIssueConfirmed(Integer serviceTicketId, Integer staffId) {
        List<StockAllocation> reserved = allocationRepo
                .findByTicketAndStatus(serviceTicketId, AllocationStatus.RESERVED);

        for (StockAllocation alloc : reserved) {
            Inventory inv = inventoryRepo
                    .findByWarehouseAndItemWithLock(alloc.getWarehouseId(), alloc.getItemId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                            "Không tìm thấy tồn kho cho itemId=" + alloc.getItemId()));

            // Kiểm tra reserved quantity hợp lệ
            if (inv.getReservedQuantity() < alloc.getQuantity()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Reserved quantity không hợp lệ cho itemId=" + alloc.getItemId());
            }

            // Giảm reserved quantity (vì đã xuất ra ngoài)
            inv.setReservedQuantity(inv.getReservedQuantity() - alloc.getQuantity());
            inventoryRepo.save(inv);

            logTransaction(alloc.getWarehouseId(), alloc.getItemId(),
                    -alloc.getQuantity(), inv.getQuantity(), "stock_allocation_commit",
                    alloc.getAllocationId(), staffId);

            // Chuyển trạng thái RESERVED → COMMITTED
            alloc.setStatus(AllocationStatus.COMMITTED);
            allocationRepo.save(alloc);
        }
    }

    /**
     * Tạo draft phiếu xuất kho khi service ticket được thanh toán.
     * Có 2 luồng:
     * 1. Nếu đã có allocation RESERVED → dùng luôn (requestIssueDraft)
     * 2. Nếu chưa có → tạo từ estimate items đã check (fallback)
     *
     * @param serviceTicketId - ID service ticket thanh toán
     * @param estimateId - ID báo giá
     * @param staffId - ID nhân viên xác nhận thanh toán
     */
    @Transactional
    public void commitOnPaid(Integer serviceTicketId, Integer estimateId, Integer staffId) {
        // Kiểm tra xem đã có phiếu xuất CONFIRMED chưa (nếu có thì bỏ qua)
        if (stockIssueRepo.existsConfirmedServiceTicketIssue(serviceTicketId)) {
            return;
        }

        // Lấy allocation RESERVED
        List<StockAllocation> reserved = allocationRepo
                .findByTicketAndStatus(serviceTicketId, AllocationStatus.RESERVED);

        // Luồng chính: nếu đã có RESERVED → dùng luôn
        if (!reserved.isEmpty()) {
            requestIssueDraft(serviceTicketId, staffId);
            return;
        }

        // Luồng fallback: lấy từ estimate items
        List<EstimateItem> estimateItems = getCheckedEstimateItems(estimateId);
        if (estimateItems.isEmpty()) {
            return;
        }

        // Nhóm theo warehouse
        Map<Integer, List<CreateStockIssueRequest.IssueItemRequest>> itemsByWarehouse =
                buildIssueItemsByWarehouseFromEstimateItems(estimateItems);

        if (itemsByWarehouse.isEmpty()) {
            return;
        }

        // Tạo draft từ estimate items
        createIssueDrafts(
                itemsByWarehouse,
                serviceTicketId,
                "Fallback: yêu cầu xuất kho theo Estimate #" + estimateId,
                staffId
        );
    }

    /**
     * Hủy giữ chỗ (release) cho tất cả allocation RESERVED của 1 service ticket.
     * Dùng khi service ticket bị hủy trước khi xuất kho.
     *
     * @param serviceTicketId - ID service ticket hủy
     * @param staffId - ID nhân viên hủy
     */
    @Transactional
    public void release(Integer serviceTicketId, Integer staffId) {
        List<StockAllocation> reserved = allocationRepo
                .findByTicketAndStatus(serviceTicketId, AllocationStatus.RESERVED);

        for (StockAllocation alloc : reserved) {
            Inventory inv = inventoryRepo
                    .findByWarehouseAndItemWithLock(alloc.getWarehouseId(), alloc.getItemId())
                    .orElse(null);

            if (inv != null) {
                // Giảm reserved quantity
                inv.setReservedQuantity(Math.max(0, inv.getReservedQuantity() - alloc.getQuantity()));
                inventoryRepo.save(inv);

                logTransaction(alloc.getWarehouseId(), alloc.getItemId(),
                        -alloc.getQuantity(), inv.getQuantity(), "stock_allocation_release",
                        alloc.getAllocationId(), staffId);
            }

            // Chuyển trạng thái RESERVED → RELEASED (hủy)
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
        // Tìm tất cả allocation (không filter status) để check COMMITTED
        List<StockAllocation> allocations = findAllocationsForEstimateItemAnyStatus(estimateItemId);

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
            StockAllocation saved = allocationRepo.save(allocation);
            estimateInternalApi.releaseEstimate(saved.getAllocationId(), saved.getQuantity(), staffId);
        }
    }

    /**
     * Tìm issue item của estimateItemId trong phiếu issueId, sau đó xóa item đó khỏi phiếu.
     * Dùng khi FE truyền cả estimateItemId + issueId (cancel 1 item cụ thể trong phiếu).
     */
    private void cancelReservedByEstimateItemInIssue(Integer estimateItemId, Integer issueId, Integer staffId) {
        // Tìm allocation của estimateItemId — nếu không có thì trace ngược qua revisedFromItemId
        List<StockAllocation> allocations = findAllocationsForEstimateItem(estimateItemId, issueId);

        if (allocations.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Không tìm thấy allocation RESERVED cho estimateItemId=" + estimateItemId);
        }

        Integer itemId = allocations.get(0).getItemId();
        Integer warehouseId = allocations.get(0).getWarehouseId();
        int cancelledQty = allocations.stream().mapToInt(StockAllocation::getQuantity).sum();

        // Release allocation của estimateItemId này
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
            StockAllocation saved = allocationRepo.save(allocation);
            estimateInternalApi.releaseEstimate(saved.getAllocationId(), saved.getQuantity(), staffId);
        }

        // Tính tổng RESERVED còn lại cho itemId trong kho sau khi cancel
        int remainingReservedQty = allocationRepo
                .findByTicketAndWarehouseAndStatus(allocations.get(0).getServiceTicketId(), warehouseId, AllocationStatus.RESERVED)
                .stream()
                .filter(a -> itemId.equals(a.getItemId()))
                .mapToInt(StockAllocation::getQuantity)
                .sum();

        // Xóa tất cả issue items của itemId này trong phiếu
        List<StockIssueItem> existingIssueItems = stockIssueItemRepo.findByIssueId(issueId)
                .stream()
                .filter(i -> itemId.equals(i.getItemId()))
                .toList();
        for (StockIssueItem issueItem : existingIssueItems) {
            stockIssueItemRepo.deleteById(issueItem.getIssueItemId());
        }

        if (remainingReservedQty > 0) {
            // Tính lại FIFO với quantity còn lại
            BigDecimal estimateUnitPrice = stockIssueService.resolveEstimateUnitPricePublic(
                    allocations.get(0).getServiceTicketId(), warehouseId, itemId);
            BigDecimal discountRate = stockIssueService.resolveDiscountRatePublic(
                    itemId, IssueType.SERVICE_TICKET, remainingReservedQty);
            BigDecimal marketSellingPrice = stockIssueService.resolveMarketSellingPricePublic(warehouseId, itemId);

            List<StockEntryItem> lots = stockEntryRepo.findFifoLots(warehouseId, itemId);
            int remaining = remainingReservedQty;
            List<StockIssueItem> newLotItems = new ArrayList<>();

            // Lấy lô mới nhất để tính giá bán fallback
            StockEntryItem latestLot = stockEntryRepo.findLatestLot(warehouseId, itemId).orElse(null);

            for (StockEntryItem lot : lots) {
                if (remaining <= 0) break;
                int consume = Math.min(remaining, lot.getRemainingQuantity());
                if (consume <= 0) continue;
                BigDecimal sellingPrice;
                if (marketSellingPrice != null) {
                    sellingPrice = marketSellingPrice;
                } else if (latestLot != null) {
                    sellingPrice = latestLot.getImportPrice().multiply(latestLot.getMarkupMultiplier())
                            .setScale(2, java.math.RoundingMode.HALF_UP);
                } else {
                    sellingPrice = lot.getImportPrice().multiply(lot.getMarkupMultiplier())
                            .setScale(2, java.math.RoundingMode.HALF_UP);
                }
                BigDecimal finalPriceBase = stockIssueService.resolveFinalPriceBasePublic(
                        IssueType.SERVICE_TICKET, estimateUnitPrice, sellingPrice);
                BigDecimal finalPrice = finalPriceBase.multiply(BigDecimal.ONE.subtract(
                        discountRate.divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP)))
                        .setScale(2, java.math.RoundingMode.HALF_UP);
                newLotItems.add(StockIssueItem.builder()
                        .issueId(issueId).itemId(itemId).entryItemId(lot.getEntryItemId())
                        .quantity(consume).exportPrice(sellingPrice).estimateUnitPrice(estimateUnitPrice)
                        .importPrice(lot.getImportPrice()).discountRate(discountRate).finalPrice(finalPrice).build());
                remaining -= consume;
            }
            if (!newLotItems.isEmpty()) {
                stockIssueItemRepo.saveAll(newLotItems);
            }
        }

        // Nếu phiếu hết item → cancel phiếu
        List<StockIssueItem> remainingItems = stockIssueItemRepo.findByIssueId(issueId);
        if (remainingItems.isEmpty()) {
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

            StockAllocation saved = allocationRepo.save(allocation);
            estimateInternalApi.releaseEstimate(saved.getAllocationId(), saved.getQuantity(), staffId);
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
            StockAllocation saved = allocationRepo.save(allocation);
            estimateInternalApi.releaseEstimate(saved.getAllocationId(), saved.getQuantity(), staffId);
        }
    }

    /**
     * Cập nhật số lượng của 1 allocation.
     * Kiểm tra xem có đủ tồn kho khả dụng khi tăng quantity.
     *
     * @param allocationId - ID allocation cần update
     * @param newQuantity - Số lượng mới
     * @param staffId - ID nhân viên cập nhật
     * @return Kết quả allocation sau khi cập nhật
     */
    @Transactional
    public StockAllocationResult updateAllocation(Integer allocationId, int newQuantity, Integer staffId) {
        StockAllocation alloc = allocationRepo.findById(allocationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy allocation id=" + allocationId));

        // Không được sửa allocation đã COMMITTED
        if (alloc.getStatus() == AllocationStatus.COMMITTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Không thể cập nhật allocation đã COMMITTED");
        }

        Inventory inv = inventoryRepo
                .findByWarehouseAndItemWithLock(alloc.getWarehouseId(), alloc.getItemId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Không tìm thấy tồn kho"));

        // Tính delta (chênh lệch)
        int delta = newQuantity - alloc.getQuantity();

        // Nếu tăng → kiểm tra đủ tồn kho khả dụng không
        if (delta > 0) {
            int availableQty = Math.max(0, inv.getQuantity() - inv.getReservedQuantity());
            if (availableQty < delta) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Không đủ tồn kho khả dụng để tăng allocation");
            }
        }

        // Cập nhật reserved quantity
        inv.setReservedQuantity(inv.getReservedQuantity() + delta);
        inventoryRepo.save(inv);

        // Cập nhật allocation
        alloc.setQuantity(newQuantity);
        allocationRepo.save(alloc);

        return toResult(alloc);
    }

    /**
     * Lấy estimate items đã được kiểm tra (check) từ 1 báo giá.
     * Filter: isChecked=true, isRemoved=false, có warehouseId, itemId, quantity > 0
     */
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

    /**
     * Tăng/giảm reserved_quantity và lưu vào database.
     * @param inventory - Tồn kho hiện tại
     * @param delta - Số lượng thay đổi (dương=tăng, âm=giảm)
     */
    private void adjustReservedQuantity(Inventory inventory, int delta) {
        inventory.setReservedQuantity(inventory.getReservedQuantity() + delta);
        inventoryRepo.save(inventory);
    }

    /**
     * Lấy service ticket ID từ báo giá.
     */
    private Integer getServiceTicketIdFromEstimate(Integer estimateId) {
        Estimate estimate = estimateRepository.findEstimateById(estimateId);
        return estimate != null ? estimate.getServiceTicketId() : null;
    }

    /**
     * Ghi log transaction vào database (bản ghi audit).
     * Tự động set type=ADJUSTMENT, timestamp=now(), v.v.
     */
    private void logTransaction(
            Integer warehouseId,
            Integer itemId,
            int qty,
            int balanceAfter,
            String refType,
            Integer refId,
            Integer staffId
    ) {
        InventoryTransaction tx = new InventoryTransaction();
        tx.setWarehouseId(warehouseId);
        tx.setItemId(itemId);
        tx.setTransactionType(InventoryTransactionType.ADJUSTMENT);
        tx.setQuantity(qty);
        tx.setBalanceAfter(balanceAfter);
        tx.setReferenceType(refType);
        tx.setReferenceId(refId);
        tx.setCreatedById(staffId);
        tx.setCreatedAt(Instant.now());
        transactionRepo.save(tx);
    }

    private Map<Integer, List<CreateStockIssueRequest.IssueItemRequest>> buildIssueItemsByWarehouseFromAllocations(
            List<StockAllocation> allocations,
            boolean skipAssignedIssue
    ) {
        /*
         * Build map warehouseId -> list of IssueItemRequest from allocations.
         * Behavior notes / edge cases:
         * - skipAssignedIssue=true: we skip allocations that already have an issueId
         *   AND that issue is still DRAFT. This avoids creating duplicate draft items
         *   for the same allocation when multiple requests run concurrently or when
         *   frontend retries. If the allocation points to an issue that is no
         *   longer DRAFT (e.g. CANCELLED/CONFIRMED), we allow reusing it.
         * - We only copy simple fields (itemId, quantity). Any further pricing /
         *   FIFO logic is handled later when creating the StockIssue draft.
         */
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
            List<CreateStockIssueRequest.IssueItemRequest> newItems = entry.getValue();

                // Nếu đã có phiếu DRAFT cho kho này → merge item mới vào phiếu đó
                /*
                 * Merge strategy notes:
                 * - We compute `totalReservedByItem` from ALL RESERVED allocations for
                 *   this ticket+warehouse. This represents the authoritative quantities
                 *   we should reflect in the draft.
                 * - We compare to the current quantities in the existing draft and
                 *   update only items where totals diverge. This avoids touching
                 *   unrelated rows and preserves any manual edits for other items.
                 * - After recomputing FIFO lot allocation for changed items we attach
                 *   any unassigned allocations to the existing draft (setIssueId).
                 */
            java.util.Optional<com.g42.platform.gms.warehouse.domain.entity.StockIssue> existingDraft =
                    stockIssueRepo.findDraftServiceTicketIssueInWarehouse(serviceTicketId, warehouseId);

            if (existingDraft.isPresent()) {
                Integer existingIssueId = existingDraft.get().getIssueId();

                // Lấy tổng quantity từ TẤT CẢ allocation RESERVED của ticket trong kho này
                Map<Integer, Integer> totalReservedByItem = allocationRepo
                        .findByTicketAndWarehouseAndStatus(serviceTicketId, warehouseId, AllocationStatus.RESERVED)
                        .stream()
                        .collect(java.util.stream.Collectors.groupingBy(
                                StockAllocation::getItemId,
                                java.util.stream.Collectors.summingInt(StockAllocation::getQuantity)));

                // Lấy quantity hiện tại trong phiếu
                Map<Integer, Integer> existingQtyByItem = stockIssueItemRepo.findByIssueId(existingIssueId)
                        .stream()
                        .collect(java.util.stream.Collectors.groupingBy(
                                com.g42.platform.gms.warehouse.domain.entity.StockIssueItem::getItemId,
                                java.util.stream.Collectors.summingInt(
                                        com.g42.platform.gms.warehouse.domain.entity.StockIssueItem::getQuantity)));

                // Tìm các itemId cần cập nhật: tổng reserved khác với phiếu hiện có
                java.util.Set<Integer> itemsToUpdate = new java.util.HashSet<>();
                // Items từ allocation mới chưa có trong phiếu
                for (CreateStockIssueRequest.IssueItemRequest req : newItems) {
                    itemsToUpdate.add(req.getItemId());
                }
                // Items đã có trong phiếu nhưng tổng reserved thay đổi
                for (Map.Entry<Integer, Integer> e : totalReservedByItem.entrySet()) {
                    if (!e.getValue().equals(existingQtyByItem.getOrDefault(e.getKey(), 0))) {
                        itemsToUpdate.add(e.getKey());
                    }
                }

                if (!itemsToUpdate.isEmpty()) {
                    for (Integer itemId : itemsToUpdate) {
                        int totalNeeded = totalReservedByItem.getOrDefault(itemId, 0);
                        if (totalNeeded <= 0) continue;

                        // Xóa dòng cũ của itemId này
                        stockIssueItemRepo.findByIssueId(existingIssueId).stream()
                                .filter(i -> itemId.equals(i.getItemId()))
                                .forEach(i -> stockIssueItemRepo.deleteById(i.getIssueItemId()));

                        // Tính FIFO với tổng quantity mới
                        BigDecimal estimateUnitPrice = stockIssueService.resolveEstimateUnitPricePublic(
                                serviceTicketId, warehouseId, itemId);
                        BigDecimal discountRate = stockIssueService.resolveDiscountRatePublic(
                                itemId, IssueType.SERVICE_TICKET, totalNeeded);
                        BigDecimal marketSellingPrice = stockIssueService.resolveMarketSellingPricePublic(
                                warehouseId, itemId);

                        List<StockEntryItem> lots = stockEntryRepo.findFifoLots(warehouseId, itemId);
                        int remaining = totalNeeded;
                        List<StockIssueItem> lotItems = new ArrayList<>();

                        // Lấy lô mới nhất để tính giá bán fallback
                        StockEntryItem latestLot = stockEntryRepo.findLatestLot(warehouseId, itemId).orElse(null);

                        for (StockEntryItem lot : lots) {
                            if (remaining <= 0) break;
                            int consume = Math.min(remaining, lot.getRemainingQuantity());
                            if (consume <= 0) continue;
                            BigDecimal sellingPrice;
                            if (marketSellingPrice != null) {
                                sellingPrice = marketSellingPrice;
                            } else if (latestLot != null) {
                                sellingPrice = latestLot.getImportPrice().multiply(latestLot.getMarkupMultiplier())
                                        .setScale(2, java.math.RoundingMode.HALF_UP);
                            } else {
                                sellingPrice = lot.getImportPrice().multiply(lot.getMarkupMultiplier())
                                        .setScale(2, java.math.RoundingMode.HALF_UP);
                            }
                            BigDecimal finalPriceBase = stockIssueService.resolveFinalPriceBasePublic(
                                    IssueType.SERVICE_TICKET, estimateUnitPrice, sellingPrice);
                            BigDecimal finalPrice = finalPriceBase.multiply(BigDecimal.ONE.subtract(
                                    discountRate.divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP)))
                                    .setScale(2, java.math.RoundingMode.HALF_UP);
                            lotItems.add(StockIssueItem.builder()
                                    .issueId(existingIssueId).itemId(itemId)
                                    .entryItemId(lot.getEntryItemId()).quantity(consume)
                                    .exportPrice(sellingPrice).estimateUnitPrice(estimateUnitPrice)
                                    .importPrice(lot.getImportPrice()).discountRate(discountRate)
                                    .finalPrice(finalPrice).build());
                            remaining -= consume;
                        }
                        if (remaining > 0) {
                            lotItems.add(StockIssueItem.builder()
                                    .issueId(existingIssueId).itemId(itemId).entryItemId(0)
                                    .quantity(remaining).exportPrice(BigDecimal.ZERO)
                                    .estimateUnitPrice(estimateUnitPrice).importPrice(BigDecimal.ZERO)
                                    .discountRate(discountRate).finalPrice(BigDecimal.ZERO).build());
                        }
                        stockIssueItemRepo.saveAll(lotItems);
                    }

                    // Gắn allocation mới vào phiếu DRAFT hiện có
                    List<StockAllocation> unassigned = allocationRepo
                            .findByTicketAndWarehouseAndStatus(serviceTicketId, warehouseId, AllocationStatus.RESERVED)
                            .stream()
                            .filter(a -> a.getIssueId() == null)
                            .toList();
                    for (StockAllocation alloc : unassigned) {
                        alloc.setIssueId(existingIssueId);
                        allocationRepo.save(alloc);
                    }
                }
                createdIssues.add(stockIssueService.toResponsePublic(existingIssueId));
                continue;
            }

            CreateStockIssueRequest issueRequest = new CreateStockIssueRequest();
            issueRequest.setWarehouseId(warehouseId);
            issueRequest.setIssueType(IssueType.SERVICE_TICKET);
            issueRequest.setIssueReason(issueReason);
            issueRequest.setServiceTicketId(serviceTicketId);
            issueRequest.setItems(newItems);
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
        logTransaction(warehouseId, itemId, qty, balanceAfter, refType, refId, staffId);
    }

    /**
     * Tìm allocation RESERVED cho estimateItemId.
     * Nếu không tìm thấy trực tiếp, trace ngược qua revisedFromItemId (khi tạo báo giá mới,
     * item mới có revisedFromItemId trỏ về item cũ — allocation vẫn gắn với item cũ).
     */
    private List<StockAllocation> findAllocationsForEstimateItem(Integer estimateItemId, Integer issueId) {
        return findAllocationsForEstimateItemInternal(estimateItemId, issueId, true);
    }

    private List<StockAllocation> findAllocationsForEstimateItemAnyStatus(Integer estimateItemId) {
        return findAllocationsForEstimateItemInternal(estimateItemId, null, false);
    }

    private List<StockAllocation> findAllocationsForEstimateItemInternal(
            Integer estimateItemId, Integer issueId, boolean onlyReserved) {
        // Thử tìm trực tiếp theo estimateItemId
        List<StockAllocation> allocations = allocationRepo.findByEstimateItemId(estimateItemId)
                .stream()
                .filter(a -> !onlyReserved || a.getStatus() == AllocationStatus.RESERVED)
                .filter(a -> issueId == null || issueId.equals(a.getIssueId()))
                .toList();

        if (!allocations.isEmpty()) {
            return allocations;
        }

        // Trace ngược qua revisedFromItemId
        /*
         * In some workflows an EstimateItem may be 'revised' and links to an older
         * item via `revisedFromItemId`. Historically allocations may be attached
         * to the old item id. We therefore walk backwards through the chain of
         * revisedFromItemId (bounded by maxDepth) to find allocations created
         * against previous item versions.
         */
        Integer currentItemId = estimateItemId;
        int maxDepth = 10;
        while (maxDepth-- > 0) {
            EstimateItem item = estimateItemRepository.findByEstimateItemId(currentItemId);
            if (item == null || item.getRevisedFromItemId() == null) {
                break;
            }
            currentItemId = item.getRevisedFromItemId();
            allocations = allocationRepo.findByEstimateItemId(currentItemId)
                    .stream()
                    .filter(a -> !onlyReserved || a.getStatus() == AllocationStatus.RESERVED)
                    .filter(a -> issueId == null || issueId.equals(a.getIssueId()))
                    .toList();
            if (!allocations.isEmpty()) {
                return allocations;
            }
        }

        return List.of();
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
