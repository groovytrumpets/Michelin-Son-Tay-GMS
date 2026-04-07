package com.g42.platform.gms.warehouse.domain.repository;

import com.g42.platform.gms.warehouse.infrastructure.entity.ReturnEntryItemJpa;
import com.g42.platform.gms.warehouse.infrastructure.entity.ReturnEntryJpa;

import java.util.Optional;

public interface ReturnEntryRepo {

    Optional<ReturnEntryJpa> findById(Integer returnId);

    ReturnEntryJpa save(ReturnEntryJpa entry);

    boolean existsByCode(String returnCode);

    Optional<ReturnEntryItemJpa> findItemById(Integer returnItemId);

    ReturnEntryItemJpa saveItem(ReturnEntryItemJpa item);
}
