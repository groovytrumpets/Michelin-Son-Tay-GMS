package com.g42.platform.gms.warehouse.app.service.returns;

import com.g42.platform.gms.common.service.ImageUploadService;
import com.g42.platform.gms.warehouse.api.dto.request.CreateReturnEntryFormRequest;
import com.g42.platform.gms.warehouse.api.dto.request.CreateReturnEntryRequest;
import com.g42.platform.gms.warehouse.api.dto.request.ReturnEntryItemRequest;
import com.g42.platform.gms.warehouse.api.dto.response.ReturnEntryItemResponse;
import com.g42.platform.gms.warehouse.api.dto.response.ReturnEntryResponse;
import com.g42.platform.gms.warehouse.domain.enums.InventoryTransactionType;
import com.g42.platform.gms.warehouse.domain.enums.ReturnEntryStatus;
import com.g42.platform.gms.warehouse.infrastructure.entity.InventoryJpa;
import com.g42.platform.gms.warehouse.infrastructure.entity.InventoryTransactionJpa;
import com.g42.platform.gms.warehouse.infrastructure.entity.ReturnEntryItemJpa;
import com.g42.platform.gms.warehouse.infrastructure.entity.ReturnEntryJpa;
import com.g42.platform.gms.warehouse.infrastructure.entity.WarehouseAttachmentJpa;
import com.g42.platform.gms.warehouse.infrastructure.repository.InventoryJpaRepo;
import com.g42.platform.gms.warehouse.infrastructure.repository.InventoryTransactionJpaRepo;
import com.g42.platform.gms.warehouse.infrastructure.repository.ReturnEntryItemJpaRepo;
import com.g42.platform.gms.warehouse.infrastructure.repository.ReturnEntryJpaRepo;
import com.g42.platform.gms.warehouse.infrastructure.repository.WarehouseAttachmentJpaRepo;
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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class ReturnEntryServiceImpl implements ReturnEntryService {

    private static final String FOLDER_RETURN_ENTRY = "garage/warehouse/return-entry";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final AtomicInteger SEQ = new AtomicInteger(0);

    private final ReturnEntryJpaRepo returnEntryJpaRepo;
    private final ReturnEntryItemJpaRepo returnEntryItemJpaRepo;
    private final InventoryJpaRepo inventoryJpaRepo;
    private final InventoryTransactionJpaRepo transactionJpaRepo;
    private final ImageUploadService imageUploadService;
    private final WarehouseAttachmentJpaRepo attachmentJpaRepo;
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
                ? request.getReturnType()
                : com.g42.platform.gms.warehouse.domain.enums.ReturnType.CUSTOMER_RETURN);
        entry.setStatus(ReturnEntryStatus.DRAFT);
        entry.setCreatedBy(staffId);

        ReturnEntryJpa saved = returnEntryJpaRepo.save(entry);

        // Hàng trả về
        for (ReturnEntryItemRequest itemReq : request.getItems()) {
            ReturnEntryItemJpa item = new ReturnEntryItemJpa();
            item.setReturnId(saved.getReturnId());
            item.setItemId(itemReq.getItemId());
            item.setQuantity(itemReq.getQuantity());
            item.setConditionNote(itemReq.getConditionNote());
            item.setExchangeItem(false);
            saved.getItems().add(item);
        }

        // Hàng đổi mới (chỉ khi EXCHANGE)
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

        return toResponse(returnEntryJpaRepo.save(saved));
    }

    /**
     * Tạo phiếu hoàn + ảnh lỗi từng item trong 1 form.
     * file_0..file_4 tương ứng với item[0]..item[4].
     */
    @Override
    @Transactional
    public ReturnEntryResponse createWithAttachments(CreateReturnEntryFormRequest req, Integer staffId) throws IOException {
        // Parse items JSON
        List<ReturnEntryItemRequest> items;
        try {
            items = objectMapper.readValue(req.getItems(),
                    new TypeReference<List<ReturnEntryItemRequest>>() {});
        } catch (Exception e) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "items không hợp lệ: " + e.getMessage());
        }

        // Tạo phiếu
        CreateReturnEntryRequest request = new CreateReturnEntryRequest();
        request.setWarehouseId(req.getWarehouseId());
        request.setReturnReason(req.getReturnReason());
        request.setSourceIssueId(req.getSourceIssueId());
        request.setReturnType(req.getReturnType() != null
                ? req.getReturnType()
                : com.g42.platform.gms.warehouse.domain.enums.ReturnType.CUSTOMER_RETURN);
        request.setItems(items);

        // Parse exchangeItems nếu có
        if (req.getExchangeItems() != null && !req.getExchangeItems().isBlank()) {
            try {
                request.setExchangeItems(objectMapper.readValue(req.getExchangeItems(),
                        new TypeReference<List<ReturnEntryItemRequest>>() {}));
            } catch (Exception e) {
                throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                        "exchangeItems không hợp lệ: " + e.getMessage());
            }
        }

        ReturnEntryResponse created = create(request, staffId);

        // Map file theo index
        org.springframework.web.multipart.MultipartFile[] files = {
            req.getFile_0(), req.getFile_1(), req.getFile_2(), req.getFile_3(), req.getFile_4()
        };

        // Lấy returnItemId theo thứ tự items đã lưu
        ReturnEntryJpa saved = findOrThrow(created.getReturnId());
        List<ReturnEntryItemJpa> savedItems = saved.getItems();

        for (int i = 0; i < savedItems.size() && i < files.length; i++) {
            org.springframework.web.multipart.MultipartFile file = files[i];
            if (file != null && !file.isEmpty()) {
                String url = imageUploadService.uploadImage(file, FOLDER_RETURN_ENTRY);
                WarehouseAttachmentJpa attachment = new WarehouseAttachmentJpa();
                attachment.setRefType(WarehouseAttachmentJpa.RefType.RETURN_ENTRY_ITEM);
                attachment.setRefId(savedItems.get(i).getReturnItemId());
                attachment.setFileUrl(url);
                attachment.setUploadedBy(staffId);
                attachmentJpaRepo.save(attachment);
            }
        }

        return toResponse(findOrThrow(created.getReturnId()));
    }

    /**
     * Upload ảnh lỗi cho một return_entry_item cụ thể.
     * ref_type = RETURN_ENTRY_ITEM, ref_id = returnItemId
     */
    @Override
    @Transactional
    public void addAttachment(Integer returnItemId, MultipartFile file, Integer staffId) throws IOException {
        ReturnEntryItemJpa item = returnEntryItemJpaRepo.findById(returnItemId)
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
        attachmentJpaRepo.save(attachment);
    }

    @Override
    @Transactional
    public ReturnEntryResponse confirm(Integer returnId, Integer staffId) {
        ReturnEntryJpa entry = findOrThrow(returnId);

        if (entry.getStatus() == ReturnEntryStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phiếu đã được xác nhận");
        }

        List<ReturnEntryItemJpa> returnItems = entry.getItems().stream()
                .filter(i -> !i.isExchangeItem()).collect(java.util.stream.Collectors.toList());
        List<ReturnEntryItemJpa> exchangeItems = entry.getItems().stream()
                .filter(ReturnEntryItemJpa::isExchangeItem).collect(java.util.stream.Collectors.toList());

        if (returnItems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Phiếu hoàn không có sản phẩm nào");
        }

        com.g42.platform.gms.warehouse.domain.enums.ReturnType type = entry.getReturnType();

        for (ReturnEntryItemJpa item : returnItems) {
            InventoryJpa inv = getOrCreateInventory(entry.getWarehouseId(), item.getItemId());

            int newQty;
            InventoryTransactionType txType;

            if (type == com.g42.platform.gms.warehouse.domain.enums.ReturnType.SUPPLIER_RETURN) {
                // Trả NCC → trừ inventory
                newQty = Math.max(0, inv.getQuantity() - item.getQuantity());
                txType = InventoryTransactionType.OUT;
            } else {
                // CUSTOMER_RETURN hoặc EXCHANGE → cộng hàng lỗi về kho
                newQty = inv.getQuantity() + item.getQuantity();
                txType = InventoryTransactionType.IN;
            }

            inv.setQuantity(newQty);
            inventoryJpaRepo.save(inv);
            saveTransaction(entry.getWarehouseId(), item.getItemId(), txType,
                    item.getQuantity(), newQty, returnId, staffId);
        }

        // EXCHANGE: trừ hàng mới xuất cho khách
        if (type == com.g42.platform.gms.warehouse.domain.enums.ReturnType.EXCHANGE) {
            for (ReturnEntryItemJpa item : exchangeItems) {
                InventoryJpa inv = getOrCreateInventory(entry.getWarehouseId(), item.getItemId());
                int newQty = Math.max(0, inv.getQuantity() - item.getQuantity());
                inv.setQuantity(newQty);
                inventoryJpaRepo.save(inv);
                saveTransaction(entry.getWarehouseId(), item.getItemId(),
                        InventoryTransactionType.OUT, item.getQuantity(), newQty, returnId, staffId);
            }
        }

        entry.setStatus(ReturnEntryStatus.CONFIRMED);
        entry.setConfirmedBy(staffId);
        entry.setConfirmedAt(LocalDateTime.now());

        return toResponse(returnEntryJpaRepo.save(entry));
    }

    private InventoryJpa getOrCreateInventory(Integer warehouseId, Integer itemId) {
        return inventoryJpaRepo.findByWarehouseIdAndItemIdWithLock(warehouseId, itemId)
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
        transactionJpaRepo.save(tx);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private ReturnEntryJpa findOrThrow(Integer returnId) {
        return returnEntryJpaRepo.findById(returnId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy phiếu hàng trả id=" + returnId));
    }

    private String generateCode() {
        String date = LocalDate.now().format(DATE_FMT);
        int seq = SEQ.incrementAndGet();
        String candidate = String.format("TH-%s-%d", date, seq);
        while (returnEntryJpaRepo.existsByReturnCode(candidate)) {
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

        List<ReturnEntryItemResponse> itemResponses = e.getItems().stream().map(i -> {
            ReturnEntryItemResponse ir = new ReturnEntryItemResponse();
            ir.setReturnItemId(i.getReturnItemId());
            ir.setItemId(i.getItemId());
            ir.setQuantity(i.getQuantity());
            ir.setConditionNote(i.getConditionNote());
            return ir;
        }).collect(Collectors.toList());
        r.setItems(itemResponses);

        return r;
    }
}
