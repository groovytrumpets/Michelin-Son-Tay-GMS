package com.g42.platform.gms.warehouse.app.service.issue;

import com.g42.platform.gms.estimation.infrastructure.entity.EstimateItemJpa;
import com.g42.platform.gms.estimation.infrastructure.entity.EstimateJpa;
import com.g42.platform.gms.estimation.infrastructure.repository.EstimateItemRepositoryJpa;
import com.g42.platform.gms.estimation.infrastructure.repository.EstimateRepositoryJpa;
import com.g42.platform.gms.auth.entity.StaffProfile;
import com.g42.platform.gms.auth.repository.StaffProfileRepo;
import com.g42.platform.gms.booking.customer.infrastructure.entity.CatalogItemJpaEntity;
import com.g42.platform.gms.catalog.infrastructure.repository.CatalogItemRepository;
import com.g42.platform.gms.billing.domain.entity.ServiceBill;
import com.g42.platform.gms.billing.domain.repository.BillingRepository;
import com.g42.platform.gms.common.service.ImageUploadService;
import com.g42.platform.gms.service_ticket_management.domain.entity.ServiceTicket;
import com.g42.platform.gms.service_ticket_management.domain.repository.ServiceTicketRepo;
import com.g42.platform.gms.warehouse.api.dto.issue.CreateStockIssueRequest;
import com.g42.platform.gms.warehouse.api.dto.issue.CreateStockIssueWithAttachmentRequest;
import com.g42.platform.gms.warehouse.api.dto.request.PatchIssueItemRequest;
import com.g42.platform.gms.warehouse.api.dto.request.UpdateStockIssueRequest;
import com.g42.platform.gms.warehouse.api.dto.response.StockIssueDetailResponse;
import com.g42.platform.gms.warehouse.api.dto.response.StockIssueResponse;
import com.g42.platform.gms.warehouse.domain.entity.Inventory;
import com.g42.platform.gms.warehouse.domain.entity.StockEntryItem;
import com.g42.platform.gms.warehouse.domain.entity.StockIssue;
import com.g42.platform.gms.warehouse.domain.entity.StockIssueItem;
import com.g42.platform.gms.warehouse.domain.enums.AllocationStatus;
import com.g42.platform.gms.warehouse.domain.enums.InventoryTransactionType;
import com.g42.platform.gms.warehouse.domain.enums.IssueType;
import com.g42.platform.gms.warehouse.domain.enums.StockIssueStatus;
import com.g42.platform.gms.warehouse.domain.repository.InventoryRepo;
import com.g42.platform.gms.warehouse.domain.repository.InventoryTransactionRepo;
import com.g42.platform.gms.warehouse.domain.repository.StockAllocationRepo;
import com.g42.platform.gms.warehouse.domain.repository.StockEntryRepo;
import com.g42.platform.gms.warehouse.domain.repository.StockIssueItemRepo;
import com.g42.platform.gms.warehouse.domain.repository.StockIssueRepo;
import com.g42.platform.gms.warehouse.domain.repository.WarehouseAttachmentRepo;
import com.g42.platform.gms.warehouse.domain.repository.WarehousePricingRepo;
import com.g42.platform.gms.warehouse.infrastructure.entity.InventoryTransactionJpa;
import com.g42.platform.gms.warehouse.infrastructure.entity.StockAllocationJpa;
import com.g42.platform.gms.warehouse.infrastructure.entity.WarehouseAttachmentJpa;
import com.g42.platform.gms.warehouse.infrastructure.entity.WarehouseJpa;
import com.g42.platform.gms.warehouse.infrastructure.entity.WarehousePricingJpa;
import com.g42.platform.gms.warehouse.infrastructure.repository.WarehouseJpaRepo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockIssueService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final AtomicInteger SEQ = new AtomicInteger(0);

    /** Hệ số giá buôn — WHOLESALE bán bằng 85% giá lẻ */
    private static final BigDecimal WHOLESALE_FACTOR = new BigDecimal("0.85");

    private final StockIssueRepo stockIssueRepo;
    private final StockAllocationRepo stockAllocationRepo;
    private final InventoryRepo inventoryRepo;
    private final InventoryTransactionRepo transactionRepo;
    private final StockEntryRepo stockEntryRepo;
    private final WarehousePricingRepo pricingRepo;
    private final com.g42.platform.gms.warehouse.app.service.discount.DiscountService discountService;
    private final StockIssueItemRepo stockIssueItemRepo;
    private final CatalogItemRepository catalogItemRepository;
    private final StaffProfileRepo staffProfileRepo;
    private final WarehouseJpaRepo warehouseJpaRepo;
    private final ServiceTicketRepo serviceTicketRepo;
    private final EstimateRepositoryJpa estimateRepositoryJpa;
    private final EstimateItemRepositoryJpa estimateItemRepositoryJpa;
    private final WarehouseAttachmentRepo attachmentRepo;
    private final ImageUploadService imageUploadService;
    private final ObjectMapper objectMapper;
    private final BillingRepository billingRepository;

    private static final String FOLDER_STOCK_ISSUE = "stock-issues";

    @Transactional
    public StockIssueResponse create(CreateStockIssueRequest request, Integer staffId) {
        Map<Integer, Integer> reservedByItem = new HashMap<>();
        if (request.getIssueType() == IssueType.SERVICE_TICKET && request.getServiceTicketId() != null) {
            List<StockAllocationJpa> reservedAllocations = stockAllocationRepo
                    .findByTicketAndWarehouseAndStatus(
                            request.getServiceTicketId(),
                            request.getWarehouseId(),
                            AllocationStatus.RESERVED);
            for (StockAllocationJpa alloc : reservedAllocations) {
                if (alloc.getIssueId() != null) {
                    continue;
                }
                reservedByItem.merge(alloc.getItemId(), alloc.getQuantity(), Integer::sum);
            }
        }

        for (CreateStockIssueRequest.IssueItemRequest item : request.getItems()) {
            int available = inventoryRepo
                    .findByWarehouseAndItem(request.getWarehouseId(), item.getItemId())
                    .map(inv -> Math.max(0, inv.getQuantity() - inv.getReservedQuantity()))
                    .orElse(0);
            int effectiveAvailable = available + reservedByItem.getOrDefault(item.getItemId(), 0);
            if (effectiveAvailable < item.getQuantity()) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Không đủ tồn kho cho itemId=" + item.getItemId()
                                + " (yêu cầu=" + item.getQuantity() + ", khả dụng=" + effectiveAvailable + ")");
            }
        }

        StockIssue issue = StockIssue.builder()
                .issueCode(generateIssueCode())
                .warehouseId(request.getWarehouseId())
                .issueType(request.getIssueType())
                .issueReason(request.getIssueReason())
                .serviceTicketId(request.getServiceTicketId())
                .discountRate(BigDecimal.ZERO)
                .status(StockIssueStatus.DRAFT)
                .createdBy(staffId)
                .build();

        StockIssue saved = stockIssueRepo.save(issue);

        if (saved.getIssueType() == IssueType.SERVICE_TICKET && saved.getServiceTicketId() != null) {
            List<StockAllocationJpa> reservedAllocations = stockAllocationRepo
                    .findByTicketAndWarehouseAndStatus(saved.getServiceTicketId(), saved.getWarehouseId(), AllocationStatus.RESERVED);
            for (StockAllocationJpa allocation : reservedAllocations) {
                if (allocation.getIssueId() != null) {
                    continue;
                }
                allocation.setIssueId(saved.getIssueId());
                stockAllocationRepo.save(allocation);
            }
        }

        List<StockIssueItem> placeholders = request.getItems().stream().map(req -> StockIssueItem.builder()
                .issueId(saved.getIssueId())
                .itemId(req.getItemId())
                .quantity(req.getQuantity())
                .entryItemId(0)
                .exportPrice(BigDecimal.ZERO)
            .estimateUnitPrice(resolveEstimateUnitPrice(saved.getServiceTicketId(), saved.getWarehouseId(), req.getItemId()))
                .importPrice(BigDecimal.ZERO)
                .discountRate(req.getDiscountRate() != null ? req.getDiscountRate() : BigDecimal.ZERO)
                .finalPrice(BigDecimal.ZERO)
                .build()).collect(Collectors.toList());

        stockIssueItemRepo.saveAll(placeholders);
        return toResponse(findOrThrow(saved.getIssueId()));
    }

    @Transactional
    public StockIssueResponse createWithAttachment(CreateStockIssueRequest request,
                                                    MultipartFile file,
                                                    Integer staffId) throws IOException {
        StockIssueResponse created = create(request, staffId);

        String url = imageUploadService.uploadImage(file, FOLDER_STOCK_ISSUE);
        WarehouseAttachmentJpa attachment = new WarehouseAttachmentJpa();
        attachment.setRefType(WarehouseAttachmentJpa.RefType.STOCK_ISSUE);
        attachment.setRefId(created.getIssueId());
        attachment.setFileUrl(url);
        attachment.setUploadedBy(staffId);
        attachmentRepo.save(attachment);

        return toResponse(findOrThrow(created.getIssueId()));
    }

    /**
     * Tạo phiếu + ảnh qua @ModelAttribute form.
     */
    @Transactional
    public StockIssueResponse createWithAttachmentForm(CreateStockIssueWithAttachmentRequest req,
                                                        Integer staffId) throws IOException {
        List<CreateStockIssueRequest.IssueItemRequest> items;
        try {
            items = objectMapper.readValue(req.getItems(),
                    new TypeReference<List<CreateStockIssueRequest.IssueItemRequest>>() {});
        } catch (Exception e) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "items không hợp lệ: " + e.getMessage());
        }

        CreateStockIssueRequest request = new CreateStockIssueRequest();
        request.setWarehouseId(req.getWarehouseId());
        request.setIssueType(IssueType.valueOf(req.getIssueType()));
        request.setIssueReason(req.getIssueReason());
        request.setServiceTicketId(req.getServiceTicketId());
        request.setItems(items);

        return createWithAttachment(request, req.getFile(), staffId);
    }

    @Transactional
    public void addAttachment(Integer issueId, MultipartFile file, Integer staffId) throws IOException {
        StockIssue issue = findOrThrow(issueId);
        if (issue.getStatus() == StockIssueStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phiếu đã được xác nhận");
        }
        String url = imageUploadService.uploadImage(file, FOLDER_STOCK_ISSUE);
        WarehouseAttachmentJpa attachment = new WarehouseAttachmentJpa();
        attachment.setRefType(WarehouseAttachmentJpa.RefType.STOCK_ISSUE);
        attachment.setRefId(issueId);
        attachment.setFileUrl(url);
        attachment.setUploadedBy(staffId);
        attachmentRepo.save(attachment);
    }

    @Transactional
    public StockIssueResponse patchItem(Integer issueId, Integer issueItemId, PatchIssueItemRequest request) {
        StockIssue issue = findOrThrow(issueId);
        if (issue.getStatus() != StockIssueStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Chỉ có thể sửa phiếu ở trạng thái DRAFT");
        }
        
        // Phiếu SERVICE_TICKET không được tự điều chỉnh
        if (issue.getIssueType() == IssueType.SERVICE_TICKET) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Phiếu xuất từ Service Ticket không được điều chỉnh. Vui lòng yêu cầu tạo đơn mới từ cửa hàng");
        }
        
        StockIssueItem item = stockIssueItemRepo.findById(issueItemId)
                .filter(i -> i.getIssueId().equals(issueId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy item id=" + issueItemId + " trong phiếu id=" + issueId));

        if (request.getQuantity() != null) item.setQuantity(request.getQuantity());
        if (request.getDiscountRate() != null) item.setDiscountRate(request.getDiscountRate());

        stockIssueItemRepo.save(item);
        return toResponse(findOrThrow(issueId));
    }

    @Transactional
    public StockIssueResponse update(Integer issueId, UpdateStockIssueRequest request) {
        StockIssue issue = findOrThrow(issueId);
        if (issue.getStatus() != StockIssueStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Chỉ có thể sửa phiếu ở trạng thái DRAFT");
        }
        
        // Phiếu SERVICE_TICKET không được tự điều chỉnh — cửa hàng phải tạo đơn mới
        if (issue.getIssueType() == IssueType.SERVICE_TICKET) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Phiếu xuất từ Service Ticket không được điều chỉnh. Vui lòng yêu cầu tạo đơn mới từ cửa hàng");
        }
        
        if (request.getIssueReason() != null) issue.setIssueReason(request.getIssueReason());

        if (request.getItems() != null) {
            // Validate inventory availability trước khi update
            for (CreateStockIssueRequest.IssueItemRequest item : request.getItems()) {
                int available = inventoryRepo
                        .findByWarehouseAndItem(issue.getWarehouseId(), item.getItemId())
                        .map(inv -> Math.max(0, inv.getQuantity() - inv.getReservedQuantity()))
                        .orElse(0);
                if (available < item.getQuantity()) {
                    throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                            "Không đủ tồn kho cho itemId=" + item.getItemId()
                                    + " (yêu cầu=" + item.getQuantity() + ", khả dụng=" + available + ")");
                }
            }
            
            stockIssueItemRepo.deleteByIssueId(issueId);
            List<StockIssueItem> newItems = request.getItems().stream().map(req -> StockIssueItem.builder()
                    .issueId(issueId)
                    .itemId(req.getItemId())
                    .quantity(req.getQuantity())
                    .entryItemId(0)
                    .exportPrice(BigDecimal.ZERO)
                    .estimateUnitPrice(resolveEstimateUnitPrice(issue.getServiceTicketId(), issue.getWarehouseId(), req.getItemId()))
                    .importPrice(BigDecimal.ZERO)
                    .discountRate(req.getDiscountRate() != null ? req.getDiscountRate() : BigDecimal.ZERO)
                    .finalPrice(BigDecimal.ZERO)
                    .build()).collect(Collectors.toList());
            stockIssueItemRepo.saveAll(newItems);
        }
        stockIssueRepo.save(issue);
        return toResponse(findOrThrow(issueId));
    }

    @Transactional
    public StockIssueResponse confirm(Integer issueId, Integer staffId) {
        StockIssue issue = findOrThrow(issueId);
        if (issue.getStatus() == StockIssueStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phiếu đã được xác nhận");
        }
        
        // Kiểm tra xem phiếu có ảnh chứng từ không
        boolean hasAttachment = attachmentRepo.existsByRefTypeAndRefId(
                WarehouseAttachmentJpa.RefType.STOCK_ISSUE, issueId);
        if (!hasAttachment) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Cần đính kèm ảnh chứng từ trước khi xác nhận");
        }

        List<StockAllocationJpa> ticketReservedAllocations = new ArrayList<>();
        Map<Integer, Integer> reservedByItem = new HashMap<>();
        if (issue.getIssueType() == IssueType.SERVICE_TICKET) {
            ticketReservedAllocations = stockAllocationRepo
                    .findByIssueIdAndStatus(issueId, AllocationStatus.RESERVED);
            if (ticketReservedAllocations.isEmpty() && issue.getServiceTicketId() != null) {
                ticketReservedAllocations = stockAllocationRepo
                        .findByTicketAndWarehouseAndStatus(
                                issue.getServiceTicketId(),
                                issue.getWarehouseId(),
                                AllocationStatus.RESERVED);
            }
            for (StockAllocationJpa alloc : ticketReservedAllocations) {
                reservedByItem.merge(alloc.getItemId(), alloc.getQuantity(), Integer::sum);
            }
        }

        List<StockIssueItem> lotItems = new ArrayList<>();

        for (StockIssueItem placeholder : issue.getItems()) {
            Integer itemId = placeholder.getItemId();
            int needed = placeholder.getQuantity();
                BigDecimal estimateUnitPrice = placeholder.getEstimateUnitPrice() != null
                    ? placeholder.getEstimateUnitPrice()
                    : resolveEstimateUnitPrice(issue.getServiceTicketId(), issue.getWarehouseId(), itemId);
            BigDecimal discountRate = placeholder.getDiscountRate() != null
                    ? placeholder.getDiscountRate() : BigDecimal.ZERO;

            if (discountRate.compareTo(BigDecimal.ZERO) == 0) {
                discountRate = discountService.resolveDiscountRate(itemId, issue.getIssueType(), needed);
            }

            Inventory inv = inventoryRepo
                    .findByWarehouseAndItemWithLock(issue.getWarehouseId(), itemId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                            "Không tìm thấy tồn kho cho itemId=" + itemId));

            int available = Math.max(0, inv.getQuantity() - inv.getReservedQuantity());
                int reservedForThisTicket = reservedByItem.getOrDefault(itemId, 0);
                int effectiveAvailable = available + reservedForThisTicket;
                if (effectiveAvailable < needed) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Không đủ tồn kho cho itemId=" + itemId
                        + " (yêu cầu=" + needed + ", khả dụng=" + effectiveAvailable + ")");
            }

            List<StockEntryItem> lots = stockEntryRepo.findFifoLots(issue.getWarehouseId(), itemId);
            int remaining = needed;

            BigDecimal marketSellingPrice = pricingRepo
                    .findActiveByWarehouseAndItem(issue.getWarehouseId(), itemId)
                    .map(WarehousePricingJpa::getSellingPrice)
                    .orElse(null);

            for (StockEntryItem lot : lots) {
                if (remaining <= 0) break;

                int consume = Math.min(remaining, lot.getRemainingQuantity());

                BigDecimal sellingPrice = marketSellingPrice != null
                        ? marketSellingPrice
                        : lot.getImportPrice()
                                .multiply(lot.getMarkupMultiplier())
                                .setScale(2, RoundingMode.HALF_UP);

                if (issue.getIssueType() == IssueType.WHOLESALE) {
                    sellingPrice = sellingPrice.multiply(WHOLESALE_FACTOR)
                            .setScale(2, RoundingMode.HALF_UP);
                }

                BigDecimal finalPriceBase = resolveFinalPriceBase(issue.getIssueType(), estimateUnitPrice, sellingPrice);

                BigDecimal finalPrice = finalPriceBase
                        .multiply(BigDecimal.ONE.subtract(
                                discountRate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)))
                        .setScale(2, RoundingMode.HALF_UP);

                StockIssueItem lotItem = StockIssueItem.builder()
                        .issueId(issueId)
                        .itemId(itemId)
                        .entryItemId(lot.getEntryItemId())
                        .quantity(consume)
                        .exportPrice(sellingPrice)
                    .estimateUnitPrice(estimateUnitPrice)
                        .importPrice(lot.getImportPrice())
                        .discountRate(discountRate)
                        .finalPrice(finalPrice)
                        .build();

                if (finalPrice.compareTo(lot.getImportPrice()) < 0) {
                    java.util.logging.Logger.getLogger(getClass().getName()).warning(
                            String.format("CẢNH BÁO BÁN LỖ: issueId=%d, itemId=%d, lô=%d, " +
                                            "giá bán=%.0f < giá vốn=%.0f",
                                    issueId, itemId, lot.getEntryItemId(),
                                    finalPrice.doubleValue(), lot.getImportPrice().doubleValue()));
                }

                lotItems.add(lotItem);
                stockEntryRepo.decreaseRemainingQuantity(lot.getEntryItemId(), consume);
                remaining -= consume;
            }

            int newQty = inv.getQuantity() - needed;
            inv.setQuantity(newQty);
            inventoryRepo.save(inv);

            InventoryTransactionJpa tx = new InventoryTransactionJpa();
            tx.setWarehouseId(issue.getWarehouseId());
            tx.setItemId(itemId);
            tx.setTransactionType(InventoryTransactionType.OUT);
            tx.setQuantity(needed);
            tx.setBalanceAfter(newQty);
            tx.setReferenceType("stock_issue");
            tx.setReferenceId(issueId);
            tx.setCreatedById(staffId);
            tx.setCreatedAt(Instant.now());
            transactionRepo.save(tx);
        }

        stockIssueItemRepo.deleteByIssueId(issueId);

        issue.setStatus(StockIssueStatus.CONFIRMED);
        issue.setConfirmedBy(staffId);
        issue.setConfirmedAt(LocalDateTime.now());
        stockIssueRepo.save(issue);

        for (StockAllocationJpa alloc : ticketReservedAllocations) {
            Inventory inv = inventoryRepo
                .findByWarehouseAndItemWithLock(alloc.getWarehouseId(), alloc.getItemId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Không tìm thấy tồn kho cho allocation itemId=" + alloc.getItemId()));

            if (inv.getReservedQuantity() < alloc.getQuantity()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Reserved quantity khong hop le cho allocation id=" + alloc.getAllocationId());
            }

            inv.setReservedQuantity(inv.getReservedQuantity() - alloc.getQuantity());
            inventoryRepo.save(inv);

            InventoryTransactionJpa reservedTx = new InventoryTransactionJpa();
            reservedTx.setWarehouseId(alloc.getWarehouseId());
            reservedTx.setItemId(alloc.getItemId());
            reservedTx.setTransactionType(InventoryTransactionType.ADJUSTMENT);
            reservedTx.setQuantity(-alloc.getQuantity());
            reservedTx.setBalanceAfter(inv.getQuantity());
            reservedTx.setReferenceType("stock_allocation_commit");
            reservedTx.setReferenceId(alloc.getAllocationId());
            reservedTx.setCreatedById(staffId);
            reservedTx.setCreatedAt(Instant.now());
            transactionRepo.save(reservedTx);

            alloc.setStatus(AllocationStatus.COMMITTED);
            stockAllocationRepo.save(alloc);
        }

        lotItems.forEach(item -> item.setIssueId(issueId));
        stockIssueItemRepo.saveAll(lotItems);

        return toResponse(findOrThrow(issueId));
    }

    @Transactional(readOnly = true)
    public List<StockIssueResponse> listByWarehouse(Integer warehouseId) {
        return stockIssueRepo.findByWarehouseId(warehouseId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<StockIssueResponse> searchByWarehouse(Integer warehouseId,
                                                      StockIssueStatus status,
                                                      IssueType issueType,
                                                      LocalDate fromDate,
                                                      LocalDate toDate,
                                                      String search,
                                                      int page,
                                                      int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return stockIssueRepo.search(warehouseId, status, issueType, fromDate, toDate, search, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public StockIssueDetailResponse getDetail(Integer issueId) {
        StockIssue issue = findOrThrow(issueId);

        Set<Integer> itemIds = issue.getItems().stream()
            .map(StockIssueItem::getItemId)
            .collect(Collectors.toSet());
        Map<Integer, String> itemNameById = catalogItemRepository.findAllById(itemIds).stream()
            .collect(Collectors.toMap(CatalogItemJpaEntity::getItemId, CatalogItemJpaEntity::getItemName));

        StockIssueDetailResponse resp = new StockIssueDetailResponse();
        resp.setIssueId(issue.getIssueId());
        resp.setIssueCode(issue.getIssueCode());
        resp.setWarehouseId(issue.getWarehouseId());
        resp.setIssueType(issue.getIssueType());
        resp.setIssueReason(issue.getIssueReason());
        resp.setServiceTicketId(issue.getServiceTicketId());
        resp.setDiscountRate(issue.getDiscountRate());
        resp.setStatus(issue.getStatus());
        resp.setConfirmedBy(issue.getConfirmedBy());
        resp.setConfirmedAt(issue.getConfirmedAt());
        resp.setCreatedBy(issue.getCreatedBy());
        resp.setCreatedAt(issue.getCreatedAt());
        enrichBillFields(issue.getServiceTicketId(), resp);

        enrichHeaderFields(
            issue.getWarehouseId(),
            issue.getServiceTicketId(),
            issue.getCreatedBy(),
            issue.getConfirmedBy(),
            resp
        );

        resp.setItems(issue.getItems().stream().map(it -> {
            StockIssueDetailResponse.IssueItemDetail d = new StockIssueDetailResponse.IssueItemDetail();
            d.setIssueItemId(it.getIssueItemId());
            d.setIssueItemCode(issue.getIssueCode() + "-L" + it.getIssueItemId());
            d.setItemId(it.getItemId());
            d.setItemName(itemNameById.get(it.getItemId()));
            d.setEntryItemId(it.getEntryItemId());

            if (it.getEntryItemId() != null && it.getEntryItemId() > 0) {
                stockEntryRepo.findItemById(it.getEntryItemId()).ifPresent(entryItem -> {
                    stockEntryRepo.findEntryById(entryItem.getEntryId()).ifPresent(entry -> {
                        d.setEntryCode(entry.getEntryCode());
                        d.setEntryLotCode(entry.getEntryCode() + "-LOT" + entryItem.getEntryItemId());
                    });
                });
            }

            d.setQuantity(it.getQuantity());
            BigDecimal exportPrice = it.getExportPrice();
            BigDecimal estimateUnitPrice = it.getEstimateUnitPrice();
            BigDecimal importPrice = it.getImportPrice();
            BigDecimal discountRate = it.getDiscountRate();
            BigDecimal finalPrice = it.getFinalPrice();
            BigDecimal grossProfit = it.getGrossProfit();

            // DRAFT issues store placeholder rows with zero prices; provide preview pricing for UI.
            if (issue.getStatus() == StockIssueStatus.DRAFT
                    && (isNullOrZero(exportPrice) || isNullOrZero(importPrice) || isNullOrZero(finalPrice))) {
                PricingPreview preview = buildDraftPricingPreview(issue, it);
                exportPrice = preview.exportPrice();
                importPrice = preview.importPrice();
                discountRate = preview.discountRate();
                finalPrice = preview.finalPrice();
                grossProfit = preview.grossProfit();
            }

            d.setExportPrice(exportPrice);
            d.setEstimateUnitPrice(estimateUnitPrice);
            d.setImportPrice(importPrice);
            d.setDiscountRate(discountRate);
            d.setFinalPrice(finalPrice);
            d.setGrossProfit(grossProfit);
            return d;
        }).collect(Collectors.toList()));

        // Thêm thông tin attachments
        resp.setAttachmentUrls(
            attachmentRepo.findByRefTypeAndRefId(
                    WarehouseAttachmentJpa.RefType.STOCK_ISSUE, issueId)
                    .stream()
                    .map(WarehouseAttachmentJpa::getFileUrl)
                    .collect(Collectors.toList())
        );

        // Tính toán totals
        int totalQty = resp.getItems().stream()
                .mapToInt(StockIssueDetailResponse.IssueItemDetail::getQuantity)
                .sum();
        resp.setTotalQuantity(totalQty);

        BigDecimal totalVal = resp.getItems().stream()
                .map(item -> item.getFinalPrice() != null
                    ? item.getFinalPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
                    : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        resp.setTotalValue(totalVal);

        return resp;
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private StockIssue findOrThrow(Integer issueId) {
        return stockIssueRepo.findById(issueId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy phiếu xuất kho id=" + issueId));
    }

    private String generateIssueCode() {
        String date = LocalDate.now().format(DATE_FMT);
        int seq = SEQ.incrementAndGet();
        String candidate = String.format("XK-%s-%d", date, seq);
        while (stockIssueRepo.existsByCode(candidate)) {
            candidate = String.format("XK-%s-%d", date, SEQ.incrementAndGet());
        }
        return candidate;
    }

    private BigDecimal resolveDiscount(Integer itemId, IssueType issueType, int quantity) {
        return discountService.resolveDiscountRate(itemId, issueType, quantity);
    }

    private PricingPreview buildDraftPricingPreview(StockIssue issue, StockIssueItem item) {
        int quantity = item.getQuantity() != null ? item.getQuantity() : 0;
        BigDecimal discountRate = item.getDiscountRate() != null
                ? item.getDiscountRate()
            : discountService.resolveDiscountRate(item.getItemId(), issue.getIssueType(), quantity);

        List<StockEntryItem> lots = stockEntryRepo.findFifoLots(issue.getWarehouseId(), item.getItemId());
        BigDecimal importPrice = computeAverageImportPrice(lots, quantity);

        BigDecimal sellingPrice = pricingRepo
                .findActiveByWarehouseAndItem(issue.getWarehouseId(), item.getItemId())
                .map(WarehousePricingJpa::getSellingPrice)
                .orElseGet(() -> {
                    if (!lots.isEmpty()) {
                        StockEntryItem first = lots.get(0);
                        return first.getImportPrice()
                                .multiply(first.getMarkupMultiplier())
                                .setScale(2, RoundingMode.HALF_UP);
                    }
                    return BigDecimal.ZERO;
                });

        if (issue.getIssueType() == IssueType.WHOLESALE) {
            sellingPrice = sellingPrice.multiply(WHOLESALE_FACTOR).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal estimateUnitPrice = item.getEstimateUnitPrice() != null
            ? item.getEstimateUnitPrice()
            : resolveEstimateUnitPrice(issue.getServiceTicketId(), issue.getWarehouseId(), item.getItemId());

        BigDecimal finalPriceBase = resolveFinalPriceBase(issue.getIssueType(), estimateUnitPrice, sellingPrice);

        BigDecimal finalPrice = finalPriceBase
                .multiply(BigDecimal.ONE.subtract(
                        discountRate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal grossProfit = finalPrice.subtract(importPrice).setScale(2, RoundingMode.HALF_UP);

        return new PricingPreview(sellingPrice, importPrice, discountRate, finalPrice, grossProfit);
    }

    private BigDecimal computeAverageImportPrice(List<StockEntryItem> lots, int quantityNeeded) {
        if (quantityNeeded <= 0 || lots.isEmpty()) {
            return BigDecimal.ZERO;
        }
        int remaining = quantityNeeded;
        int consumed = 0;
        BigDecimal totalCost = BigDecimal.ZERO;

        for (StockEntryItem lot : lots) {
            if (remaining <= 0) {
                break;
            }
            int take = Math.min(remaining, lot.getRemainingQuantity());
            if (take <= 0) {
                continue;
            }
            totalCost = totalCost.add(lot.getImportPrice().multiply(BigDecimal.valueOf(take)));
            consumed += take;
            remaining -= take;
        }

        if (consumed == 0) {
            return BigDecimal.ZERO;
        }
        return totalCost.divide(BigDecimal.valueOf(consumed), 2, RoundingMode.HALF_UP);
    }

    private boolean isNullOrZero(BigDecimal value) {
        return value == null || value.compareTo(BigDecimal.ZERO) == 0;
    }

    private BigDecimal resolveFinalPriceBase(IssueType issueType, BigDecimal estimateUnitPrice, BigDecimal warehouseSellingPrice) {
        if (issueType == IssueType.SERVICE_TICKET && estimateUnitPrice != null && estimateUnitPrice.compareTo(BigDecimal.ZERO) > 0) {
            return estimateUnitPrice;
        }
        return warehouseSellingPrice;
    }

    private BigDecimal resolveEstimateUnitPrice(Integer serviceTicketId, Integer warehouseId, Integer itemId) {
        if (serviceTicketId == null || itemId == null) {
            return null;
        }
        Optional<EstimateJpa> latestEstimateOpt = estimateRepositoryJpa.findTopByServiceTicketIdOrderByVersionDesc(serviceTicketId);
        if (latestEstimateOpt.isEmpty()) {
            return null;
        }
        Integer estimateId = latestEstimateOpt.get().getId();
        return estimateItemRepositoryJpa.findByEstimateId(estimateId).stream()
                .filter(i -> itemId.equals(i.getItemId()))
                .filter(i -> warehouseId == null || warehouseId.equals(i.getWarehouseId()))
                .filter(i -> !Boolean.TRUE.equals(i.getIsRemoved()))
                .filter(i -> Boolean.TRUE.equals(i.getIsChecked()))
                .map(EstimateItemJpa::getUnitPrice)
                .filter(p -> p != null)
                .findFirst()
                .orElse(null);
    }

    private record PricingPreview(
            BigDecimal exportPrice,
            BigDecimal importPrice,
            BigDecimal discountRate,
            BigDecimal finalPrice,
            BigDecimal grossProfit) {
    }

    private StockIssueResponse toResponse(StockIssue e) {
        StockIssueResponse r = new StockIssueResponse();
        r.setIssueId(e.getIssueId());
        r.setIssueCode(e.getIssueCode());
        r.setWarehouseId(e.getWarehouseId());
        r.setIssueType(e.getIssueType());
        r.setIssueReason(e.getIssueReason());
        r.setServiceTicketId(e.getServiceTicketId());
        r.setDiscountRate(e.getDiscountRate());
        r.setStatus(e.getStatus());
        r.setConfirmedBy(e.getConfirmedBy());
        r.setConfirmedAt(e.getConfirmedAt());
        r.setCreatedBy(e.getCreatedBy());
        r.setCreatedAt(e.getCreatedAt());
        enrichBillFields(e.getServiceTicketId(), r);

        WarehouseJpa warehouse = e.getWarehouseId() != null
                ? warehouseJpaRepo.findById(e.getWarehouseId()).orElse(null)
                : null;
        if (warehouse != null) {
            r.setWarehouseCode(warehouse.getWarehouseCode());
            r.setWarehouseName(warehouse.getWarehouseName());
        }

        if (e.getServiceTicketId() != null) {
            ServiceTicket ticket = serviceTicketRepo.findByServiceTicketId(e.getServiceTicketId());
            if (ticket != null) {
                r.setServiceTicketCode(ticket.getTicketCode());
            }
        }

        if (e.getCreatedBy() != null) {
            StaffProfile createdBy = staffProfileRepo.findById(e.getCreatedBy()).orElse(null);
            if (createdBy != null) {
                r.setCreatedByName(createdBy.getFullName());
            }
        }

        if (e.getConfirmedBy() != null) {
            StaffProfile confirmedBy = staffProfileRepo.findById(e.getConfirmedBy()).orElse(null);
            if (confirmedBy != null) {
                r.setConfirmedByName(confirmedBy.getFullName());
            }
        }

        // Thêm số lượng attachments
        int attachmentCount = (int) attachmentRepo.findByRefTypeAndRefId(
                WarehouseAttachmentJpa.RefType.STOCK_ISSUE, e.getIssueId()).size();
        r.setAttachmentCount(attachmentCount);

        return r;
    }

    private void enrichHeaderFields(Integer warehouseId,
                                    Integer serviceTicketId,
                                    Integer createdById,
                                    Integer confirmedById,
                                    StockIssueDetailResponse resp) {
        if (warehouseId != null) {
            WarehouseJpa warehouse = warehouseJpaRepo.findById(warehouseId).orElse(null);
            if (warehouse != null) {
                resp.setWarehouseCode(warehouse.getWarehouseCode());
                resp.setWarehouseName(warehouse.getWarehouseName());
            }
        }

        if (serviceTicketId != null) {
            ServiceTicket ticket = serviceTicketRepo.findByServiceTicketId(serviceTicketId);
            if (ticket != null) {
                resp.setServiceTicketCode(ticket.getTicketCode());
            }
        }

        if (createdById != null) {
            StaffProfile createdBy = staffProfileRepo.findById(createdById).orElse(null);
            if (createdBy != null) {
                resp.setCreatedByName(createdBy.getFullName());
            }
        }

        if (confirmedById != null) {
            StaffProfile confirmedBy = staffProfileRepo.findById(confirmedById).orElse(null);
            if (confirmedBy != null) {
                resp.setConfirmedByName(confirmedBy.getFullName());
            }
        }
    }

    private void enrichBillFields(Integer serviceTicketId, StockIssueResponse resp) {
        if (serviceTicketId == null) {
            resp.setHasBill(false);
            resp.setBillId(null);
            return;
        }
        ServiceBill bill = billingRepository.getBillingByServiceTicket(serviceTicketId);
        resp.setHasBill(bill != null);
        resp.setBillId(bill != null ? bill.getBillId() : null);
    }

    private void enrichBillFields(Integer serviceTicketId, StockIssueDetailResponse resp) {
        if (serviceTicketId == null) {
            resp.setHasBill(false);
            resp.setBillId(null);
            return;
        }
        ServiceBill bill = billingRepository.getBillingByServiceTicket(serviceTicketId);
        resp.setHasBill(bill != null);
        resp.setBillId(bill != null ? bill.getBillId() : null);
    }
}
