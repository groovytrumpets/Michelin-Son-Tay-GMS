package com.g42.platform.gms.warehouse.infrastructure.repository.impl;

import com.g42.platform.gms.warehouse.domain.entity.WarehouseAttachment;
import com.g42.platform.gms.warehouse.domain.repository.WarehouseAttachmentRepo;
import com.g42.platform.gms.warehouse.infrastructure.entity.WarehouseAttachmentJpa;
import com.g42.platform.gms.warehouse.infrastructure.repository.WarehouseAttachmentJpaRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Infrastructure Adapter: implements WarehouseAttachmentRepo (domain port).
 *
 * Bảng warehouse_attachment lưu metadata ảnh chứng từ cho nhiều loại chứng từ kho.
 * Dùng pattern polymorphic reference (ref_type + ref_id) thay vì nhiều bảng riêng:
 *
 *   ref_type = STOCK_ENTRY  + ref_id = entryId  → ảnh phiếu nhập kho
 *   ref_type = STOCK_ISSUE  + ref_id = issueId  → ảnh phiếu xuất kho
 *   ref_type = RETURN_ENTRY + ref_id = returnId → ảnh phiếu trả hàng
 *
 * Lưu ý về RefType enum:
 *   Domain có WarehouseAttachment.RefType (domain enum)
 *   JPA có WarehouseAttachmentJpa.RefType (JPA enum, map xuống DB)
 *   Impl phải convert giữa 2 enum này qua toJpaRefType()
 *   (2 enum tách biệt để domain không phụ thuộc vào JPA annotation)
 *
 * Luồng upload ảnh (StockEntryService.uploadAndSaveAttachment):
 *   1. imageUploadService.uploadImage(file) → upload lên cloud → trả về URL
 *   2. Tạo WarehouseAttachment domain với refType=STOCK_ENTRY, refId=entryId, fileUrl=url
 *   3. attachmentRepo.save(attachment)
 *        → WarehouseAttachmentRepoImpl.save(...)
 *        → toJpa(attachment) → WarehouseAttachmentJpa
 *        → jpaRepo.save(jpa)
 *        → SQL: INSERT INTO warehouse_attachment (ref_type, ref_id, file_url, uploaded_by, uploaded_at)
 *        → toDomain(saved) → trả về với attachmentId đã được DB sinh ra
 */
@Repository
@RequiredArgsConstructor
public class WarehouseAttachmentRepoImpl implements WarehouseAttachmentRepo {

    private final WarehouseAttachmentJpaRepo jpaRepo;

    /**
     * Lưu metadata ảnh chứng từ.
     * SQL: INSERT INTO warehouse_attachment (ref_type, ref_id, file_url, uploaded_by, uploaded_at)
     *      VALUES (?, ?, ?, ?, NOW())
     */
    @Override
    public WarehouseAttachment save(WarehouseAttachment attachment) {
        WarehouseAttachmentJpa saved = jpaRepo.save(toJpa(attachment));
        return toDomain(saved);
    }

    /**
     * Kiểm tra phiếu đã có ảnh chứng từ chưa.
     * SQL: SELECT COUNT(*) > 0 FROM warehouse_attachment WHERE ref_type = ? AND ref_id = ?
     * Dùng trong confirm() để bắt buộc có ảnh trước khi xác nhận.
     */
    @Override
    public boolean existsByRefTypeAndRefId(WarehouseAttachment.RefType refType, Integer refId) {
        return jpaRepo.existsByRefTypeAndRefId(toJpaRefType(refType), refId);
    }

    /**
     * Lấy danh sách ảnh chứng từ của 1 phiếu.
     * SQL: SELECT * FROM warehouse_attachment WHERE ref_type = ? AND ref_id = ?
     * Dùng trong toResponse() để trả về danh sách URL ảnh cho client.
     */
    @Override
    public List<WarehouseAttachment> findByRefTypeAndRefId(WarehouseAttachment.RefType refType, Integer refId) {
        return jpaRepo.findByRefTypeAndRefId(toJpaRefType(refType), refId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    /** WarehouseAttachmentJpa → WarehouseAttachment domain */
    private WarehouseAttachment toDomain(WarehouseAttachmentJpa jpa) {
        WarehouseAttachment domain = new WarehouseAttachment();
        domain.setAttachmentId(jpa.getAttachmentId());
        domain.setRefType(WarehouseAttachment.RefType.valueOf(jpa.getRefType().name())); // JPA enum → domain enum
        domain.setRefId(jpa.getRefId());
        domain.setFileUrl(jpa.getFileUrl());
        domain.setUploadedBy(jpa.getUploadedBy());
        domain.setUploadedAt(jpa.getUploadedAt());
        return domain;
    }

    /** WarehouseAttachment domain → WarehouseAttachmentJpa */
    private WarehouseAttachmentJpa toJpa(WarehouseAttachment domain) {
        WarehouseAttachmentJpa jpa = new WarehouseAttachmentJpa();
        jpa.setAttachmentId(domain.getAttachmentId());
        jpa.setRefType(toJpaRefType(domain.getRefType())); // domain enum → JPA enum
        jpa.setRefId(domain.getRefId());
        jpa.setFileUrl(domain.getFileUrl());
        jpa.setUploadedBy(domain.getUploadedBy());
        jpa.setUploadedAt(domain.getUploadedAt());
        return jpa;
    }

    /**
     * Convert domain RefType → JPA RefType.
     * Dùng valueOf(name()) vì 2 enum có cùng tên các giá trị (STOCK_ENTRY, STOCK_ISSUE, RETURN_ENTRY).
     */
    private WarehouseAttachmentJpa.RefType toJpaRefType(WarehouseAttachment.RefType refType) {
        return WarehouseAttachmentJpa.RefType.valueOf(refType.name());
    }
}
