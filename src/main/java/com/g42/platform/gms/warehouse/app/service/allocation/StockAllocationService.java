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

    /**
     * RESERVE: Giữ chỗ hàng từ Estimate
     * 
     * REPOSITORY LAYER EXPLANATION:
     * ──────────────────────────────────────────────────────────────────────
     * 1. estimateItemRepository.findByEstimateId(estimateId)
     *    - Lấy từ ESTIMATE_ITEM table: tất cả dòng item của estimate này
     *    - Dữ liệu trả về: List<EstimateItem>
     *    - Tại sao: để biết cần reserve những item nào, số lượng bao nhiêu
     * 
     * 2. allocationRepo.findByEstimateItemId(estimateItemId)
     *    - Lấy từ STOCK_ALLOCATION table: allocation cũ của estimate item này
     *    - Dữ liệu trả về: List<StockAllocation> (thường chỉ 1 record)
     *    - Tại sao: check xem đã reserve item này chưa
     * 
     * 3. inventoryRepo.findByWarehouseAndItemWithLock(warehouseId, itemId)
     *    - Lấy từ INVENTORY table: tồn kho của hàng tại kho
     *    - QUAN TRỌNG: dùng WITH LOCK (SELECT FOR UPDATE)
     *    - Tại sao lock? Vì 2 request cùng lúc có thể tăng reserved_quantity
     *      nếu không lock, tồn kho sẽ tính sai!
     *    - Dữ liệu trả về: 
     *      * quantity: tổng hàng hiện có
     *      * reserved_quantity: hàng đang bị giữ chỗ
     *      * available = quantity - reserved_quantity
     * 
     * 4. allocationRepo.save(allocation)
     *    - Lưu vào STOCK_ALLOCATION table: record allocation mới
     *    - Với fields: estimateItemId, warehouseId, itemId, quantity, status=RESERVED
     *    - Trả về: allocation có ID tự động sinh
     * 
     * 5. transactionRepo.save(tx)
     *    - Lưu vào INVENTORY_TRANSACTION table: để audit trail
     *    - Dùng để track ai reserve, khi nào, số lượng bao nhiêu
     * ──────────────────────────────────────────────────────────────────────
     * 
     * LOGIC:
     * - Lặp qua từng item trong Estimate (chỉ lấy những item hợp lệ: có warehouse, item id, quantity > 0)
     * - Với mỗi item, kiểm tra:
     *   1. Nếu allocation đã tồn tại + COMMITTED -> skip (không thể thay đổi)
     *   2. Nếu chưa có allocation hoặc allocation RESERVED -> update/create
     *   3. Kiểm tra hàng có đủ không, nếu thiếu thêm vào shortages
     * - Lưu StockAllocation với status RESERVED
     * - Tăng reserved_quantity trong Inventory (để biết là hàng này đang được giữ)
     * - Return danh sách các item thiếu hàng
     */
    @Transactional
    public List<StockShortageInfo> reserve(Integer estimateId, Integer staffId) {
        // ═══════════════════════════════════════════════════════════════════════════
        // BƯỚC 1: Lấy tất cả items từ Estimate
        // ═══════════════════════════════════════════════════════════════════════════
        // Repo call: estimateItemRepository.findByEstimateId(estimateId)
        // - Truy vấn ESTIMATE_ITEM table: lấy tất cả items của estimate này
        // - Filter: chỉ lấy items checked, không removed, có warehouse, có item id, qty > 0
        // - Trả về: List<EstimateItem> (mỗi item là 1 dòng trong estimate)
        List<EstimateItem> estimateItems = getCheckedEstimateItems(estimateId);
        List<StockShortageInfo> shortages = new ArrayList<>();

        // ═══════════════════════════════════════════════════════════════════════════
        // BƯỚC 2: Lặp qua từng item để giữ chỗ
        // ═══════════════════════════════════════════════════════════════════════════
        for (EstimateItem estimateItem : estimateItems) {
            int requiredQuantity = estimateItem.getQuantity(); // Cần bao nhiêu?

            // ───────────────────────────────────────────────────────────────────────
            // Check 1: Kiểm tra có allocation cũ không (từ lần reserve trước)
            // ───────────────────────────────────────────────────────────────────────
            // Repo call: allocationRepo.findByEstimateItemId(estimateItemId)
            // - Truy vấn STOCK_ALLOCATION table: lấy allocations của item này
            // - Dữ liệu trả về: List<StockAllocation> (thường chỉ 0-1 record)
            // - Tại sao: để check xem đã reserve lần nào chưa
            StockAllocation existingAllocation = allocationRepo.findByEstimateItemId(estimateItem.getId())
                    .stream().findFirst().orElse(null);
            
            // Nếu đã COMMITTED (đã confirm phiếu xuất) -> không thể thay đổi, skip
            if (existingAllocation != null && existingAllocation.getStatus() == AllocationStatus.COMMITTED) {
                continue;
            }

            // ───────────────────────────────────────────────────────────────────────
            // Check 2: Lấy inventory với LOCK
            // ───────────────────────────────────────────────────────────────────────
            // Repo call: inventoryRepo.findByWarehouseAndItemWithLock(warehouseId, itemId)
            // - Truy vấn INVENTORY table: lấy tồn kho của hàng tại kho
            // - WITH LOCK: đặt SELECT FOR UPDATE để tránh race condition
            // - Dữ liệu trả về: Inventory { quantity, reserved_quantity }
            // - Tại sao lock? Nếu 2 request cùng lúc update quantity, có thể sai số!
            Inventory inventory = inventoryRepo.findByWarehouseAndItemWithLock(
                    estimateItem.getWarehouseId(), estimateItem.getItemId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.UNPROCESSABLE_ENTITY,
                            "Không tìm thấy hàng itemId=" + estimateItem.getItemId()
                    ));

            // Tính available = tổng hàng - hàng đã giữ chỗ
            int availableQuantity = Math.max(0, inventory.getQuantity() - inventory.getReservedQuantity());
            
            // Tính delta = phần chênh lệch cần tăng/giảm so với allocation trước
            int reserveDelta = existingAllocation != null
                    ? requiredQuantity - existingAllocation.getQuantity()
                    : requiredQuantity;

            // ───────────────────────────────────────────────────────────────────────
            // Check 3: Kiểm tra có đủ hàng không
            // ───────────────────────────────────────────────────────────────────────
            // Nếu cần thêm hàng mà không đủ available -> thêm vào danh sách thiếu
            if (reserveDelta > 0 && availableQuantity < reserveDelta) {
                shortages.add(new StockShortageInfo(
                        estimateItem.getWarehouseId(),
                        estimateItem.getItemId(),
                        reserveDelta,
                        availableQuantity
                ));
                continue; // Bỏ qua item này, không tạo allocation
            }

            // ───────────────────────────────────────────────────────────────────────
            // Trường hợp 1: Đã có allocation RESERVED -> UPDATE
            // ───────────────────────────────────────────────────────────────────────
            if (existingAllocation != null) {
                if (reserveDelta == 0) {
                    continue; // Quantity không thay đổi, skip
                }

                // Update quantity trong allocation
                // Repo call: allocationRepo.save(allocation)
                // - Update STOCK_ALLOCATION table: cập nhật quantity
                // - Dữ liệu: existingAllocation với quantity mới
                existingAllocation.setQuantity(requiredQuantity);
                allocationRepo.save(existingAllocation);

                // Tăng/giảm reserved_quantity trong inventory
                // Repo call: inventoryRepo.save(inventory)
                // - Update INVENTORY table: cập nhật reserved_quantity
                // - Lưu ý: đã có lock từ findByWarehouseAndItemWithLock()
                inventory.setReservedQuantity(inventory.getReservedQuantity() + reserveDelta);
                inventoryRepo.save(inventory);

                // Log giao dịch (để track ai, khi nào, thay đổi gì)
                // Repo call: transactionRepo.save(tx)
                // - Lưu INVENTORY_TRANSACTION table: audit trail
                // - Dữ liệu: warehouse, item, type=ADJUSTMENT, qty=delta, refType=stock_allocation
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
                continue;
            }

            // ───────────────────────────────────────────────────────────────────────
            // Trường hợp 2: Chưa có allocation -> TẠO MỚI
            // ───────────────────────────────────────────────────────────────────────
            Integer serviceTicketId = estimateRepository.findEstimateById(estimateId) != null
                    ? estimateRepository.findEstimateById(estimateId).getServiceTicketId()
                    : null;

            StockAllocation allocation = new StockAllocation();
            allocation.setServiceTicketId(serviceTicketId);
            allocation.setEstimateItemId(estimateItem.getId());
            allocation.setWarehouseId(estimateItem.getWarehouseId());
            allocation.setItemId(estimateItem.getItemId());
            allocation.setQuantity(requiredQuantity);
            allocation.setStatus(AllocationStatus.RESERVED); // Status = RESERVED (chưa confirm)
            allocation.setCreatedBy(staffId);
            
            // Repo call: allocationRepo.save(allocation)
            // - Lưu STOCK_ALLOCATION table: tạo record allocation mới
            // - Database tự sinh ID cho allocation này
            StockAllocation savedAllocation = allocationRepo.save(allocation);

            // Tăng reserved_quantity trong inventory
            // Repo call: inventoryRepo.save(inventory)
            // - Update INVENTORY table: cập nhật reserved_quantity
            inventory.setReservedQuantity(inventory.getReservedQuantity() + requiredQuantity);
            inventoryRepo.save(inventory);

            // Log giao dịch
            // Repo call: transactionRepo.save(tx)
            // - Lưu INVENTORY_TRANSACTION table: audit trail cho lần reserve này
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

    /**
     * REQUEST ISSUE DRAFT: Tạo phiếu xuất kho (draft state) từ allocations RESERVED
     * 
     * REPOSITORY LAYER:
     * ──────────────────────────────────────────────────────────────────────
     * 1. allocationRepo.findByTicketAndStatus(ticketId, RESERVED)
     *    - Lấy từ STOCK_ALLOCATION table: tất cả allocations RESERVED của ticket
     *    - Dữ liệu trả về: List<StockAllocation>
     *    - Tại sao: để tìm những hàng đã được reserve, sẵn sàng để xuất
     * 
     * 2. stockIssueRepo.existsDraftServiceTicketIssueInWarehouse(ticketId, warehouseId)
     *    - Kiểm tra từ STOCK_ISSUE table: đã có draft issue cho warehouse này chưa?
     *    - Dữ liệu trả về: boolean (true/false)
     *    - Tại sao: tránh tạo draft trùng lặp
     * 
     * 3. stockIssueService.create(issueRequest, staffId)
     *    - Gọi service khác để tạo issue draft
     *    - Sẽ lưu STOCK_ISSUE + STOCK_ISSUE_ITEM tables
     * 
     * 4. serviceTicketRepo.save(ticket)
     *    - Update SERVICE_TICKET table: thay đổi status ESTIMATED -> PENDING
     * ──────────────────────────────────────────────────────────────────────
     */
    @Transactional
    public List<StockIssueResponse> requestIssueDraft(Integer serviceTicketId, Integer staffId) {
        // ═══════════════════════════════════════════════════════════════════════════
        // BƯỚC 1: Lấy tất cả RESERVED allocations
        // ═══════════════════════════════════════════════════════════════════════════
        // Repo call: allocationRepo.findByTicketAndStatus(serviceTicketId, RESERVED)
        // - Truy vấn STOCK_ALLOCATION table: tất cả allocations RESERVED của ticket
        // - Dữ liệu trả về: List<StockAllocation>
        // - Tại sao: để tìm những hàng đã được reserve, sẵn sàng để tạo phiếu xuất
        List<StockAllocation> reserved = allocationRepo
                .findByTicketAndStatus(serviceTicketId, AllocationStatus.RESERVED);

        if (reserved.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Không có allocation RESERVED cho service ticket này");
        }

        // Nhóm allocations theo warehouse (một service ticket có thể lấy hàng từ nhiều kho)
        
        // ═══════════════════════════════════════════════════════════════════════════
        // BƯỚC 2: Nhóm allocations theo warehouse
        // ═══════════════════════════════════════════════════════════════════════════
        // Vì sao nhóm? 1 service ticket có thể lấy hàng từ NHIỀU kho khác nhau
        // → cần tạo NHIỀU phiếu xuất (1 phiếu/warehouse)
        // Dữ liệu: Map<WarehouseId, List<IssueItem>>
        Map<Integer, List<CreateStockIssueRequest.IssueItemRequest>> issueItemsByWarehouse = new HashMap<>();
        for (StockAllocation alloc : reserved) {
            if (alloc.getIssueId() != null) {
                // Nếu allocation đã được gắn vào issue rồi -> skip (tránh tạo draft trùng)
                continue;
            }
            
            // Thêm item vào danh sách theo warehouse
            CreateStockIssueRequest.IssueItemRequest item = new CreateStockIssueRequest.IssueItemRequest();
            item.setItemId(alloc.getItemId());
            item.setQuantity(alloc.getQuantity());
            item.setDiscountRate(BigDecimal.ZERO);
            issueItemsByWarehouse.computeIfAbsent(alloc.getWarehouseId(), k -> new ArrayList<>()).add(item);
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // BƯỚC 3: Tạo draft issue cho mỗi warehouse
        // ═══════════════════════════════════════════════════════════════════════════
        List<StockIssueResponse> createdIssues = new ArrayList<>();
        for (Map.Entry<Integer, List<CreateStockIssueRequest.IssueItemRequest>> entry : issueItemsByWarehouse.entrySet()) {
            Integer warehouseId = entry.getKey();
            
            // ───────────────────────────────────────────────────────────────────────
            // Check: Đã có draft issue cho warehouse này chưa?
            // ───────────────────────────────────────────────────────────────────────
            // Repo call: stockIssueRepo.existsDraftServiceTicketIssueInWarehouse(ticketId, warehouseId)
            // - Truy vấn STOCK_ISSUE table: kiểm tra có draft issue nào
            // - Dữ liệu trả về: boolean
            // - Tại sao: tránh tạo draft trùng lặp (idempotent)
            if (stockIssueRepo.existsDraftServiceTicketIssueInWarehouse(serviceTicketId, warehouseId)) {
                continue; // Đã có draft rồi, skip
            }

            // Tạo issue request
            CreateStockIssueRequest issueRequest = new CreateStockIssueRequest();
            issueRequest.setWarehouseId(warehouseId);
            issueRequest.setIssueType(IssueType.SERVICE_TICKET); // Loại phiếu xuất = SERVICE_TICKET
            issueRequest.setIssueReason("Yêu cầu xuất kho từ ServiceTicket #" + serviceTicketId);
            issueRequest.setServiceTicketId(serviceTicketId);
            issueRequest.setItems(entry.getValue());
            
            // ───────────────────────────────────────────────────────────────────────
            // Gọi StockIssueService để tạo draft
            // ───────────────────────────────────────────────────────────────────────
            // Sẽ lưu STOCK_ISSUE + STOCK_ISSUE_ITEM tables
            // (Chi tiết: xem StockIssueService.create())
            createdIssues.add(stockIssueService.create(issueRequest, staffId));
        }

        if (createdIssues.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Không có allocation RESERVED mới để tạo phiếu xuất kho");
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // BƯỚC 4: Update service ticket status
        // ═══════════════════════════════════════════════════════════════════════════
        // Thay đổi: ESTIMATED (chưa reserve) -> PENDING (đã tạo phiếu xuất, chờ confirm)
        // Repo call: serviceTicketRepo.save(ticket)
        // - Update SERVICE_TICKET table: cập nhật ticket_status
        ServiceTicket ticket = serviceTicketRepo.findByServiceTicketId(serviceTicketId);
        if (ticket != null && ticket.getTicketStatus() == TicketStatus.ESTIMATED) {
            ticket.setTicketStatus(TicketStatus.PENDING);
            serviceTicketRepo.save(ticket);
        }

        return createdIssues;
    }

    /**
     * COMMIT RESERVED AFTER ISSUE CONFIRMED: Chuyển status RESERVED -> COMMITTED khi phiếu xuất được confirm
     * 
     * REPOSITORY LAYER:
     * ──────────────────────────────────────────────────────────────────────
     * 1. allocationRepo.findByTicketAndStatus(ticketId, RESERVED)
     *    - Lấy từ STOCK_ALLOCATION table: tất cả allocations RESERVED
     *    - Dữ liệu trả về: List<StockAllocation>
     * 
     * 2. inventoryRepo.findByWarehouseAndItemWithLock(warehouseId, itemId)
     *    - Lấy INVENTORY với LOCK
     *    - Dữ liệu trả về: Inventory { quantity, reserved_quantity }
     * 
     * 3. inventoryRepo.save(inventory)
     *    - Update INVENTORY table: giảm reserved_quantity
     *    - Vì hàng đã commit, không còn "giữ chỗ"
     * 
     * 4. allocationRepo.save(allocation)
     *    - Update STOCK_ALLOCATION table: thay đổi status RESERVED -> COMMITTED
     * 
     * 5. transactionRepo.save(tx)
     *    - Lưu INVENTORY_TRANSACTION table: audit trail
     * ──────────────────────────────────────────────────────────────────────
     */
    @Transactional
    public void commitReservedAfterIssueConfirmed(Integer serviceTicketId, Integer staffId) {
        // ═══════════════════════════════════════════════════════════════════════════
        // BƯỚC 1: Lấy tất cả RESERVED allocations
        // ═══════════════════════════════════════════════════════════════════════════
        // Repo call: allocationRepo.findByTicketAndStatus(ticketId, RESERVED)
        // - Truy vấn STOCK_ALLOCATION table: tất cả allocations RESERVED của ticket
        // - Dữ liệu trả về: List<StockAllocation>
        // - Tại sao: khi issue được confirm, tất cả RESERVED allocations phải commit
        List<StockAllocation> reserved = allocationRepo
                .findByTicketAndStatus(serviceTicketId, AllocationStatus.RESERVED);

        for (StockAllocation alloc : reserved) {
            // ───────────────────────────────────────────────────────────────────────
            // Lấy inventory với LOCK
            // ───────────────────────────────────────────────────────────────────────
            // Repo call: inventoryRepo.findByWarehouseAndItemWithLock(warehouseId, itemId)
            // - Lấy INVENTORY với LOCK (SELECT FOR UPDATE)
            // - Dữ liệu: Inventory { quantity, reserved_quantity }
            // - Tại sao lock? Để tránh race condition khi update reserved_quantity
            Inventory inventory = inventoryRepo
                    .findByWarehouseAndItemWithLock(alloc.getWarehouseId(), alloc.getItemId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                            "Không tìm thấy hàng itemId=" + alloc.getItemId()));

            // Kiểm tra reserved_quantity có hợp lệ không
            if (inventory.getReservedQuantity() < alloc.getQuantity()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Reserved quantity không hợp lệ cho itemId=" + alloc.getItemId());
            }

            // ───────────────────────────────────────────────────────────────────────
            // Giảm reserved_quantity
            // ───────────────────────────────────────────────────────────────────────
            // Vì hàng này đã commit, không còn "giữ chỗ"
            // Repo call: inventoryRepo.save(inventory)
            // - Update INVENTORY table: cập nhật reserved_quantity
            inventory.setReservedQuantity(inventory.getReservedQuantity() - alloc.getQuantity());
            inventoryRepo.save(inventory);

            // Log transaction
            // Repo call: transactionRepo.save(tx)
            // - Lưu INVENTORY_TRANSACTION table: audit trail
            // - refType = "stock_allocation_commit" (để biết đây là action commit)
            logTransaction(
                    alloc.getWarehouseId(),
                    alloc.getItemId(),
                    InventoryTransactionType.ADJUSTMENT,
                    -alloc.getQuantity(),
                    inventory.getQuantity(),
                    "stock_allocation_commit", // Ref type để biết đây là action commit
                    alloc.getAllocationId(),
                    staffId
            );

            // ───────────────────────────────────────────────────────────────────────
            // Update allocation status
            // ───────────────────────────────────────────────────────────────────────
            // Repo call: allocationRepo.save(allocation)
            // - Update STOCK_ALLOCATION table: thay đổi status RESERVED -> COMMITTED
            alloc.setStatus(AllocationStatus.COMMITTED);
            allocationRepo.save(alloc);
        }
    }

    /**
     * COMMIT ON PAID: Tạo issue draft khi thanh toán được confirm
     * - Kiểm tra xem đã có CONFIRMED issue từ trước chưa
     * - Nếu có RESERVED allocations -> tạo issue draft từ đó
     * - Nếu không (fallback) -> tạo issue từ estimate items
     */
    @Transactional
    public void commitOnPaid(Integer serviceTicketId, Integer estimateId, Integer staffId) {
        // Nếu đã có confirmed issue rồi -> không cần làm gì
        if (stockIssueRepo.existsConfirmedServiceTicketIssue(serviceTicketId)) {
            return;
        }

        // Kiểm tra có RESERVED allocations không
        List<StockAllocation> reserved = allocationRepo
                .findByTicketAndStatus(serviceTicketId, AllocationStatus.RESERVED);

        if (!reserved.isEmpty()) {
            // Luồng chính: đã có RESERVED -> tạo draft issue từ allocations
            requestIssueDraft(serviceTicketId, staffId);
            return;
        }

        // Fallback: không có RESERVED -> tạo issue từ estimate items (nếu có)
        List<EstimateItem> estimateItems = getCheckedEstimateItems(estimateId);

        if (estimateItems.isEmpty()) {
            return;
        }

        // Nhóm estimate items theo warehouse
        Map<Integer, List<CreateStockIssueRequest.IssueItemRequest>> issueItemsByWarehouse = new HashMap<>();
        for (EstimateItem estimateItem : estimateItems) {
            CreateStockIssueRequest.IssueItemRequest item = new CreateStockIssueRequest.IssueItemRequest();
            item.setItemId(estimateItem.getItemId());
            item.setQuantity(estimateItem.getQuantity());
            item.setDiscountRate(BigDecimal.ZERO);
            issueItemsByWarehouse.computeIfAbsent(estimateItem.getWarehouseId(), k -> new ArrayList<>()).add(item);
        }

        if (issueItemsByWarehouse.isEmpty()) {
            return;
        }

        // Tạo draft issue cho mỗi warehouse
        for (Map.Entry<Integer, List<CreateStockIssueRequest.IssueItemRequest>> entry : issueItemsByWarehouse.entrySet()) {
            Integer warehouseId = entry.getKey();
            
            if (stockIssueRepo.existsDraftServiceTicketIssueInWarehouse(serviceTicketId, warehouseId)) {
                continue;
            }

            CreateStockIssueRequest issueRequest = new CreateStockIssueRequest();
            issueRequest.setWarehouseId(warehouseId);
            issueRequest.setIssueType(IssueType.SERVICE_TICKET);
            issueRequest.setIssueReason("Fallback yêu cầu xuất kho theo Estimate #" + estimateId);
            issueRequest.setServiceTicketId(serviceTicketId);
            issueRequest.setItems(entry.getValue());
            
            stockIssueService.create(issueRequest, staffId);
        }
    }

    /**
     * RELEASE: Hủy toàn bộ reserved allocations của service ticket
     * 
     * REPOSITORY LAYER:
     * ──────────────────────────────────────────────────────────────────────
     * 1. allocationRepo.findByTicketAndStatus(ticketId, RESERVED)
     *    - Lấy tất cả allocations RESERVED để hủy
     * 
     * 2. inventoryRepo.findByWarehouseAndItemWithLock()
     *    - Lấy inventory với LOCK để update reserved_quantity
     * 
     * 3. inventoryRepo.save(inventory)
     *    - Giảm reserved_quantity (hàng được release)
     * 
     * 4. transactionRepo.save(tx)
     *    - Log audit trail
     * 
     * 5. allocationRepo.save(allocation)
     *    - Cập nhật status RESERVED -> RELEASED
     * ──────────────────────────────────────────────────────────────────────
     */
    @Transactional
    public void release(Integer serviceTicketId, Integer staffId) {
        // ═══════════════════════════════════════════════════════════════════════════
        // BƯỚC 1: Lấy tất cả RESERVED allocations
        // ═══════════════════════════════════════════════════════════════════════════
        // Repo call: allocationRepo.findByTicketAndStatus(ticketId, RESERVED)
        // - Truy vấn STOCK_ALLOCATION table: lấy tất cả allocations RESERVED
        // - Dữ liệu trả về: List<StockAllocation>
        List<StockAllocation> reserved = allocationRepo
                .findByTicketAndStatus(serviceTicketId, AllocationStatus.RESERVED);

        for (StockAllocation alloc : reserved) {
            // ───────────────────────────────────────────────────────────────────────
            // Lấy inventory và giảm reserved_quantity
            // ───────────────────────────────────────────────────────────────────────
            // Repo call: inventoryRepo.findByWarehouseAndItemWithLock()
            // - Lấy INVENTORY với LOCK
            // - Tại sao: để update reserved_quantity an toàn
            Inventory inventory = inventoryRepo
                    .findByWarehouseAndItemWithLock(alloc.getWarehouseId(), alloc.getItemId())
                    .orElse(null);

            if (inventory != null) {
                // Giảm reserved_quantity (hàng được release, không còn giữ chỗ)
                int updatedReserved = Math.max(0, inventory.getReservedQuantity() - alloc.getQuantity());
                inventory.setReservedQuantity(updatedReserved);
                
                // Repo call: inventoryRepo.save(inventory)
                // - Update INVENTORY table: cập nhật reserved_quantity
                inventoryRepo.save(inventory);

                // Log transaction
                // Repo call: transactionRepo.save(tx)
                // - Lưu INVENTORY_TRANSACTION table
                // - refType = "stock_allocation_release" (để biết là release action)
                logTransaction(
                        alloc.getWarehouseId(),
                        alloc.getItemId(),
                        InventoryTransactionType.ADJUSTMENT,
                        -alloc.getQuantity(),
                        inventory.getQuantity(),
                        "stock_allocation_release", // Ref type = release
                        alloc.getAllocationId(),
                        staffId
                );
            }

            // ───────────────────────────────────────────────────────────────────────
            // Update allocation status
            // ───────────────────────────────────────────────────────────────────────
            // Repo call: allocationRepo.save(allocation)
            // - Update STOCK_ALLOCATION table: thay đổi status RESERVED -> RELEASED
            alloc.setStatus(AllocationStatus.RELEASED);
            allocationRepo.save(alloc);
        }
    }

    /**
     * CANCEL STOCK ALLOCATION: Hủy allocation (có thể hủy khi chưa xuất kho)
        *
        * Lưu ý tham số:
        * - estimateItemId: dùng cho scenario hủy theo estimate item
        * - issueId: dùng cho scenario hủy theo issue
        * - issueItemId: optional, chỉ dùng khi muốn hủy 1 item cụ thể trong issue
        *
     * - Có 3 scenario:
     *   1. Hủy allocation của 1 estimate item cụ thể (chưa gắn issue)
     *   2. Hủy allocations của 1 issue cụ thể (chưa được confirm)
     *   3. Hủy allocation của 1 item cụ thể trong issue (chưa được confirm)
     */
    @Transactional
    public void cancelStockAllocation(Integer estimateItemId, Integer issueId, Integer issueItemId, Integer staffId) {
        if (estimateItemId == null && issueId == null && issueItemId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Phải cung cấp estimateItemId, issueId, hoặc issueItemId");
        }

        // Scenario 3: Hủy allocation của item cụ thể trong issue
        if (issueItemId != null) {
            cancelReservedWithIssueItem(issueId, issueItemId, staffId);
            return;
        }

        // Scenario 2: Hủy allocations của toàn issue
        if (issueId != null) {
            cancelReservedWithIssue(issueId, staffId);
            return;
        }

        // Scenario 1: Hủy allocations của estimate item
        cancelReservedWithoutIssue(estimateItemId, staffId);
    }

    /**
     * Backward-compatible overload: giữ signature cũ (không có issueItemId).
     * Không đổi logic: chỉ delegate sang hàm chính với issueItemId = null.
     */
    @Transactional
    public void cancelStockAllocation(Integer estimateItemId, Integer issueId, Integer staffId) {
        cancelStockAllocation(estimateItemId, issueId, null, staffId);
    }

    /**
     * Hủy allocations của 1 estimate item (chưa gắn vào issue)
     */
    private void cancelReservedWithoutIssue(Integer estimateItemId, Integer staffId) {
        // Lấy tất cả allocations của estimate item này
        List<StockAllocation> allocations = allocationRepo.findByEstimateItemId(estimateItemId);

        if (allocations.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Không tìm thấy allocation cho estimateItemId=" + estimateItemId);
        }

        for (StockAllocation allocation : allocations) {
            // Không thể hủy nếu đã gắn issue
            if (allocation.getIssueId() != null) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Allocation đã gắn phiếu xuất, không thể hủy");
            }

            // Không thể hủy nếu đã COMMITTED
            if (allocation.getStatus() == AllocationStatus.COMMITTED) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Allocation đã COMMITTED, không thể hủy");
            }

            // Nếu đã RELEASED rồi -> skip
            if (allocation.getStatus() == AllocationStatus.RELEASED) {
                continue;
            }

            // Giảm reserved_quantity
            Inventory inventory = inventoryRepo
                    .findByWarehouseAndItemWithLock(allocation.getWarehouseId(), allocation.getItemId())
                    .orElse(null);

            if (inventory != null) {
                int updatedReserved = Math.max(0, inventory.getReservedQuantity() - allocation.getQuantity());
                inventory.setReservedQuantity(updatedReserved);
                inventoryRepo.save(inventory);

                // Log transaction
                logTransaction(
                        allocation.getWarehouseId(),
                        allocation.getItemId(),
                        InventoryTransactionType.ADJUSTMENT,
                        -allocation.getQuantity(),
                        inventory.getQuantity(),
                        "stock_allocation_cancel", // Ref type = cancel
                        allocation.getAllocationId(),
                        staffId
                );
            }

            // Chuyển status -> RELEASED
            allocation.setStatus(AllocationStatus.RELEASED);
            allocationRepo.save(allocation);
        }
    }

    /**
     * Hủy allocations RESERVED của 1 issue (chưa confirm)
     */
    private void cancelReservedWithIssue(Integer issueId, Integer staffId) {
        // Lấy tất cả RESERVED allocations của issue này
        List<StockAllocation> allocations = allocationRepo.findByIssueIdAndStatus(issueId, AllocationStatus.RESERVED);

        if (allocations.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Không tìm thấy allocation RESERVED cho issue=" + issueId);
        }

        for (StockAllocation allocation : allocations) {
            // Giảm reserved_quantity (allocation vẫn ở RESERVED)
            Inventory inventory = inventoryRepo
                    .findByWarehouseAndItemWithLock(allocation.getWarehouseId(), allocation.getItemId())
                    .orElse(null);

            if (inventory != null) {
                int updatedReserved = Math.max(0, inventory.getReservedQuantity() - allocation.getQuantity());
                inventory.setReservedQuantity(updatedReserved);
                inventoryRepo.save(inventory);

                // Log transaction
                logTransaction(
                        allocation.getWarehouseId(),
                        allocation.getItemId(),
                        InventoryTransactionType.ADJUSTMENT,
                        -allocation.getQuantity(),
                        inventory.getQuantity(),
                        "stock_allocation_cancel_with_issue", // Ref type = cancel từ issue
                        allocation.getAllocationId(),
                        staffId
                );
            }

            // Chuyển status -> RELEASED
            allocation.setStatus(AllocationStatus.RELEASED);
            allocationRepo.save(allocation);
        }
    }

    /**
     * Hủy allocation của 1 item cụ thể trong issue
     */
    private void cancelReservedWithIssueItem(Integer issueId, Integer issueItemId, Integer staffId) {
        // Lấy tất cả RESERVED allocations của issue này
        List<StockAllocation> allocations = allocationRepo.findByIssueIdAndStatus(issueId, AllocationStatus.RESERVED);

        // Tìm allocation của item cụ thể
        StockAllocation allocation = allocations.stream()
                .filter(a -> a.getItemId().equals(issueItemId))
                .findFirst()
                .orElse(null);

        if (allocation == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Không tìm thấy allocation RESERVED cho issue=" + issueId + " và item=" + issueItemId);
        }

        // Giảm reserved_quantity
        Inventory inventory = inventoryRepo
                .findByWarehouseAndItemWithLock(allocation.getWarehouseId(), allocation.getItemId())
                .orElse(null);

        if (inventory != null) {
            int updatedReserved = Math.max(0, inventory.getReservedQuantity() - allocation.getQuantity());
            inventory.setReservedQuantity(updatedReserved);
            inventoryRepo.save(inventory);

            // Log transaction
            logTransaction(
                    allocation.getWarehouseId(),
                    allocation.getItemId(),
                    InventoryTransactionType.ADJUSTMENT,
                    -allocation.getQuantity(),
                    inventory.getQuantity(),
                    "stock_allocation_cancel_item", // Ref type = cancel item từ issue
                    allocation.getAllocationId(),
                    staffId
            );
        }

        // Chuyển status -> RELEASED
        allocation.setStatus(AllocationStatus.RELEASED);
        allocationRepo.save(allocation);
    }

    /**
     * UPDATE ALLOCATION: Cập nhật số lượng của allocation
     * - Chỉ cập nhật được nếu status không phải COMMITTED
     * - Tính delta (chênh lệch) và kiểm tra có đủ hàng không
     * - Cập nhật reserved_quantity tương ứng
     */
    @Transactional
    public StockAllocationResult updateAllocation(Integer allocationId, int newQuantity, Integer staffId) {
        // Lấy allocation
        StockAllocation allocation = allocationRepo.findById(allocationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy allocation id=" + allocationId));

        // Không thể cập nhật nếu COMMITTED
        if (allocation.getStatus() == AllocationStatus.COMMITTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Không thể cập nhật allocation đã COMMITTED");
        }

        // Lấy inventory
        Inventory inventory = inventoryRepo
                .findByWarehouseAndItemWithLock(allocation.getWarehouseId(), allocation.getItemId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Không tìm thấy hàng"));

        // Tính delta (chênh lệch quantity cần thay đổi)
        int delta = newQuantity - allocation.getQuantity();
        
        // Nếu cần tăng -> kiểm tra có đủ hàng available không
        if (delta > 0) {
            int available = Math.max(0, inventory.getQuantity() - inventory.getReservedQuantity());
            if (available < delta) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Không đủ hàng để tăng allocation");
            }
        }

        // Cập nhật reserved_quantity
        inventory.setReservedQuantity(inventory.getReservedQuantity() + delta);
        inventoryRepo.save(inventory);

        // Cập nhật allocation quantity
        allocation.setQuantity(newQuantity);
        allocationRepo.save(allocation);

        // Convert allocation -> result DTO
        StockAllocationResult r = new StockAllocationResult();
        r.setAllocationId(allocation.getAllocationId());
        r.setServiceTicketId(allocation.getServiceTicketId());
        r.setIssueId(allocation.getIssueId());
        r.setEstimateItemId(allocation.getEstimateItemId());
        r.setWarehouseId(allocation.getWarehouseId());
        r.setItemId(allocation.getItemId());
        r.setQuantity(allocation.getQuantity());
        r.setStatus(allocation.getStatus());
        return r;
    }

    // ============== HELPER METHODS ==============

    /**
     * Lấy các estimate items hợp lệ (đã checked, không removed, có đủ thông tin)
     */
    private List<EstimateItem> getCheckedEstimateItems(Integer estimateId) {
        return estimateItemRepository.findByEstimateId(estimateId)
                .stream()
                .filter(i -> i.getWarehouseId() != null)        // Có warehouse
                .filter(i -> i.getItemId() != null)             // Có item
                .filter(i -> i.getQuantity() != null && i.getQuantity() > 0) // Có quantity > 0
                .filter(i -> !Boolean.TRUE.equals(i.getIsRemoved())) // Không removed
                .filter(i -> Boolean.TRUE.equals(i.getIsChecked())) // Đã checked
                .toList();
    }

    /**
     * Lấy service ticket id từ estimate
     */
    private Integer getServiceTicketIdFromEstimate(Integer estimateId) {
        Estimate estimate = estimateRepository.findEstimateById(estimateId);
        return estimate != null ? estimate.getServiceTicketId() : null;
    }

    /**
     * Log transaction vào database
     * - type: ADJUSTMENT (tất cả các thay đổi về allocation)
     * - qty: số lượng thay đổi (+/-)
     * - balanceAfter: tổng số lượng sau thay đổi
     * - refType: loại reference (stock_allocation, stock_allocation_commit, etc.)
     * - refId: id của reference (allocation id)
     */
    private void logTransaction(
            Integer warehouseId,
            Integer itemId,
            InventoryTransactionType type,  // Loại transaction
            int qty,                        // Số lượng (+/-)
            int balanceAfter,              // Tổng hàng sau thay đổi
            String refType,                // Loại reference (để biết action nào)
            Integer refId,                 // ID của allocation
            Integer staffId                // Ai thực hiện
    ) {
        InventoryTransaction tx = new InventoryTransaction();
        tx.setWarehouseId(warehouseId);
        tx.setItemId(itemId);
        tx.setTransactionType(type);      // Kiểu: ADJUSTMENT
        tx.setQuantity(qty);              // Delta
        tx.setBalanceAfter(balanceAfter); // Số lượng cuối
        tx.setReferenceType(refType);     // stock_allocation, stock_allocation_commit, etc.
        tx.setReferenceId(refId);         // allocation id
        tx.setCreatedById(staffId);       // User id
        tx.setCreatedAt(Instant.now());   // Thời gian
        transactionRepo.save(tx);
    }
}
