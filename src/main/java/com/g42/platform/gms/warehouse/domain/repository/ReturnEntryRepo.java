package com.g42.platform.gms.warehouse.domain.repository;

import com.g42.platform.gms.warehouse.domain.enums.ReturnEntryStatus;
import com.g42.platform.gms.warehouse.domain.enums.ReturnType;
import com.g42.platform.gms.warehouse.domain.entity.ReturnEntry;
import com.g42.platform.gms.warehouse.domain.entity.ReturnEntryItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReturnEntryRepo {

    Optional<ReturnEntry> findById(Integer returnId);

    List<ReturnEntry> findByWarehouseId(Integer warehouseId);

    Page<ReturnEntry> search(Integer warehouseId,
                             ReturnEntryStatus status,
                             ReturnType returnType,
                             LocalDate fromDate,
                             LocalDate toDate,
                             String search,
                             Pageable pageable);

    ReturnEntry save(ReturnEntry entry);

    boolean existsByCode(String returnCode);

    Optional<ReturnEntryItem> findItemById(Integer returnItemId);

    ReturnEntryItem saveItem(ReturnEntryItem item);
}
