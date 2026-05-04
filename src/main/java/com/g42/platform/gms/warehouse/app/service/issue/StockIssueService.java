package com.g42.platform.gms.warehouse.app.service.issue;

import com.g42.platform.gms.estimation.domain.entity.Estimate;
import com.g42.platform.gms.estimation.domain.entity.EstimateItem;
import com.g42.platform.gms.estimation.domain.repository.EstimateItemRepository;
import com.g42.platform.gms.estimation.domain.repository.EstimateRepository;
import com.g42.platform.gms.auth.entity.StaffProfile;
import com.g42.platform.gms.auth.repository.StaffProfileRepo;
import com.g42.platform.gms.billing.domain.entity.ServiceBill;
import com.g42.platform.gms.billing.domain.repository.BillingRepository;
import com.g42.platform.gms.common.service.ImageUploadService;
import com.g42.platform.gms.service_ticket_management.domain.entity.ServiceTicket;
import com.g42.platform.gms.service_ticket_management.domain.repository.ServiceTicketRepo;
import com.g42.platform.gms.warehouse.api.dto.issue.CreateStockIssueRequest;
import com.g42.platform.gms.warehouse.api.dto.issue.CreateStockIssueWithAttachmentRequest;
import com.g42.platform.gms.warehouse.api.dto.issue.StockAllocationUpdatePayload;
import com.g42.platform.gms.warehouse.api.dto.request.PatchIssueItemRequest;
import com.g42.platform.gms.warehouse.api.dto.request.UpdateStockIssueRequest;
import com.g42.platform.gms.warehouse.api.dto.response.StockIssueDetailResponse;
import com.g42.platform.gms.warehouse.api.dto.response.StockIssueResponse;
import com.g42.platform.gms.warehouse.domain.entity.Inventory;
import com.g42.platform.gms.warehouse.domain.entity.InventoryTransaction;
import com.g42.platform.gms.warehouse.domain.entity.StockAllocation;
import com.g42.platform.gms.warehouse.domain.entity.StockEntryItem;
import com.g42.platform.gms.warehouse.domain.entity.Warehouse;
import com.g42.platform.gms.warehouse.domain.entity.WarehouseAttachment;
import com.g42.platform.gms.warehouse.domain.entity.WarehousePricing;
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
import com.g42.platform.gms.warehouse.domain.repository.WarehouseRepo;
import com.g42.platform.gms.warehouse.domain.repository.PartCatalogRepo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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

    /*
     * Mô tả các dependency (repo/service) chính và vai trò của chúng trong service này:
     * - stockIssueRepo / stockIssueItemRepo: lưu trữ và truy vấn StockIssue / StockIssueItem.
     * - stockAllocationRepo: đọc/ghi StockAllocation; khi tạo DRAFT có thể gắn allocation.setIssueId(...).
     * - inventoryRepo: đọc/cập nhật Inventory; các thao tác giảm/điều chỉnh tồn kho phải dùng cơ chế với lock.
     * - transactionRepo: ghi InventoryTransaction để audit (OUT, ADJUSTMENT, v.v.).
     * - stockEntryRepo: truy vấn lô hàng (StockEntryItem) theo FIFO để xác định lô sử dụng và tính giá nhập.
     * - pricingRepo / discountService: tính giá bán, áp dụng giảm giá và quy tắc wholesale.
     * - attachmentRepo / imageUploadService: quản lý ảnh/chứng từ khi xác nhận phiếu.
     * - messagingTemplate: gửi cập nhật real-time (websocket) cho frontend về allocation/issue status.
     * - billingRepository / serviceTicketRepo: liên kết với billing và service ticket khi cần tạo bill hoặc cập nhật trạng thái.
     *
     * Ghi chú nghiệp vụ (workflow):
     * - `create(...)` tạo DRAFT: chỉ tính toán lô theo FIFO và tạo các dòng tạm (placeholder) nếu thiếu hàng.
     * - `confirm(...)` mới thực sự giảm `stockEntry.remainingQuantity` và `inventory.quantity`,
     *   đồng thời ghi `InventoryTransaction` và chuyển allocation RESERVED → COMMITTED.
     * - Việc này phải nằm trong cùng transaction để đảm bảo tính nguyên tử và tránh vượt bán (oversell).
     */

    private final StockIssueRepo stockIssueRepo;
    private final StockAllocationRepo stockAllocationRepo;
    private final InventoryRepo inventoryRepo;
    private final InventoryTransactionRepo transactionRepo;
    private final StockEntryRepo stockEntryRepo;
    private final WarehousePricingRepo pricingRepo;
    private final com.g42.platform.gms.warehouse.app.service.discount.DiscountService discountService;
    private final StockIssueItemRepo stockIssueItemRepo;
    private final PartCatalogRepo partCatalogRepo;
    private final StaffProfileRepo staffProfileRepo;
    private final WarehouseRepo warehouseRepo;
    private final ServiceTicketRepo serviceTicketRepo;
    private final EstimateRepository estimateRepository;
    private final EstimateItemRepository estimateItemRepository;
    private final WarehouseAttachmentRepo attachmentRepo;
    private final ImageUploadService imageUploadService;
    private final ObjectMapper objectMapper;
    private final BillingRepository billingRepository;

    private final SimpMessagingTemplate messagingTemplate;

    private static final String FOLDER_STOCK_ISSUE = "stock-issues";

    /**
     * Tạo phiếu xuất kho (DRAFT).
     * 
     * Quy trình:
     * 1. Kiểm tra tồn kho khả dụng (quantity - reserved_quantity)
     * 2. Tạo phiếu với status=DRAFT
     * 3. Nếu là SERVICE_TICKET: gắn allocation RESERVED vào phiếu (setIssueId)
     * 4. Tính FIFO + giá (dùng lot cũ nhất trước)
     *    - Nếu đủ lô: tạo item với giá thực (entryItemId > 0)
     *    - Nếu thiếu: tạo placeholder item với giá=0 (entryItemId=0, là tín hiệu chưa có hàng)
     * 5. Lưu draft items vào database
     *
     * Lưu ý quan trọng:
     * - Draft chỉ là dự tính (calculation), không thay đổi inventory/allocation
     * - Khi confirm(), mới thực sự trừ inventory.quantity + giảm stockEntry.remainingQuantity
     * - Placeholder rows (giá=0, entryItemId=0) báo hiệu demand chưa có lô hàng từ supplier
     *
     * @param request - Yêu cầu tạo phiếu (warehouseId, items, issueType, serviceTicketId, v.v.)
     * @param staffId - ID nhân viên tạo
     * @return Phiếu xuất đã tạo (DRAFT status)
     */
    @Transactional
    public StockIssueResponse create(CreateStockIssueRequest request, Integer staffId) {
        // Bước 1: Tính tổng reserved_quantity cho từng item từ allocation
        Map<Integer, Integer> reservedByItem = new HashMap<>();
        if (request.getIssueType() == IssueType.SERVICE_TICKET && request.getServiceTicketId() != null) {
            List<StockAllocation> reservedAllocations = stockAllocationRepo
                    .findByTicketAndWarehouseAndStatus(
                            request.getServiceTicketId(),
                            request.getWarehouseId(),
                            AllocationStatus.RESERVED);
            for (StockAllocation alloc : reservedAllocations) {
                // Bỏ qua allocation đã gắn issueId (tránh tính trùng)
                if (alloc.getIssueId() != null) continue;
                reservedByItem.merge(alloc.getItemId(), alloc.getQuantity(), Integer::sum);
            }
        }

        // Bước 2: Kiểm tra tồn kho khả dụng cho từng item trong request
        for (CreateStockIssueRequest.IssueItemRequest item : request.getItems()) {
            int available = inventoryRepo
                    .findByWarehouseAndItem(request.getWarehouseId(), item.getItemId())
                    .map(inv -> Math.max(0, inv.getQuantity() - inv.getReservedQuantity()))
                    .orElse(0);
            // Tồn kho khả dụng = available + reserved (allocation mà đang muốn xuất này)
            int effectiveAvailable = available + reservedByItem.getOrDefault(item.getItemId(), 0);
            if (effectiveAvailable < item.getQuantity()) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Không đủ tồn kho cho itemId=" + item.getItemId()
                                + " (yêu cầu=" + item.getQuantity() + ", khả dụng=" + effectiveAvailable + ")");
            }
        }

        // Bước 3: Tạo phiếu với status=DRAFT
        StockIssue issue = StockIssue.builder()
                .issueCode(generateIssueCode())  // Tự động sinh mã phiếu
                .warehouseId(request.getWarehouseId())
                .issueType(request.getIssueType())
                .issueReason(request.getIssueReason())
                .serviceTicketId(request.getServiceTicketId())
                .discountRate(BigDecimal.ZERO)
                .status(StockIssueStatus.DRAFT)
                .createdBy(staffId)
                .build();

        StockIssue saved = stockIssueRepo.save(issue);

        // Bước 4: Nếu là SERVICE_TICKET → gắn allocation vào phiếu
        if (saved.getIssueType() == IssueType.SERVICE_TICKET && saved.getServiceTicketId() != null) {
            List<StockAllocation> reservedAllocations = stockAllocationRepo
                    .findByTicketAndWarehouseAndStatus(saved.getServiceTicketId(), saved.getWarehouseId(), AllocationStatus.RESERVED);
            for (StockAllocation allocation : reservedAllocations) {
                if (allocation.getIssueId() != null) continue;
                allocation.setIssueId(saved.getIssueId());  // Gắn phiếu vào allocation
                stockAllocationRepo.save(allocation);
            }
        }

        // Bước 5: Tính FIFO + giá từ entry items
        List<StockIssueItem> draftItems = new ArrayList<>();
        for (CreateStockIssueRequest.IssueItemRequest req : request.getItems()) {
            Integer itemId = req.getItemId();
            int needed = req.getQuantity();

            // Lấy estimate unit price (nếu là SERVICE_TICKET)
            BigDecimal estimateUnitPrice = resolveEstimateUnitPrice(
                    saved.getServiceTicketId(), saved.getWarehouseId(), itemId);

            // Lấy hoặc tính discount rate
            BigDecimal discountRate = req.getDiscountRate() != null && req.getDiscountRate().compareTo(BigDecimal.ZERO) != 0
                    ? req.getDiscountRate()
                    : discountService.resolveDiscountRate(itemId, saved.getIssueType(), needed);

            // Lấy market selling price
            BigDecimal marketSellingPrice = pricingRepo
                    .findActiveByWarehouseAndItem(saved.getWarehouseId(), itemId)
                    .map(WarehousePricing::getSellingPrice)
                    .orElse(null);

            // Lấy danh sách lô hàng theo FIFO (cũ nhất trước)
            List<StockEntryItem> lots = stockEntryRepo.findFifoLots(saved.getWarehouseId(), itemId);
            int remaining = needed;

            /*
             * FIFO allocation details:
             * - We iterate lots oldest-first and consume remaining quantities from
             *   each lot up to its remainingQuantity. This ensures correct costing
             *   and tracking per lot (for warranties / traceability).
             * - We do NOT change inventory quantities here; draft creation only
             *   records which lot(s) would be used. Actual remainingQuantity is
             *   decreased when the issue is CONFIRMED.
             */
            // Tính toán từng lô
            for (StockEntryItem lot : lots) {
                if (remaining <= 0) break;
                int consume = Math.min(remaining, lot.getRemainingQuantity());  // Lấy cái nhỏ hơn
                if (consume <= 0) continue;

                // Tính selling price
                BigDecimal sellingPrice = marketSellingPrice != null
                        ? marketSellingPrice
                        : lot.getImportPrice().multiply(lot.getMarkupMultiplier()).setScale(2, RoundingMode.HALF_UP);

                // Nếu là WHOLESALE → giảm giá 15% (0.85)
                if (saved.getIssueType() == IssueType.WHOLESALE) {
                    sellingPrice = sellingPrice.multiply(WHOLESALE_FACTOR).setScale(2, RoundingMode.HALF_UP);
                }

                // Tính final price base
                BigDecimal finalPriceBase = resolveFinalPriceBase(saved.getIssueType(), estimateUnitPrice, sellingPrice);
                // Áp dụng discount
                BigDecimal finalPrice = finalPriceBase
                        .multiply(BigDecimal.ONE.subtract(
                                discountRate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)))
                        .setScale(2, RoundingMode.HALF_UP);

                // Tạo issue item
                draftItems.add(StockIssueItem.builder()
                        .issueId(saved.getIssueId())
                        .itemId(itemId)
                        .entryItemId(lot.getEntryItemId())  // Lô hàng này
                        .quantity(consume)
                        .exportPrice(sellingPrice)
                        .estimateUnitPrice(estimateUnitPrice)
                        .importPrice(lot.getImportPrice())
                        .discountRate(discountRate)
                        .finalPrice(finalPrice)
                        .build());

                remaining -= consume;
            }

            // Nếu thiếu hàng (chưa nhập kho): tạo placeholder item với giá=0
            if (remaining > 0) {
                /*
                 * Placeholder rows represent demand that cannot be fulfilled from
                 * existing lots (stock not yet received). We set entryItemId=0
                 * and zero prices as a signal to frontend/operators that this
                 * quantity is pending. When stock arrives, the draft may be
                 * re-calculated or a new draft created to cover the shortage.
                 */
                draftItems.add(StockIssueItem.builder()
                        .issueId(saved.getIssueId())
                        .itemId(itemId)
                        .entryItemId(0)  // Không có lô (placeholder)
                        .quantity(remaining)
                        .exportPrice(BigDecimal.ZERO)
                        .estimateUnitPrice(estimateUnitPrice)
                        .importPrice(BigDecimal.ZERO)
                        .discountRate(discountRate)
                        .finalPrice(BigDecimal.ZERO)
                        .build());
            }
        }

        // Lưu tất cả draft items
        stockIssueItemRepo.saveAll(draftItems);
        return toResponse(findOrThrow(saved.getIssueId()));
    }

    @Transactional
    public StockIssueResponse createWithAttachment(CreateStockIssueRequest request,
                                                    MultipartFile file,
                                                    Integer staffId) throws IOException {
        StockIssueResponse created = create(request, staffId);

        String url = imageUploadService.uploadImage(file, FOLDER_STOCK_ISSUE);
        WarehouseAttachment attachment = new WarehouseAttachment();
        attachment.setRefType(WarehouseAttachment.RefType.STOCK_ISSUE);
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
        WarehouseAttachment attachment = new WarehouseAttachment();
        attachment.setRefType(WarehouseAttachment.RefType.STOCK_ISSUE);
        attachment.setRefId(issueId);
        attachment.setFileUrl(url);
        attachment.setUploadedBy(staffId);
        attachmentRepo.save(attachment);
    }

    /**
     * Sửa thông tin của 1 item trong phiếu (quantity, discountRate).
     * Chỉ có thể sửa khi phiếu ở trạng thái DRAFT.
     * Phiếu SERVICE_TICKET không được sửa → phải tạo đơn mới.
     *
     * @param issueId - ID phiếu
     * @param issueItemId - ID item trong phiếu cần sửa
     * @param request - Dữ liệu sửa (quantity, discountRate)
     * @return Phiếu sau khi sửa
     */
    @Transactional
    public StockIssueResponse patchItem(Integer issueId, Integer issueItemId, PatchIssueItemRequest request) {
        StockIssue issue = findOrThrow(issueId);

        // Kiểm tra phiếu ở trạng thái DRAFT
        if (issue.getStatus() != StockIssueStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Chỉ có thể sửa phiếu ở trạng thái DRAFT");
        }

        // Phiếu SERVICE_TICKET không được sửa
        if (issue.getIssueType() == IssueType.SERVICE_TICKET) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Phiếu xuất từ Service Ticket không được điều chỉnh. Vui lòng yêu cầu tạo đơn mới từ cửa hàng");
        }

        // Lấy item cần sửa
        StockIssueItem item = stockIssueItemRepo.findById(issueItemId)
                .filter(i -> i.getIssueId().equals(issueId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy item id=" + issueItemId + " trong phiếu id=" + issueId));

        // Cập nhật quantity (nếu có)
        if (request.getQuantity() != null) item.setQuantity(request.getQuantity());

        // Cập nhật discountRate (nếu có)
        if (request.getDiscountRate() != null) item.setDiscountRate(request.getDiscountRate());

        // Lưu thay đổi
        stockIssueItemRepo.save(item);
        return toResponse(findOrThrow(issueId));
    }

    /**
     * Xóa 1 item khỏi phiếu xuất DRAFT.
     * 
     * Quy trình:
     * 1. Kiểm tra phiếu ở trạng thái DRAFT
     * 2. Nếu là SERVICE_TICKET: release allocation tương ứng của item này
     * 3. Xóa issue item khỏi phiếu
     * 4. Nếu phiếu hết item → tự động hủy phiếu
     *
     * @param issueId - ID phiếu
     * @param issueItemId - ID item trong phiếu cần xóa
     * @param staffId - ID nhân viên xóa
     * @return Phiếu sau khi xóa (hoặc phiếu CANCELLED nếu hết item)
     */
    @Transactional
    public StockIssueResponse removeItem(Integer issueId, Integer issueItemId, Integer staffId) {
        StockIssue issue = findOrThrow(issueId);

        // Kiểm tra phiếu ở trạng thái DRAFT
        if (issue.getStatus() != StockIssueStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Chỉ có thể xóa item khi phiếu ở trạng thái DRAFT");
        }

        // Lấy item cần xóa
        StockIssueItem item = stockIssueItemRepo.findById(issueItemId)
                .filter(i -> i.getIssueId().equals(issueId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy item id=" + issueItemId + " trong phiếu id=" + issueId));

        // Nếu là SERVICE_TICKET: release allocation của item này
        if (issue.getIssueType() == IssueType.SERVICE_TICKET && issue.getServiceTicketId() != null) {
            // Tìm allocation RESERVED của item này trong phiếu
            List<StockAllocation> allocations = stockAllocationRepo
                    .findByTicketAndWarehouseAndStatus(issue.getServiceTicketId(), issue.getWarehouseId(), AllocationStatus.RESERVED)
                    .stream()
                    .filter(a -> item.getItemId().equals(a.getItemId()) && issueId.equals(a.getIssueId()))
                    .toList();

            for (StockAllocation alloc : allocations) {
                inventoryRepo.findByWarehouseAndItemWithLock(alloc.getWarehouseId(), alloc.getItemId())
                        .ifPresent(inv -> {
                            inv.setReservedQuantity(Math.max(0, inv.getReservedQuantity() - alloc.getQuantity()));
                            inventoryRepo.save(inv);
                        });
                // Chuyển allocation RESERVED → RELEASED
                alloc.setStatus(AllocationStatus.RELEASED);
                stockAllocationRepo.save(alloc);
            }
        }

        // Xóa issue item khỏi phiếu
        stockIssueItemRepo.deleteById(issueItemId);

        // Nếu phiếu hết item → tự động hủy phiếu
        List<StockIssueItem> remaining = stockIssueItemRepo.findByIssueId(issueId);
        if (remaining.isEmpty()) {
            issue.setStatus(StockIssueStatus.CANCELLED);
            stockIssueRepo.save(issue);
        }

        return toResponse(findOrThrow(issueId));
    }

    /**
     * Cập nhật danh sách items trong phiếu xuất DRAFT.
     * 
     * Quy trình:
     * 1. Kiểm tra phiếu ở trạng thái DRAFT
     * 2. Kiểm tra phiếu SERVICE_TICKET không được sửa (phải tạo đơn mới từ cửa hàng)
     * 3. Validate tồn kho khả dụng cho tất cả items
     * 4. Xóa hết items cũ
     * 5. Tính lại FIFO + giá cho items mới
     * 6. Lưu items mới
     *
     * @param issueId - ID phiếu cần update
     * @param request - Yêu cầu update (items mới, issueReason, v.v.)
     * @return Phiếu đã cập nhật
     */
    @Transactional
    public StockIssueResponse update(Integer issueId, UpdateStockIssueRequest request) {
        StockIssue issue = findOrThrow(issueId);

        // Kiểm tra phiếu ở trạng thái DRAFT
        if (issue.getStatus() != StockIssueStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Chỉ có thể sửa phiếu ở trạng thái DRAFT");
        }

        // Phiếu SERVICE_TICKET không được sửa → phải tạo đơn mới từ cửa hàng
        if (issue.getIssueType() == IssueType.SERVICE_TICKET) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Phiếu xuất từ Service Ticket không được điều chỉnh. Vui lòng yêu cầu tạo đơn mới từ cửa hàng");
        }

        // Cập nhật lý do xuất (nếu có)
        if (request.getIssueReason() != null) issue.setIssueReason(request.getIssueReason());

        // Nếu có items mới → validate + tính lại FIFO
        if (request.getItems() != null) {
            // Bước 3: Validate tồn kho khả dụng cho tất cả items
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

            // Bước 4: Xóa tất cả items cũ
            stockIssueItemRepo.deleteByIssueId(issueId);

            // Bước 5: Tính lại FIFO + giá cho items mới
            List<StockIssueItem> newItems = new ArrayList<>();
            for (CreateStockIssueRequest.IssueItemRequest req : request.getItems()) {
                Integer itemId = req.getItemId();
                int needed = req.getQuantity();

                // Lấy pricing info
                BigDecimal estimateUnitPrice = resolveEstimateUnitPrice(issue.getServiceTicketId(), issue.getWarehouseId(), itemId);
                BigDecimal discountRate = req.getDiscountRate() != null && req.getDiscountRate().compareTo(BigDecimal.ZERO) != 0
                        ? req.getDiscountRate()
                        : discountService.resolveDiscountRate(itemId, issue.getIssueType(), needed);
                BigDecimal marketSellingPrice = pricingRepo.findActiveByWarehouseAndItem(issue.getWarehouseId(), itemId)
                        .map(WarehousePricing::getSellingPrice).orElse(null);

                // FIFO calculation
                List<StockEntryItem> lots = stockEntryRepo.findFifoLots(issue.getWarehouseId(), itemId);
                int remaining = needed;
                for (StockEntryItem lot : lots) {
                    if (remaining <= 0) break;
                    int consume = Math.min(remaining, lot.getRemainingQuantity());
                    if (consume <= 0) continue;
                    BigDecimal sellingPrice = marketSellingPrice != null ? marketSellingPrice
                            : lot.getImportPrice().multiply(lot.getMarkupMultiplier()).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal finalPriceBase = resolveFinalPriceBase(issue.getIssueType(), estimateUnitPrice, sellingPrice);
                    BigDecimal finalPrice = finalPriceBase.multiply(BigDecimal.ONE.subtract(
                            discountRate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP))).setScale(2, RoundingMode.HALF_UP);
                    newItems.add(StockIssueItem.builder().issueId(issueId).itemId(itemId).entryItemId(lot.getEntryItemId())
                            .quantity(consume).exportPrice(sellingPrice).estimateUnitPrice(estimateUnitPrice)
                            .importPrice(lot.getImportPrice()).discountRate(discountRate).finalPrice(finalPrice).build());
                    remaining -= consume;
                }

                // Nếu thiếu hàng → placeholder item
                if (remaining > 0) {
                    newItems.add(StockIssueItem.builder().issueId(issueId).itemId(itemId).entryItemId(0)
                            .quantity(remaining).exportPrice(BigDecimal.ZERO).estimateUnitPrice(estimateUnitPrice)
                            .importPrice(BigDecimal.ZERO).discountRate(discountRate).finalPrice(BigDecimal.ZERO).build());
                }
            }
            // Bước 6: Lưu items mới
            stockIssueItemRepo.saveAll(newItems);
        }

        stockIssueRepo.save(issue);
        return toResponse(findOrThrow(issueId));
    }

    /**
     * Xác nhận (confirm) phiếu xuất kho.
     *
     * Quy trình:
     * 1. Kiểm tra phiếu chưa CONFIRMED + có ảnh đính kèm
     * 2. Giảm remainingQuantity của các lô (decreaseRemainingQuantity)
     * 3. Giảm quantity trong inventory (trừ hàng thực)
     * 4. Ghi log transaction (audit)
     * 5. Giảm reserved_quantity từ allocation (vì đã xuất)
     * 6. Chuyển allocation từ RESERVED → COMMITTED
     * 7. Gửi WebSocket message (real-time update)
     *
     * @param issueId - ID phiếu cần xác nhận
     * @param staffId - ID nhân viên xác nhận
     * @return Phiếu đã xác nhận
     */
    @Transactional
    public StockIssueResponse confirm(Integer issueId, Integer staffId) {
        StockIssue issue = findOrThrow(issueId);

        // Kiểm tra phiếu chưa CONFIRMED
        if (issue.getStatus() == StockIssueStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phiếu đã được xác nhận");
        }

        // Kiểm tra có ảnh đính kèm (yêu cầu bắt buộc)
        boolean hasAttachment = attachmentRepo.existsByRefTypeAndRefId(
            WarehouseAttachment.RefType.STOCK_ISSUE, issueId);
        if (!hasAttachment) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Cần đính kèm ảnh chứng từ trước khi xác nhận");
        }

        // Lấy allocation RESERVED để sau này release reserved_quantity
        List<StockAllocation> ticketReservedAllocations = new ArrayList<>();
        if (issue.getIssueType() == IssueType.SERVICE_TICKET) {
            ticketReservedAllocations = stockAllocationRepo.findByIssueIdAndStatus(issueId, AllocationStatus.RESERVED);
            if (ticketReservedAllocations.isEmpty() && issue.getServiceTicketId() != null) {
                ticketReservedAllocations = stockAllocationRepo.findByTicketAndWarehouseAndStatus(
                        issue.getServiceTicketId(), issue.getWarehouseId(), AllocationStatus.RESERVED);
            }
        }

        // Bước 2-4: Giảm hàng trong kho
        // Nhóm issue items theo itemId để trừ inventory 1 lần/item
        Map<Integer, Integer> totalByItem = new HashMap<>();
        for (StockIssueItem item : issue.getItems()) {
            // Nếu item này liên kết lô → giảm remainingQuantity của lô
            if (item.getEntryItemId() != null && item.getEntryItemId() > 0) {
                stockEntryRepo.decreaseRemainingQuantity(item.getEntryItemId(), item.getQuantity());
            }
            totalByItem.merge(item.getItemId(), item.getQuantity(), Integer::sum);
        }

        // Trừ inventory quantity + ghi log transaction
        for (Map.Entry<Integer, Integer> entry : totalByItem.entrySet()) {
            Integer itemId = entry.getKey();
            int needed = entry.getValue();

            Inventory inv = inventoryRepo
                    .findByWarehouseAndItemWithLock(issue.getWarehouseId(), itemId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                            "Không tìm thấy tồn kho cho itemId=" + itemId));

            int newQty = inv.getQuantity() - needed;
            inv.setQuantity(newQty);
            inventoryRepo.save(inv);

            // Ghi audit log
            InventoryTransaction tx = new InventoryTransaction();
            tx.setWarehouseId(issue.getWarehouseId());
            tx.setItemId(itemId);
            tx.setTransactionType(InventoryTransactionType.OUT);  // Xuất kho
            tx.setQuantity(needed);
            tx.setBalanceAfter(newQty);
            tx.setReferenceType("stock_issue");
            tx.setReferenceId(issueId);
            tx.setCreatedById(staffId);
            tx.setCreatedAt(Instant.now());
            transactionRepo.save(tx);
        }

        // Bước 5: Cập nhật trạng thái phiếu
        issue.setStatus(StockIssueStatus.CONFIRMED);
        issue.setConfirmedBy(staffId);
        issue.setConfirmedAt(LocalDateTime.now());
        stockIssueRepo.save(issue);

        // Bước 6: Release reserved_quantity từ allocation
        for (StockAllocation alloc : ticketReservedAllocations) {
            Inventory inv = inventoryRepo
                .findByWarehouseAndItemWithLock(alloc.getWarehouseId(), alloc.getItemId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Không tìm thấy tồn kho cho allocation itemId=" + alloc.getItemId()));

            if (inv.getReservedQuantity() < alloc.getQuantity()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Reserved quantity không hợp lệ cho allocation id=" + alloc.getAllocationId());
            }

            // Giảm reserved (vì đã xuất thực)
            inv.setReservedQuantity(inv.getReservedQuantity() - alloc.getQuantity());
            inventoryRepo.save(inv);

            // Ghi audit log
            InventoryTransaction reservedTx = new InventoryTransaction();
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

            // Chuyển allocation RESERVED → COMMITTED
            alloc.setStatus(AllocationStatus.COMMITTED);
            stockAllocationRepo.save(alloc);

            // Bước 7: Gửi WebSocket message (real-time update cho client)
            if (alloc.getEstimateItemId()!=null && alloc.getServiceTicketId()!=null) {
                StockAllocationUpdatePayload payload = new StockAllocationUpdatePayload(
                        alloc.getEstimateItemId(),
                        alloc.getAllocationId(),
                        alloc.getStatus().name()
                );
                messagingTemplate.convertAndSend
                        ("/topic/service-ticket/"+alloc.getServiceTicketId()+"/stock-update", payload);
            }
        }

        return toResponse(findOrThrow(issueId));
    }

    /**
     * Hủy phiếu xuất kho.
     * Luồng: issue (DRAFT) → cancel allocation (RESERVED → RELEASED).
     *
     * Quy trình:
     * 1. Kiểm tra phiếu chưa CANCELLED + chưa CONFIRMED
     *    (Phiếu CONFIRMED không được hủy, phải tạo return entry nếu muốn trả hàng)
     * 2. Nếu là SERVICE_TICKET: release tất cả allocation RESERVED
     *    - Giảm inventory.reservedQuantity
     *    - Chuyển allocation RESERVED → RELEASED (hủy giữ chỗ)
     * 3. Chuyển phiếu sang trạng thái CANCELLED
     *
     * Lưu ý:
     * - Phiếu DRAFT (draft chỉ là tính toán) có thể hủy bất kỳ lúc nào
     * - Phiếu CONFIRMED (đã xuất kho) không được hủy trực tiếp, phải dùng return entry
     * - Release allocation = hủy bỏ giữ chỗ, hàng trở về khả dụng cho thiếu cầu khác
     *
     * @param issueId - ID phiếu cần hủy
     * @param staffId - ID nhân viên hủy
     * @return Phiếu đã hủy (CANCELLED)
     */
    @Transactional
    public StockIssueResponse cancel(Integer issueId, Integer staffId) {
        StockIssue issue = findOrThrow(issueId);

        // Kiểm tra chưa CANCELLED hoặc CONFIRMED
        if (issue.getStatus() == StockIssueStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phiếu đã bị hủy");
        }
        if (issue.getStatus() == StockIssueStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Không thể hủy phiếu đã được xác nhận");
        }

        // Nếu là SERVICE_TICKET: release allocation
        if (issue.getIssueType() == IssueType.SERVICE_TICKET && issue.getServiceTicketId() != null) {
            List<StockAllocation> allocations = stockAllocationRepo
                    .findByIssueIdAndStatus(issueId, AllocationStatus.RESERVED);

            for (StockAllocation allocation : allocations) {
                Inventory inventory = inventoryRepo
                        .findByWarehouseAndItemWithLock(allocation.getWarehouseId(), allocation.getItemId())
                        .orElse(null);

                if (inventory != null) {
                    int updatedReserved = Math.max(0, inventory.getReservedQuantity() - allocation.getQuantity());
                    inventory.setReservedQuantity(updatedReserved);
                    inventoryRepo.save(inventory);
                }

                // Chuyển allocation RESERVED → RELEASED (hủy giữ chỗ)
                allocation.setStatus(AllocationStatus.RELEASED);
                stockAllocationRepo.save(allocation);
            }
        }

        // Chuyển phiếu sang CANCELLED
        issue.setStatus(StockIssueStatus.CANCELLED);
        return toResponse(stockIssueRepo.save(issue));
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
        Map<Integer, String> itemNameById = partCatalogRepo.findNamesByIds(itemIds.stream().toList());

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
                    WarehouseAttachment.RefType.STOCK_ISSUE, issueId)
                    .stream()
                    .map(WarehouseAttachment::getFileUrl)
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
            .map(WarehousePricing::getSellingPrice)
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
        Estimate latestEstimate = estimateRepository.findEstimateByServiceIdAndLatestVerson(serviceTicketId);
        if (latestEstimate == null) {
            return null;
        }
        Integer estimateId = latestEstimate.getId();
        return estimateItemRepository.findByEstimateId(estimateId).stream()
                .filter(i -> itemId.equals(i.getItemId()))
                .filter(i -> warehouseId == null || warehouseId.equals(i.getWarehouseId()))
                .filter(i -> !Boolean.TRUE.equals(i.getIsRemoved()))
                .filter(i -> Boolean.TRUE.equals(i.getIsChecked()))
                .map(EstimateItem::getUnitPrice)
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

    public StockIssueResponse toResponsePublic(Integer issueId) {
        return toResponse(findOrThrow(issueId));
    }

    public BigDecimal resolveEstimateUnitPricePublic(Integer serviceTicketId, Integer warehouseId, Integer itemId) {
        return resolveEstimateUnitPrice(serviceTicketId, warehouseId, itemId);
    }

    public BigDecimal resolveDiscountRatePublic(Integer itemId, IssueType issueType, int quantity) {
        return discountService.resolveDiscountRate(itemId, issueType, quantity);
    }

    public BigDecimal resolveMarketSellingPricePublic(Integer warehouseId, Integer itemId) {
        return pricingRepo.findActiveByWarehouseAndItem(warehouseId, itemId)
                .map(WarehousePricing::getSellingPrice)
                .orElse(null);
    }

    public BigDecimal resolveFinalPriceBasePublic(IssueType issueType, BigDecimal estimateUnitPrice, BigDecimal sellingPrice) {
        return resolveFinalPriceBase(issueType, estimateUnitPrice, sellingPrice);
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

        Warehouse warehouse = e.getWarehouseId() != null
            ? warehouseRepo.findById(e.getWarehouseId()).orElse(null)
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
            WarehouseAttachment.RefType.STOCK_ISSUE, e.getIssueId()).size();
        r.setAttachmentCount(attachmentCount);

        return r;
    }

    private void enrichHeaderFields(Integer warehouseId,
                                    Integer serviceTicketId,
                                    Integer createdById,
                                    Integer confirmedById,
                                    StockIssueDetailResponse resp) {
        if (warehouseId != null) {
            Warehouse warehouse = warehouseRepo.findById(warehouseId).orElse(null);
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
