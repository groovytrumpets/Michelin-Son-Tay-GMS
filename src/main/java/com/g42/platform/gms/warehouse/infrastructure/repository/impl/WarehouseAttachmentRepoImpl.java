package com.g42.platform.gms.warehouse.infrastructure.repository.impl;

import com.g42.platform.gms.warehouse.domain.entity.WarehouseAttachment;
import com.g42.platform.gms.warehouse.domain.repository.WarehouseAttachmentRepo;
import com.g42.platform.gms.warehouse.infrastructure.entity.WarehouseAttachmentJpa;
import com.g42.platform.gms.warehouse.infrastructure.repository.WarehouseAttachmentJpaRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class WarehouseAttachmentRepoImpl implements WarehouseAttachmentRepo {

    private final WarehouseAttachmentJpaRepo jpaRepo;

    @Override
    public WarehouseAttachment save(WarehouseAttachment attachment) {
        WarehouseAttachmentJpa saved = jpaRepo.save(toJpa(attachment));
        return toDomain(saved);
    }

    @Override
    public boolean existsByRefTypeAndRefId(WarehouseAttachment.RefType refType, Integer refId) {
        return jpaRepo.existsByRefTypeAndRefId(toJpaRefType(refType), refId);
    }

    @Override
    public List<WarehouseAttachment> findByRefTypeAndRefId(WarehouseAttachment.RefType refType, Integer refId) {
        return jpaRepo.findByRefTypeAndRefId(toJpaRefType(refType), refId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private WarehouseAttachment toDomain(WarehouseAttachmentJpa jpa) {
        WarehouseAttachment domain = new WarehouseAttachment();
        domain.setAttachmentId(jpa.getAttachmentId());
        domain.setRefType(WarehouseAttachment.RefType.valueOf(jpa.getRefType().name()));
        domain.setRefId(jpa.getRefId());
        domain.setFileUrl(jpa.getFileUrl());
        domain.setUploadedBy(jpa.getUploadedBy());
        domain.setUploadedAt(jpa.getUploadedAt());
        return domain;
    }

    private WarehouseAttachmentJpa toJpa(WarehouseAttachment domain) {
        WarehouseAttachmentJpa jpa = new WarehouseAttachmentJpa();
        jpa.setAttachmentId(domain.getAttachmentId());
        jpa.setRefType(toJpaRefType(domain.getRefType()));
        jpa.setRefId(domain.getRefId());
        jpa.setFileUrl(domain.getFileUrl());
        jpa.setUploadedBy(domain.getUploadedBy());
        jpa.setUploadedAt(domain.getUploadedAt());
        return jpa;
    }

    private WarehouseAttachmentJpa.RefType toJpaRefType(WarehouseAttachment.RefType refType) {
        return WarehouseAttachmentJpa.RefType.valueOf(refType.name());
    }
}
