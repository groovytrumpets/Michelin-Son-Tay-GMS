package com.g42.platform.gms.warehouse.app.service.returns;

import com.g42.platform.gms.warehouse.api.dto.request.CreateReturnEntryFormRequest;
import com.g42.platform.gms.warehouse.api.dto.request.CreateReturnEntryRequest;
import com.g42.platform.gms.warehouse.api.dto.response.ReturnEntryResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

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

    /** Xác nhận hàng trả → cộng inventory cho từng item, ghi audit log */
    ReturnEntryResponse confirm(Integer returnId, Integer staffId);
}
