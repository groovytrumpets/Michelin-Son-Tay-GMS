package com.g42.platform.gms.warehouse.app.service.returns;

import com.g42.platform.gms.warehouse.api.dto.request.CreateReturnEntryFormRequest;
import com.g42.platform.gms.warehouse.api.dto.request.CreateReturnEntryRequest;
import com.g42.platform.gms.warehouse.api.dto.request.PatchReturnItemRequest;
import com.g42.platform.gms.warehouse.api.dto.request.UpdateReturnEntryRequest;
import com.g42.platform.gms.warehouse.api.dto.response.ReturnEntryResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface ReturnEntryService {

    /** Tạo phiếu hàng trả (DRAFT) với nhiều sản phẩm */
    ReturnEntryResponse create(CreateReturnEntryRequest request, Integer staffId);

    /**
     * Tạo phiếu hoàn + ảnh lỗi từng item trong 1 form (multipart/form-data).
     * file_0..file_4 tương ứng với item theo index.
     */
    ReturnEntryResponse createWithAttachments(CreateReturnEntryFormRequest request, Integer staffId) throws IOException;

    /**
     * Đính kèm ảnh lỗi cho một sản phẩm cụ thể trong phiếu hoàn.
     * @param returnItemId ID của return_entry_item
     */
    void addAttachment(Integer returnItemId, MultipartFile file, Integer staffId) throws IOException;

    /** Danh sách phiếu hoàn theo kho, mới nhất trước */
    List<ReturnEntryResponse> listByWarehouse(Integer warehouseId);

    /** Chi tiết phiếu hoàn kèm danh sách sản phẩm */
    ReturnEntryResponse getDetail(Integer returnId);

    /** Cập nhật từng item trong phiếu hoàn — chỉ khi DRAFT */
    ReturnEntryResponse patchItem(Integer returnId, Integer returnItemId, PatchReturnItemRequest request);

    /** Cập nhật phiếu hoàn — chỉ khi DRAFT */
    ReturnEntryResponse update(Integer returnId, UpdateReturnEntryRequest request);

    /** Xác nhận hàng trả → cộng inventory cho từng item, ghi audit log */
    ReturnEntryResponse confirm(Integer returnId, Integer staffId);
}
