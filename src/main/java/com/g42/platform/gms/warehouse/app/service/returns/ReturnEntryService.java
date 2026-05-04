package com.g42.platform.gms.warehouse.app.service.returns;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.g42.platform.gms.auth.entity.StaffProfile;
import com.g42.platform.gms.auth.repository.StaffProfileRepo;
import com.g42.platform.gms.common.service.ImageUploadService;
import com.g42.platform.gms.estimation.api.internal.EstimateInternalApi;
import com.g42.platform.gms.warehouse.api.dto.request.CreateReturnEntryFormRequest;
import com.g42.platform.gms.warehouse.api.dto.request.CreateReturnEntryRequest;
import com.g42.platform.gms.warehouse.api.dto.request.ReturnEntryItemRequest;
import com.g42.platform.gms.warehouse.api.dto.request.PatchReturnItemRequest;
import com.g42.platform.gms.warehouse.api.dto.request.UpdateReturnEntryRequest;
import com.g42.platform.gms.warehouse.api.dto.response.ReturnEntryItemResponse;
import com.g42.platform.gms.warehouse.api.dto.response.ReturnEntryResponse;
import com.g42.platform.gms.warehouse.domain.enums.InventoryTransactionType;
import com.g42.platform.gms.warehouse.domain.enums.ReturnEntryStatus;
import com.g42.platform.gms.warehouse.domain.enums.ReturnType;
import com.g42.platform.gms.warehouse.domain.repository.InventoryRepo;
import com.g42.platform.gms.warehouse.domain.repository.InventoryTransactionRepo;
import com.g42.platform.gms.warehouse.domain.repository.ReturnEntryRepo;
import com.g42.platform.gms.warehouse.domain.repository.StockIssueRepo;
import com.g42.platform.gms.warehouse.domain.repository.WarehouseAttachmentRepo;
import com.g42.platform.gms.warehouse.domain.repository.PartCatalogRepo;
import com.g42.platform.gms.warehouse.domain.entity.Inventory;
import com.g42.platform.gms.warehouse.domain.entity.InventoryTransaction;
import com.g42.platform.gms.warehouse.domain.entity.ReturnEntry;
import com.g42.platform.gms.warehouse.domain.entity.ReturnEntryItem;
import com.g42.platform.gms.warehouse.domain.entity.StockIssue;
import com.g42.platform.gms.warehouse.domain.entity.StockIssueItem;
import com.g42.platform.gms.warehouse.domain.entity.StockAllocation;
import com.g42.platform.gms.warehouse.domain.entity.StockEntry;
import com.g42.platform.gms.warehouse.domain.entity.StockEntryItem;
import com.g42.platform.gms.warehouse.domain.entity.Warehouse;
import com.g42.platform.gms.warehouse.domain.entity.WarehouseAttachment;
import com.g42.platform.gms.warehouse.domain.enums.AllocationStatus;
import com.g42.platform.gms.warehouse.domain.repository.WarehouseRepo;
import com.g42.platform.gms.estimation.domain.repository.EstimateItemRepository;
import com.g42.platform.gms.estimation.domain.entity.EstimateItem;
import com.g42.platform.gms.warehouse.domain.repository.StockAllocationRepo;
import com.g42.platform.gms.service_ticket_management.domain.repository.ServiceTicketRepo;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
/**
 * Service xử lý toàn bộ luồng "Return Entry" (phiếu hoàn) trong module Warehouse.
 *
 * Mục đích chính:
 * - Tạo phiếu hoàn (khách trả / trả nhà cung cấp / đổi hàng) từ phiếu xuất gốc (StockIssue)
 * - Xác nhận phiếu hoàn: cập nhật `Inventory`, `StockEntryItem.remainingQuantity`,
 *   ghi `InventoryTransaction`, và release/adjust `StockAllocation` khi cần
 * - Hủy/patch/update phiếu hoàn trước khi confirm
 *
 * Các dependency chính (injected repos/services):
 * - `returnEntryRepo`: lưu/đọc `ReturnEntry` và `ReturnEntryItem` (domain port)
 * - `inventoryRepo`: đọc và cập nhật tồn kho; khi thay đổi quantity nên dùng
 *     `findByWarehouseAndItemWithLock(...)` ở service layer để đảm bảo SELECT FOR UPDATE
 * - `transactionRepo`: ghi log audit `InventoryTransaction` sau mọi thay đổi tồn kho
 * - `stockEntryRepo`: truy vấn lô (lots) và gọi `increaseRemainingQuantity(...)` khi hoàn
 * - `stockIssueRepo` / `stockIssueItemRepo`: tra cứu phiếu xuất nguồn và dòng xuất để map lô
 * - `stockAllocationRepo`: kiểm tra/điều chỉnh allocation (COMMITTED → RELEASED hoặc tách allocation)
 * - `estimateInternalApi`: gọi API nội bộ để cập nhật trạng thái estimate khi release allocation
 *
 * Luồng chính (tóm tắt):
 * 1) Tạo phiếu hoàn (Create): kiểm tra phiếu xuất nguồn (nếu có) đã CONFIRMED,
 *    validate allocation/entryItem, và nếu cần mở rộng dòng trả theo lô (FIFO).
 * 2) Xác nhận phiếu (Confirm):
 *    - `CUSTOMER_RETURN`: tăng tồn kho (IN); nếu có `entryItemId` thì tăng
 *      `StockEntryItem.remainingQuantity` của lô tương ứng; release/adjust allocation
 *      (COMMITTED → RELEASED hoặc giảm qty và tạo allocation RELEASED mới).
 *    - `SUPPLIER_RETURN`: giảm tồn kho (OUT) vì trả hàng cho nhà cung cấp.
 *    - `EXCHANGE`: vừa giảm hàng cấp (OUT) vừa có thể tăng lô mới (tùy trường hợp).
 *    - Ghi `InventoryTransaction` cho từng thay đổi để audit.
 * 3) Hủy phiếu (Cancel): chỉ cho phép khi phiếu đang ở trạng thái SUBMITTED (không tác
 *    động lên inventory).
 *
 * Ghi chú về đồng thời (concurrency) và nguyên tắc bất biến:
 * - Khi thay đổi `inventory.quantity` hoặc `inventory.reservedQuantity`, phải đọc
 *   bằng `findByWarehouseAndItemWithLock(...)` (SELECT FOR UPDATE) trong cùng một
 *   transaction để tránh race condition.
 * - Khi cập nhật `StockEntryItem.remainingQuantity` (thuật toán FIFO), repo dùng
 *   `@Modifying` UPDATE (cập nhật nguyên tử) để tránh ghi đè khi có nhiều request
 *   thao tác cùng lô đồng thời.
 * - Allocation chỉ được release khi ở trạng thái `COMMITTED`; khi hoàn một phần,
 *   allocation gốc sẽ giảm qty và tạo allocation mới ở trạng thái `RELEASED`.
 */
