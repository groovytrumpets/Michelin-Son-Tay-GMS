package com.g42.platform.gms.warehouse.app.service.returns;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.g42.platform.gms.common.service.ImageUploadService;
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
import com.g42.platform.gms.warehouse.domain.repository.WarehouseAttachmentRepo;
import com.g42.platform.gms.warehouse.infrastructure.entity.InventoryJpa;
import com.g42.platform.gms.warehouse.infrastructure.entity.InventoryTransactionJpa;
import com.g42.platform.gms.warehouse.infrastructure.entity.ReturnEntryItemJpa;
import com.g42.platform.gms.warehouse.infrastructure.entity.ReturnEntryJpa;
import com.g42.platform.gms.warehouse.infrastructure.entity.WarehouseAttachmentJpa;
import lombok.RequiredArgsConstructor;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReturnEntryServiceImpl implements ReturnEntryService {

    private static final String FOLDER_RETURN_ENTRY = "garage/warehouse/return-entry";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final AtomicInteger SEQ = new AtomicInteger(0);

    private final ReturnEntryRepo returnEntryRepo;
    private final InventoryRepo inventoryRepo;
    private final InventoryTransactionRepo transactionRepo;
    private final WarehouseAttachmentRepo attachmentRepo;
    private final ImageUploadService imageUploadService;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Override
    @Transactional
    public ReturnEntryResponse create(CreateReturnEntryRequest request, Integer staffId) {
        ReturnEntryJpa entry = new ReturnEntryJpa();
        entry.setReturnCode(generateCode());
        entry.setWarehouseId(request.getWarehouseId());
        entry.setReturnReason(request.getReturnReason());
        entry.setSourceIssueId(request.getSourceIssueId());
        entry.setReturnType(request.getReturnType() != null
                ? request.getReturnType() : ReturnType.CUSTOMER_RETURN);
        entry.setStatus(ReturnEntryStatus.DRAFT);
        entry.setCreatedBy(staffId);

        ReturnEntryJpa saved = returnEntryRepo.save(entry);

        for (ReturnEntryItemRequest itemReq : request.getItems()) {
            ReturnEntryItemJpa item = new ReturnEntryItemJpa();
            item.setReturnId(saved.getReturnId());
            item.setItemId(itemReq.getItemId());
            item.setQuantity(itemReq.getQuantity());
            item.setConditionNote(itemReq.getConditionNote());
            item.setExchangeItem(false);
            saved.getItems().add(item);
        }

        if (request.getExchangeItems() != null) {
            for (ReturnEntryItemRequest itemReq : request.getExchangeItems()) {
                ReturnEntryItemJpa item = new ReturnEntryItemJpa();
                item.setReturnId(saved.getReturnId());
                item.setItemId(itemReq.getItemId());
                item.setQuantity(itemReq.getQuantity());
                item.setConditionNote(null);
                item.setExchangeItem(true);
                saved.getItems().add(item);
            }
        }

        return toResponse(returnEntryRepo.save(saved));
    }

    @Override
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

        ReturnEntryResponse created = create(request, staffId);

        MultipartFile[] files = {
            req.getFile_0(), req.getFile_1(), req.getFile_2(), req.getFile_3(), req.getFile_4()
        };

        ReturnEntryJpa saved = findOrThrow(created.getReturnId());
        List<ReturnEntryItemJpa> savedItems = saved.getItems();

        for (int i = 0; i < savedItems.size() && i < files.length; i++) {
            MultipartFile file = files[i];
            if (file != null && !file.isEmpty()) {
                String url = imageUploadService.uploadImage(file, FOLDER_RETURN_ENTRY);
                WarehouseAttachmentJpa attachment = new WarehouseAttachmentJpa();
                attachment.setRefType(WarehouseAttachmentJpa.RefType.RETURN_ENTRY_ITEM);
                attachment.setRefId(savedItems.get(i).getReturnItemId());
                attachment.setFileUrl(url);
                attachment.setUploadedBy(staffId);
                attachmentRepo.save(attachment);
            }
        }

        return toResponse(findOrThrow(created.getReturnId()));
    }

    @Override
    @Transactional
    public void addAttachment(Integer returnItemId, MultipartFile file, Integer staffId) throws IOException {
        ReturnEntryItemJpa item = returnEntryRepo.findItemById(returnItemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy return_entry_item id=" + returnItemId));

        ReturnEntryJpa entry = findOrThrow(item.getReturnId());
        if (entry.getStatus() == ReturnEntryStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Không thể thêm ảnh cho phiếu đã xác nhận");
        }

        String url = imageUploadService.uploadImage(file, FOLDER_RETURN_ENTRY);

        WarehouseAttachmentJpa attachment = new WarehouseAttachmentJpa();
        attachment.setRefType(WarehouseAttachmentJpa.RefType.RETURN_ENTRY_ITEM);
        attachment.setRefId(returnItemId);
        attachment.setFileUrl(url);
        attachment.setUploadedBy(staffId);
        attachmentRepo.save(attachment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReturnEntryResponse> listByWarehouse(Integer warehouseId) {
        return returnEntryRepo.findByWarehouseId(warehouseId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ReturnEntryResponse getDetail(Integer returnId) {
        return toResponse(findOrThrow(returnId));
    }

    @Override
    @Transactional
    public ReturnEntryResponse patchItem(Integer returnId, Integer returnItemId, PatchReturnItemRequest request) {
        ReturnEntryJpa entry = findOrThrow(returnId);
        if (entry.getStatus() != ReturnEntryStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Chỉ có thể sửa phiếu ở trạng thái DRAFT");
        }
        ReturnEntryItemJpa item = returnEntryRepo.findItemById(returnItemId)
                .filter(i -> i.getReturnId().equals(returnId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy item id=" + returnItemId + " trong phiếu id=" + returnId));

        if (request.getQuantity() != null) item.setQuantity(request.getQuantity());
        if (request.getConditionNote() != null) item.setConditionNote(request.getConditionNote());

        returnEntryRepo.saveItem(item);
        return toResponse(findOrThrow(returnId));
    }

    @Override
    @Transactional
    public ReturnEntryResponse update(Integer returnId, UpdateReturnEntryRequest request) {
        ReturnEntryJpa entry = findOrThrow(returnId);
        if (entry.getStatus() != ReturnEntryStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Chỉ có thể sửa phiếu ở trạng thái DRAFT");
        }
        if (request.getReturnReason() != null) entry.setReturnReason(request.getReturnReason());

        if (request.getItems() != null || request.getExchangeItems() != null) {
            entry.getItems().clear();
            if (request.getItems() != null) {
                for (ReturnEntryItemRequest itemReq : request.getItems()) {
                    ReturnEntryItemJpa item = new ReturnEntryItemJpa();
                    item.setReturnId(returnId);
                    item.setItemId(itemReq.getItemId());
                    item.setQuantity(itemReq.getQuantity());
                    item.setConditionNote(itemReq.getConditionNote());
                    item.setExchangeItem(false);
                    entry.getItems().add(item);
                }
            }
            if (request.getExchangeItems() != null) {
                for (ReturnEntryItemRequest itemReq : request.getExchangeItems()) {
                    ReturnEntryItemJpa item = new ReturnEntryItemJpa();
                    item.setReturnId(returnId);
                    item.setItemId(itemReq.getItemId());
                    item.setQuantity(itemReq.getQuantity());
                    item.setConditionNote(null);
                    item.setExchangeItem(true);
                    entry.getItems().add(item);
                }
            }
        }
        return toResponse(returnEntryRepo.save(entry));
    }

    @Override
    @Transactional
    public ReturnEntryResponse confirm(Integer returnId, Integer staffId) {
        ReturnEntryJpa entry = findOrThrow(returnId);

        if (entry.getStatus() == ReturnEntryStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phiếu đã được xác nhận");
        }

        List<ReturnEntryItemJpa> returnItems = entry.getItems().stream()
                .filter(i -> !i.isExchangeItem()).collect(Collectors.toList());
        List<ReturnEntryItemJpa> exchangeItems = entry.getItems().stream()
                .filter(ReturnEntryItemJpa::isExchangeItem).collect(Collectors.toList());

        if (returnItems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Phiếu hoàn không có sản phẩm nào");
        }

        ReturnType type = entry.getReturnType();

        for (ReturnEntryItemJpa item : returnItems) {
            InventoryJpa inv = getOrCreateInventory(entry.getWarehouseId(), item.getItemId());

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
            saveTransaction(entry.getWarehouseId(), item.getItemId(), txType,
                    item.getQuantity(), newQty, returnId, staffId);
        }

        if (type == ReturnType.EXCHANGE) {
            for (ReturnEntryItemJpa item : exchangeItems) {
                InventoryJpa inv = getOrCreateInventory(entry.getWarehouseId(), item.getItemId());
                int newQty = Math.max(0, inv.getQuantity() - item.getQuantity());
                inv.setQuantity(newQty);
                inventoryRepo.save(inv);
                saveTransaction(entry.getWarehouseId(), item.getItemId(),
                        InventoryTransactionType.OUT, item.getQuantity(), newQty, returnId, staffId);
            }
        }

        entry.setStatus(ReturnEntryStatus.CONFIRMED);
        entry.setConfirmedBy(staffId);
        entry.setConfirmedAt(LocalDateTime.now());

        return toResponse(returnEntryRepo.save(entry));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private InventoryJpa getOrCreateInventory(Integer warehouseId, Integer itemId) {
        return inventoryRepo.findByWarehouseAndItemWithLock(warehouseId, itemId)
                .orElseGet(() -> {
                    InventoryJpa newInv = new InventoryJpa();
                    newInv.setWarehouseId(warehouseId);
                    newInv.setItemId(itemId);
                    newInv.setQuantity(0);
                    newInv.setReservedQuantity(0);
                    return newInv;
                });
    }

    private void saveTransaction(Integer warehouseId, Integer itemId,
                                  InventoryTransactionType type, int qty, int balance,
                                  Integer returnId, Integer staffId) {
        InventoryTransactionJpa tx = new InventoryTransactionJpa();
        tx.setWarehouseId(warehouseId);
        tx.setItemId(itemId);
        tx.setTransactionType(type);
        tx.setQuantity(qty);
        tx.setBalanceAfter(balance);
        tx.setReferenceType("return_entry");
        tx.setReferenceId(returnId);
        tx.setCreatedById(staffId);
        tx.setCreatedAt(Instant.now());
        transactionRepo.save(tx);
    }

    private ReturnEntryJpa findOrThrow(Integer returnId) {
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

    private ReturnEntryResponse toResponse(ReturnEntryJpa e) {
        ReturnEntryResponse r = new ReturnEntryResponse();
        r.setReturnId(e.getReturnId());
        r.setReturnCode(e.getReturnCode());
        r.setWarehouseId(e.getWarehouseId());
        r.setReturnReason(e.getReturnReason());
        r.setSourceIssueId(e.getSourceIssueId());
        r.setStatus(e.getStatus());
        r.setConfirmedBy(e.getConfirmedBy());
        r.setConfirmedAt(e.getConfirmedAt());
        r.setCreatedBy(e.getCreatedBy());
        r.setCreatedAt(e.getCreatedAt());

        r.setItems(e.getItems().stream().map(i -> {
            ReturnEntryItemResponse ir = new ReturnEntryItemResponse();
            ir.setReturnItemId(i.getReturnItemId());
            ir.setItemId(i.getItemId());
            ir.setQuantity(i.getQuantity());
            ir.setConditionNote(i.getConditionNote());
            return ir;
        }).collect(Collectors.toList()));

        return r;
    }
}
