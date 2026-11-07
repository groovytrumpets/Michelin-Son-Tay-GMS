package com.g42.platform.gms.warehouse.app.service.allocation;

import com.g42.platform.gms.common.util.Qty;
import com.g42.platform.gms.warehouse.app.service.issue.StockIssueService;
import com.g42.platform.gms.warehouse.app.service.returns.ReturnEntryService;
import com.g42.platform.gms.warehouse.app.service.serial.ItemSerialService;
import com.g42.platform.gms.warehouse.domain.entity.Inventory;
import com.g42.platform.gms.warehouse.domain.entity.InventoryTransaction;
import com.g42.platform.gms.warehouse.domain.entity.StockAllocation;
import com.g42.platform.gms.warehouse.domain.entity.StockIssue;
import com.g42.platform.gms.warehouse.domain.entity.StockIssueItem;
import com.g42.platform.gms.warehouse.domain.enums.AllocationStatus;
import com.g42.platform.gms.warehouse.domain.enums.InventoryTransactionType;
import com.g42.platform.gms.warehouse.domain.enums.StockIssueStatus;
import com.g42.platform.gms.warehouse.domain.repository.InventoryRepo;
import com.g42.platform.gms.warehouse.domain.repository.InventoryTransactionRepo;
import com.g42.platform.gms.warehouse.domain.repository.StockAllocationRepo;
import com.g42.platform.gms.warehouse.domain.repository.StockEntryRepo;
import com.g42.platform.gms.warehouse.domain.repository.StockIssueItemRepo;
import com.g42.platform.gms.warehouse.domain.repository.StockIssueRepo;
import com.g42.platform.gms.warehouse.infrastructure.entity.CatalogItemJpa;
import com.g42.platform.gms.warehouse.infrastructure.repository.CatalogItemJpaRepo;
import com.g42.platform.gms.warehouse.infrastructure.repository.ReturnEntryJpaRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Hoàn tác MỌI tác động kho của một phiếu dịch vụ bị huỷ vì tạo trùng.
 *
 * <p>Phiếu trùng là phiếu "không có thật": hàng ngoài đời chỉ rời kho một lần, ở phiếu gốc. Nên
 * những gì phiếu trùng đã giữ / đã xuất trong hệ thống phải quay lại đúng như chưa từng có:
 * <ol>
 *   <li>Allocation còn RESERVED → nhả giữ hàng (StockAllocationService.release, nhả cả serial).</li>
 *   <li>Phiếu xuất DRAFT → huỷ (chưa trừ tồn nên chỉ lật trạng thái).</li>
 *   <li>Phiếu xuất CONFIRMED → cộng lại tồn kho, cộng lại lô (remaining_quantity), serial về
 *       IN_STOCK, allocation COMMITTED → RELEASED, phiếu xuất → CANCELLED. Ghi inventory_transaction
 *       IN (reference_type = {@value #REFERENCE_TYPE}) nên cặp OUT/IN vẫn còn để truy vết.</li>
 *   <li>Phiếu hoàn đang chờ duyệt lập từ các phiếu xuất đó → huỷ, nếu không kho duyệt sau là cộng
 *       tồn lần hai.</li>
 * </ol>
 *
 * <p>Phần nào đã được khách trả về kho bằng phiếu hoàn CUSTOMER_RETURN đã xác nhận thì không cộng
 * lại nữa. Phiếu đổi hàng / trả nhà cung cấp đã xác nhận thì từ chối — hai loại đó có hàng mới
 * xuất ra hoặc hàng đã rời hẳn khỏi garage, tự hoàn tác dễ sai, phải xử lý tay.
 *
 * <p>Chạy với {@code dryRun = true} để xem trước (không ghi gì), dùng cho popup xác nhận.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketStockReversalService {

    public static final String REFERENCE_TYPE = "service_ticket_void";

    public enum LineAction { RELEASE_RESERVED, RETURN_TO_STOCK }

    /** Một dòng hàng sẽ / đã được hoàn tác. */
    public record Line(LineAction action, Integer warehouseId, Integer itemId, String itemName, String sku,
                       BigDecimal quantity, String issueCode) {
    }

    /** Kết quả (hoặc bản xem trước) của việc hoàn tác kho. */
    public record Result(List<Line> lines, int draftIssuesCancelled, int confirmedIssuesReversed,
                         int openReturnsCancelled, List<String> issueCodes) {
        public boolean isEmpty() {
            return lines.isEmpty() && draftIssuesCancelled == 0 && confirmedIssuesReversed == 0
                    && openReturnsCancelled == 0;
        }
    }

    private final StockAllocationService stockAllocationService;
    private final StockIssueService stockIssueService;
    private final ReturnEntryService returnEntryService;
    private final ItemSerialService itemSerialService;
    private final StockAllocationRepo allocationRepo;
    private final StockIssueRepo stockIssueRepo;
    private final StockIssueItemRepo stockIssueItemRepo;
    private final StockEntryRepo stockEntryRepo;
    private final InventoryRepo inventoryRepo;
    private final InventoryTransactionRepo transactionRepo;
    private final ReturnEntryJpaRepo returnEntryJpaRepo;
    private final CatalogItemJpaRepo catalogItemJpaRepo;

    @Transactional(readOnly = true)
    public Result preview(Integer serviceTicketId) {
        return run(serviceTicketId, null, null, true);
    }

    /**
     * Hoàn tác thật. Phải chạy trong cùng transaction với việc huỷ phiếu/hoá đơn để lỗi ở bất kỳ
     * bước nào cũng quay lui toàn bộ.
     */
    @Transactional
    public Result reverse(Integer serviceTicketId, Integer staffId, String note) {
        if (staffId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Thiếu thông tin nhân viên thực hiện.");
        }
        return run(serviceTicketId, staffId, note, false);
    }

    private Result run(Integer serviceTicketId, Integer staffId, String note, boolean dryRun) {
        List<StockIssue> issues = stockIssueRepo.findByServiceTicketId(serviceTicketId);
        List<StockIssue> draftIssues = issues.stream()
                .filter(i -> i.getStatus() == StockIssueStatus.DRAFT).toList();
        List<StockIssue> confirmedIssues = issues.stream()
                .filter(i -> i.getStatus() == StockIssueStatus.CONFIRMED).toList();
        List<Integer> liveIssueIds = new ArrayList<>();
        draftIssues.forEach(i -> liveIssueIds.add(i.getIssueId()));
        confirmedIssues.forEach(i -> liveIssueIds.add(i.getIssueId()));

        List<Integer> confirmedIds = confirmedIssues.stream().map(StockIssue::getIssueId).toList();
        if (!confirmedIds.isEmpty()
                && returnEntryJpaRepo.countConfirmedNonCustomerReturnsBySourceIssueIds(confirmedIds) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Phiếu này có phiếu đổi hàng / trả nhà cung cấp đã xác nhận — hệ thống không tự hoàn tác "
                            + "được, cần kho xử lý tay trước khi huỷ phiếu trùng.");
        }

        List<Integer> openReturnIds = liveIssueIds.isEmpty() ? List.of()
                : returnEntryJpaRepo.findOpenReturnIdsBySourceIssueIds(liveIssueIds);

        // ---- Dựng danh sách dòng hàng (dùng chung cho xem trước và chạy thật) ----
        List<StockAllocation> reserved = allocationRepo.findByTicketAndStatus(serviceTicketId, AllocationStatus.RESERVED);
        Map<Integer, String> issueCodeById = issues.stream()
                .collect(Collectors.toMap(StockIssue::getIssueId, i -> Objects.toString(i.getIssueCode(), ""), (a, b) -> a));

        Map<Integer, List<StockIssueItem>> itemsByIssue = new HashMap<>();
        Set<Integer> issueItemIds = new HashSet<>();
        for (StockIssue issue : confirmedIssues) {
            List<StockIssueItem> items = stockIssueItemRepo.findByIssueId(issue.getIssueId());
            itemsByIssue.put(issue.getIssueId(), items);
            items.forEach(it -> issueItemIds.add(it.getIssueItemId()));
        }
        Map<Integer, BigDecimal> returnedByIssueItem = new HashMap<>();
        if (!issueItemIds.isEmpty()) {
            for (Object[] row : returnEntryJpaRepo.sumConfirmedCustomerReturnByIssueItemIds(issueItemIds)) {
                returnedByIssueItem.put(((Number) row[0]).intValue(), toBigDecimal(row[1]));
            }
        }

        Set<Integer> itemIds = new HashSet<>();
        reserved.forEach(a -> itemIds.add(a.getItemId()));
        itemsByIssue.values().forEach(list -> list.forEach(it -> itemIds.add(it.getItemId())));
        Map<Integer, CatalogItemJpa> catalog = itemIds.isEmpty() ? Map.of()
                : catalogItemJpaRepo.findAllById(itemIds).stream()
                        .collect(Collectors.toMap(CatalogItemJpa::getItemId, Function.identity(), (a, b) -> a));

        List<Line> lines = new ArrayList<>();
        for (StockAllocation alloc : reserved) {
            lines.add(line(LineAction.RELEASE_RESERVED, alloc.getWarehouseId(), alloc.getItemId(),
                    alloc.getQuantity(), issueCodeById.get(alloc.getIssueId()), catalog));
        }
        // Dòng phiếu xuất đã xác nhận còn "ở ngoài kho" (đã trừ phần khách trả về)
        List<PendingReturn> toReturn = new ArrayList<>();
        for (StockIssue issue : confirmedIssues) {
            for (StockIssueItem it : itemsByIssue.getOrDefault(issue.getIssueId(), List.of())) {
                BigDecimal remaining = Qty.subFloorZero(Qty.nz(it.getQuantity()),
                        returnedByIssueItem.getOrDefault(it.getIssueItemId(), BigDecimal.ZERO));
                if (remaining.signum() <= 0) {
                    continue;
                }
                toReturn.add(new PendingReturn(issue, it, remaining));
                lines.add(line(LineAction.RETURN_TO_STOCK, issue.getWarehouseId(), it.getItemId(),
                        remaining, issue.getIssueCode(), catalog));
            }
        }

        List<String> touchedCodes = new ArrayList<>();
        draftIssues.forEach(i -> touchedCodes.add(i.getIssueCode()));
        confirmedIssues.forEach(i -> touchedCodes.add(i.getIssueCode()));
        Result result = new Result(lines, draftIssues.size(), confirmedIssues.size(), openReturnIds.size(), touchedCodes);
        if (dryRun) {
            return result;
        }

        // ---- Chạy thật ----
        // 1) Phiếu hoàn chờ duyệt: huỷ trước, để không ai duyệt nó sau khi hàng đã được cộng lại.
        for (Integer returnId : openReturnIds) {
            returnEntryService.cancel(returnId, staffId);
        }

        // 2) Nhả giữ hàng TRƯỚC khi huỷ phiếu xuất nháp: release() nhả cả serial đang giữ, còn
        //    StockIssueService.cancel() chỉ nhả số lượng — gọi ngược thứ tự là serial kẹt RESERVED.
        stockAllocationService.release(serviceTicketId, staffId);
        for (StockIssue draft : draftIssues) {
            stockIssueService.cancel(draft.getIssueId(), staffId);
        }

        // 3) Phiếu xuất đã xác nhận: cộng lại tồn, lô, serial.
        String txNote = buildNote(note);
        for (PendingReturn pending : toReturn) {
            StockIssue issue = pending.issue();
            StockIssueItem it = pending.item();
            BigDecimal qty = pending.quantity();
            Integer warehouseId = issue.getWarehouseId();

            Inventory inv = inventoryRepo.findByWarehouseAndItemWithLock(warehouseId, it.getItemId())
                    .orElseGet(() -> Inventory.builder()
                            .warehouseId(warehouseId)
                            .itemId(it.getItemId())
                            .quantity(BigDecimal.ZERO)
                            .reservedQuantity(BigDecimal.ZERO)
                            .build());
            BigDecimal newQty = Qty.add(Qty.nz(inv.getQuantity()), qty);
            inv.setQuantity(newQty);
            inventoryRepo.save(inv);

            Integer entryItemId = it.getEntryItemId() != null && it.getEntryItemId() > 0 ? it.getEntryItemId() : null;
            if (entryItemId != null) {
                stockEntryRepo.increaseRemainingQuantity(entryItemId, qty);
            }
            // Serial đã bán ở phiếu xuất này quay lại kho (không phải hàng lỗi)
            itemSerialService.restoreForReturn(issue.getIssueId(), it.getItemId(), entryItemId, qty, warehouseId, false);

            InventoryTransaction tx = new InventoryTransaction();
            tx.setWarehouseId(warehouseId);
            tx.setItemId(it.getItemId());
            tx.setEntryItemId(entryItemId);
            tx.setTransactionType(InventoryTransactionType.IN);
            tx.setQuantity(qty);
            tx.setBalanceAfter(newQty);
            tx.setReferenceType(REFERENCE_TYPE);
            tx.setReferenceId(serviceTicketId);
            tx.setNotes(txNote + " (phiếu xuất " + issue.getIssueCode() + ")");
            tx.setCreatedById(staffId);
            tx.setCreatedAt(Instant.now());
            transactionRepo.save(tx);
        }

        for (StockIssue issue : confirmedIssues) {
            for (StockAllocation alloc : allocationRepo.findByIssueIdAndStatus(issue.getIssueId(), AllocationStatus.COMMITTED)) {
                alloc.setStatus(AllocationStatus.RELEASED);
                allocationRepo.save(alloc);
            }
            StockIssue fresh = stockIssueRepo.findById(issue.getIssueId()).orElse(issue);
            fresh.setStatus(StockIssueStatus.CANCELLED);
            String reason = Objects.toString(fresh.getIssueReason(), "").trim();
            fresh.setIssueReason((reason.isEmpty() ? "" : reason + " — ") + "[Đã hoàn kho: " + txNote + "]");
            stockIssueRepo.save(fresh);
        }

        log.warn("Đã hoàn tác kho cho phiếu dịch vụ #{} (huỷ trùng): {} dòng, {} phiếu xuất nháp, {} phiếu xuất đã xác nhận, {} phiếu hoàn chờ duyệt",
                serviceTicketId, lines.size(), draftIssues.size(), confirmedIssues.size(), openReturnIds.size());
        return result;
    }

    private record PendingReturn(StockIssue issue, StockIssueItem item, BigDecimal quantity) {
    }

    private static String buildNote(String note) {
        String trimmed = note == null ? "" : note.trim();
        String base = "Huỷ phiếu dịch vụ trùng";
        String text = trimmed.isEmpty() ? base : base + ": " + trimmed;
        return text.length() > 400 ? text.substring(0, 400) : text;
    }

    private static Line line(LineAction action, Integer warehouseId, Integer itemId, BigDecimal qty,
                             String issueCode, Map<Integer, CatalogItemJpa> catalog) {
        CatalogItemJpa item = catalog.get(itemId);
        return new Line(action, warehouseId, itemId,
                item != null ? item.getItemName() : ("Mặt hàng #" + itemId),
                item != null ? item.getSku() : null,
                qty, issueCode);
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal bd) return bd;
        return new BigDecimal(value.toString());
    }
}
