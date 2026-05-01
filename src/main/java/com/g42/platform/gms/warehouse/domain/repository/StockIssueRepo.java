package com.g42.platform.gms.warehouse.domain.repository;

import com.g42.platform.gms.warehouse.domain.entity.StockIssue;
import com.g42.platform.gms.warehouse.domain.enums.IssueType;
import com.g42.platform.gms.warehouse.domain.enums.StockIssueStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StockIssueRepo {
    Optional<StockIssue> findById(Integer issueId);
    List<StockIssue> findByWarehouseId(Integer warehouseId);
    List<StockIssue> findByServiceTicketId(Integer serviceTicketId);
    Page<StockIssue> search(Integer warehouseId,
                            StockIssueStatus status,
                            IssueType issueType,
                            LocalDate fromDate,
                            LocalDate toDate,
                            String search,
                            Pageable pageable);
    StockIssue save(StockIssue issue);
    boolean existsByCode(String issueCode);
    boolean existsConfirmedServiceTicketIssue(Integer serviceTicketId);
    boolean existsDraftServiceTicketIssue(Integer serviceTicketId);
    boolean existsDraftServiceTicketIssueInWarehouse(Integer serviceTicketId, Integer warehouseId);

    Optional<StockIssue> findDraftServiceTicketIssueInWarehouse(Integer serviceTicketId, Integer warehouseId);
}