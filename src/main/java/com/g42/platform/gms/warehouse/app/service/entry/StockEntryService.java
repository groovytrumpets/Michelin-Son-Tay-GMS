package com.g42.platform.gms.warehouse.app.service.entry;

import com.g42.platform.gms.warehouse.api.dto.entry.CreateStockEntryRequest;
import com.g42.platform.gms.warehouse.api.dto.entry.CreateStockEntryWithAttachmentRequest;
import com.g42.platform.gms.warehouse.api.dto.response.StockEntryResponse;
import com.g42.platform.gms.warehouse.api.dto.request.PatchEntryItemRequest;
import com.g42.platform.gms.warehouse.api.dto.request.UpdateStockEntryRequest;
import com.g42.platform.gms.warehouse.domain.enums.StockEntryStatus;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface StockEntryService {

    /** Danh sách phiếu nhập theo kho, filter theo status (null = tất cả) */
    List<StockEntryResponse> listByWarehouse(Integer warehouseId, StockEntryStatus status);

    /** Lấy chi tiết 1 phiếu */
    StockEntryResponse getById(Integer entryId);

    /** Cập nhật từng item trong phiếu nhập — chỉ khi DRAFT */
    StockEntryResponse patchItem(Integer entryId, Integer entryItemId, PatchEntryItemRequest request);

    /** Cập nhật phiếu nhập kho — chỉ khi DRAFT */
    StockEntryResponse update(Integer entryId, UpdateStockEntryRequest request);

    /** Tạo phiếu nhập kho (DRAFT) */
    StockEntryResponse create(CreateStockEntryRequest request, Integer staffId);

    /** Tạo phiếu + đính kèm ảnh trong 1 request (multipart) */
    StockEntryResponse createWithAttachment(CreateStockEntryRequest request, MultipartFile file, Integer staffId) throws IOException;

    /** Tạo phiếu + đính kèm ảnh qua @ModelAttribute form (items là JSON string) */
    StockEntryResponse createWithAttachmentForm(CreateStockEntryWithAttachmentRequest request, Integer staffId) throws IOException;

    /** Đính kèm ảnh chứng từ */
    void addAttachment(Integer entryId, MultipartFile file, Integer staffId) throws IOException;

    /** Xác nhận phiếu nhập → cộng current_quantity, cập nhật import_price, ghi audit log */
    StockEntryResponse confirm(Integer entryId, Integer staffId);
}
