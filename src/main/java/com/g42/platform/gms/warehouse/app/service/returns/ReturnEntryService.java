package com.g42.platform.gms.warehouse.app.service.returns;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.g42.platform.gms.auth.entity.StaffProfile;
import com.g42.platform.gms.auth.repository.StaffProfileRepo;
import com.g42.platform.gms.common.service.ImageUploadService;
import com.g42.platform.gms.estimation.api.internal.EstimateInternalApi;
import com.g42.platform.gms.warehouse.api.dto.request.CreateReturnEntryFormRequest;
import com.g42.platform.gms.warehouse.api.dto.request.CreateReturnEntryRequest;
import com.g42.platform.gms.warehouse.api.dto.request.ReturnEntryItemRequest;
import com.g42.platform.gms.warehouse.api.dto.request.PatchReturnItemRequest;
import com.g42.platform.gms.warehouse.api.dto.request.UpdateReturnEntryRequest;
import com.g42.platform.gms.warehouse.api.dto.response.ReturnEntryItemResponse;
import com.g42.platform.gms.warehouse.api.dto.response.ReturnEntryResponse;
import com.g42.platform.gms.warehouse.domain.enums.InventoryTransactionType;
import com.g42.platform.gms.warehouse.domain.enums.ReturnEntryStatus;
import com.g42.platform.gms.warehouse.domain.enums.ReturnType;
import com.g42.platform.gms.warehouse.domain.repository.InventoryRepo;
import com.g42.platform.gms.warehouse.domain.repository.InventoryTransactionRepo;
import com.g42.platform.gms.warehouse.domain.repository.ReturnEntryRepo;
import com.g42.platform.gms.warehouse.domain.repository.StockIssueRepo;
import com.g42.platform.gms.warehouse.domain.repository.WarehouseAttachmentRepo;
import com.g42.platform.gms.warehouse.domain.repository.PartCatalogRepo;
import com.g42.platform.gms.warehouse.domain.entity.Inventory;
import com.g42.platform.gms.warehouse.domain.entity.InventoryTransaction;
import com.g42.platform.gms.warehouse.domain.entity.ReturnEntry;
import com.g42.platform.gms.warehouse.domain.entity.ReturnEntryItem;
import com.g42.platform.gms.warehouse.domain.entity.StockIssue;
import com.g42.platform.gms.warehouse.domain.entity.StockIssueItem;
import com.g42.platform.gms.warehouse.domain.entity.StockAllocation;
import com.g42.platform.gms.warehouse.domain.entity.StockEntry;
import com.g42.platform.gms.warehouse.domain.entity.StockEntryItem;
import com.g42.platform.gms.warehouse.domain.entity.Warehouse;
import com.g42.platform.gms.warehouse.domain.entity.WarehouseAttachment;
import com.g42.platform.gms.warehouse.domain.enums.AllocationStatus;
import com.g42.platform.gms.warehouse.domain.repository.WarehouseRepo;
import com.g42.platform.gms.estimation.domain.repository.EstimateItemRepository;
import com.g42.platform.gms.estimation.domain.entity.EstimateItem;
import com.g42.platform.gms.warehouse.domain.repository.StockAllocationRepo;
import com.g42.platform.gms.service_ticket_management.domain.repository.ServiceTicketRepo;
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

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReturnEntryService {

    private static final String FOLDER_RETURN_ENTRY = "garage/warehouse/return-entry";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final AtomicInteger SEQ = new AtomicInteger(0);

    private final ReturnEntryRepo returnEntryRepo;
    private final InventoryRepo inventoryRepo;
    private final InventoryTransactionRepo transactionRepo;
    private final WarehouseAttachmentRepo attachmentRepo;
    private final ImageUploadService imageUploadService;
    private final WarehouseRepo warehouseRepo;
    private final StockIssueRepo stockIssueRepo;
    private final com.g42.platform.gms.warehouse.domain.repository.StockIssueItemRepo stockIssueItemRepo;
    private final StockAllocationRepo stockAllocationRepo;
    private final com.g42.platform.gms.warehouse.domain.repository.StockEntryRepo stockEntryRepo;
    private final StaffProfileRepo staffProfileRepo;
    private final PartCatalogRepo partCatalogRepo;
    private final EstimateItemRepository estimateItemRepository;
    private final ServiceTicketRepo serviceTicketRepo;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final EstimateInternalApi estimateInternalApi;

    @Transactional
    public ReturnEntryResponse create(CreateReturnEntryRequest request, Integer staffId) {
        validateSourceIssueConfirmed(request.getSourceIssueId());

        ReturnEntry entry = new ReturnEntry();
        entry.setReturnCode(generateCode());
        entry.setWarehouseId(request.getWarehouseId());
        entry.setReturnReason(request.getReturnReason());
        entry.setSourceIssueId(request.getSourceIssueId());
        entry.setReturnType(request.getReturnType() != null
                ? request.getReturnType() : ReturnType.CUSTOMER_RETURN);
        entry.setStatus(ReturnEntryStatus.SUBMITTED);
        entry.setCreatedBy(staffId);

        ReturnEntry saved = returnEntryRepo.save(entry);

        for (ReturnEntryItemRequest itemReq : request.getItems()) {
            validateAllocation(request.getSourceIssueId(), itemReq);
            resolveIssueItemAndEntryFromAllocation(itemReq);
            validateEntryItem(request.getWarehouseId(), itemReq);
            validateDuplicateAllocationOnCreate(itemReq.getAllocationId());

            ReturnEntryItem item = new ReturnEntryItem();
            item.setReturnId(saved.getReturnId());
            item.setItemId(itemReq.getItemId());
            item.setAllocationId(itemReq.getAllocationId());
            item.setSourceIssueItemId(itemReq.getSourceIssueItemId());
            item.setEntryItemId(itemReq.getEntryItemId());
            item.setQuantity(itemReq.getQuantity());
            item.setConditionNote(itemReq.getConditionNote());
            item.setExchangeItem(false);
            saved.getItems().add(item);
        }

        if (request.getExchangeItems() != null) {
            for (ReturnEntryItemRequest itemReq : request.getExchangeItems()) {
                validateEntryItem(request.getWarehouseId(), itemReq);
                ReturnEntryItem item = new ReturnEntryItem();
                item.setReturnId(saved.getReturnId());
                item.setItemId(itemReq.getItemId());
                item.setAllocationId(itemReq.getAllocationId());
                item.setEntryItemId(itemReq.getEntryItemId());
                item.setQuantity(itemReq.getQuantity());
                item.setConditionNote(null);
                item.setExchangeItem(true);
                saved.getItems().add(item);
            }
        }

        return toResponse(returnEntryRepo.save(saved));
    }

    @Transactional
    public ReturnEntryResponse createWithAttachments(CreateReturnEntryFormRequest req, Integer staffId) throws IOException {
        List<ReturnEntryItemRequest> items;
        try {
            items = objectMapper.readValue(req.getItems(),
                    new TypeReference<List<ReturnEntryItemRequest>>() {});
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "items không hợp lệ: " + e.getMessage());
        }

        CreateReturnEntryRequest request = new CreateReturnEntryRequest();
        request.setWarehouseId(req.getWarehouseId());
        request.setReturnReason(req.getReturnReason());
        request.setSourceIssueId(req.getSourceIssueId());
        request.setReturnType(req.getReturnType() != null ? req.getReturnType() : ReturnType.CUSTOMER_RETURN);
        request.setItems(items);

        if (req.getExchangeItems() != null && !req.getExchangeItems().isBlank()) {
            try {
                request.setExchangeItems(objectMapper.readValue(req.getExchangeItems(),
                        new TypeReference<List<ReturnEntryItemRequest>>() {}));
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "exchangeItems không hợp lệ: " + e.getMessage());
            }
        }

        List<MultipartFile> files = new java.util.ArrayList<>();
        // Gom từ List<files>
        if (req.getFiles() != null) {
            req.getFiles().stream().filter(f -> f != null && !f.isEmpty()).forEach(files::add);
        }
        // Gom từ file_0..file_4 (backward compatible)
        java.util.stream.Stream.of(req.getFile_0(), req.getFile_1(), req.getFile_2(), req.getFile_3(), req.getFile_4())
                .filter(f -> f != null && !f.isEmpty())
                .forEach(files::add);

        if (files.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Cần đính kèm ít nhất 1 ảnh chứng minh");
        }

        ReturnEntryResponse created = create(request, staffId);

        // Lấy item đầu tiên (không phải exchange) để gắn tất cả ảnh vào
        ReturnEntry saved = findOrThrow(created.getReturnId());
        ReturnEntryItem firstItem = saved.getItems().stream()
                .filter(i -> !i.isExchangeItem())
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Không tìm thấy item để gắn ảnh"));

        for (MultipartFile file : files) {
            String url = imageUploadService.uploadImage(file, FOLDER_RETURN_ENTRY);
            WarehouseAttachment attachment = new WarehouseAttachment();
            attachment.setRefType(WarehouseAttachment.RefType.RETURN_ENTRY_ITEM);
            attachment.setRefId(firstItem.getReturnItemId());
            attachment.setFileUrl(url);
            attachment.setUploadedBy(staffId);
            attachmentRepo.save(attachment);
        }

        return toResponse(findOrThrow(created.getReturnId()));
    }
    @Transactional
    public void addAttachment(Integer returnItemId, MultipartFile file, Integer staffId) throws IOException {
        ReturnEntryItem item = returnEntryRepo.findItemById(returnItemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy return_entry_item id=" + returnItemId));

        ReturnEntry entry = findOrThrow(item.getReturnId());
        if (entry.getStatus() == ReturnEntryStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Không thể thêm ảnh cho phiếu đã xác nhận");
        }

        String url = imageUploadService.uploadImage(file, FOLDER_RETURN_ENTRY);

        WarehouseAttachment attachment = new WarehouseAttachment();
        attachment.setRefType(WarehouseAttachment.RefType.RETURN_ENTRY_ITEM);
        attachment.setRefId(returnItemId);
        attachment.setFileUrl(url);
        attachment.setUploadedBy(staffId);
        attachmentRepo.save(attachment);
    }
    @Transactional(readOnly = true)
    public List<ReturnEntryResponse> listByWarehouse(Integer warehouseId) {
        return returnEntryRepo.findByWarehouseId(warehouseId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<ReturnEntryResponse> searchByWarehouse(Integer warehouseId,
                                                       ReturnEntryStatus status,
                                                       ReturnType returnType,
                                                       LocalDate fromDate,
                                                       LocalDate toDate,
                                                       String search,
                                                       int page,
                                                       int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return returnEntryRepo.search(warehouseId, status, returnType, fromDate, toDate, search, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ReturnEntryResponse getDetail(Integer returnId) {
        return toResponse(findOrThrow(returnId));
    }

    @Transactional
    public ReturnEntryResponse patchItem(Integer returnId, Integer returnItemId, PatchReturnItemRequest request) {
        ReturnEntry entry = findOrThrow(returnId);
        if (entry.getStatus() != ReturnEntryStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Chỉ có thể sửa phiếu ở trạng thái DRAFT");
        }
        ReturnEntryItem item = returnEntryRepo.findItemById(returnItemId)
                .filter(i -> i.getReturnId().equals(returnId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy item id=" + returnItemId + " trong phiếu id=" + returnId));

        if (request.getQuantity() != null) item.setQuantity(request.getQuantity());
        if (request.getConditionNote() != null) item.setConditionNote(request.getConditionNote());

        returnEntryRepo.saveItem(item);
        return toResponse(findOrThrow(returnId));
    }

    @Transactional
    public ReturnEntryResponse update(Integer returnId, UpdateReturnEntryRequest request) {
        ReturnEntry entry = findOrThrow(returnId);
        if (entry.getStatus() != ReturnEntryStatus.SUBMITTED) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Chỉ có thể sửa phiếu ở trạng thái SUBMITTED");
        }
        validateSourceIssueConfirmed(entry.getSourceIssueId());
        if (request.getReturnReason() != null) entry.setReturnReason(request.getReturnReason());

        if (request.getItems() != null || request.getExchangeItems() != null) {
            entry.getItems().clear();
            if (request.getItems() != null) {
                for (ReturnEntryItemRequest itemReq : request.getItems()) {
                    validateAllocation(entry.getSourceIssueId(), itemReq);
                    resolveIssueItemAndEntryFromAllocation(itemReq);
                    validateEntryItem(entry.getWarehouseId(), itemReq);
                    validateDuplicateAllocationOnUpdate(itemReq.getAllocationId(), returnId);

                    ReturnEntryItem item = new ReturnEntryItem();
                    item.setReturnId(returnId);
                    item.setItemId(itemReq.getItemId());
                    item.setAllocationId(itemReq.getAllocationId());
                    item.setSourceIssueItemId(itemReq.getSourceIssueItemId());
                    item.setEntryItemId(itemReq.getEntryItemId());
                    item.setQuantity(itemReq.getQuantity());
                    item.setConditionNote(itemReq.getConditionNote());
                    item.setExchangeItem(false);
                    entry.getItems().add(item);
                }
            }
            if (request.getExchangeItems() != null) {
                for (ReturnEntryItemRequest itemReq : request.getExchangeItems()) {
                    validateEntryItem(entry.getWarehouseId(), itemReq);
                    ReturnEntryItem item = new ReturnEntryItem();
                    item.setReturnId(returnId);
                    item.setItemId(itemReq.getItemId());
                    item.setAllocationId(itemReq.getAllocationId());
                    item.setEntryItemId(itemReq.getEntryItemId());
                    item.setQuantity(itemReq.getQuantity());
                    item.setConditionNote(null);
                    item.setExchangeItem(true);
                    entry.getItems().add(item);
                }
            }
        }
        return toResponse(returnEntryRepo.save(entry));
    }

    /**
     * Hủy phiếu hoàn khi chưa confirm (SUBMITTED).
     * Phiếu hoàn chưa confirm chưa tác động gì đến inventory nên chỉ cần đổi status.
     */
    @Transactional
    public ReturnEntryResponse cancel(Integer returnId, Integer staffId) {
        ReturnEntry entry = findOrThrow(returnId);

        if (entry.getStatus() == ReturnEntryStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Không thể hủy phiếu hoàn đã được xác nhận");
        }

        if (entry.getStatus() == ReturnEntryStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phiếu hoàn đã bị hủy");
        }

        entry.setStatus(ReturnEntryStatus.CANCELLED);
        return toResponse(returnEntryRepo.save(entry));
    }

    @Transactional
    public ReturnEntryResponse confirm(Integer returnId, Integer staffId) {
        ReturnEntry entry = findOrThrow(returnId);

        if (entry.getStatus() != ReturnEntryStatus.SUBMITTED) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Chỉ có thể confirm phiếu ở trạng thái SUBMITTED");
        }

        List<ReturnEntryItem> returnItems = entry.getItems().stream()
                .filter(i -> !i.isExchangeItem()).collect(Collectors.toList());
        List<ReturnEntryItem> exchangeItems = entry.getItems().stream()
                .filter(ReturnEntryItem::isExchangeItem).collect(Collectors.toList());

        if (returnItems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Phiếu hoàn không có sản phẩm nào");
        }

        ReturnType type = entry.getReturnType();

        for (ReturnEntryItem item : returnItems) {
            Inventory inv = getOrCreateInventory(entry.getWarehouseId(), item.getItemId());

            int newQty;
            InventoryTransactionType txType;

            if (type == ReturnType.SUPPLIER_RETURN) {
                newQty = Math.max(0, inv.getQuantity() - item.getQuantity());
                txType = InventoryTransactionType.OUT;
            } else {
                newQty = inv.getQuantity() + item.getQuantity();
                txType = InventoryTransactionType.IN;
            }

            inv.setQuantity(newQty);
            inventoryRepo.save(inv);
            saveTransaction(entry.getWarehouseId(), item.getItemId(), item.getEntryItemId(), txType,
                    item.getQuantity(), newQty, returnId, staffId);

            if (item.getEntryItemId() != null) {
                stockEntryRepo.increaseRemainingQuantity(item.getEntryItemId(), item.getQuantity());
            }

            if (item.getAllocationId() != null) {
                Integer savedEstimateItemId = estimateInternalApi.releaseEstimate(item.getAllocationId(), item.getQuantity(), staffId);
                releaseAllocation(item.getAllocationId(), item.getQuantity(), staffId,savedEstimateItemId);
            }
        }

        if (type == ReturnType.EXCHANGE) {
            for (ReturnEntryItem item : exchangeItems) {
                Inventory inv = getOrCreateInventory(entry.getWarehouseId(), item.getItemId());
                int newQty = Math.max(0, inv.getQuantity() - item.getQuantity());
                inv.setQuantity(newQty);
                inventoryRepo.save(inv);
                saveTransaction(entry.getWarehouseId(), item.getItemId(), item.getEntryItemId(),
                        InventoryTransactionType.OUT, item.getQuantity(), newQty, returnId, staffId);
                if (item.getEntryItemId() != null) {
                    stockEntryRepo.increaseRemainingQuantity(item.getEntryItemId(), item.getQuantity());
                }
            }
        }

        entry.setStatus(ReturnEntryStatus.CONFIRMED);
        entry.setConfirmedBy(staffId);
        entry.setConfirmedAt(LocalDateTime.now());

        return toResponse(returnEntryRepo.save(entry));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Inventory getOrCreateInventory(Integer warehouseId, Integer itemId) {
        return inventoryRepo.findByWarehouseAndItemWithLock(warehouseId, itemId)
                .orElseGet(() -> Inventory.builder()
                        .warehouseId(warehouseId)
                        .itemId(itemId)
                        .quantity(0)
                        .reservedQuantity(0)
                        .build());
    }

    private void saveTransaction(Integer warehouseId, Integer itemId, Integer entryItemId,
                                 InventoryTransactionType type, int qty, int balance,
                                 Integer returnId, Integer staffId) {
        InventoryTransaction tx = new InventoryTransaction();
        tx.setWarehouseId(warehouseId);
        tx.setItemId(itemId);
        tx.setEntryItemId(entryItemId);
        tx.setTransactionType(type);
        tx.setQuantity(qty);
        tx.setBalanceAfter(balance);
        tx.setReferenceType("return_entry");
        tx.setReferenceId(returnId);
        tx.setCreatedById(staffId);
        tx.setCreatedAt(Instant.now());
        transactionRepo.save(tx);
    }

    private void releaseAllocation(Integer allocationId, Integer returnQuantity, Integer staffId, Integer savedEstimateItemId) {
        StockAllocation allocation = stockAllocationRepo.findById(allocationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy allocation id=" + allocationId));

        if (allocation.getStatus() != AllocationStatus.COMMITTED) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Chỉ được hoàn khi allocation đã COMMITTED");
        }

        if (allocation.getQuantity() == null || allocation.getQuantity() < returnQuantity) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Số lượng hoàn vượt quá allocation đã chọn");
        }

        if (allocation.getQuantity().equals(returnQuantity)) {
            allocation.setStatus(AllocationStatus.RELEASED);
            stockAllocationRepo.save(allocation);
            return;
        }

        allocation.setQuantity(allocation.getQuantity() - returnQuantity);
        stockAllocationRepo.save(allocation);

        StockAllocation released = new StockAllocation();
        released.setServiceTicketId(allocation.getServiceTicketId());

        released.setEstimateItemId(savedEstimateItemId);
        Integer estimateId = allocation.getEstimateId();
        if (estimateId == null && allocation.getEstimateItemId() != null) {
            EstimateItem estimateItem = estimateItemRepository.findByEstimateItemId(allocation.getEstimateItemId());
            if (estimateItem != null) {
                estimateId = estimateItem.getEstimateId();
            }
        }
        released.setEstimateId(estimateId);
        released.setWarehouseId(allocation.getWarehouseId());
        released.setItemId(allocation.getItemId());
        released.setIssueId(allocation.getIssueId());
        released.setQuantity(returnQuantity);

        released.setStatus(AllocationStatus.RELEASED);
        released.setCreatedBy(allocation.getCreatedBy() != null ? allocation.getCreatedBy() : staffId);
        StockAllocation stockAllocation = stockAllocationRepo.save(released);
        System.out.println("DEBUG: "+stockAllocation.getEstimateItemId());
    }

    private ReturnEntry findOrThrow(Integer returnId) {
        return returnEntryRepo.findById(returnId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy phiếu hàng trả id=" + returnId));
    }

    private String generateCode() {
        String date = LocalDate.now().format(DATE_FMT);
        int seq = SEQ.incrementAndGet();
        String candidate = String.format("TH-%s-%d", date, seq);
        while (returnEntryRepo.existsByCode(candidate)) {
            candidate = String.format("TH-%s-%d", date, SEQ.incrementAndGet());
        }
        return candidate;
    }

    private ReturnEntryResponse toResponse(ReturnEntry e) {
        ReturnEntryResponse r = new ReturnEntryResponse();
        r.setReturnId(e.getReturnId());
        r.setReturnCode(e.getReturnCode());
        r.setWarehouseId(e.getWarehouseId());
        r.setReturnReason(e.getReturnReason());
        r.setReturnType(e.getReturnType());
        r.setSourceIssueId(e.getSourceIssueId());
        r.setStatus(e.getStatus());
        r.setConfirmedBy(e.getConfirmedBy());
        r.setConfirmedAt(e.getConfirmedAt());
        r.setCreatedBy(e.getCreatedBy());
        r.setCreatedAt(e.getCreatedAt());

        if (e.getWarehouseId() != null) {
            Warehouse warehouse = warehouseRepo.findById(e.getWarehouseId()).orElse(null);
            if (warehouse != null) {
                r.setWarehouseCode(warehouse.getWarehouseCode());
                r.setWarehouseName(warehouse.getWarehouseName());
            }
        }

        if (e.getSourceIssueId() != null) {
            stockIssueRepo.findById(e.getSourceIssueId()).ifPresent(sourceIssue -> {
                r.setSourceIssueCode(sourceIssue.getIssueCode());
                // Lấy serviceTicketId/Code từ phiếu xuất nguồn
                if (sourceIssue.getServiceTicketId() != null) {
                    r.setServiceTicketId(sourceIssue.getServiceTicketId());
                    com.g42.platform.gms.service_ticket_management.domain.entity.ServiceTicket ticket =
                            serviceTicketRepo.findByServiceTicketId(sourceIssue.getServiceTicketId());
                    if (ticket != null) {
                        r.setServiceTicketCode(ticket.getTicketCode());
                    }
                }
            });
        }

        if (e.getCreatedBy() != null) {
            StaffProfile createdBy = staffProfileRepo.findById(e.getCreatedBy()).orElse(null);
            if (createdBy != null) r.setCreatedByName(createdBy.getFullName());
        }

        if (e.getConfirmedBy() != null) {
            StaffProfile confirmedBy = staffProfileRepo.findById(e.getConfirmedBy()).orElse(null);
            if (confirmedBy != null) r.setConfirmedByName(confirmedBy.getFullName());
        }

        Set<Integer> itemIds = e.getItems().stream()
                .map(ReturnEntryItem::getItemId)
                .collect(Collectors.toSet());
        Map<Integer, String> itemNameById = partCatalogRepo.findNamesByIds(itemIds.stream().toList());

        // Lấy giá từ stock_issue_item để hiển thị unitPrice
        Map<Integer, java.math.BigDecimal> priceByIssueItemId = new java.util.HashMap<>();
        Map<Integer, String> issueItemCodeById = new java.util.HashMap<>();
        if (e.getSourceIssueId() != null) {
            stockIssueItemRepo.findByIssueId(e.getSourceIssueId()).forEach(si -> {
                if (si.getIssueItemId() != null) {
                    priceByIssueItemId.put(si.getIssueItemId(), si.getFinalPrice());
                }
            });
        }

        r.setItems(e.getItems().stream().map(i -> {
            ReturnEntryItemResponse ir = new ReturnEntryItemResponse();
            ir.setReturnItemId(i.getReturnItemId());
            ir.setItemId(i.getItemId());
            ir.setItemName(itemNameById.get(i.getItemId()));
            ir.setAllocationId(i.getAllocationId());
            ir.setSourceIssueItemId(i.getSourceIssueItemId());
            ir.setEntryItemId(i.getEntryItemId());
            ir.setQuantity(i.getQuantity());
            ir.setConditionNote(i.getConditionNote());

            // Enrich giá từ issue item
            if (i.getSourceIssueItemId() != null) {
                java.math.BigDecimal unitPrice = priceByIssueItemId.get(i.getSourceIssueItemId());
                ir.setUnitPrice(unitPrice);
                if (unitPrice != null && i.getQuantity() != null) {
                    ir.setTotalPrice(unitPrice.multiply(java.math.BigDecimal.valueOf(i.getQuantity())));
                }
            }

            // Enrich thông tin lô nhập
            if (i.getEntryItemId() != null) {
                stockEntryRepo.findItemById(i.getEntryItemId()).ifPresent(entryItem -> {
                    stockEntryRepo.findEntryById(entryItem.getEntryId()).ifPresent(entry -> {
                        ir.setEntryCode(entry.getEntryCode());
                        ir.setEntryLotCode(entry.getEntryCode() + "-LOT" + entryItem.getEntryItemId());
                    });
                });
            }

            ir.setAttachmentUrls(
                    attachmentRepo.findByRefTypeAndRefId(
                                    WarehouseAttachment.RefType.RETURN_ENTRY_ITEM,
                                    i.getReturnItemId())
                            .stream()
                            .map(WarehouseAttachment::getFileUrl)
                            .collect(Collectors.toList())
            );
            return ir;
        }).collect(Collectors.toList()));

        return r;
    }

    private void validateAllocation(Integer sourceIssueId, ReturnEntryItemRequest itemReq) {
        if (sourceIssueId == null && itemReq.getAllocationId() == null) {
            return;
        }

        if (sourceIssueId == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "sourceIssueId là bắt buộc khi truyền allocationId");
        }

        if (itemReq.getAllocationId() == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "allocationId là bắt buộc khi có sourceIssueId");
        }

        StockIssue sourceIssue = stockIssueRepo.findById(sourceIssueId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Không tìm thấy phiếu xuất nguồn id=" + sourceIssueId));

        StockAllocation allocation = stockAllocationRepo.findById(itemReq.getAllocationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Không tìm thấy allocation id=" + itemReq.getAllocationId()));

        if (allocation.getIssueId() != null && !allocation.getIssueId().equals(sourceIssueId)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "allocationId không thuộc phiếu xuất nguồn đã chọn");
        }

        if (allocation.getItemId() == null || !allocation.getItemId().equals(itemReq.getItemId())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "allocationId không khớp với itemId");
        }

        if (allocation.getStatus() != AllocationStatus.COMMITTED) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "allocationId phải ở trạng thái COMMITTED");
        }

        // Kiểm tra tổng quantity đã hoàn (active) không vượt quá tổng quantity gốc của allocation
        // Khi hoàn một phần, allocation bị tách: qty giảm xuống, tạo allocation mới RELEASED.
        // Tổng gốc = qty hiện tại + tổng đã hoàn active trước đó.
        int alreadyReturned = returnEntryRepo.sumActiveReturnedQuantityByAllocationId(itemReq.getAllocationId());
        int requestedQty = itemReq.getQuantity() != null ? itemReq.getQuantity() : 0;
        int originalQty = allocation.getQuantity() + alreadyReturned; // tổng qty gốc
        if (alreadyReturned + requestedQty > originalQty) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Tổng số lượng hoàn (" + (alreadyReturned + requestedQty) + ") vượt quá tổng allocation ("
                            + originalQty + ")");
        }

    }

    private void validateEntryItem(Integer warehouseId, ReturnEntryItemRequest itemReq) {
        if (itemReq.getEntryItemId() == null) {
            return;
        }

        StockEntryItem lot = stockEntryRepo.findItemById(itemReq.getEntryItemId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Không tìm thấy lô nhập id=" + itemReq.getEntryItemId()));

        if (lot.getItemId() == null || !lot.getItemId().equals(itemReq.getItemId())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "entryItemId không khớp với itemId");
        }

        if (lot.getEntryId() == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Không xác định được phiếu nhập của lô đã chọn");
        }

        StockEntry entry = stockEntryRepo.findEntryById(lot.getEntryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Không tìm thấy phiếu nhập của lô id=" + itemReq.getEntryItemId()));

        if (entry.getWarehouseId() == null || !entry.getWarehouseId().equals(warehouseId)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "entryItemId không thuộc kho đã chọn");
        }
    }

    private void resolveIssueItemAndEntryFromAllocation(ReturnEntryItemRequest itemReq) {
        if (itemReq.getAllocationId() == null) {
            return;
        }

        StockAllocation allocation = stockAllocationRepo.findById(itemReq.getAllocationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Không tìm thấy allocation id=" + itemReq.getAllocationId()));

        // Ưu tiên issueId từ allocation; fallback sang sourceIssueId từ request (allocation cũ không có issueId)
        Integer issueId = allocation.getIssueId();
        if (issueId == null) {
            issueId = itemReq.getSourceIssueId();
        }

        if (issueId == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Không xác định được phiếu xuất cho allocation=" + itemReq.getAllocationId()
                            + ". Vui lòng truyền sourceIssueId");
        }

        List<StockIssueItem> issueItems = stockIssueItemRepo.findByIssueId(issueId);
        StockIssueItem matchedIssueItem = null;

        for (StockIssueItem issueItem : issueItems) {
            if (issueItem.getItemId() == null || !issueItem.getItemId().equals(itemReq.getItemId())) {
                continue;
            }

            if (itemReq.getEntryItemId() != null) {
                if (issueItem.getEntryItemId() == null || !issueItem.getEntryItemId().equals(itemReq.getEntryItemId())) {
                    continue;
                }
            }

            if (matchedIssueItem != null) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "allocation này map tới nhiều issueItem/lô, vui lòng truyền entryItemId rõ ràng");
            }
            matchedIssueItem = issueItem;
        }

        if (matchedIssueItem == null) {
            // Fallback: allocation cũ có thể không còn issue item (đã bị xóa/thay thế).
            // Thử tìm bất kỳ issue item nào của itemId trong issueId đó.
            matchedIssueItem = issueItems.stream()
                    .filter(i -> itemReq.getItemId().equals(i.getItemId()))
                    .findFirst()
                    .orElse(null);
        }

        if (matchedIssueItem == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Không tìm thấy dòng xuất phù hợp từ allocation=" + itemReq.getAllocationId());
        }

        Integer resolvedIssueItemId = matchedIssueItem.getIssueItemId();
        if (resolvedIssueItemId == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Không xác định được issueItemId từ allocation đã chọn");
        }

        Integer resolvedEntryItemId = matchedIssueItem.getEntryItemId();
        if (resolvedEntryItemId == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Không xác định được entryItemId từ lô đã chọn");
        }

        itemReq.setSourceIssueItemId(resolvedIssueItemId);
        itemReq.setEntryItemId(resolvedEntryItemId);
    }

    private void validateRequiredAttachments(List<ReturnEntryItemRequest> items, MultipartFile[] files) {
        if (items == null || items.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "items không được rỗng");
        }

        if (items.size() > files.length) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Hiện chỉ hỗ trợ tối đa 5 ảnh (file_0..file_4) cho 5 dòng hàng trả");
        }

        for (int i = 0; i < items.size(); i++) {
            MultipartFile file = files[i];
            if (file == null || file.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Thiếu ảnh chứng minh cho item index " + i + " (file_" + i + ")");
            }
        }
    }

    private void validateSourceIssueConfirmed(Integer sourceIssueId) {
        if (sourceIssueId == null) {
            return;
        }

        StockIssue sourceIssue = stockIssueRepo.findById(sourceIssueId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Không tìm thấy phiếu xuất nguồn id=" + sourceIssueId));

        if (sourceIssue.getStatus() != com.g42.platform.gms.warehouse.domain.enums.StockIssueStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Chỉ được hoàn khi phiếu xuất nguồn đã CONFIRMED");
        }
    }

    private void validateDuplicateSourceIssueItemOnCreate(Integer sourceIssueItemId) {
        if (sourceIssueItemId == null) {
            return;
        }

        if (returnEntryRepo.existsAnyBySourceIssueItemId(sourceIssueItemId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Dòng xuất này đã có phiếu hoàn, không thể tạo trùng");
        }
    }

    private void validateDuplicateSourceIssueItemOnUpdate(Integer sourceIssueItemId, Integer returnId) {
        if (sourceIssueItemId == null) {
            return;
        }

        if (returnEntryRepo.existsAnyBySourceIssueItemIdExcludingReturnId(sourceIssueItemId, returnId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Dòng xuất này đã có phiếu hoàn khác, không thể cập nhật trùng");
        }
    }

    private void validateDuplicateAllocationOnCreate(Integer allocationId) {
        // Không block tạo nhiều phiếu hoàn cho cùng allocationId.
        // Việc kiểm soát số lượng đã được xử lý trong validateAllocation().
    }

    private void validateDuplicateAllocationOnUpdate(Integer allocationId, Integer returnId) {
        // Không block update nhiều phiếu hoàn cho cùng allocationId.
        // Việc kiểm soát số lượng đã được xử lý trong validateAllocation().
    }
}
