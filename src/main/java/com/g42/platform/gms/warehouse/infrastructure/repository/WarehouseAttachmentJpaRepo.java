package com.g42.platform.gms.warehouse.infrastructure.repository;

import com.g42.platform.gms.warehouse.infrastructure.entity.WarehouseAttachmentJpa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WarehouseAttachmentJpaRepo extends JpaRepository<WarehouseAttachmentJpa, Integer> {

    List<WarehouseAttachmentJpa> findByRefTypeAndRefId(
            WarehouseAttachmentJpa.RefType refType, Integer refId);

    boolean existsByRefTypeAndRefId(
            WarehouseAttachmentJpa.RefType refType, Integer refId);
}