public class ReturnEntryService {

    private static final String FOLDER_RETURN_ENTRY = "garage/warehouse/return-entry";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final AtomicInteger SEQ = new AtomicInteger(0);

    private final ReturnEntryRepo returnEntryRepo;
    private final InventoryRepo inventoryRepo;
    private final InventoryTransactionRepo transactionRepo;
    private final WarehouseAttachmentRepo attachmentRepo;
    private final ImageUploadService imageUploadService;
    private final WarehouseRepo warehouseRepo;
    private final StockIssueRepo stockIssueRepo;
    private final com.g42.platform.gms.warehouse.domain.repository.StockIssueItemRepo stockIssueItemRepo;
    private final StockAllocationRepo stockAllocationRepo;
    private final com.g42.platform.gms.warehouse.domain.repository.StockEntryRepo stockEntryRepo;
    private final StaffProfileRepo staffProfileRepo;
    private final PartCatalogRepo partCatalogRepo;
    private final EstimateItemRepository estimateItemRepository;
    private final ServiceTicketRepo serviceTicketRepo;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final EstimateInternalApi estimateInternalApi;

    @Transactional
    public ReturnEntryResponse create(CreateReturnEntryRequest request, Integer staffId) {
        // Phiếu xuất nguồn phải CONFIRMED trước khi tạo phiếu hoàn
        validateSourceIssueConfirmed(request.getSourceIssueId());

        // Khởi tạo phiếu hoàn với metadata cơ bản
        ReturnEntry entry = new ReturnEntry();
        entry.setReturnCode(generateCode());
        entry.setWarehouseId(request.getWarehouseId());
        entry.setReturnReason(request.getReturnReason());
        entry.setSourceIssueId(request.getSourceIssueId());
        entry.setReturnType(request.getReturnType() != null ? request.getReturnType() : ReturnType.CUSTOMER_RETURN);
        entry.setStatus(ReturnEntryStatus.SUBMITTED); // chưa ảnh hưởng tồn kho
        entry.setCreatedBy(staffId);

        // Lưu trước để có returnId gắn vào các item con
        ReturnEntry saved = returnEntryRepo.save(entry);

        // Xử lý từng dòng hàng trả
        for (ReturnEntryItemRequest itemReq : request.getItems()) {
            validateAllocation(request.getSourceIssueId(), itemReq); // kiểm tra qty không vượt allocation
            validateDuplicateAllocationOnCreate(itemReq.getAllocationId());
            // Mở rộng 1 dòng request thành nhiều dòng theo lô (FIFO từ issue gốc)
            saved.getItems().addAll(expandReturnItemsByLot(itemReq, saved.getReturnId(), request.getSourceIssueId()));
        }

        // Xử lý hàng đổi (cấp lại cho khách) — không ảnh hưởng allocation trả
        if (request.getExchangeItems() != null) {
            for (ReturnEntryItemRequest itemReq : request.getExchangeItems()) {
                validateEntryItem(request.getWarehouseId(), itemReq);
                saved.getItems().add(buildExchangeItem(itemReq, saved.getReturnId()));
            }
        }

        return toResponse(returnEntryRepo.save(saved));
    }

    @Transactional
    public ReturnEntryResponse createWithAttachments(CreateReturnEntryFormRequest req, Integer staffId) throws IOException {
        // Bắt đầu: parse và validate input multipart form
        // - `items` là JSON string, cần parse thành List<ReturnEntryItemRequest>
        // - `exchangeItems` tương tự nếu có
        // - gom các MultipartFile từ các trường files và file_0..file_4
        // Mọi lỗi parse/thiếu file sẽ ném ResponseStatusException (BAD_REQUEST / UNPROCESSABLE_ENTITY)
        List<ReturnEntryItemRequest> items;
        try {
            items = objectMapper.readValue(req.getItems(),
                    new TypeReference<List<ReturnEntryItemRequest>>() {});
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "items không hợp lệ: " + e.getMessage());
        }
        //buy x get y
//        Map<Integer, Integer> returnAllocationMap = items.stream()
//                .collect(Collectors.toMap(ReturnEntryItemRequest::getAllocationId, ReturnEntryItemRequest::getQuantity));
//            estimateInternalApi.validatePromotion(returnAllocationMap);

        // Tạo DTO nội bộ `CreateReturnEntryRequest` từ form đã parse
        // (sử dụng cùng cấu trúc như endpoint JSON để tái sử dụng `create(...)`)
        CreateReturnEntryRequest request = new CreateReturnEntryRequest();
        request.setWarehouseId(req.getWarehouseId());
        request.setReturnReason(req.getReturnReason());
        request.setSourceIssueId(req.getSourceIssueId());
        request.setReturnType(req.getReturnType() != null ? req.getReturnType() : ReturnType.CUSTOMER_RETURN);
        request.setItems(items);

