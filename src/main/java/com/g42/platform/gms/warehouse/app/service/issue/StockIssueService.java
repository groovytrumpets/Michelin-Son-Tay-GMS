package com.g42.platform.gms.warehouse.app.service.issue;


import com.g42.platform.gms.common.util.Qty;
import com.g42.platform.gms.estimation.domain.entity.Estimate;
import com.g42.platform.gms.estimation.domain.entity.EstimateItem;
import com.g42.platform.gms.estimation.domain.repository.EstimateItemRepository;
import com.g42.platform.gms.estimation.domain.repository.EstimateRepository;
import com.g42.platform.gms.auth.entity.StaffProfile;
import com.g42.platform.gms.auth.repository.StaffProfileRepo;
import com.g42.platform.gms.dashboard.application.service.StaffNotifyService;
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
import com.g42.platform.gms.warehouse.domain.entity.CatalogItem;
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
import java.util.HashSet;
import java.util.Objects;
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

    private final com.g42.platform.gms.customer.infrastructure.repository.CustomerProfileJpaRepo customerProfileJpaRepo;
    private final com.g42.platform.gms.vehicle.repository.VehicleRepository vehicleRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final StaffNotifyService staffNotifyService;
    private final com.g42.platform.gms.warehouse.app.service.catalog.ItemQuantityPolicy itemQuantityPolicy;
    private final com.g42.platform.gms.warehouse.app.service.serial.ItemSerialService itemSerialService;

    private static final String FOLDER_STOCK_ISSUE = "stock-issues";

    @Transactional
    public StockIssueResponse create(CreateStockIssueRequest request, Integer staffId) {
        // Tính reserved quantity từ allocation (chỉ SERVICE_TICKET)
        Map<Integer, BigDecimal> reservedByItem = buildReservedByItemMap(request);

        // Kiểm tra tồn kho khả dụng = (quantity - reserved) + reserved đang muốn xuất
        for (CreateStockIssueRequest.IssueItemRequest item : request.getItems()) {
            itemQuantityPolicy.validateStockQuantity(item.getItemId(), item.getQuantity(), null);
            BigDecimal available = inventoryRepo
                    .findByWarehouseAndItem(request.getWarehouseId(), item.getItemId())
                    .map(inv -> Qty.subFloorZero(inv.getQuantity(), inv.getReservedQuantity()))
                    .orElse(BigDecimal.ZERO);
            BigDecimal effectiveAvailable = available.add(reservedByItem.getOrDefault(item.getItemId(), BigDecimal.ZERO));
            if (Qty.lt(effectiveAvailable, item.getQuantity())) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Không đủ tồn kho cho itemId=" + item.getItemId()
                                + " (yêu cầu=" + Qty.text(item.getQuantity()) + ", khả dụng=" + Qty.text(effectiveAvailable) + ")");
            }
        }

        String receiverName = request.getReceiverName();
        String receiverPhone = request.getReceiverPhone();
        String licensePlate = request.getLicensePlate();

        if (request.getServiceTicketId() != null) {
            ServiceTicket ticket = serviceTicketRepo.findByServiceTicketId(request.getServiceTicketId());
            if (ticket != null) {
                if ((receiverName == null || receiverName.trim().isEmpty()) && ticket.getCustomerId() != null) {
                    com.g42.platform.gms.customer.infrastructure.entity.CustomerProfileJpa cust =
                            customerProfileJpaRepo.findByCustomerId(ticket.getCustomerId());
                    if (cust != null) {
                        receiverName = cust.getFullName();
                        receiverPhone = cust.getPhone();
                    }
                }
                if ((licensePlate == null || licensePlate.trim().isEmpty()) && ticket.getVehicleId() != null) {
                    var vOpt = vehicleRepository.findById(ticket.getVehicleId());
                    if (vOpt.isPresent()) {
                        var v = vOpt.get();
                        licensePlate = v.getLicensePlate();
                        if (receiverName == null || receiverName.trim().isEmpty()) {
                            if (v.getCustomer() != null) {
                                receiverName = v.getCustomer().getFullName();
                                receiverPhone = v.getCustomer().getPhone();
                            }
                        }
                    }
                }
            }
        }

        // Tạo phiếu DRAFT
        StockIssue saved = stockIssueRepo.save(StockIssue.builder()
                .issueCode(generateIssueCode())
                .warehouseId(request.getWarehouseId())
                .issueType(request.getIssueType())
                .issueReason(request.getIssueReason())
                .serviceTicketId(request.getServiceTicketId())
                .receiverName(receiverName)
                .receiverPhone(receiverPhone)
                .licensePlate(licensePlate)
                .discountRate(BigDecimal.ZERO)
                .status(StockIssueStatus.DRAFT)
                .createdBy(staffId)
                .build());

        // Gắn allocation RESERVED vào phiếu (chỉ SERVICE_TICKET)
        if (saved.getIssueType() == IssueType.SERVICE_TICKET && saved.getServiceTicketId() != null) {
            stockAllocationRepo
                    .findByTicketAndWarehouseAndStatus(saved.getServiceTicketId(), saved.getWarehouseId(), AllocationStatus.RESERVED)
                    .stream()
                    .filter(a -> a.getIssueId() == null)
                    .forEach(a -> {
                        a.setIssueId(saved.getIssueId());
                        stockAllocationRepo.save(a);
                    });
        }

        // Tính FIFO + giá cho từng item và lưu
        List<StockIssueItem> draftItems = new ArrayList<>();
        for (CreateStockIssueRequest.IssueItemRequest req : request.getItems()) {
            draftItems.addAll(buildFifoItems(saved, req.getItemId(), req.getQuantity(), req.getDiscountRate(), req.getEntryItemId()));
        }
        stockIssueItemRepo.saveAll(draftItems);

        StockIssueResponse response = toResponse(findOrThrow(saved.getIssueId()));
        notifyManagersAboutStockIssue(saved, "Tạo phiếu xuất kho", "Có phiếu yêu cầu xuất kho mới " + saved.getIssueCode() + " cần xử lý.");
        return response;
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
        request.setReceiverName(req.getReceiverName());
        request.setReceiverPhone(req.getReceiverPhone());
        request.setLicensePlate(req.getLicensePlate());
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

        if (request.getQuantity() != null) itemQuantityPolicy.validateStockQuantity(item.getItemId(), request.getQuantity(), null);
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
                            inv.setReservedQuantity(Qty.subFloorZero(inv.getReservedQuantity(), alloc.getQuantity()));
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
                itemQuantityPolicy.validateStockQuantity(item.getItemId(), item.getQuantity(), null);
                BigDecimal available = inventoryRepo
                        .findByWarehouseAndItem(issue.getWarehouseId(), item.getItemId())
                        .map(inv -> Qty.subFloorZero(inv.getQuantity(), inv.getReservedQuantity()))
                        .orElse(BigDecimal.ZERO);
                if (Qty.lt(available, item.getQuantity())) {
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
                newItems.addAll(buildFifoItems(issue, req.getItemId(), req.getQuantity(), req.getDiscountRate(), req.getEntryItemId()));
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
        return confirm(issueId, staffId, true);
    }

    /**
     * Biến thể cho luồng xuất kho tự động (bán linh kiện: xuất ngay khi thu tiền).
     * Ở đó không có nhân viên kho bấm xác nhận nên không thể bắt buộc ảnh chứng từ;
     * mọi bước trừ tồn / ghi audit / commit allocation vẫn chạy y hệt luồng thủ công.
     */
    @Transactional
    public StockIssueResponse confirm(Integer issueId, Integer staffId, boolean requireAttachment) {
        StockIssue issue = findOrThrow(issueId);

        // Kiểm tra phiếu chưa CONFIRMED
        if (issue.getStatus() == StockIssueStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phiếu đã được xác nhận");
        }

        // Kiểm tra có ảnh đính kèm (bắt buộc với luồng nhân viên kho bấm xác nhận)
        if (requireAttachment) {
            boolean hasAttachment = attachmentRepo.existsByRefTypeAndRefId(
                WarehouseAttachment.RefType.STOCK_ISSUE, issueId);
            if (!hasAttachment) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Cần đính kèm ảnh chứng từ trước khi xác nhận");
            }
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
        //
        // Đọc dòng phiếu thẳng từ DB thay vì dùng issue.getItems(): quan hệ @OneToMany
        // của StockIssueJpa là LAZY và các dòng con được lưu bằng repo riêng, nên khi
        // create(...) rồi confirm(...) chạy trong CÙNG một transaction (luồng bán linh
        // kiện: thu tiền là xuất kho ngay), entity cha còn nằm trong persistence context
        // với collection rỗng đã khởi tạo — findById trả lại đúng instance đó và vòng lặp
        // dưới đây sẽ không chạy lần nào, tồn kho không bị trừ mà phiếu vẫn CONFIRMED.
        List<StockIssueItem> issueItems = stockIssueItemRepo.findByIssueId(issueId);
        if (issueItems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Phiếu xuất kho không có dòng hàng nào để xuất");
        }

        // Nhóm issue items theo itemId để trừ inventory 1 lần/item
        Map<Integer, BigDecimal> totalByItem = new HashMap<>();
        for (StockIssueItem item : issueItems) {
            // Nếu item này liên kết lô → giảm remainingQuantity của lô
            if (item.getEntryItemId() != null && item.getEntryItemId() > 0) {
                stockEntryRepo.decreaseRemainingQuantity(item.getEntryItemId(), item.getQuantity());
            }
            totalByItem.merge(item.getItemId(), item.getQuantity(), BigDecimal::add);
        }

        // Hàng theo serial: đánh dấu đã bán đúng số serial của từng lô vừa trừ
        itemSerialService.consumeForIssue(issueId,
                issueItems.stream()
                        .map(i -> new com.g42.platform.gms.warehouse.app.service.serial.ItemSerialService.IssueLine(
                                i.getItemId(), i.getEntryItemId(), i.getQuantity()))
                        .toList(),
                serialHolderEstimateItemIds(issue, ticketReservedAllocations));

        // Trừ inventory quantity + ghi log transaction
        for (Map.Entry<Integer, BigDecimal> entry : totalByItem.entrySet()) {
            Integer itemId = entry.getKey();
            BigDecimal needed = entry.getValue();

            Inventory inv = inventoryRepo
                    .findByWarehouseAndItemWithLock(issue.getWarehouseId(), itemId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                            "Không tìm thấy tồn kho cho itemId=" + itemId));

            BigDecimal newQty = Qty.sub(inv.getQuantity(), needed);
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

            if (Qty.lt(inv.getReservedQuantity(), alloc.getQuantity())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Reserved quantity không hợp lệ cho allocation id=" + alloc.getAllocationId());
            }

            // Giảm reserved (vì đã xuất thực)
            inv.setReservedQuantity(Qty.sub(inv.getReservedQuantity(), alloc.getQuantity()));
            inventoryRepo.save(inv);

            // Ghi audit log
            InventoryTransaction reservedTx = new InventoryTransaction();
            reservedTx.setWarehouseId(alloc.getWarehouseId());
            reservedTx.setItemId(alloc.getItemId());
            reservedTx.setTransactionType(InventoryTransactionType.ADJUSTMENT);
            reservedTx.setQuantity(alloc.getQuantity().negate());
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

        StockIssueResponse response = toResponse(findOrThrow(issueId));
        notifyManagersAboutStockIssue(issue, "Xác nhận phiếu xuất kho", "Phiếu xuất kho " + issue.getIssueCode() + " đã được xác nhận thành công.");
        return response;
    }

    private void notifyManagersAboutStockIssue(StockIssue issue, String title, String message) {
        try {
            java.util.Set<Integer> targetStaffIds = new java.util.HashSet<>();
            for (String role : List.of("MANAGER", "WAREHOUSE_MANAGER", "ADMIN")) {
                List<StaffProfile> profiles = staffProfileRepo.findByRoleCode(role);
                if (profiles != null) {
                    for (StaffProfile profile : profiles) {
                        if (profile.getStaffId() != null) {
                            targetStaffIds.add(profile.getStaffId());
                        }
                    }
                }
            }
            for (Integer managerId : targetStaffIds) {
                Integer senderId = issue.getCreatedBy();
                if (senderId == null || senderId == 0) {
                    senderId = managerId;
                }
                staffNotifyService.createNotificationAssignAuto(
                    managerId,
                    title + ": " + issue.getIssueCode(),
                    message,
                    senderId,
                    "http://localhost:5173/warehouse-stock-issues/" + issue.getIssueId()
                );
            }
        } catch (Exception e) {
            // Ignore error
        }
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
                    BigDecimal updatedReserved = Qty.subFloorZero(inventory.getReservedQuantity(), allocation.getQuantity());
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
        // Enrich: mã SKU — để hiển thị "Mã sản phẩm" thay vì lộ ra itemId (khóa DB) ngoài UI.
        Map<Integer, String> skuById = partCatalogRepo.findAllItemsByIds(itemIds.stream().toList()).stream()
                .collect(Collectors.toMap(CatalogItem::getItemId, CatalogItem::getSku, (a, b) -> a));

        // Tạo map từ itemId → allocationId để gắn vào response
        Map<Integer, Integer> allocationIdByItemId = new HashMap<>();
        if (issue.getIssueType() == IssueType.SERVICE_TICKET) {
            // Tìm allocations RESERVED hoặc COMMITTED của phiếu này
            List<StockAllocation> allocations = stockAllocationRepo.findByIssueIdAndStatus(issueId, AllocationStatus.RESERVED);
            if (allocations.isEmpty()) {
                allocations = stockAllocationRepo.findByIssueIdAndStatus(issueId, AllocationStatus.COMMITTED);
            }
            for (StockAllocation alloc : allocations) {
                allocationIdByItemId.put(alloc.getItemId(), alloc.getAllocationId());
            }
        }

        StockIssueDetailResponse resp = new StockIssueDetailResponse();
        resp.setIssueId(issue.getIssueId());
        resp.setIssueCode(issue.getIssueCode());
        resp.setWarehouseId(issue.getWarehouseId());
        resp.setIssueType(issue.getIssueType());
        resp.setIssueReason(issue.getIssueReason());
        resp.setServiceTicketId(issue.getServiceTicketId());
        resp.setReceiverName(issue.getReceiverName());
        resp.setReceiverPhone(issue.getReceiverPhone());
        resp.setLicensePlate(issue.getLicensePlate());
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
            d.setSku(skuById.get(it.getItemId()));
            d.setEntryItemId(it.getEntryItemId());
            d.setAllocationId(allocationIdByItemId.get(it.getItemId()));

            if (it.getEntryItemId() != null && it.getEntryItemId() > 0) {
                stockEntryRepo.findItemById(it.getEntryItemId()).ifPresent(entryItem -> {
                    stockEntryRepo.findEntryById(entryItem.getEntryId()).ifPresent(entry -> {
                        d.setEntryCode(entry.getEntryCode());
                        d.setEntryLotCode(entry.getEntryCode());
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
        BigDecimal totalQty = resp.getItems().stream()
                .map(StockIssueDetailResponse.IssueItemDetail::getQuantity)
                .reduce(BigDecimal.ZERO, Qty::add);
        resp.setTotalQuantity(totalQty);

        BigDecimal totalVal = resp.getItems().stream()
                .map(item -> item.getFinalPrice() != null
                    ? item.getFinalPrice().multiply(Qty.nz(item.getQuantity()))
                    : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        resp.setTotalValue(totalVal);

        return resp;
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /**
     * Dòng báo giá có thể đang giữ serial cho phiếu xuất này: dòng của allocation, cộng mọi
     * dòng thuộc các bản báo giá của cùng phiếu dịch vụ (bản mới nhận lại serial của bản cũ
     * trong khi allocation vẫn trỏ về dòng bản cũ).
     */
    private List<Integer> serialHolderEstimateItemIds(StockIssue issue, List<StockAllocation> allocations) {
        Set<Integer> ids = new HashSet<>();
        allocations.stream().map(StockAllocation::getEstimateItemId).filter(Objects::nonNull).forEach(ids::add);
        if (issue.getServiceTicketId() != null) {
            List<Integer> estimateIds = estimateRepository.getListOfEstimateByServiceTiketCode(issue.getServiceTicketId())
                    .stream().map(com.g42.platform.gms.estimation.domain.entity.Estimate::getId).toList();
            if (!estimateIds.isEmpty()) {
                estimateItemRepository.findByEstimateIds(estimateIds).stream()
                        .map(com.g42.platform.gms.estimation.domain.entity.EstimateItem::getId)
                        .filter(Objects::nonNull)
                        .forEach(ids::add);
            }
        }
        return new ArrayList<>(ids);
    }

    /** Tổng hợp reserved quantity theo itemId từ allocation RESERVED chưa gắn phiếu. */
    private Map<Integer, BigDecimal> buildReservedByItemMap(CreateStockIssueRequest request) {
        if (request.getIssueType() != IssueType.SERVICE_TICKET || request.getServiceTicketId() == null) {
            return Map.of();
        }
        Map<Integer, BigDecimal> result = new HashMap<>();
        stockAllocationRepo
                .findByTicketAndWarehouseAndStatus(request.getServiceTicketId(), request.getWarehouseId(), AllocationStatus.RESERVED)
                .stream()
                .filter(a -> a.getIssueId() == null)
                .forEach(a -> result.merge(a.getItemId(), a.getQuantity(), BigDecimal::add));
        return result;
    }

    /**
     * Tính FIFO và tạo danh sách StockIssueItem cho một itemId.
     * - importPrice: theo từng lô consume (FIFO chuẩn)
     * - sellingPrice: ưu tiên market price, fallback dùng lô mới nhất * markup
     * - Nếu thiếu hàng: thêm placeholder row (entryItemId=0, giá=0)
     */
    private List<StockIssueItem> buildFifoItems(StockIssue issue, Integer itemId, BigDecimal needed, BigDecimal requestedDiscount, Integer selectedEntryItemId) {
        BigDecimal estimateUnitPrice = resolveEstimateUnitPrice(issue.getServiceTicketId(), issue.getWarehouseId(), itemId);

        // SERVICE_TICKET: discount kho không áp dụng, giá báo giá đã là giá chốt
        // NORMAL/WHOLESALE: áp discount kho
        BigDecimal discountRate = (issue.getIssueType() == IssueType.SERVICE_TICKET)
                ? BigDecimal.ZERO
                : (requestedDiscount != null && requestedDiscount.compareTo(BigDecimal.ZERO) != 0)
                        ? requestedDiscount
                        : discountService.resolveDiscountRate(itemId, issue.getIssueType(), needed);

        WarehousePricing marketPricing = pricingRepo
                .findActiveByWarehouseAndItem(issue.getWarehouseId(), itemId)
                .orElse(null);

        List<StockEntryItem> lots = new ArrayList<>();
        if (selectedEntryItemId != null && selectedEntryItemId > 0) {
            stockEntryRepo.findItemById(selectedEntryItemId).ifPresent(lots::add);
        } else {
            lots = stockEntryRepo.findFifoLots(issue.getWarehouseId(), itemId);
        }
        StockEntryItem latestLot = stockEntryRepo.findLatestLot(issue.getWarehouseId(), itemId).orElse(null);

        List<StockIssueItem> items = new ArrayList<>();
        BigDecimal remaining = needed;

        for (StockEntryItem lot : lots) {
            if (remaining.signum() <= 0) break;
            BigDecimal consume = Qty.min(remaining, lot.getRemainingQuantity());
            if (consume.signum() <= 0) continue;

            BigDecimal sellingPrice = resolveSellingPrice(marketPricing, latestLot, lot, issue.getIssueType());

            // final_price = giá kho thu được per unit (trước thuế, sau discount kho)
            // SERVICE_TICKET: dùng estimateUnitPrice (giá báo giá trước thuế)
            // NORMAL/WHOLESALE: export_price sau discount kho
            BigDecimal finalPrice = applyDiscount(
                    resolveFinalPriceBase(issue.getIssueType(), estimateUnitPrice, sellingPrice),
                    discountRate);

            items.add(StockIssueItem.builder()
                    .issueId(issue.getIssueId())
                    .itemId(itemId)
                    .entryItemId(lot.getEntryItemId())
                    .quantity(consume)
                    .exportPrice(sellingPrice)
                    .estimateUnitPrice(estimateUnitPrice)
                    .importPrice(lot.getImportPrice())
                    .discountRate(discountRate)
                    .finalPrice(finalPrice)
                    .build());

            remaining = remaining.subtract(consume);
        }

        // Placeholder row khi không đủ lô hàng trong kho
        if (remaining.signum() > 0) {
            items.add(StockIssueItem.builder()
                    .issueId(issue.getIssueId())
                    .itemId(itemId)
                    .entryItemId(0)
                    .quantity(remaining)
                    .exportPrice(BigDecimal.ZERO)
                    .estimateUnitPrice(estimateUnitPrice)
                    .importPrice(BigDecimal.ZERO)
                    .discountRate(discountRate)
                    .finalPrice(BigDecimal.ZERO)
                    .build());
        }

        return items;
    }

    /**
     * Tính giá bán:
     * 1. Market price (từ WarehousePricing) nếu có
     * 2. Lô mới nhất * markup nếu không có market price
     * 3. Lô hiện tại * markup làm fallback cuối
     * Áp dụng WHOLESALE_FACTOR (0.85) nếu là phiếu buôn.
     */
    private BigDecimal resolveSellingPrice(WarehousePricing pricing, StockEntryItem latestLot,
                                           StockEntryItem currentLot, IssueType issueType) {
        BigDecimal price;
        if (pricing != null) {
            if (issueType == IssueType.WHOLESALE) {
                price = (pricing.getSellingPriceWholesale() != null && pricing.getSellingPriceWholesale().compareTo(BigDecimal.ZERO) > 0)
                        ? pricing.getSellingPriceWholesale()
                        : pricing.getSellingPrice().multiply(WHOLESALE_FACTOR).setScale(2, RoundingMode.HALF_UP);
            } else {
                price = pricing.getSellingPrice();
            }
        } else {
            BigDecimal importPrice = BigDecimal.ZERO;
            BigDecimal multiplier = BigDecimal.ONE;
            StockEntryItem targetLot = latestLot != null ? latestLot : currentLot;
            if (targetLot != null) {
                importPrice = targetLot.getImportPrice();
                if (issueType == IssueType.WHOLESALE) {
                    multiplier = (targetLot.getMarkupMultiplierWholesale() != null && targetLot.getMarkupMultiplierWholesale().compareTo(BigDecimal.ZERO) > 0)
                            ? targetLot.getMarkupMultiplierWholesale()
                            : targetLot.getMarkupMultiplier().multiply(WHOLESALE_FACTOR).setScale(4, RoundingMode.HALF_UP);
                } else {
                    multiplier = targetLot.getMarkupMultiplier();
                }
            }
            price = importPrice.multiply(multiplier != null ? multiplier : BigDecimal.ONE).setScale(2, RoundingMode.HALF_UP);
        }
        return price;
    }

    /** Áp dụng discount rate (%) vào giá. */
    private BigDecimal applyDiscount(BigDecimal price, BigDecimal discountRate) {
        return price
                .multiply(BigDecimal.ONE.subtract(discountRate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)))
                .setScale(2, RoundingMode.HALF_UP);
    }

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

    private PricingPreview buildDraftPricingPreview(StockIssue issue, StockIssueItem item) {
        BigDecimal quantity = Qty.nz(item.getQuantity());
        BigDecimal discountRate = item.getDiscountRate() != null
                ? item.getDiscountRate()
                : discountService.resolveDiscountRate(item.getItemId(), issue.getIssueType(), quantity);

        List<StockEntryItem> lots;
        if (item.getEntryItemId() != null && item.getEntryItemId() > 0) {
            lots = new ArrayList<>();
            stockEntryRepo.findItemById(item.getEntryItemId()).ifPresent(lots::add);
        } else {
            lots = stockEntryRepo.findFifoLots(issue.getWarehouseId(), item.getItemId());
        }
        BigDecimal importPrice = computeAverageImportPrice(lots, quantity);

        StockEntryItem latestLot = stockEntryRepo.findLatestLot(issue.getWarehouseId(), item.getItemId()).orElse(null);
        WarehousePricing marketPricing = pricingRepo.findActiveByWarehouseAndItem(issue.getWarehouseId(), item.getItemId()).orElse(null);

        // Dùng lô đầu tiên làm currentLot fallback khi tính sellingPrice
        StockEntryItem firstLot = lots.isEmpty() ? null : lots.get(0);
        BigDecimal sellingPrice = (marketPricing != null || firstLot != null)
                ? resolveSellingPrice(marketPricing, latestLot, firstLot != null ? firstLot : latestLot, issue.getIssueType())
                : BigDecimal.ZERO;

        BigDecimal estimateUnitPrice = item.getEstimateUnitPrice() != null
                ? item.getEstimateUnitPrice()
                : resolveEstimateUnitPrice(issue.getServiceTicketId(), issue.getWarehouseId(), item.getItemId());

        BigDecimal finalPrice = applyDiscount(resolveFinalPriceBase(issue.getIssueType(), estimateUnitPrice, sellingPrice), discountRate);
        BigDecimal grossProfit = finalPrice.subtract(importPrice).setScale(2, RoundingMode.HALF_UP);

        return new PricingPreview(sellingPrice, importPrice, discountRate, finalPrice, grossProfit);
    }

    private BigDecimal computeAverageImportPrice(List<StockEntryItem> lots, BigDecimal quantityNeeded) {
        if (!Qty.isPositive(quantityNeeded) || lots.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal remaining = quantityNeeded;
        BigDecimal consumed = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;

        for (StockEntryItem lot : lots) {
            if (remaining.signum() <= 0) {
                break;
            }
            BigDecimal take = Qty.min(remaining, lot.getRemainingQuantity());
            if (take.signum() <= 0) {
                continue;
            }
            totalCost = totalCost.add(lot.getImportPrice().multiply(take));
            consumed = consumed.add(take);
            remaining = remaining.subtract(take);
        }

        if (consumed.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return totalCost.divide(consumed, 2, RoundingMode.HALF_UP);
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
        // Lấy row mới nhất (max id) khi cùng itemId có nhiều dòng trong 1 estimate
        return estimateItemRepository.findByEstimateId(latestEstimate.getId()).stream()
                .filter(i -> itemId.equals(i.getItemId()))
                .filter(i -> warehouseId == null || warehouseId.equals(i.getWarehouseId()))
                .filter(i -> !Boolean.TRUE.equals(i.getIsRemoved()))
                .filter(i -> Boolean.TRUE.equals(i.getIsChecked()))
                .max(java.util.Comparator.comparingInt(i -> i.getId() != null ? i.getId() : 0))
                .map(EstimateItem::getUnitPrice)
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

    public BigDecimal resolveDiscountRatePublic(Integer itemId, IssueType issueType, BigDecimal quantity) {
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
        r.setReceiverName(e.getReceiverName());
        r.setReceiverPhone(e.getReceiverPhone());
        r.setLicensePlate(e.getLicensePlate());
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

                if ((r.getReceiverName() == null || r.getReceiverName().trim().isEmpty()) && ticket.getCustomerId() != null) {
                    com.g42.platform.gms.customer.infrastructure.entity.CustomerProfileJpa cust =
                            customerProfileJpaRepo.findByCustomerId(ticket.getCustomerId());
                    if (cust != null) {
                        r.setReceiverName(cust.getFullName());
                        r.setReceiverPhone(cust.getPhone());
                    }
                }

                if ((r.getLicensePlate() == null || r.getLicensePlate().trim().isEmpty()) && ticket.getVehicleId() != null) {
                    vehicleRepository.findById(ticket.getVehicleId()).ifPresent(v -> {
                        r.setLicensePlate(v.getLicensePlate());
                        if (r.getReceiverName() == null || r.getReceiverName().trim().isEmpty()) {
                            if (v.getCustomer() != null) {
                                r.setReceiverName(v.getCustomer().getFullName());
                                r.setReceiverPhone(v.getCustomer().getPhone());
                            }
                        }
                    });
                }
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
