package com.g42.platform.gms.warehouse.app.service.entry;

/**
 * ============================================================
 * LUỒNG NHẬP KHO (Stock Entry Flow)
 * ============================================================
 *
 * Nhập kho là quá trình ghi nhận hàng hóa từ nhà cung cấp vào kho.
 * Luồng gồm 3 giai đoạn chính:
 *
 *  [1] TẠO PHIẾU (DRAFT)
 *      Nhân viên tạo phiếu nhập với danh sách hàng hóa và giá nhập.
 *      Phiếu ở trạng thái DRAFT — chưa ảnh hưởng đến tồn kho.
 *      Có 2 cách tạo:
 *        - POST /stock-entries          → JSON only (ảnh upload sau)
 *        - POST /stock-entries/with-attachment → JSON + ảnh trong 1 form
 *
 *  [2] CHỈNH SỬA (vẫn DRAFT)
 *      Trong khi còn DRAFT, nhân viên có thể:
 *        - PUT  /{id}              → sửa thông tin header (NCC, ngày, ghi chú, thay items)
 *        - PATCH /{id}/items/{itemId} → sửa từng dòng item (số lượng, giá, markup)
 *        - POST /{id}/attachments  → upload thêm ảnh chứng từ
 *
 *  [3] XÁC NHẬN (DRAFT → CONFIRMED)
 *      POST /{id}/confirm
 *      Khi xác nhận, hệ thống:
 *        a. Kiểm tra có ảnh chứng từ (bắt buộc)
 *        b. Kiểm tra phiếu có ít nhất 1 item
 *        c. Tăng inventory.quantity cho từng item (dùng row-lock tránh race condition)
 *        d. Ghi InventoryTransaction để audit trail
 *        e. Đánh dấu phiếu CONFIRMED — không thể sửa nữa
 *
 * Sau khi CONFIRMED:
 *   - Tồn kho (Inventory) đã được cộng thêm
 *   - Mỗi lô hàng (StockEntryItem) có remainingQuantity = quantity ban đầu
 *   - remainingQuantity sẽ giảm dần khi có phiếu xuất kho (StockIssue) theo FIFO
 *
 * Sơ đồ trạng thái:
 *   DRAFT ──[confirm]──► CONFIRMED
 *     ▲
 *     └── có thể sửa thoải mái khi còn DRAFT
 *
 * ============================================================
 */

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.g42.platform.gms.auth.entity.StaffProfile;
import com.g42.platform.gms.auth.repository.StaffProfileRepo;
import com.g42.platform.gms.common.service.ImageUploadService;
import com.g42.platform.gms.warehouse.api.dto.entry.CreateStockEntryRequest;
import com.g42.platform.gms.warehouse.api.dto.entry.CreateStockEntryWithAttachmentRequest;
import com.g42.platform.gms.warehouse.api.dto.entry.StockEntryItemRequest;
import com.g42.platform.gms.warehouse.api.dto.request.PatchEntryItemRequest;
import com.g42.platform.gms.warehouse.api.dto.request.UpdateStockEntryRequest;
import com.g42.platform.gms.warehouse.api.dto.response.StockEntryItemResponse;
import com.g42.platform.gms.warehouse.api.dto.response.StockEntryResponse;
import com.g42.platform.gms.warehouse.domain.entity.Inventory;
import com.g42.platform.gms.warehouse.domain.entity.InventoryTransaction;
import com.g42.platform.gms.warehouse.domain.entity.StockEntry;
import com.g42.platform.gms.warehouse.domain.entity.StockEntryItem;
import com.g42.platform.gms.warehouse.domain.entity.Warehouse;
import com.g42.platform.gms.warehouse.domain.entity.WarehouseAttachment;
import com.g42.platform.gms.warehouse.domain.enums.InventoryTransactionType;
import com.g42.platform.gms.warehouse.domain.enums.StockEntryStatus;
import com.g42.platform.gms.warehouse.domain.repository.InventoryRepo;
import com.g42.platform.gms.warehouse.domain.repository.InventoryTransactionRepo;
import com.g42.platform.gms.warehouse.domain.repository.PartCatalogRepo;
import com.g42.platform.gms.warehouse.domain.repository.StockEntryRepo;
import com.g42.platform.gms.warehouse.domain.repository.WarehouseAttachmentRepo;
import com.g42.platform.gms.warehouse.domain.repository.WarehouseRepo;
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
public class StockEntryService {

