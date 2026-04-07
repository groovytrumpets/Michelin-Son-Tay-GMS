package com.g42.platform.gms.warehouse.app.service.returns;

import com.g42.platform.gms.common.service.ImageUploadService;
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

    @Override
    @Transactional
    public ReturnEntryResponse create(CreateReturnEntryRequest request, Integer staffId) {
        ReturnEntryJpa entry = new ReturnEntryJpa();
        entry.setReturnCode(generateCode());
        entry.setWarehouseId(request.getWarehouseId());
        entry.setReturnReason(request.getReturnReason());
        entry.setSourceIssueId(request.getSourceIssueId());
        entry.setStatus(ReturnEntryStatus.DRAFT);
        entry.setCreatedBy(staffId);

        ReturnEntryJpa saved = returnEntryJpaRepo.save(entry);

        for (ReturnEntryItemRequest itemReq : request.getItems()) {
            ReturnEntryItemJpa item = new ReturnEntryItemJpa();
            item.setReturnId(saved.getReturnId());
            item.setItemId(itemReq.getItemId());
            item.setQuantity(itemReq.getQuantity());
            item.setConditionNote(itemReq.getConditionNote());
            saved.getItems().add(item);
        }

        return toResponse(returnEntryJpaRepo.save(saved));
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
        if (entry.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Phiếu hoàn không có sản phẩm nào");
        }

        for (ReturnEntryItemJpa item : entry.getItems()) {
            InventoryJpa inv = inventoryJpaRepo
                    .findByWarehouseIdAndItemIdWithLock(entry.getWarehouseId(), item.getItemId())
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
            inventoryJpaRepo.save(inv);

            InventoryTransactionJpa tx = new InventoryTransactionJpa();
            tx.setWarehouseId(entry.getWarehouseId());
            tx.setItemId(item.getItemId());
            tx.setTransactionType(InventoryTransactionType.IN);
            tx.setQuantity(item.getQuantity());
            tx.setBalanceAfter(newQty);
            tx.setReferenceType("return_entry");
            tx.setReferenceId(returnId);
            tx.setCreatedById(staffId);
            tx.setCreatedAt(Instant.now());
            transactionJpaRepo.save(tx);
        }

        entry.setStatus(ReturnEntryStatus.CONFIRMED);
        entry.setConfirmedBy(staffId);
        entry.setConfirmedAt(LocalDateTime.now());

        return toResponse(returnEntryJpaRepo.save(entry));
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
