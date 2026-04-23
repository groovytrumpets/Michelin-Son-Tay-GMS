package com.g42.platform.gms.warehouse.domain.repository;

import com.g42.platform.gms.warehouse.domain.entity.WarehouseAttachment;

import java.util.List;

public interface WarehouseAttachmentRepo {

    WarehouseAttachment save(WarehouseAttachment attachment);

    boolean existsByRefTypeAndRefId(WarehouseAttachment.RefType refType, Integer refId);

    List<WarehouseAttachment> findByRefTypeAndRefId(WarehouseAttachment.RefType refType, Integer refId);
}
