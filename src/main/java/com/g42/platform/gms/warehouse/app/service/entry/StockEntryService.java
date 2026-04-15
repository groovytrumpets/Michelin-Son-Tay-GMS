package com.g42.platform.gms.warehouse.app.service.entry;

import com.g42.platform.gms.auth.entity.StaffProfile;
import com.g42.platform.gms.auth.repository.StaffProfileRepo;
import com.g42.platform.gms.booking.customer.infrastructure.entity.CatalogItemJpaEntity;
import com.g42.platform.gms.catalog.infrastructure.repository.CatalogItemRepository;
import com.g42.platform.gms.common.service.ImageUploadService;
import com.g42.platform.gms.warehouse.api.dto.entry.CreateStockEntryRequest;
import com.g42.platform.gms.warehouse.api.dto.entry.CreateStockEntryWithAttachmentRequest;
import com.g42.platform.gms.warehouse.api.dto.entry.StockEntryItemRequest;
import com.g42.platform.gms.warehouse.api.dto.request.PatchEntryItemRequest;
import com.g42.platform.gms.warehouse.api.dto.request.UpdateStockEntryRequest;
import com.g42.platform.gms.warehouse.api.dto.response.StockEntryItemResponse;
import com.g42.platform.gms.warehouse.api.dto.response.StockEntryResponse;
import com.g42.platform.gms.warehouse.domain.entity.Inventory;
import com.g42.platform.gms.warehouse.domain.entity.StockEntry;
import com.g42.platform.gms.warehouse.domain.entity.StockEntryItem;
import com.g42.platform.gms.warehouse.domain.enums.InventoryTransactionType;
import com.g42.platform.gms.warehouse.domain.enums.StockEntryStatus;
import com.g42.platform.gms.warehouse.domain.repository.InventoryRepo;
import com.g42.platform.gms.warehouse.domain.repository.InventoryTransactionRepo;
import com.g42.platform.gms.warehouse.domain.repository.StockEntryRepo;
import com.g42.platform.gms.warehouse.domain.repository.WarehouseAttachmentRepo;
import com.g42.platform.gms.warehouse.infrastructure.entity.InventoryTransactionJpa;
import com.g42.platform.gms.warehouse.infrastructure.entity.WarehouseJpa;
import com.g42.platform.gms.warehouse.infrastructure.entity.WarehouseAttachmentJpa;
import com.g42.platform.gms.warehouse.infrastructure.repository.WarehouseJpaRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockEntryService {

    private static final String FOLDER_STOCK_ENTRY = "garage/warehouse/stock-entry";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final AtomicInteger SEQ = new AtomicInteger(0);

    private final StockEntryRepo stockEntryRepo;
    private final InventoryRepo inventoryRepo;
    private final InventoryTransactionRepo transactionRepo;
    private final ImageUploadService imageUploadService;
    private final WarehouseAttachmentRepo attachmentRepo;
    private final WarehouseJpaRepo warehouseJpaRepo;
    private final StaffProfileRepo staffProfileRepo;
    private final CatalogItemRepository catalogItemRepository;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Transactional(readOnly = true)
    public List<StockEntryResponse> listByWarehouse(Integer warehouseId, StockEntryStatus status) {
        List<StockEntry> entries = status != null
                ? stockEntryRepo.findByWarehouseIdAndStatus(warehouseId, status)
                : stockEntryRepo.findByWarehouseId(warehouseId);
        return entries.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<StockEntryResponse> searchByWarehouse(Integer warehouseId,
                                                      StockEntryStatus status,
                                                      LocalDate fromDate,
                                                      LocalDate toDate,
                                                      String search,
                                                      int page,
                                                      int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return stockEntryRepo.search(warehouseId, status, fromDate, toDate, search, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public StockEntryResponse getById(Integer entryId) {
        return toResponse(findOrThrow(entryId));
    }

    @Transactional
    public StockEntryResponse patchItem(Integer entryId, Integer entryItemId, PatchEntryItemRequest request) {
        StockEntry entry = findOrThrow(entryId);
        if (entry.getStatus() != StockEntryStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Chỉ có thể sửa phiếu ở trạng thái DRAFT");
        }
        StockEntryItem item = stockEntryRepo.findItemById(entryItemId)
                .filter(i -> i.getEntryId().equals(entryId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy item id=" + entryItemId + " trong phiếu id=" + entryId));

        if (request.getQuantity() != null) {
            item.setQuantity(request.getQuantity());
            item.setRemainingQuantity(request.getQuantity());
        }
        if (request.getImportPrice() != null) item.setImportPrice(request.getImportPrice());
        if (request.getMarkupMultiplier() != null) item.setMarkupMultiplier(request.getMarkupMultiplier());
        if (request.getNotes() != null) item.setNotes(request.getNotes());

        stockEntryRepo.saveItem(item);
        return toResponse(findOrThrow(entryId));
    }

    @Transactional
    public StockEntryResponse update(Integer entryId, UpdateStockEntryRequest request) {
        StockEntry entry = findOrThrow(entryId);
        if (entry.getStatus() != StockEntryStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Chỉ có thể sửa phiếu ở trạng thái DRAFT");
        }
        if (request.getSupplierName() != null) entry.setSupplierName(request.getSupplierName());
        if (request.getEntryDate() != null) entry.setEntryDate(request.getEntryDate());
        if (request.getNotes() != null) entry.setNotes(request.getNotes());

        if (request.getItems() != null) {
            List<StockEntryItem> newItems = new ArrayList<>();
            for (StockEntryItemRequest itemReq : request.getItems()) {
                newItems.add(StockEntryItem.builder()
                        .entryId(entry.getEntryId())
                        .itemId(itemReq.getItemId())
                        .quantity(itemReq.getQuantity())
                        .importPrice(itemReq.getImportPrice())
                        .markupMultiplier(itemReq.getMarkupMultiplier() != null
                                ? itemReq.getMarkupMultiplier() : BigDecimal.ONE)
                        .remainingQuantity(itemReq.getQuantity())
                        .notes(itemReq.getNotes())
                        .build());
            }
            entry.setItems(newItems);
        }
        return toResponse(stockEntryRepo.save(entry));
    }

    @Transactional
    public StockEntryResponse create(CreateStockEntryRequest request, Integer staffId) {
        StockEntry entry = StockEntry.builder()
                .entryCode(generateCode())
                .warehouseId(request.getWarehouseId())
                .supplierName(request.getSupplierName())
                .entryDate(request.getEntryDate() != null ? request.getEntryDate() : LocalDate.now())
                .notes(request.getNotes())
                .status(StockEntryStatus.DRAFT)
                .createdBy(staffId)
                .build();

        StockEntry saved = stockEntryRepo.save(entry);

        List<StockEntryItem> items = new ArrayList<>();
        for (StockEntryItemRequest itemReq : request.getItems()) {
            items.add(StockEntryItem.builder()
                    .entryId(saved.getEntryId())
                    .itemId(itemReq.getItemId())
                    .quantity(itemReq.getQuantity())
                    .importPrice(itemReq.getImportPrice())
                    .markupMultiplier(itemReq.getMarkupMultiplier() != null
                            ? itemReq.getMarkupMultiplier() : BigDecimal.ONE)
                    .remainingQuantity(itemReq.getQuantity())
                    .notes(itemReq.getNotes())
                    .build());
        }
        saved.setItems(items);
        return toResponse(stockEntryRepo.save(saved));
    }

    /**
     * Tạo phiếu + upload ảnh chứng từ trong 1 request multipart.
     */
    @Transactional
    public StockEntryResponse createWithAttachment(CreateStockEntryRequest request,
                                                    MultipartFile file,
                                                    Integer staffId) throws IOException {
        StockEntryResponse created = create(request, staffId);

        String url = imageUploadService.uploadImage(file, FOLDER_STOCK_ENTRY);
        WarehouseAttachmentJpa attachment = new WarehouseAttachmentJpa();
        attachment.setRefType(WarehouseAttachmentJpa.RefType.STOCK_ENTRY);
        attachment.setRefId(created.getEntryId());
        attachment.setFileUrl(url);
        attachment.setUploadedBy(staffId);
        attachmentRepo.save(attachment);

        return toResponse(findOrThrow(created.getEntryId()));
    }

    /**
     * Tạo phiếu + ảnh qua @ModelAttribute form.
     */
    @Transactional
    public StockEntryResponse createWithAttachmentForm(CreateStockEntryWithAttachmentRequest req,
                                                        Integer staffId) throws IOException {
        List<StockEntryItemRequest> items;
        try {
            items = objectMapper.readValue(req.getItems(),
                    new TypeReference<List<StockEntryItemRequest>>() {});
        } catch (Exception e) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "items không hợp lệ: " + e.getMessage());
        }

        CreateStockEntryRequest request = new CreateStockEntryRequest();
        request.setWarehouseId(req.getWarehouseId());
        request.setSupplierName(req.getSupplierName());
        request.setNotes(req.getNotes());
        request.setEntryDate(req.getEntryDate() != null && !req.getEntryDate().isBlank()
                ? LocalDate.parse(req.getEntryDate()) : LocalDate.now());
        request.setItems(items);

        return createWithAttachment(request, req.getFile(), staffId);
    }

    @Transactional
    public void addAttachment(Integer entryId, MultipartFile file, Integer staffId) throws IOException {
        StockEntry entry = findOrThrow(entryId);
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

    @Transactional
    public StockEntryResponse confirm(Integer entryId, Integer staffId) {
        StockEntry entry = findOrThrow(entryId);

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

        for (StockEntryItem item : entry.getItems()) {
            Inventory inv = inventoryRepo
                    .findByWarehouseAndItemWithLock(entry.getWarehouseId(), item.getItemId())
                    .orElseGet(() -> Inventory.builder()
                            .warehouseId(entry.getWarehouseId())
                            .itemId(item.getItemId())
                            .quantity(0)
                            .reservedQuantity(0)
                            .build());

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

    public BigDecimal findLatesFallBackPrice(Integer itemId, Integer warehouseId) {
        return stockEntryRepo.findLatesFallBackPrice(itemId, warehouseId);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private StockEntry findOrThrow(Integer entryId) {
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

    private StockEntryResponse toResponse(StockEntry e) {
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

        if (e.getWarehouseId() != null) {
            WarehouseJpa warehouse = warehouseJpaRepo.findById(e.getWarehouseId()).orElse(null);
            if (warehouse != null) {
                r.setWarehouseCode(warehouse.getWarehouseCode());
                r.setWarehouseName(warehouse.getWarehouseName());
            }
        }

        if (e.getCreatedBy() != null) {
            StaffProfile createdBy = staffProfileRepo.findById(e.getCreatedBy()).orElse(null);
            if (createdBy != null) {
                r.setCreatedByName(createdBy.getFullName());
            }
        }

        if (e.getConfirmedBy() != null) {
            StaffProfile confirmedBy = staffProfileRepo.findById(e.getConfirmedBy()).orElse(null);
            if (confirmedBy != null) {
                r.setConfirmedByName(confirmedBy.getFullName());
            }
        }

        Set<Integer> itemIds = e.getItems().stream()
                .map(StockEntryItem::getItemId)
                .collect(Collectors.toSet());
        Map<Integer, String> itemNameById = catalogItemRepository.findAllById(itemIds).stream()
                .collect(Collectors.toMap(CatalogItemJpaEntity::getItemId, CatalogItemJpaEntity::getItemName));

        List<StockEntryItemResponse> itemResponses = e.getItems().stream().map(i -> {
            StockEntryItemResponse ir = new StockEntryItemResponse();
            ir.setEntryItemId(i.getEntryItemId());
            ir.setItemId(i.getItemId());
            ir.setItemName(itemNameById.get(i.getItemId()));
            ir.setQuantity(i.getQuantity());
            ir.setImportPrice(i.getImportPrice());
            ir.setMarkupMultiplier(i.getMarkupMultiplier());
            ir.setRemainingQuantity(i.getRemainingQuantity());
            ir.setNotes(i.getNotes());
            return ir;
        }).collect(Collectors.toList());
        r.setItems(itemResponses);

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
