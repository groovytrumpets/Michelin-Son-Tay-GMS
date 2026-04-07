package com.g42.platform.gms.warehouse.app.service.entry;

import com.g42.platform.gms.common.service.ImageUploadService;
import com.g42.platform.gms.warehouse.api.dto.entry.CreateStockEntryRequest;
import com.g42.platform.gms.warehouse.api.dto.entry.CreateStockEntryWithAttachmentRequest;
import com.g42.platform.gms.warehouse.api.dto.entry.StockEntryItemRequest;
import com.g42.platform.gms.warehouse.api.dto.response.StockEntryItemResponse;
import com.g42.platform.gms.warehouse.api.dto.response.StockEntryResponse;
import com.g42.platform.gms.warehouse.domain.enums.InventoryTransactionType;
import com.g42.platform.gms.warehouse.domain.enums.StockEntryStatus;
import com.g42.platform.gms.warehouse.domain.repository.InventoryRepo;
import com.g42.platform.gms.warehouse.domain.repository.InventoryTransactionRepo;
import com.g42.platform.gms.warehouse.domain.repository.StockEntryRepo;
import com.g42.platform.gms.warehouse.domain.repository.WarehouseAttachmentRepo;
import com.g42.platform.gms.warehouse.infrastructure.entity.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockEntryServiceImpl implements StockEntryService {

    private static final String FOLDER_STOCK_ENTRY = "garage/warehouse/stock-entry";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final AtomicInteger SEQ = new AtomicInteger(0);

    private final StockEntryRepo stockEntryRepo;
    private final InventoryRepo inventoryRepo;
    private final InventoryTransactionRepo transactionRepo;
    private final ImageUploadService imageUploadService;
    private final WarehouseAttachmentRepo attachmentRepo;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules(); // hỗ trợ LocalDate

    @Override
    @Transactional(readOnly = true)
    public List<StockEntryResponse> listByWarehouse(Integer warehouseId, StockEntryStatus status) {
        List<StockEntryJpa> entries = status != null
                ? stockEntryRepo.findByWarehouseIdAndStatus(warehouseId, status)
                : stockEntryRepo.findByWarehouseId(warehouseId);
        return entries.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public StockEntryResponse getById(Integer entryId) {
        return toResponse(findOrThrow(entryId));
    }

    @Override
    @Transactional
    public StockEntryResponse create(CreateStockEntryRequest request, Integer staffId) {
        StockEntryJpa entry = new StockEntryJpa();
        entry.setEntryCode(generateCode());
        entry.setWarehouseId(request.getWarehouseId());
        entry.setSupplierName(request.getSupplierName());
        entry.setEntryDate(request.getEntryDate() != null ? request.getEntryDate() : LocalDate.now());
        entry.setNotes(request.getNotes());
        entry.setStatus(StockEntryStatus.DRAFT);
        entry.setCreatedBy(staffId);

        StockEntryJpa saved = stockEntryRepo.save(entry);

        for (StockEntryItemRequest itemReq : request.getItems()) {
            StockEntryItemJpa item = new StockEntryItemJpa();
            item.setEntryId(saved.getEntryId());
            item.setItemId(itemReq.getItemId());
            item.setQuantity(itemReq.getQuantity());
            item.setImportPrice(itemReq.getImportPrice());
            item.setMarkupMultiplier(itemReq.getMarkupMultiplier() != null
                    ? itemReq.getMarkupMultiplier() : java.math.BigDecimal.ONE);
            item.setRemainingQuantity(itemReq.getQuantity());
            item.setNotes(itemReq.getNotes());
            saved.getItems().add(item);
        }

        return toResponse(stockEntryRepo.save(saved));
    }

    /**
     * Tạo phiếu + upload ảnh chứng từ trong 1 request multipart.
     * Client gửi: part "data" (JSON) + part "file" (ảnh).
     */
    @Override
    @Transactional
    public StockEntryResponse createWithAttachment(CreateStockEntryRequest request,
                                                    MultipartFile file,
                                                    Integer staffId) throws IOException {
        // 1. Tạo phiếu DRAFT
        StockEntryResponse created = create(request, staffId);

        // 2. Upload ảnh và lưu attachment trong cùng transaction
        String url = imageUploadService.uploadImage(file, FOLDER_STOCK_ENTRY);
        WarehouseAttachmentJpa attachment = new WarehouseAttachmentJpa();
        attachment.setRefType(WarehouseAttachmentJpa.RefType.STOCK_ENTRY);
        attachment.setRefId(created.getEntryId());
        attachment.setFileUrl(url);
        attachment.setUploadedBy(staffId);
        attachmentRepo.save(attachment);

        // Re-fetch để response có đủ attachments
        return toResponse(findOrThrow(created.getEntryId()));
    }

    /**
     * Tạo phiếu + ảnh qua @ModelAttribute form.
     * items là JSON string: [{"itemId":8,"quantity":50,"importPrice":45000,"markupMultiplier":1.5}]
     */
    @Override
    @Transactional
    public StockEntryResponse createWithAttachmentForm(CreateStockEntryWithAttachmentRequest req,
                                                        Integer staffId) throws IOException {
        // Parse items JSON string
        List<StockEntryItemRequest> items;
        try {
            items = objectMapper.readValue(req.getItems(),
                    new TypeReference<List<StockEntryItemRequest>>() {});
        } catch (Exception e) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "items không hợp lệ: " + e.getMessage());
        }

        // Build CreateStockEntryRequest
        CreateStockEntryRequest request = new CreateStockEntryRequest();
        request.setWarehouseId(req.getWarehouseId());
        request.setSupplierName(req.getSupplierName());
        request.setNotes(req.getNotes());
        request.setEntryDate(req.getEntryDate() != null && !req.getEntryDate().isBlank()
                ? LocalDate.parse(req.getEntryDate()) : LocalDate.now());
        request.setItems(items);

        return createWithAttachment(request, req.getFile(), staffId);
    }

    @Override
    @Transactional
    public void addAttachment(Integer entryId, MultipartFile file, Integer staffId) throws IOException {
        StockEntryJpa entry = findOrThrow(entryId);
        if (entry.getStatus() == StockEntryStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Không thể thêm ảnh cho phiếu đã xác nhận");
        }

        String url = imageUploadService.uploadImage(file, FOLDER_STOCK_ENTRY);

        WarehouseAttachmentJpa attachment = new WarehouseAttachmentJpa();
        attachment.setRefType(WarehouseAttachmentJpa.RefType.STOCK_ENTRY);
        attachment.setRefId(entryId);
        attachment.setFileUrl(url);
        attachment.setUploadedBy(staffId);
        attachmentRepo.save(attachment);
    }

    @Override
    @Transactional
    public StockEntryResponse confirm(Integer entryId, Integer staffId) {
        StockEntryJpa entry = findOrThrow(entryId);

        if (entry.getStatus() == StockEntryStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phiếu đã được xác nhận");
        }
        boolean hasAttachment = attachmentRepo.existsByRefTypeAndRefId(
                WarehouseAttachmentJpa.RefType.STOCK_ENTRY, entryId);
        if (!hasAttachment) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Cần đính kèm ảnh chứng từ trước khi xác nhận");
        }
        if (entry.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Phiếu nhập không có sản phẩm nào");
        }

        for (StockEntryItemJpa item : entry.getItems()) {
            InventoryJpa inv = inventoryRepo
                    .findByWarehouseAndItemWithLock(entry.getWarehouseId(), item.getItemId())
                    .orElseGet(() -> {
                        InventoryJpa newInv = new InventoryJpa();
                        newInv.setWarehouseId(entry.getWarehouseId());
                        newInv.setItemId(item.getItemId());
                        newInv.setQuantity(0);
                        newInv.setReservedQuantity(0);
                        return newInv;
                    });

            int newQty = inv.getQuantity() + item.getQuantity();
            inv.setQuantity(newQty);
            inventoryRepo.save(inv);

            InventoryTransactionJpa tx = new InventoryTransactionJpa();
            tx.setWarehouseId(entry.getWarehouseId());
            tx.setItemId(item.getItemId());
            tx.setTransactionType(InventoryTransactionType.IN);
            tx.setQuantity(item.getQuantity());
            tx.setBalanceAfter(newQty);
            tx.setReferenceType("stock_entry");
            tx.setReferenceId(entryId);
            tx.setCreatedById(staffId);
            tx.setCreatedAt(Instant.now());
            transactionRepo.save(tx);
        }

        entry.setStatus(StockEntryStatus.CONFIRMED);
        entry.setConfirmedBy(staffId);
        entry.setConfirmedAt(LocalDateTime.now());

        return toResponse(stockEntryRepo.save(entry));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private StockEntryJpa findOrThrow(Integer entryId) {
        return stockEntryRepo.findById(entryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy phiếu nhập kho id=" + entryId));
    }

    private String generateCode() {
        String date = LocalDate.now().format(DATE_FMT);
        int seq = SEQ.incrementAndGet();
        String candidate = String.format("NK-%s-%d", date, seq);
        while (stockEntryRepo.existsByCode(candidate)) {
            candidate = String.format("NK-%s-%d", date, SEQ.incrementAndGet());
        }
        return candidate;
    }

    private StockEntryResponse toResponse(StockEntryJpa e) {
        StockEntryResponse r = new StockEntryResponse();
        r.setEntryId(e.getEntryId());
        r.setEntryCode(e.getEntryCode());
        r.setWarehouseId(e.getWarehouseId());
        r.setSupplierName(e.getSupplierName());
        r.setEntryDate(e.getEntryDate());
        r.setStatus(e.getStatus());
        r.setNotes(e.getNotes());
        r.setConfirmedBy(e.getConfirmedBy());
        r.setConfirmedAt(e.getConfirmedAt());
        r.setCreatedBy(e.getCreatedBy());
        r.setCreatedAt(e.getCreatedAt());

        List<StockEntryItemResponse> itemResponses = e.getItems().stream().map(i -> {
            StockEntryItemResponse ir = new StockEntryItemResponse();
            ir.setEntryItemId(i.getEntryItemId());
            ir.setItemId(i.getItemId());
            ir.setQuantity(i.getQuantity());
            ir.setImportPrice(i.getImportPrice());
            ir.setMarkupMultiplier(i.getMarkupMultiplier());
            ir.setRemainingQuantity(i.getRemainingQuantity());
            ir.setNotes(i.getNotes());
            return ir;
        }).collect(Collectors.toList());
        r.setItems(itemResponses);

        // Load danh sách URL ảnh chứng từ
        if (e.getEntryId() != null) {
            List<String> urls = attachmentRepo
                    .findByRefTypeAndRefId(WarehouseAttachmentJpa.RefType.STOCK_ENTRY, e.getEntryId())
                    .stream()
                    .map(WarehouseAttachmentJpa::getFileUrl)
                    .collect(Collectors.toList());
            r.setAttachments(urls);
        }

        return r;
    }
}
