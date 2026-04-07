package com.g42.platform.gms.warehouse.app.service.issue;

import com.g42.platform.gms.warehouse.api.dto.issue.CreateStockIssueRequest;
import com.g42.platform.gms.warehouse.api.dto.response.StockIssueDetailResponse;
import com.g42.platform.gms.warehouse.api.dto.response.StockIssueResponse;
import com.g42.platform.gms.warehouse.domain.enums.InventoryTransactionType;
import com.g42.platform.gms.warehouse.domain.enums.StockIssueStatus;
import com.g42.platform.gms.warehouse.domain.repository.InventoryRepo;
import com.g42.platform.gms.warehouse.domain.repository.InventoryTransactionRepo;
import com.g42.platform.gms.warehouse.domain.repository.StockEntryRepo;
import com.g42.platform.gms.warehouse.domain.repository.StockIssueRepo;
import com.g42.platform.gms.warehouse.infrastructure.entity.*;
import com.g42.platform.gms.warehouse.infrastructure.entity.WarehousePricingJpa;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.g42.platform.gms.warehouse.infrastructure.repository.WarehousePricingJpaRepo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockIssueServiceImpl implements StockIssueService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final AtomicInteger SEQ = new AtomicInteger(0);

    private final StockIssueRepo stockIssueRepo;
    private final InventoryRepo inventoryRepo;
    private final InventoryTransactionRepo transactionRepo;
    private final StockEntryRepo stockEntryRepo;
    private final WarehousePricingJpaRepo pricingRepo;

    /**
     * Tạo phiếu xuất DRAFT.
     * Chỉ lưu itemId + quantity + discountRate — giá sẽ được resolve theo FIFO khi confirm.
     * Mỗi IssueItemRequest tạo 1 placeholder row với entryItemId=0, giá=0.
     */
    @Override
    @Transactional
    public StockIssueResponse create(CreateStockIssueRequest request, Integer staffId) {
        for (CreateStockIssueRequest.IssueItemRequest item : request.getItems()) {
            int available = inventoryRepo
                    .findByWarehouseAndItem(request.getWarehouseId(), item.getItemId())
                    .map(inv -> Math.max(0, inv.getQuantity() - inv.getReservedQuantity()))
                    .orElse(0);
            if (available < item.getQuantity()) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Không đủ tồn kho cho itemId=" + item.getItemId()
                                + " (yêu cầu=" + item.getQuantity() + ", khả dụng=" + available + ")");
            }
        }

        StockIssueJpa issue = new StockIssueJpa();
        issue.setIssueCode(generateIssueCode());
        issue.setWarehouseId(request.getWarehouseId());
        issue.setIssueType(request.getIssueType());
        issue.setIssueReason(request.getIssueReason());
        issue.setServiceTicketId(request.getServiceTicketId());
        issue.setDiscountRate(BigDecimal.ZERO);
        issue.setStatus(StockIssueStatus.DRAFT);
        issue.setCreatedBy(staffId);

        List<StockIssueItemJpa> placeholders = request.getItems().stream().map(req -> {
            StockIssueItemJpa it = new StockIssueItemJpa();
            it.setItemId(req.getItemId());
            it.setQuantity(req.getQuantity());
            it.setEntryItemId(0); // placeholder — sẽ được split khi confirm
            it.setExportPrice(BigDecimal.ZERO);
            it.setImportPrice(BigDecimal.ZERO);
            it.setDiscountRate(req.getDiscountRate() != null ? req.getDiscountRate() : BigDecimal.ZERO);
            it.setFinalPrice(BigDecimal.ZERO);
            return it;
        }).collect(Collectors.toList());

        issue.setItems(placeholders);
        StockIssueJpa saved = stockIssueRepo.save(issue);
        saved.getItems().forEach(it -> it.setIssueId(saved.getIssueId()));
        stockIssueRepo.save(saved);
        return toResponse(saved);
    }

    /**
     * Confirm phiếu xuất — FIFO split-by-lot.
     *
     * Với mỗi placeholder item:
     *   1. Lấy danh sách lô FIFO còn hàng (remaining_quantity > 0), cũ nhất trước
     *   2. Consume từng lô, tạo 1 StockIssueItemJpa per lô
     *   3. Trừ remaining_quantity của lô
     *   4. Trừ inventory.quantity
     *   5. Ghi audit log
     * Sau đó xóa placeholders, lưu lot-split items.
     */
    @Override
    @Transactional
    public StockIssueResponse confirm(Integer issueId, Integer staffId) {
        StockIssueJpa issue = findOrThrow(issueId);
        if (issue.getStatus() == StockIssueStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phiếu đã được xác nhận");
        }

        List<StockIssueItemJpa> lotItems = new ArrayList<>();

        for (StockIssueItemJpa placeholder : issue.getItems()) {
            Integer itemId = placeholder.getItemId();
            int needed = placeholder.getQuantity();
            BigDecimal discountRate = placeholder.getDiscountRate() != null
                    ? placeholder.getDiscountRate() : BigDecimal.ZERO;

            // Lock inventory row
            InventoryJpa inv = inventoryRepo
                    .findByWarehouseAndItemWithLock(issue.getWarehouseId(), itemId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                            "Không tìm thấy tồn kho cho itemId=" + itemId));

            int available = Math.max(0, inv.getQuantity() - inv.getReservedQuantity());
            if (available < needed) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Không đủ tồn kho cho itemId=" + itemId
                                + " (yêu cầu=" + needed + ", khả dụng=" + available + ")");
            }

            // FIFO: lấy lô cũ nhất còn hàng
            List<StockEntryItemJpa> lots = stockEntryRepo.findFifoLots(issue.getWarehouseId(), itemId);
            int remaining = needed;

            // Lấy giá bán theo 2 tầng:
            // Tầng 1: warehouse_pricing.selling_price (giá thị trường, ưu tiên)
            // Tầng 2: import_price × markup_multiplier của lô (fallback)
            BigDecimal marketSellingPrice = pricingRepo
                    .findByWarehouseIdAndItemIdAndIsActiveTrue(issue.getWarehouseId(), itemId)
                    .map(WarehousePricingJpa::getSellingPrice)
                    .orElse(null); // null = dùng fallback từ lô

            for (StockEntryItemJpa lot : lots) {
                if (remaining <= 0) break;

                int consume = Math.min(remaining, lot.getRemainingQuantity());

                // 2 tầng giá bán:
                // Tầng 1: warehouse_pricing (giá thị trường) — nếu có
                // Tầng 2: import_price × markup_multiplier của lô — fallback
                BigDecimal sellingPrice = marketSellingPrice != null
                        ? marketSellingPrice
                        : lot.getImportPrice()
                                .multiply(lot.getMarkupMultiplier())
                                .setScale(2, RoundingMode.HALF_UP);

                BigDecimal finalPrice = sellingPrice
                        .multiply(BigDecimal.ONE.subtract(
                                discountRate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)))
                        .setScale(2, RoundingMode.HALF_UP);

                // 1 dòng per lô — export_price = giá thị trường, import_price = giá vốn lô
                StockIssueItemJpa lotItem = new StockIssueItemJpa();
                lotItem.setIssueId(issueId);
                lotItem.setItemId(itemId);
                lotItem.setEntryItemId(lot.getEntryItemId());
                lotItem.setQuantity(consume);
                lotItem.setExportPrice(sellingPrice);   // giá bán (thị trường hoặc fallback lô)
                lotItem.setImportPrice(lot.getImportPrice()); // giá vốn lô FIFO
                lotItem.setDiscountRate(discountRate);
                lotItem.setFinalPrice(finalPrice);
                lotItems.add(lotItem);

                // Trừ remaining_quantity của lô
                lot.setRemainingQuantity(lot.getRemainingQuantity() - consume);
                stockEntryRepo.saveItem(lot);

                remaining -= consume;
            }

            // Trừ inventory
            int newQty = inv.getQuantity() - needed;
            inv.setQuantity(newQty);
            inventoryRepo.save(inv);

            // Audit log
            InventoryTransactionJpa tx = new InventoryTransactionJpa();
            tx.setWarehouseId(issue.getWarehouseId());
            tx.setItemId(itemId);
            tx.setTransactionType(InventoryTransactionType.OUT);
            tx.setQuantity(needed);
            tx.setBalanceAfter(newQty);
            tx.setReferenceType("stock_issue");
            tx.setReferenceId(issueId);
            tx.setCreatedById(staffId);
            tx.setCreatedAt(Instant.now());
            transactionRepo.save(tx);
        }

        // Xóa placeholders, thay bằng lot-split items
        issue.getItems().clear();
        stockIssueRepo.save(issue);
        issue.getItems().addAll(lotItems);

        issue.setStatus(StockIssueStatus.CONFIRMED);
        issue.setConfirmedBy(staffId);
        issue.setConfirmedAt(LocalDateTime.now());

        return toResponse(stockIssueRepo.save(issue));
    }

    @Override
    @Transactional(readOnly = true)
    public StockIssueDetailResponse getDetail(Integer issueId) {
        StockIssueJpa issue = findOrThrow(issueId);

        StockIssueDetailResponse resp = new StockIssueDetailResponse();
        resp.setIssueId(issue.getIssueId());
        resp.setIssueCode(issue.getIssueCode());
        resp.setWarehouseId(issue.getWarehouseId());
        resp.setIssueType(issue.getIssueType());
        resp.setIssueReason(issue.getIssueReason());
        resp.setServiceTicketId(issue.getServiceTicketId());
        resp.setDiscountRate(issue.getDiscountRate());
        resp.setStatus(issue.getStatus());
        resp.setConfirmedBy(issue.getConfirmedBy());
        resp.setConfirmedAt(issue.getConfirmedAt());
        resp.setCreatedBy(issue.getCreatedBy());
        resp.setCreatedAt(issue.getCreatedAt());

        resp.setItems(issue.getItems().stream().map(it -> {
            StockIssueDetailResponse.IssueItemDetail d = new StockIssueDetailResponse.IssueItemDetail();
            d.setIssueItemId(it.getIssueItemId());
            d.setItemId(it.getItemId());
            d.setEntryItemId(it.getEntryItemId());
            d.setQuantity(it.getQuantity());
            d.setExportPrice(it.getExportPrice());
            d.setImportPrice(it.getImportPrice());
            d.setDiscountRate(it.getDiscountRate());
            d.setFinalPrice(it.getFinalPrice());
            d.setGrossProfit(it.getGrossProfit());
            return d;
        }).collect(Collectors.toList()));

        return resp;
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private StockIssueJpa findOrThrow(Integer issueId) {
        return stockIssueRepo.findById(issueId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy phiếu xuất kho id=" + issueId));
    }

    private String generateIssueCode() {
        String date = LocalDate.now().format(DATE_FMT);
        int seq = SEQ.incrementAndGet();
        String candidate = String.format("XK-%s-%d", date, seq);
        while (stockIssueRepo.existsByCode(candidate)) {
            candidate = String.format("XK-%s-%d", date, SEQ.incrementAndGet());
        }
        return candidate;
    }

    private StockIssueResponse toResponse(StockIssueJpa e) {
        StockIssueResponse r = new StockIssueResponse();
        r.setIssueId(e.getIssueId());
        r.setIssueCode(e.getIssueCode());
        r.setWarehouseId(e.getWarehouseId());
        r.setIssueType(e.getIssueType());
        r.setIssueReason(e.getIssueReason());
        r.setServiceTicketId(e.getServiceTicketId());
        r.setDiscountRate(e.getDiscountRate());
        r.setStatus(e.getStatus());
        r.setConfirmedBy(e.getConfirmedBy());
        r.setConfirmedAt(e.getConfirmedAt());
        r.setCreatedBy(e.getCreatedBy());
        r.setCreatedAt(e.getCreatedAt());
        return r;
    }
}
