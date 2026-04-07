package com.g42.platform.gms.warehouse.infrastructure.repository.impl;

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
    public WarehouseAttachmentJpa save(WarehouseAttachmentJpa attachment) {
        return jpaRepo.save(attachment);
    }

    @Override
    public boolean existsByRefTypeAndRefId(WarehouseAttachmentJpa.RefType refType, Integer refId) {
        return jpaRepo.existsByRefTypeAndRefId(refType, refId);
    }

    @Override
    public List<WarehouseAttachmentJpa> findByRefTypeAndRefId(WarehouseAttachmentJpa.RefType refType, Integer refId) {
        return jpaRepo.findByRefTypeAndRefId(refType, refId);
    }
}