    // Thư mục lưu ảnh chứng từ trên cloud storage
    private static final String FOLDER_STOCK_ENTRY = "garage/warehouse/stock-entry";

    // Format ngày dùng trong mã phiếu: NK-20250504-1
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    // TODO: SEQ dùng AtomicInteger static sẽ reset về 0 khi restart app.
    //       Nên thay bằng sequence từ DB hoặc đọc max code hiện tại khi khởi động.
    //       Hiện tại vòng while check trùng là workaround tạm thời.
    private static final AtomicInteger SEQ = new AtomicInteger(0);

    // ── Repositories & Services được inject qua constructor (Lombok @RequiredArgsConstructor) ──
    private final StockEntryRepo stockEntryRepo;         // CRUD phiếu nhập + items
    private final InventoryRepo inventoryRepo;           // đọc/ghi tồn kho (có row-lock khi confirm)
    private final InventoryTransactionRepo transactionRepo; // ghi audit log mỗi lần tồn kho thay đổi
    private final ImageUploadService imageUploadService; // upload ảnh lên cloud storage
    private final WarehouseAttachmentRepo attachmentRepo;   // lưu metadata ảnh chứng từ
    private final WarehouseRepo warehouseRepo;           // lấy tên/mã kho để hiển thị
    private final StaffProfileRepo staffProfileRepo;     // lấy tên nhân viên để hiển thị
    private final PartCatalogRepo partCatalogRepo;       // lấy tên sản phẩm theo itemId (batch)

