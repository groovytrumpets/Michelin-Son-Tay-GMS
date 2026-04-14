package com.g42.platform.gms.warehouse.domain.repository;

import com.g42.platform.gms.warehouse.domain.enums.ReturnEntryStatus;
import com.g42.platform.gms.warehouse.domain.enums.ReturnType;
import com.g42.platform.gms.warehouse.infrastructure.entity.ReturnEntryItemJpa;
import com.g42.platform.gms.warehouse.infrastructure.entity.ReturnEntryJpa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReturnEntryRepo {

    Optional<ReturnEntryJpa> findById(Integer returnId);

    List<ReturnEntryJpa> findByWarehouseId(Integer warehouseId);

    Page<ReturnEntryJpa> search(Integer warehouseId,
                                ReturnEntryStatus status,
                                ReturnType returnType,
                                LocalDate fromDate,
                                LocalDate toDate,
                                String search,
                                Pageable pageable);

    ReturnEntryJpa save(ReturnEntryJpa entry);

    boolean existsByCode(String returnCode);

    Optional<ReturnEntryItemJpa> findItemById(Integer returnItemId);

    ReturnEntryItemJpa saveItem(ReturnEntryItemJpa item);
}