        if (req.getExchangeItems() != null && !req.getExchangeItems().isBlank()) {
            try {
                request.setExchangeItems(objectMapper.readValue(req.getExchangeItems(),
                        new TypeReference<List<ReturnEntryItemRequest>>() {}));
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "exchangeItems không hợp lệ: " + e.getMessage());
            }
        }

        // Gom tất cả multipart files vào 1 list để gắn vào return entry
        List<MultipartFile> files = new java.util.ArrayList<>();
        // Gom từ List<files>
        if (req.getFiles() != null) {
            req.getFiles().stream().filter(f -> f != null && !f.isEmpty()).forEach(files::add);
        }
        // Gom từ file_0..file_4 (tương thích ngược)
        java.util.stream.Stream.of(req.getFile_0(), req.getFile_1(), req.getFile_2(), req.getFile_3(), req.getFile_4())
                .filter(f -> f != null && !f.isEmpty())
                .forEach(files::add);

        // Bắt buộc phải có ít nhất 1 ảnh kèm theo (business rule UI expects it)
        if (files.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "Cần đính kèm ít nhất 1 ảnh chứng minh");
        }

        // Gọi lại service `create(...)` để tái sử dụng luồng tạo phiếu (logic đã test)
        ReturnEntryResponse created = create(request, staffId);

        // Gắn các file ảnh lên item đầu tiên (không phải exchange)
        // Lưu ý: hiện thiết kế backend gán tất cả ảnh cho 1 dòng item, FE cần tương ứng
        ReturnEntry saved = findOrThrow(created.getReturnId());
        ReturnEntryItem firstItem = saved.getItems().stream()
                .filter(i -> !i.isExchangeItem())
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Không tìm thấy item để gắn ảnh"));

        for (MultipartFile file : files) {
            String url = imageUploadService.uploadImage(file, FOLDER_RETURN_ENTRY);
            WarehouseAttachment attachment = new WarehouseAttachment();
            attachment.setRefType(WarehouseAttachment.RefType.RETURN_ENTRY_ITEM);
            attachment.setRefId(firstItem.getReturnItemId());
            attachment.setFileUrl(url);
            attachment.setUploadedBy(staffId);
            attachmentRepo.save(attachment);
        }

        return toResponse(findOrThrow(created.getReturnId()));
    }
    @Transactional
    public void addAttachment(Integer returnItemId, MultipartFile file, Integer staffId) throws IOException {
        ReturnEntryItem item = returnEntryRepo.findItemById(returnItemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy return_entry_item id=" + returnItemId));

        ReturnEntry entry = findOrThrow(item.getReturnId());
        if (entry.getStatus() == ReturnEntryStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Không thể thêm ảnh cho phiếu đã xác nhận");
        }

        String url = imageUploadService.uploadImage(file, FOLDER_RETURN_ENTRY);

        WarehouseAttachment attachment = new WarehouseAttachment();
        attachment.setRefType(WarehouseAttachment.RefType.RETURN_ENTRY_ITEM);
        attachment.setRefId(returnItemId);
        attachment.setFileUrl(url);
        attachment.setUploadedBy(staffId);
        attachmentRepo.save(attachment);
    }
    @Transactional(readOnly = true)
    public List<ReturnEntryResponse> listByWarehouse(Integer warehouseId) {
        return returnEntryRepo.findByWarehouseId(warehouseId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<ReturnEntryResponse> searchByWarehouse(Integer warehouseId,
                                                       ReturnEntryStatus status,
                                                       ReturnType returnType,
                                                       LocalDate fromDate,
                                                       LocalDate toDate,
                                                       String search,
                                                       int page,
                                                       int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return returnEntryRepo.search(warehouseId, status, returnType, fromDate, toDate, search, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ReturnEntryResponse getDetail(Integer returnId) {
        return toResponse(findOrThrow(returnId));
    }

    @Transactional
    public ReturnEntryResponse patchItem(Integer returnId, Integer returnItemId, PatchReturnItemRequest request) {
        ReturnEntry entry = findOrThrow(returnId);
        if (entry.getStatus() != ReturnEntryStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Chỉ có thể sửa phiếu ở trạng thái DRAFT");
        }
        ReturnEntryItem item = returnEntryRepo.findItemById(returnItemId)
                .filter(i -> i.getReturnId().equals(returnId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy item id=" + returnItemId + " trong phiếu id=" + returnId));

        if (request.getQuantity() != null) item.setQuantity(request.getQuantity());
        if (request.getConditionNote() != null) item.setConditionNote(request.getConditionNote());

        returnEntryRepo.saveItem(item);
        return toResponse(findOrThrow(returnId));
    }

    @Transactional
    public ReturnEntryResponse update(Integer returnId, UpdateReturnEntryRequest request) {
        ReturnEntry entry = findOrThrow(returnId);
        if (entry.getStatus() != ReturnEntryStatus.SUBMITTED) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Chỉ có thể sửa phiếu ở trạng thái SUBMITTED");
        }
        validateSourceIssueConfirmed(entry.getSourceIssueId());
        if (request.getReturnReason() != null) entry.setReturnReason(request.getReturnReason());

        if (request.getItems() != null || request.getExchangeItems() != null) {
            entry.getItems().clear();
            if (request.getItems() != null) {
                for (ReturnEntryItemRequest itemReq : request.getItems()) {
                    validateAllocation(entry.getSourceIssueId(), itemReq);
                    resolveIssueItemAndEntryFromAllocation(itemReq);
                    validateEntryItem(entry.getWarehouseId(), itemReq);
                    validateDuplicateAllocationOnUpdate(itemReq.getAllocationId(), returnId);

                    ReturnEntryItem item = new ReturnEntryItem();
                    item.setReturnId(returnId);
                    item.setItemId(itemReq.getItemId());
                    item.setAllocationId(itemReq.getAllocationId());
                    item.setSourceIssueItemId(itemReq.getSourceIssueItemId());
                    item.setEntryItemId(itemReq.getEntryItemId());
                    item.setQuantity(itemReq.getQuantity());
                    item.setConditionNote(itemReq.getConditionNote());
                    item.setExchangeItem(false);
                    entry.getItems().add(item);
                }
            }
            if (request.getExchangeItems() != null) {
                for (ReturnEntryItemRequest itemReq : request.getExchangeItems()) {
                    validateEntryItem(entry.getWarehouseId(), itemReq);
                    ReturnEntryItem item = new ReturnEntryItem();
                    item.setReturnId(returnId);
                    item.setItemId(itemReq.getItemId());
                    item.setAllocationId(itemReq.getAllocationId());
                    item.setEntryItemId(itemReq.getEntryItemId());
                    item.setQuantity(itemReq.getQuantity());
                    item.setConditionNote(null);
                    item.setExchangeItem(true);
                    entry.getItems().add(item);
                }
            }
        }
        return toResponse(returnEntryRepo.save(entry));
    }

    /**
     * Hủy phiếu hoàn (Cancel).
     *
     * Quy tắc:
     * - Chỉ cho phép hủy khi phiếu ở trạng thái SUBMITTED (chưa confirm). Phiếu đã
     *   CONFIRMED không thể hủy vì đã tác động lên inventory.
     * - Hủy chỉ thay đổi trạng thái sang CANCELLED và lưu; không tác động inventory
     *   hay allocation (vì confirm mới thay đổi allocation/inventory).
     *
     * @param returnId id phiếu cần hủy
     * @param staffId id nhân viên thực hiện
     */
    @Transactional
    public ReturnEntryResponse cancel(Integer returnId, Integer staffId) {
        // Tải phiếu hoàn từ DB; ném NOT_FOUND nếu không tồn tại
        ReturnEntry entry = findOrThrow(returnId);

        // Không cho phép hủy phiếu đã xác nhận vì đã tác động tới tồn kho
        if (entry.getStatus() == ReturnEntryStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Không thể hủy phiếu hoàn đã được xác nhận");
        }

        // Nếu đã bị hủy, trả lỗi CONFLICT
        if (entry.getStatus() == ReturnEntryStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phiếu hoàn đã bị hủy");
        }
        // Đánh dấu là CANCELLED và lưu; không tác động inventory hay allocation
        entry.setStatus(ReturnEntryStatus.CANCELLED);
        return toResponse(returnEntryRepo.save(entry));
    }

    /**
     * Xác nhận (confirm) phiếu hoàn.
     *
     * Mục tiêu: khi confirm, phiếu hoàn sẽ thực sự ảnh hưởng tới tồn kho và allocation:
     * - CUSTOMER_RETURN: tăng `inventory.quantity` (IN), nếu dòng có `entryItemId` thì
     *   gọi `stockEntryRepo.increaseRemainingQuantity(...)` để cộng lại remainingQuantity của lô;
     *   release/adjust allocation tương ứng (CALL `estimateInternalApi.releaseEstimate` và
     *   `releaseAllocation(...)` để chuyển COMMITTED → RELEASED hoặc tách allocation).
     * - SUPPLIER_RETURN: giảm `inventory.quantity` (OUT) vì hàng được trả về nhà cung cấp.
     * - EXCHANGE: kết hợp cả giảm hàng cấp (OUT) và có thể tăng lại lô tương ứng.
     *
     * Quy trình thực hiện (chi tiết):
     * 1. Kiểm tra trạng thái phiếu là SUBMITTED
     * 2. Phân tách items thành returnItems (thực tế trả) và exchangeItems (hàng cấp đổi)
     * 3. Với mỗi returnItem:
     *    - Lấy hoặc tạo Inventory bằng `getOrCreateInventory(...)` (sử dụng lock khi đọc)
     *    - Tính `newQty` theo ReturnType (IN/OUT)
     *    - Ghi `inventoryRepo.save(inv)` (trong cùng transaction)
     *    - Ghi audit `InventoryTransaction` bằng `saveTransaction(...)`
     *    - Nếu item liên kết `entryItemId` (lô cụ thể): gọi
     *      `stockEntryRepo.increaseRemainingQuantity(entryItemId, qty)` để làm cho lô
     *      có thêm remainingQuantity (lô trở lại khả dụng)
     *    - Nếu item liên kết `allocationId`: gọi `estimateInternalApi.releaseEstimate(...)`
     *      để cập nhật estimate, sau đó `releaseAllocation(...)` để điều chỉnh allocation
     * 4. Xử lý exchangeItems: giảm inventory và (nếu có) tăng remainingQuantity tương ứng
     * 5. Set trạng thái phiếu = CONFIRMED, ghi confirmedBy/At
     *
     * Concurrency notes:
     * - `getOrCreateInventory` dùng `inventoryRepo.findByWarehouseAndItemWithLock(...)`
     *   để đảm bảo SELECT FOR UPDATE trước khi cập nhật quantity.
     * - `stockEntryRepo.increaseRemainingQuantity(...)` là @Modifying UPDATE (atomic) nên
     *   an toàn khi nhiều request thao tác lô cùng lúc.
     *
     * @param returnId id phiếu cần confirm
     * @param staffId id nhân viên thực hiện
     */
    @Transactional
    public ReturnEntryResponse confirm(Integer returnId, Integer staffId) {
        ReturnEntry entry = findOrThrow(returnId);

        // Chỉ confirm được khi đang SUBMITTED
        if (entry.getStatus() != ReturnEntryStatus.SUBMITTED) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Chỉ có thể confirm phiếu ở trạng thái SUBMITTED");
        }

        // Tách items: dòng trả thực tế vs dòng hàng đổi cấp lại
        List<ReturnEntryItem> returnItems = entry.getItems().stream()
                .filter(i -> !i.isExchangeItem()).collect(Collectors.toList());
        List<ReturnEntryItem> exchangeItems = entry.getItems().stream()
                .filter(ReturnEntryItem::isExchangeItem).collect(Collectors.toList());

        if (returnItems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Phiếu hoàn không có sản phẩm nào");
        }

        ReturnType type = entry.getReturnType();

        // Cập nhật tồn kho và lô cho từng dòng trả
        for (ReturnEntryItem item : returnItems) {
            Inventory inv = getOrCreateInventory(entry.getWarehouseId(), item.getItemId());

            // SUPPLIER_RETURN: trả về nhà cung cấp → giảm tồn kho (OUT)
            // CUSTOMER_RETURN: khách trả lại → tăng tồn kho (IN)
            boolean isSupplierReturn = type == ReturnType.SUPPLIER_RETURN;
            int newQty = isSupplierReturn
                    ? Math.max(0, inv.getQuantity() - item.getQuantity())
                    : inv.getQuantity() + item.getQuantity();
            InventoryTransactionType txType = isSupplierReturn
                    ? InventoryTransactionType.OUT
                    : InventoryTransactionType.IN;

            inv.setQuantity(newQty);
            inventoryRepo.save(inv);

            // Ghi audit log
            saveTransaction(entry.getWarehouseId(), item.getItemId(), item.getEntryItemId(),
                    txType, item.getQuantity(), newQty, returnId, staffId);

            // Cộng lại remainingQuantity của lô nhập nếu item gắn với lô cụ thể
            if (item.getEntryItemId() != null) {
                stockEntryRepo.increaseRemainingQuantity(item.getEntryItemId(), item.getQuantity());
            }
        }

        // Release allocation: gộp qty theo allocationId để tránh gọi nhiều lần
        // khi 1 allocation được split thành nhiều lot rows trong return_entry_item
        Map<Integer, Integer> qtyByAllocationId = new java.util.LinkedHashMap<>();
        for (ReturnEntryItem item : returnItems) {
            if (item.getAllocationId() != null) {
                qtyByAllocationId.merge(item.getAllocationId(), item.getQuantity(), Integer::sum);
            }
        }
        for (Map.Entry<Integer, Integer> e : qtyByAllocationId.entrySet()) {
            // Cập nhật estimate trước, lấy estimateItemId mới để gắn vào allocation RELEASED
            Integer savedEstimateItemId = estimateInternalApi.releaseEstimate(e.getKey(), e.getValue(), staffId);
            releaseAllocation(e.getKey(), e.getValue(), staffId, savedEstimateItemId);
        }

        // Xử lý hàng đổi (EXCHANGE): cấp hàng mới cho khách → giảm tồn kho (OUT)
        if (type == ReturnType.EXCHANGE) {
            for (ReturnEntryItem item : exchangeItems) {
                Inventory inv = getOrCreateInventory(entry.getWarehouseId(), item.getItemId());
                int newQty = Math.max(0, inv.getQuantity() - item.getQuantity());
                inv.setQuantity(newQty);
                inventoryRepo.save(inv);
                saveTransaction(entry.getWarehouseId(), item.getItemId(), item.getEntryItemId(),
                        InventoryTransactionType.OUT, item.getQuantity(), newQty, returnId, staffId);
                if (item.getEntryItemId() != null) {
                    stockEntryRepo.increaseRemainingQuantity(item.getEntryItemId(), item.getQuantity());
                }
            }
        }

        // Đánh dấu phiếu đã xác nhận
        entry.setStatus(ReturnEntryStatus.CONFIRMED);
        entry.setConfirmedBy(staffId);
        entry.setConfirmedAt(LocalDateTime.now());
        return toResponse(returnEntryRepo.save(entry));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /** Lấy inventory với SELECT FOR UPDATE lock, hoặc tạo mới (chưa persist) nếu chưa có. */
    private Inventory getOrCreateInventory(Integer warehouseId, Integer itemId) {
        return inventoryRepo.findByWarehouseAndItemWithLock(warehouseId, itemId)
                .orElseGet(() -> Inventory.builder()
                        .warehouseId(warehouseId)
                        .itemId(itemId)
                        .quantity(0)
                        .reservedQuantity(0)
                        .build());
    }

    /** Ghi audit log cho mỗi thay đổi tồn kho. */
    private void saveTransaction(Integer warehouseId, Integer itemId, Integer entryItemId,
                                 InventoryTransactionType type, int qty, int balance,
                                 Integer returnId, Integer staffId) {
        InventoryTransaction tx = new InventoryTransaction();
        tx.setWarehouseId(warehouseId);
        tx.setItemId(itemId);
        tx.setEntryItemId(entryItemId);
        tx.setTransactionType(type);
        tx.setQuantity(qty);
        tx.setBalanceAfter(balance);
        tx.setReferenceType("return_entry");
        tx.setReferenceId(returnId);
        tx.setCreatedById(staffId);
        tx.setCreatedAt(Instant.now());
        transactionRepo.save(tx);
    }

    /**
     * Release / adjust an allocation when items are returned.
     *
     * Behaviour:
     * - Only allow releasing if the allocation is currently COMMITTED.
     * - If the returned quantity equals allocation.quantity -> set allocation.status = RELEASED
     * - If partial return: subtract returnQuantity from allocation.quantity and
     *   create a new allocation record in RELEASED state representing the returned qty.
     *
     * Integration note:
     * - Calls to `estimateInternalApi.releaseEstimate(...)` (performed before calling
     *   this method) provide updated estimate item id to link the new released allocation.
     * - Persisting the new released allocation happens via `stockAllocationRepo.save(...)`.
     *
     * @param allocationId id allocation gốc (COMMITTED)
     * @param returnQuantity số lượng đang hoàn
     * @param staffId id nhân viên thực hiện
     * @param savedEstimateItemId estimate item id trả về từ estimateInternalApi (có thể null)
     */
    private void releaseAllocation(Integer allocationId, Integer returnQuantity, Integer staffId, Integer savedEstimateItemId) {
        StockAllocation allocation = stockAllocationRepo.findById(allocationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy allocation id=" + allocationId));

        // Chỉ release được khi allocation đang COMMITTED
        if (allocation.getStatus() != AllocationStatus.COMMITTED) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Chỉ được hoàn khi allocation đã COMMITTED");
        }

        // Số lượng hoàn không được vượt quá qty còn lại của allocation
        if (allocation.getQuantity() == null || allocation.getQuantity() < returnQuantity) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Số lượng hoàn vượt quá allocation đã chọn");
        }

        // Hoàn toàn bộ → chuyển allocation sang RELEASED
        if (allocation.getQuantity().equals(returnQuantity)) {
            allocation.setStatus(AllocationStatus.RELEASED);
            stockAllocationRepo.save(allocation);
            return;
        }

        // Hoàn một phần → giảm qty allocation gốc, tạo allocation RELEASED mới cho phần đã hoàn
        allocation.setQuantity(allocation.getQuantity() - returnQuantity);
        stockAllocationRepo.save(allocation);

        // Resolve estimateId cho allocation mới
        Integer estimateId = allocation.getEstimateId();
        if (estimateId == null && allocation.getEstimateItemId() != null) {
            EstimateItem estimateItem = estimateItemRepository.findByEstimateItemId(allocation.getEstimateItemId());
            if (estimateItem != null) estimateId = estimateItem.getEstimateId();
        }

        // Tạo allocation RELEASED đại diện cho phần đã hoàn
        StockAllocation released = new StockAllocation();
        released.setServiceTicketId(allocation.getServiceTicketId());
        released.setEstimateItemId(savedEstimateItemId); // estimateItemId từ releaseEstimate API
        released.setEstimateId(estimateId);
        released.setWarehouseId(allocation.getWarehouseId());
        released.setItemId(allocation.getItemId());
        released.setIssueId(allocation.getIssueId());
        released.setQuantity(returnQuantity);
        released.setStatus(AllocationStatus.RELEASED);
        released.setCreatedBy(allocation.getCreatedBy() != null ? allocation.getCreatedBy() : staffId);
        stockAllocationRepo.save(released);
    }

    private List<ReturnEntryItem> expandReturnItemsByLot(
            ReturnEntryItemRequest itemReq, Integer returnId, Integer sourceIssueId) {
        List<ReturnEntryItem> result = new ArrayList<>();

        // Không có allocationId → không cần phân lô, trả 1 dòng tổng
        if (itemReq.getAllocationId() == null) {
            result.add(buildReturnItem(itemReq, returnId, null, null, itemReq.getQuantity()));
            return result;
        }

        // Lấy allocation để xác định issueId
        StockAllocation allocation = stockAllocationRepo.findById(itemReq.getAllocationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Không tìm thấy allocation id=" + itemReq.getAllocationId()));

        // Ưu tiên issueId từ allocation, fallback sang sourceIssueId
        Integer issueId = allocation.getIssueId() != null ? allocation.getIssueId() : sourceIssueId;
        if (issueId == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Không xác định được phiếu xuất cho allocation=" + itemReq.getAllocationId());
        }

        // Lấy các issue items có entryItemId hợp lệ (đã gắn lô) của itemId này
        List<StockIssueItem> issueItems = stockIssueItemRepo.findByIssueId(issueId).stream()
                .filter(si -> itemReq.getItemId().equals(si.getItemId()))
                .filter(si -> si.getEntryItemId() != null && si.getEntryItemId() > 0)
                .collect(Collectors.toList());

        // Không có lô → fallback 1 dòng gắn allocationId
        if (issueItems.isEmpty()) {
            result.add(buildReturnItem(itemReq, returnId, itemReq.getAllocationId(), null, itemReq.getQuantity()));
            return result;
        }

        // Phân bổ số lượng trả theo thứ tự lô (FIFO theo thứ tự issue items)
        int remaining = itemReq.getQuantity() != null ? itemReq.getQuantity() : 0;
        for (StockIssueItem si : issueItems) {
            if (remaining <= 0) break;
            int consume = Math.min(remaining, si.getQuantity() != null ? si.getQuantity() : 0);
            if (consume <= 0) continue;
            // Tạo 1 dòng hoàn gắn với lô cụ thể (entryItemId) và issueItemId
            ReturnEntryItem item = buildReturnItem(itemReq, returnId, itemReq.getAllocationId(), si.getIssueItemId(), consume);
            item.setEntryItemId(si.getEntryItemId());
            result.add(item);
            remaining -= consume;
        }

        // Nếu còn dư (không đủ lô) → thêm 1 dòng không gắn lô
        if (remaining > 0) {
            result.add(buildReturnItem(itemReq, returnId, itemReq.getAllocationId(), null, remaining));
        }

        return result;
    }

    /** Tạo ReturnEntryItem từ request với các field cơ bản. */
    private ReturnEntryItem buildReturnItem(ReturnEntryItemRequest req, Integer returnId,
                                             Integer allocationId, Integer sourceIssueItemId, int quantity) {
        ReturnEntryItem item = new ReturnEntryItem();
        item.setReturnId(returnId);
        item.setItemId(req.getItemId());
        item.setAllocationId(allocationId);
        item.setSourceIssueItemId(sourceIssueItemId);
        item.setQuantity(quantity);
        item.setConditionNote(req.getConditionNote());
        item.setExchangeItem(false);
        return item;
    }

    /** Tạo ReturnEntryItem cho hàng đổi (exchange). */
    private ReturnEntryItem buildExchangeItem(ReturnEntryItemRequest req, Integer returnId) {
        ReturnEntryItem item = new ReturnEntryItem();
        item.setReturnId(returnId);
        item.setItemId(req.getItemId());
        item.setAllocationId(req.getAllocationId());
        item.setEntryItemId(req.getEntryItemId());
        item.setQuantity(req.getQuantity());
        item.setConditionNote(null);
        item.setExchangeItem(true);
        return item;
    }

    private ReturnEntry findOrThrow(Integer returnId) {
        return returnEntryRepo.findById(returnId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy phiếu hàng trả id=" + returnId));
    }

    private String generateCode() {
        String date = LocalDate.now().format(DATE_FMT);
        int seq = SEQ.incrementAndGet();
        String candidate = String.format("TH-%s-%d", date, seq);
        while (returnEntryRepo.existsByCode(candidate)) {
            candidate = String.format("TH-%s-%d", date, SEQ.incrementAndGet());
        }
        return candidate;
    }

    private ReturnEntryResponse toResponse(ReturnEntry e) {
        ReturnEntryResponse r = new ReturnEntryResponse();
        r.setReturnId(e.getReturnId());
        r.setReturnCode(e.getReturnCode());
        r.setWarehouseId(e.getWarehouseId());
        r.setReturnReason(e.getReturnReason());
        r.setReturnType(e.getReturnType());
        r.setSourceIssueId(e.getSourceIssueId());
        r.setStatus(e.getStatus());
        r.setConfirmedBy(e.getConfirmedBy());
        r.setConfirmedAt(e.getConfirmedAt());
        r.setCreatedBy(e.getCreatedBy());
        r.setCreatedAt(e.getCreatedAt());

        if (e.getWarehouseId() != null) {
            Warehouse warehouse = warehouseRepo.findById(e.getWarehouseId()).orElse(null);
            if (warehouse != null) {
                r.setWarehouseCode(warehouse.getWarehouseCode());
                r.setWarehouseName(warehouse.getWarehouseName());
            }
        }

        if (e.getSourceIssueId() != null) {
            stockIssueRepo.findById(e.getSourceIssueId()).ifPresent(sourceIssue -> {
                r.setSourceIssueCode(sourceIssue.getIssueCode());
                // Lấy serviceTicketId/Code từ phiếu xuất nguồn
                if (sourceIssue.getServiceTicketId() != null) {
                    r.setServiceTicketId(sourceIssue.getServiceTicketId());
                    com.g42.platform.gms.service_ticket_management.domain.entity.ServiceTicket ticket =
                            serviceTicketRepo.findByServiceTicketId(sourceIssue.getServiceTicketId());
                    if (ticket != null) {
                        r.setServiceTicketCode(ticket.getTicketCode());
                    }
                }
            });
        }

        if (e.getCreatedBy() != null) {
            StaffProfile createdBy = staffProfileRepo.findById(e.getCreatedBy()).orElse(null);
            if (createdBy != null) r.setCreatedByName(createdBy.getFullName());
        }

        if (e.getConfirmedBy() != null) {
            StaffProfile confirmedBy = staffProfileRepo.findById(e.getConfirmedBy()).orElse(null);
            if (confirmedBy != null) r.setConfirmedByName(confirmedBy.getFullName());
        }

        Set<Integer> itemIds = e.getItems().stream()
                .map(ReturnEntryItem::getItemId)
                .collect(Collectors.toSet());
        Map<Integer, String> itemNameById = partCatalogRepo.findNamesByIds(itemIds.stream().toList());

        // Lấy giá từ stock_issue_item để hiển thị unitPrice
        Map<Integer, java.math.BigDecimal> priceByIssueItemId = new java.util.HashMap<>();
        Map<Integer, String> issueItemCodeById = new java.util.HashMap<>();
        if (e.getSourceIssueId() != null) {
            stockIssueItemRepo.findByIssueId(e.getSourceIssueId()).forEach(si -> {
                if (si.getIssueItemId() != null) {
                    priceByIssueItemId.put(si.getIssueItemId(), si.getFinalPrice());
                }
            });
        }

        r.setItems(e.getItems().stream().map(i -> {
            ReturnEntryItemResponse ir = new ReturnEntryItemResponse();
            ir.setReturnItemId(i.getReturnItemId());
            ir.setItemId(i.getItemId());
            ir.setItemName(itemNameById.get(i.getItemId()));
            ir.setAllocationId(i.getAllocationId());
            ir.setSourceIssueItemId(i.getSourceIssueItemId());
            ir.setEntryItemId(i.getEntryItemId());
            ir.setQuantity(i.getQuantity());
            ir.setConditionNote(i.getConditionNote());

            // Bổ sung giá từ issue item
            if (i.getSourceIssueItemId() != null) {
                java.math.BigDecimal unitPrice = priceByIssueItemId.get(i.getSourceIssueItemId());
                ir.setUnitPrice(unitPrice);
                if (unitPrice != null && i.getQuantity() != null) {
                    ir.setTotalPrice(unitPrice.multiply(java.math.BigDecimal.valueOf(i.getQuantity())));
                }
            }

            // Bổ sung thông tin lô nhập
            if (i.getEntryItemId() != null) {
                stockEntryRepo.findItemById(i.getEntryItemId()).ifPresent(entryItem -> {
                    stockEntryRepo.findEntryById(entryItem.getEntryId()).ifPresent(entry -> {
                        ir.setEntryCode(entry.getEntryCode());
                        ir.setEntryLotCode(entry.getEntryCode() + "-LOT" + entryItem.getEntryItemId());
                    });
                });
            }

            ir.setAttachmentUrls(
                    attachmentRepo.findByRefTypeAndRefId(
                                    WarehouseAttachment.RefType.RETURN_ENTRY_ITEM,
                                    i.getReturnItemId())
                            .stream()
                            .map(WarehouseAttachment::getFileUrl)
                            .collect(Collectors.toList())
            );
            return ir;
        }).collect(Collectors.toList()));

        return r;
    }

    private void validateAllocation(Integer sourceIssueId, ReturnEntryItemRequest itemReq) {
        // Mục tiêu validateAllocation:
        // - đảm bảo allocation tồn tại và thuộc sourceIssue nếu sourceIssueId được cung cấp
        // - đảm bảo itemId khớp
        // - allocation phải ở trạng thái COMMITTED
        // - tổng số lượng đã hoàn (đã active) + requested <= tổng qty gốc của allocation
        if (sourceIssueId == null && itemReq.getAllocationId() == null) {
            return;
        }

        if (sourceIssueId == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "sourceIssueId là bắt buộc khi truyền allocationId");
        }

        if (itemReq.getAllocationId() == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "allocationId là bắt buộc khi có sourceIssueId");
        }

        // Lấy phiếu xuất nguồn để xác thực allocation.issueId nếu cần
        StockIssue sourceIssue = stockIssueRepo.findById(sourceIssueId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Không tìm thấy phiếu xuất nguồn id=" + sourceIssueId));

        // Lấy allocation gốc
        StockAllocation allocation = stockAllocationRepo.findById(itemReq.getAllocationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Không tìm thấy allocation id=" + itemReq.getAllocationId()));

        // Nếu allocation chứa issueId, phải khớp với sourceIssueId
        if (allocation.getIssueId() != null && !allocation.getIssueId().equals(sourceIssueId)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "allocationId không thuộc phiếu xuất nguồn đã chọn");
        }

        if (allocation.getItemId() == null || !allocation.getItemId().equals(itemReq.getItemId())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "allocationId không khớp với itemId");
        }

        // Allocation phải ở trạng thái COMMITTED để có thể release khi trả
        if (allocation.getStatus() != AllocationStatus.COMMITTED) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "allocationId phải ở trạng thái COMMITTED");
        }

        // Kiểm tra tổng quantity đã hoàn (active) không vượt quá tổng quantity gốc của allocation
        // Khi hoàn một phần, allocation bị tách: qty giảm xuống, tạo allocation mới RELEASED.
        // Tổng gốc = qty hiện tại + tổng đã hoàn active trước đó.
        // Tính tổng đã hoàn trước đó (active) để đảm bảo không vượt quá tổng gốc
        int alreadyReturned = returnEntryRepo.sumActiveReturnedQuantityByAllocationId(itemReq.getAllocationId());
        int requestedQty = itemReq.getQuantity() != null ? itemReq.getQuantity() : 0;
        int originalQty = allocation.getQuantity() + alreadyReturned; // tổng qty gốc
        // Nếu tổng (đã hoàn + lần này) vượt quá tổng gốc -> lỗi
        if (alreadyReturned + requestedQty > originalQty) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Tổng số lượng hoàn (" + (alreadyReturned + requestedQty) + ") vượt quá tổng allocation ("
                            + originalQty + ")");
        }

    }

    private void validateEntryItem(Integer warehouseId, ReturnEntryItemRequest itemReq) {
        // Nếu FE không truyền entryItemId thì không cần kiểm tra thêm
        if (itemReq.getEntryItemId() == null) {
            return;
        }

        // Lấy thông tin lô nhập (StockEntryItem) theo id
        StockEntryItem lot = stockEntryRepo.findItemById(itemReq.getEntryItemId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Không tìm thấy lô nhập id=" + itemReq.getEntryItemId()));

        // Kiểm tra itemId của lô phải khớp với itemId được yêu cầu
        if (lot.getItemId() == null || !lot.getItemId().equals(itemReq.getItemId())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "entryItemId không khớp với itemId");
        }

        // Lô phải gắn với một phiếu nhập (entryId): nếu thiếu, đó là dữ liệu bất thường
        if (lot.getEntryId() == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "Không xác định được phiếu nhập của lô đã chọn");
        }

        // Kiểm tra phiếu nhập phải thuộc cùng kho với return request
        StockEntry entry = stockEntryRepo.findEntryById(lot.getEntryId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Không tìm thấy phiếu nhập của lô id=" + itemReq.getEntryItemId()));

        if (entry.getWarehouseId() == null || !entry.getWarehouseId().equals(warehouseId)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "entryItemId không thuộc kho đã chọn");
        }
    }

    private void resolveIssueItemAndEntryFromAllocation(ReturnEntryItemRequest itemReq) {
        if (itemReq.getAllocationId() == null) {
            return;
        }

        StockAllocation allocation = stockAllocationRepo.findById(itemReq.getAllocationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Không tìm thấy allocation id=" + itemReq.getAllocationId()));

        // Ưu tiên issueId từ allocation; fallback sang sourceIssueId từ request (allocation cũ không có issueId)
        Integer issueId = allocation.getIssueId();
        if (issueId == null) {
            issueId = itemReq.getSourceIssueId();
        }

        if (issueId == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Không xác định được phiếu xuất cho allocation=" + itemReq.getAllocationId()
                            + ". Vui lòng truyền sourceIssueId");
        }

        List<StockIssueItem> issueItems = stockIssueItemRepo.findByIssueId(issueId);
        List<StockIssueItem> candidates = issueItems.stream()
                .filter(i -> i.getItemId() != null && i.getItemId().equals(itemReq.getItemId()))
                .filter(i -> itemReq.getEntryItemId() == null
                        || (i.getEntryItemId() != null && i.getEntryItemId().equals(itemReq.getEntryItemId())))
                .collect(java.util.stream.Collectors.toList());

        StockIssueItem matchedIssueItem = null;

        if (candidates.size() == 1) {
            matchedIssueItem = candidates.get(0);
        } else if (candidates.size() > 1) {
            // Nhiều dòng cùng itemId: ưu tiên dòng có entryItemId hợp lệ (> 0)
            // Mỗi dòng đại diện cho 1 lô — lấy dòng đầu tiên có lô hợp lệ
            // entryItemId sẽ được set từ issue item, không cần FE truyền
            matchedIssueItem = candidates.stream()
                    .filter(i -> i.getEntryItemId() != null && i.getEntryItemId() > 0)
                    .findFirst()
                    .orElse(candidates.get(0));
        }

        if (matchedIssueItem == null) {
            // Fallback: allocation cũ có thể không còn issue item (đã bị xóa/thay thế).
            // Thử tìm bất kỳ issue item nào của itemId trong issueId đó.
            matchedIssueItem = issueItems.stream()
                    .filter(i -> itemReq.getItemId().equals(i.getItemId()))
                    .findFirst()
                    .orElse(null);
        }

        if (matchedIssueItem == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Không tìm thấy dòng xuất phù hợp từ allocation=" + itemReq.getAllocationId());
        }

        Integer resolvedIssueItemId = matchedIssueItem.getIssueItemId();
        if (resolvedIssueItemId == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Không xác định được issueItemId từ allocation đã chọn");
        }

        Integer resolvedEntryItemId = matchedIssueItem.getEntryItemId();
        if (resolvedEntryItemId == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Không xác định được entryItemId từ lô đã chọn");
        }

        itemReq.setSourceIssueItemId(resolvedIssueItemId);
        itemReq.setEntryItemId(resolvedEntryItemId);
    }

    private void validateRequiredAttachments(List<ReturnEntryItemRequest> items, MultipartFile[] files) {
        if (items == null || items.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "items không được rỗng");
        }

        if (items.size() > files.length) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Hiện chỉ hỗ trợ tối đa 5 ảnh (file_0..file_4) cho 5 dòng hàng trả");
        }

        for (int i = 0; i < items.size(); i++) {
            MultipartFile file = files[i];
            if (file == null || file.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Thiếu ảnh chứng minh cho item index " + i + " (file_" + i + ")");
            }
        }
    }

    private void validateSourceIssueConfirmed(Integer sourceIssueId) {
        if (sourceIssueId == null) {
            return;
        }

        // Kiểm tra tồn tại và trạng thái của phiếu xuất nguồn
        StockIssue sourceIssue = stockIssueRepo.findById(sourceIssueId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Không tìm thấy phiếu xuất nguồn id=" + sourceIssueId));

        // Chỉ cho phép tạo phiếu hoàn nếu phiếu xuất nguồn đã được CONFIRMED
        if (sourceIssue.getStatus() != com.g42.platform.gms.warehouse.domain.enums.StockIssueStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Chỉ được hoàn khi phiếu xuất nguồn đã CONFIRMED");
        }
    }

    private void validateDuplicateSourceIssueItemOnCreate(Integer sourceIssueItemId) {
        if (sourceIssueItemId == null) {
            return;
        }

        if (returnEntryRepo.existsAnyBySourceIssueItemId(sourceIssueItemId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Dòng xuất này đã có phiếu hoàn, không thể tạo trùng");
        }
    }

    private void validateDuplicateSourceIssueItemOnUpdate(Integer sourceIssueItemId, Integer returnId) {
        if (sourceIssueItemId == null) {
            return;
        }

        if (returnEntryRepo.existsAnyBySourceIssueItemIdExcludingReturnId(sourceIssueItemId, returnId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Dòng xuất này đã có phiếu hoàn khác, không thể cập nhật trùng");
        }
    }

    private void validateDuplicateAllocationOnCreate(Integer allocationId) {
        // NO-OP hiện tại: không chặn tạo nhiều phiếu hoàn cùng `allocationId` ở layer này.
        // Lý do: ràng buộc số lượng/over-return được kiểm tra trong `validateAllocation()`.
        // Nếu trong tương lai muốn nghiêm ngặt hơn (chặn duplicate hoàn song song),
        // có thể thêm kiểm tra ở đây dựa trên business rule.
    }

    private void validateDuplicateAllocationOnUpdate(Integer allocationId, Integer returnId) {
        // Không block update nhiều phiếu hoàn cho cùng allocationId.
        // Việc kiểm soát số lượng đã được xử lý trong validateAllocation().
    }
}
