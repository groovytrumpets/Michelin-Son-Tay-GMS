package com.g42.platform.gms.warehouse.domain.repository;

import com.g42.platform.gms.warehouse.infrastructure.entity.WarehouseAttachmentJpa;

import java.util.List;

public interface WarehouseAttachmentRepo {

    WarehouseAttachmentJpa save(WarehouseAttachmentJpa attachment);

    boolean existsByRefTypeAndRefId(WarehouseAttachmentJpa.RefType refType, Integer refId);

    List<WarehouseAttachmentJpa> findByRefTypeAndRefId(WarehouseAttachmentJpa.RefType refType, Integer refId);
}