    // ObjectMapper để parse JSON string "items" từ multipart form
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    // ─────────────────────────────────────────────────────────────────────────
    // QUERY — đọc dữ liệu, không thay đổi gì
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Lấy toàn bộ phiếu nhập của 1 kho — dùng cho export Excel.
     * Không phân trang vì export cần lấy hết.
     *
     * @param status null = lấy tất cả trạng thái, không null = lọc theo trạng thái
     */
    @Transactional(readOnly = true)
    public List<StockEntryResponse> listByWarehouse(Integer warehouseId, StockEntryStatus status) {
        List<StockEntry> entries = (status != null)
                ? stockEntryRepo.findByWarehouseIdAndStatus(warehouseId, status)
                : stockEntryRepo.findByWarehouseId(warehouseId);
        return entries.stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * Tìm kiếm phiếu nhập có filter + phân trang — dùng cho màn hình danh sách.
     *
     * Các filter hỗ trợ:
     *   - status: DRAFT / CONFIRMED
     *   - fromDate / toDate: lọc theo entryDate
     *   - search: tìm theo mã phiếu hoặc tên nhà cung cấp
     *
     * Kết quả sắp xếp mới nhất lên đầu (DESC createdAt).
     */
    @Transactional(readOnly = true)
    public Page<StockEntryResponse> searchByWarehouse(
            Integer warehouseId,
            StockEntryStatus status,
            LocalDate fromDate,
            LocalDate toDate,
            String search,
            int page,
            int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return stockEntryRepo.search(warehouseId, status, fromDate, toDate, search, pageable)
                .map(this::toResponse);
    }

    /**
     * Lấy chi tiết 1 phiếu nhập theo ID.
     * Trả về đầy đủ: header + items + ảnh chứng từ + tên kho/nhân viên.
     */
    @Transactional(readOnly = true)
    public StockEntryResponse getById(Integer entryId) {
        return toResponse(findOrThrow(entryId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CREATE — Giai đoạn 1: Tạo phiếu nhập ở trạng thái DRAFT
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Tạo phiếu nhập DRAFT từ JSON request.
     *
     * Cách dùng: khi frontend muốn tạo phiếu trước, upload ảnh sau.
     * Ảnh chứng từ có thể upload riêng qua POST /{id}/attachments.
     *
     * Lưu ý: save 2 lần vì cần entryId trước để gán vào từng item.
     *   Lần 1: save header → lấy entryId được DB sinh ra
     *   Lần 2: save lại kèm items đã có entryId
     */
    @Transactional
    public StockEntryResponse create(CreateStockEntryRequest request, Integer staffId) {
        // Bước 1: Tạo header phiếu nhập, chưa có items
        StockEntry entry = StockEntry.builder()
                .entryCode(generateCode())           // sinh mã NK-yyyyMMdd-{seq}
                .warehouseId(request.getWarehouseId())
                .supplierName(request.getSupplierName())
                .entryDate(request.getEntryDate() != null ? request.getEntryDate() : LocalDate.now())
                .notes(request.getNotes())
                .status(StockEntryStatus.DRAFT)      // luôn bắt đầu bằng DRAFT
                .createdBy(staffId)
                .build();

        // Bước 2: Save lần 1 để lấy entryId từ DB
        StockEntry saved = stockEntryRepo.save(entry);

        // Bước 3: Build items với entryId vừa có, rồi save lần 2
        saved.setItems(buildEntryItems(request.getItems(), saved.getEntryId()));
        return toResponse(stockEntryRepo.save(saved));
    }

    /**
     * Tạo phiếu nhập + upload ảnh chứng từ trong 1 request multipart/form-data.
     *
     * Cách dùng: khi frontend muốn gửi ảnh ngay lúc tạo phiếu (tiện hơn cho mobile).
     * items phải truyền dưới dạng JSON string vì multipart không hỗ trợ nested object.
     *
     * Ví dụ field "items":
     *   [{"itemId":8,"quantity":50,"importPrice":45000,"markupMultiplier":1.5}]
     *
     * Luồng xử lý:
     *   1. Parse JSON string → List<StockEntryItemRequest>
     *   2. Gọi create() để tạo phiếu DRAFT
     *   3. Upload ảnh lên cloud, lưu URL vào WarehouseAttachment
     *   4. Trả về phiếu đã có ảnh
     */
    @Transactional
    public StockEntryResponse createWithAttachmentForm(
            CreateStockEntryWithAttachmentRequest req,
            Integer staffId) throws IOException {

        // Bước 1: Parse JSON string "items" thành list object
        List<StockEntryItemRequest> items = parseItemsJson(req.getItems());

        // Bước 2: Chuyển sang CreateStockEntryRequest để tái dùng create()
        CreateStockEntryRequest request = new CreateStockEntryRequest();
        request.setWarehouseId(req.getWarehouseId());
        request.setSupplierName(req.getSupplierName());
        request.setNotes(req.getNotes());
        request.setEntryDate(parseEntryDate(req.getEntryDate())); // parse "2025-05-04" → LocalDate
        request.setItems(items);

        // Bước 3: Tạo phiếu DRAFT
        StockEntryResponse created = create(request, staffId);

        // Bước 4: Upload ảnh và lưu attachment record
        uploadAndSaveAttachment(created.getEntryId(), req.getFile(), staffId);

        // Bước 5: Reload để trả về response đầy đủ (kèm URL ảnh vừa upload)
        return toResponse(findOrThrow(created.getEntryId()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UPDATE — Giai đoạn 2: Chỉnh sửa phiếu khi còn DRAFT
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Cập nhật thông tin header của phiếu nhập — chỉ khi DRAFT.
     *
     * Áp dụng partial update: chỉ field nào != null mới được cập nhật.
     * Nếu truyền items → replace toàn bộ danh sách items hiện tại (không merge từng cái).
     * Nếu không truyền items → giữ nguyên items cũ.
     *
     * Dùng khi: nhân viên muốn sửa tên NCC, ngày nhập, ghi chú, hoặc thay đổi nhiều items cùng lúc.
     */
    @Transactional
    public StockEntryResponse update(Integer entryId, UpdateStockEntryRequest request) {
        // Kiểm tra phiếu tồn tại và đang DRAFT (throw 404 / 422 nếu không hợp lệ)
        StockEntry entry = findDraftOrThrow(entryId);

        // Partial update: chỉ set khi có giá trị mới
        if (request.getSupplierName() != null) entry.setSupplierName(request.getSupplierName());
        if (request.getEntryDate() != null) entry.setEntryDate(request.getEntryDate());
        if (request.getNotes() != null) entry.setNotes(request.getNotes());

        // Nếu truyền items → xóa hết items cũ, thay bằng list mới
        if (request.getItems() != null) {
            entry.setItems(buildEntryItems(request.getItems(), entryId));
        }

        return toResponse(stockEntryRepo.save(entry));
    }

    /**
     * Sửa 1 dòng item cụ thể trong phiếu nhập — chỉ khi DRAFT.
     *
     * Dùng khi: nhân viên chỉ muốn sửa số lượng hoặc giá của 1 sản phẩm,
     * không muốn gửi lại toàn bộ danh sách items.
     *
     * Áp dụng partial update: field nào null → giữ nguyên giá trị cũ.
     *
     * Lưu ý quan trọng: khi sửa quantity khi còn DRAFT,
     * phải sync remainingQuantity = quantity mới.
     * Vì remainingQuantity chỉ có ý nghĩa sau khi CONFIRMED (dùng cho FIFO xuất kho),
     * nên khi còn DRAFT nó phải luôn bằng quantity.
     */
    @Transactional
    public StockEntryResponse patchItem(Integer entryId, Integer entryItemId, PatchEntryItemRequest request) {
        // Kiểm tra phiếu tồn tại và đang DRAFT
        findDraftOrThrow(entryId);

        // Tìm item, đồng thời verify item đó thuộc đúng phiếu này (tránh sửa nhầm item của phiếu khác)
        StockEntryItem item = stockEntryRepo.findItemById(entryItemId)
                .filter(i -> i.getEntryId().equals(entryId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy item id=" + entryItemId + " trong phiếu id=" + entryId));

        // Partial update từng field
        if (request.getQuantity() != null) {
            item.setQuantity(request.getQuantity());
            // Sync remainingQuantity = quantity khi còn DRAFT
            // (sau CONFIRMED, remainingQuantity sẽ giảm dần theo FIFO xuất kho)
            item.setRemainingQuantity(request.getQuantity());
        }
        if (request.getImportPrice() != null) item.setImportPrice(request.getImportPrice());
        if (request.getMarkupMultiplier() != null) item.setMarkupMultiplier(request.getMarkupMultiplier());
        if (request.getNotes() != null) item.setNotes(request.getNotes());

        stockEntryRepo.saveItem(item);

        // Reload toàn bộ phiếu để trả về response đầy đủ
        return toResponse(findOrThrow(entryId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ATTACHMENT — Upload ảnh chứng từ (bắt buộc trước khi confirm)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Upload ảnh chứng từ riêng cho phiếu đã tạo — chỉ khi DRAFT.
     *
     * Dùng khi: nhân viên tạo phiếu trước (POST /stock-entries),
     * sau đó chụp ảnh hóa đơn/biên bản và upload riêng qua endpoint này.
     *
     * Ảnh chứng từ là bắt buộc — confirm sẽ bị từ chối nếu chưa có ảnh.
     * Có thể upload nhiều ảnh cho 1 phiếu (gọi endpoint này nhiều lần).
     */
    @Transactional
    public void addAttachment(Integer entryId, MultipartFile file, Integer staffId) throws IOException {
        findDraftOrThrow(entryId); // chỉ cho phép upload khi còn DRAFT
        uploadAndSaveAttachment(entryId, file, staffId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CONFIRM — Giai đoạn 3: Xác nhận phiếu → cộng tồn kho
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Xác nhận phiếu nhập → cộng tồn kho (DRAFT → CONFIRMED).
     *
     * Đây là bước quan trọng nhất: sau khi confirm, hàng hóa chính thức
     * được ghi nhận vào kho và có thể xuất đi (StockIssue).
     *
     * Toàn bộ method chạy trong 1 transaction:
     *   - Nếu bất kỳ bước nào lỗi → rollback toàn bộ, tồn kho không bị thay đổi.
     *
     * Các bước kiểm tra trước khi confirm:
     *   [Guard 1] Phiếu chưa CONFIRMED (tránh confirm 2 lần → cộng kép tồn kho)
     *   [Guard 2] Có ảnh chứng từ đính kèm (bắt buộc theo quy trình)
     *   [Guard 3] Phiếu có ít nhất 1 item (không confirm phiếu rỗng)
     *
     * Sau khi qua guards, với mỗi item:
     *   - Tìm bản ghi Inventory (warehouse + item), dùng SELECT FOR UPDATE (row-lock)
     *     để tránh race condition khi 2 request confirm cùng lúc
     *   - Nếu chưa có bản ghi Inventory → tạo mới với quantity = 0
     *   - Cộng thêm quantity từ lô nhập vào tồn kho hiện tại
     *   - Ghi InventoryTransaction (audit log) để truy vết sau này
     *
     * Sau khi xử lý xong tất cả items:
     *   - Cập nhật status → CONFIRMED, ghi confirmedBy + confirmedAt
     *
     * Về remainingQuantity:
     *   - Khi tạo item: remainingQuantity = quantity (toàn bộ lô chưa xuất)
     *   - Sau mỗi lần xuất kho (StockIssue confirm): remainingQuantity giảm dần
     *   - Dùng để tính FIFO: lô nào nhập trước thì xuất trước
     */
    @Transactional
    public StockEntryResponse confirm(Integer entryId, Integer staffId) {
        StockEntry entry = findOrThrow(entryId);

        // [Guard 1] Không cho confirm 2 lần — tránh cộng kép tồn kho
        if (entry.getStatus() == StockEntryStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phiếu đã được xác nhận");
        }

        // [Guard 2] Bắt buộc phải có ảnh chứng từ (hóa đơn NCC, biên bản nhập)
        boolean hasAttachment = attachmentRepo.existsByRefTypeAndRefId(
                WarehouseAttachment.RefType.STOCK_ENTRY, entryId);
        if (!hasAttachment) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Cần đính kèm ảnh chứng từ trước khi xác nhận");
        }

        // [Guard 3] Phiếu phải có ít nhất 1 sản phẩm
        if (entry.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Phiếu nhập không có sản phẩm nào");
        }

        // Cộng tồn kho cho từng item trong phiếu
        for (StockEntryItem item : entry.getItems()) {
            increaseInventory(entry.getWarehouseId(), item, entryId, staffId);
        }

        // Đánh dấu phiếu đã xác nhận
        entry.setStatus(StockEntryStatus.CONFIRMED);
        entry.setConfirmedBy(staffId);
        entry.setConfirmedAt(LocalDateTime.now());

        return toResponse(stockEntryRepo.save(entry));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // INTERNAL / HELPER — các method private dùng nội bộ
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Dùng bởi PricingService để lấy giá nhập gần nhất của 1 sản phẩm làm fallback.
     *
     * Khi warehouse_pricing chưa được cấu hình cho sản phẩm đó,
     * hệ thống dùng: giá bán = importPrice × markupMultiplier từ lô nhập gần nhất.
     */
    public BigDecimal findLatesFallBackPrice(Integer itemId, Integer warehouseId) {
        return stockEntryRepo.findLatesFallBackPrice(itemId, warehouseId);
    }

    /**
     * Tăng tồn kho cho 1 item và ghi audit log.
     *
     * Dùng SELECT FOR UPDATE (row-lock) để tránh race condition:
     * Nếu 2 request confirm cùng lúc cho cùng 1 item trong cùng 1 kho,
     * request thứ 2 sẽ chờ request thứ 1 commit xong mới đọc số lượng mới.
     * → Đảm bảo tồn kho không bị tính sai.
     *
     * Nếu chưa có bản ghi Inventory (sản phẩm mới nhập lần đầu vào kho này)
     * → tạo mới với quantity = 0 trước khi cộng thêm.
     */
    private void increaseInventory(Integer warehouseId, StockEntryItem item, Integer entryId, Integer staffId) {
        // Tìm tồn kho hiện tại, dùng lock để tránh race condition
        Inventory inventory = inventoryRepo
                .findByWarehouseAndItemWithLock(warehouseId, item.getItemId())
                .orElseGet(() -> Inventory.builder()
                        .warehouseId(warehouseId)
                        .itemId(item.getItemId())
                        .quantity(0)
                        .reservedQuantity(0)
                        .build());

        // Cộng thêm số lượng từ lô nhập vào tồn kho
        int balanceAfter = inventory.getQuantity() + item.getQuantity();
        inventory.setQuantity(balanceAfter);
        inventoryRepo.save(inventory);

        // Ghi audit log để truy vết: ai nhập, bao nhiêu, tồn kho sau khi nhập là bao nhiêu
        saveInventoryTransaction(warehouseId, item.getItemId(), item.getQuantity(), balanceAfter, entryId, staffId);
    }

    /**
     * Ghi 1 bản ghi InventoryTransaction để audit trail.
     *
     * Mỗi lần tồn kho thay đổi (nhập/xuất/trả) đều phải ghi log này.
     * Dùng để:
     *   - Truy vết lịch sử tồn kho theo thời gian
     *   - Đối chiếu khi kiểm kê
     *   - Báo cáo nhập/xuất theo kỳ
     *
     * referenceType = "stock_entry" + referenceId = entryId
     * → biết log này sinh ra từ phiếu nhập nào.
     */
    private void saveInventoryTransaction(
            Integer warehouseId, Integer itemId,
            Integer quantity, Integer balanceAfter,
            Integer entryId, Integer staffId) {
        InventoryTransaction tx = new InventoryTransaction();
        tx.setWarehouseId(warehouseId);
        tx.setItemId(itemId);
        tx.setTransactionType(InventoryTransactionType.IN); // IN = nhập kho
        tx.setQuantity(quantity);           // số lượng thay đổi trong lần này
        tx.setBalanceAfter(balanceAfter);   // tồn kho sau khi thay đổi (để dễ tra cứu)
        tx.setReferenceType("stock_entry"); // loại chứng từ gốc
        tx.setReferenceId(entryId);         // ID chứng từ gốc
        tx.setCreatedById(staffId);
        tx.setCreatedAt(Instant.now());
        transactionRepo.save(tx);
    }

    /**
     * Upload file ảnh lên cloud storage và lưu metadata vào DB.
     *
     * WarehouseAttachment dùng pattern polymorphic reference:
     *   refType = STOCK_ENTRY / STOCK_ISSUE / RETURN_ENTRY
     *   refId   = ID của chứng từ tương ứng
     * → 1 bảng attachment dùng chung cho nhiều loại chứng từ.
     */
    private void uploadAndSaveAttachment(Integer entryId, MultipartFile file, Integer staffId) throws IOException {
        // Upload lên cloud, nhận về URL công khai
        String url = imageUploadService.uploadImage(file, FOLDER_STOCK_ENTRY);

        // Lưu metadata vào DB để query sau
        WarehouseAttachment attachment = new WarehouseAttachment();
        attachment.setRefType(WarehouseAttachment.RefType.STOCK_ENTRY);
        attachment.setRefId(entryId);
        attachment.setFileUrl(url);
        attachment.setUploadedBy(staffId);
        attachmentRepo.save(attachment);
    }

    /**
     * Chuyển list StockEntryItemRequest → list StockEntryItem domain entity.
     *
     * Lưu ý: remainingQuantity được set = quantity ngay khi tạo.
     * Ý nghĩa: toàn bộ lô hàng chưa xuất đi đâu cả.
     * Sau khi CONFIRMED và có StockIssue, remainingQuantity sẽ giảm dần theo FIFO.
     *
     * markupMultiplier mặc định = 1.0 nếu không truyền
     * (giá bán = giá nhập × 1.0 = bằng giá nhập, không markup).
     */
    private List<StockEntryItem> buildEntryItems(List<StockEntryItemRequest> requests, Integer entryId) {
        List<StockEntryItem> items = new ArrayList<>();
        for (StockEntryItemRequest req : requests) {
            items.add(StockEntryItem.builder()
                    .entryId(entryId)
                    .itemId(req.getItemId())
                    .quantity(req.getQuantity())
                    .importPrice(req.getImportPrice())
                    .markupMultiplier(req.getMarkupMultiplier() != null ? req.getMarkupMultiplier() : BigDecimal.ONE)
                    .remainingQuantity(req.getQuantity()) // ban đầu = quantity, chưa xuất gì
                    .notes(req.getNotes())
                    .build());
        }
        return items;
    }

    /**
     * Parse JSON string thành List<StockEntryItemRequest>.
     *
     * Cần thiết vì multipart/form-data không hỗ trợ nested JSON object trực tiếp.
     * Frontend phải serialize items thành string rồi gửi trong 1 field text.
     *
     * Ví dụ: items = "[{\"itemId\":8,\"quantity\":50,\"importPrice\":45000}]"
     */
    private List<StockEntryItemRequest> parseItemsJson(String itemsJson) {
        try {
            return objectMapper.readValue(itemsJson, new TypeReference<List<StockEntryItemRequest>>() {});
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "items không hợp lệ: " + e.getMessage());
        }
    }

    /**
     * Parse ngày nhập từ string "yyyy-MM-dd" → LocalDate.
     * Trả về hôm nay nếu string null hoặc blank.
     */
    private LocalDate parseEntryDate(String entryDate) {
        if (entryDate == null || entryDate.isBlank()) return LocalDate.now();
        return LocalDate.parse(entryDate);
    }

    /**
     * Tìm phiếu nhập theo ID, throw 404 nếu không tồn tại.
     * Dùng ở mọi nơi cần load phiếu — tránh lặp code try/orElseThrow.
     */
    private StockEntry findOrThrow(Integer entryId) {
        return stockEntryRepo.findById(entryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy phiếu nhập kho id=" + entryId));
    }

    /**
     * Tìm phiếu nhập đang DRAFT, throw lỗi nếu không hợp lệ.
     *   - 404 nếu phiếu không tồn tại
     *   - 422 nếu phiếu đã CONFIRMED (không thể sửa nữa)
     *
     * Dùng ở tất cả các method chỉ được phép chạy khi phiếu còn DRAFT.
     */
    private StockEntry findDraftOrThrow(Integer entryId) {
        StockEntry entry = findOrThrow(entryId);
        if (entry.getStatus() != StockEntryStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Chỉ có thể sửa phiếu ở trạng thái DRAFT");
        }
        return entry;
    }

    /**
     * Sinh mã phiếu nhập theo format: NK-yyyyMMdd-{seq}
     * Ví dụ: NK-20250504-1, NK-20250504-2, ...
     *
     * Vòng while check trùng là workaround cho trường hợp SEQ bị reset khi restart.
     * TODO: Thay SEQ bằng sequence từ DB để đảm bảo unique tuyệt đối.
     */
    private String generateCode() {
        String date = LocalDate.now().format(DATE_FMT);
        String candidate = String.format("NK-%s-%d", date, SEQ.incrementAndGet());
        while (stockEntryRepo.existsByCode(candidate)) {
            candidate = String.format("NK-%s-%d", date, SEQ.incrementAndGet());
        }
        return candidate;
    }

    /**
     * Map StockEntry domain entity → StockEntryResponse DTO để trả về cho client.
     *
     * Method này thực hiện nhiều query phụ để enrich dữ liệu:
     *   - warehouseRepo: lấy tên/mã kho
     *   - staffProfileRepo: lấy tên nhân viên tạo + xác nhận
     *   - partCatalogRepo.findNamesByIds(): batch lookup tên sản phẩm (tránh N+1)
     *   - attachmentRepo: lấy danh sách URL ảnh chứng từ
     *
     * Lưu ý hiệu năng: khi gọi toResponse() trong vòng lặp (listByWarehouse),
     * warehouse/staff queries sẽ bị lặp N lần. Cần cache hoặc batch load nếu list lớn.
     */
    private StockEntryResponse toResponse(StockEntry entry) {
        StockEntryResponse r = new StockEntryResponse();

        // Map các field cơ bản 1-1
        r.setEntryId(entry.getEntryId());
        r.setEntryCode(entry.getEntryCode());
        r.setWarehouseId(entry.getWarehouseId());
        r.setSupplierName(entry.getSupplierName());
        r.setEntryDate(entry.getEntryDate());
        r.setStatus(entry.getStatus());
        r.setNotes(entry.getNotes());
        r.setConfirmedBy(entry.getConfirmedBy());
        r.setConfirmedAt(entry.getConfirmedAt());
        r.setCreatedBy(entry.getCreatedBy());
        r.setCreatedAt(entry.getCreatedAt());

        // Enrich: tên và mã kho (để hiển thị trên UI, không cần client gọi thêm API)
        if (entry.getWarehouseId() != null) {
            warehouseRepo.findById(entry.getWarehouseId()).ifPresent(wh -> {
                r.setWarehouseCode(wh.getWarehouseCode());
                r.setWarehouseName(wh.getWarehouseName());
            });
        }

        // Enrich: tên nhân viên tạo phiếu
        if (entry.getCreatedBy() != null) {
            staffProfileRepo.findById(entry.getCreatedBy())
                    .map(StaffProfile::getFullName)
                    .ifPresent(r::setCreatedByName);
        }

        // Enrich: tên nhân viên xác nhận (null nếu chưa confirm)
        if (entry.getConfirmedBy() != null) {
            staffProfileRepo.findById(entry.getConfirmedBy())
                    .map(StaffProfile::getFullName)
                    .ifPresent(r::setConfirmedByName);
        }

        // Enrich: tên sản phẩm — batch lookup 1 query cho tất cả items (tránh N+1)
        // Ví dụ: phiếu có 10 items → 1 query lấy 10 tên, không phải 10 query riêng lẻ
        Set<Integer> itemIds = entry.getItems().stream()
                .map(StockEntryItem::getItemId)
                .collect(Collectors.toSet());
        Map<Integer, String> itemNameById = partCatalogRepo.findNamesByIds(itemIds.stream().toList());

        List<StockEntryItemResponse> itemResponses = entry.getItems().stream().map(i -> {
            StockEntryItemResponse ir = new StockEntryItemResponse();
            ir.setEntryItemId(i.getEntryItemId());
            ir.setItemId(i.getItemId());
            ir.setItemName(itemNameById.get(i.getItemId())); // lấy từ map đã batch load
            ir.setQuantity(i.getQuantity());
            ir.setImportPrice(i.getImportPrice());
            ir.setMarkupMultiplier(i.getMarkupMultiplier());
            ir.setRemainingQuantity(i.getRemainingQuantity()); // bao nhiêu chưa xuất kho
            ir.setNotes(i.getNotes());
            return ir;
        }).collect(Collectors.toList());
        r.setItems(itemResponses);

        // Enrich: danh sách URL ảnh chứng từ đính kèm
        if (entry.getEntryId() != null) {
            List<String> urls = attachmentRepo
                    .findByRefTypeAndRefId(WarehouseAttachment.RefType.STOCK_ENTRY, entry.getEntryId())
                    .stream()
                    .map(WarehouseAttachment::getFileUrl)
                    .collect(Collectors.toList());
            r.setAttachments(urls);
        }

        return r;
    }
}
