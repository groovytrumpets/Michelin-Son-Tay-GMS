package com.g42.platform.gms.warehouse.app.service.returns;

import com.g42.platform.gms.warehouse.api.dto.request.CreateReturnEntryRequest;
import com.g42.platform.gms.warehouse.api.dto.response.ReturnEntryResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface ReturnEntryService {

    /** Tạo phiếu hàng trả (DRAFT) với nhiều sản phẩm */
    ReturnEntryResponse create(CreateReturnEntryRequest request, Integer staffId);

    /**
     * Đính kèm ảnh lỗi cho một sản phẩm cụ thể trong phiếu hoàn.
     * @param returnItemId ID của return_entry_item
     */
    void addAttachment(Integer returnItemId, MultipartFile file, Integer staffId) throws IOException;

    /** Xác nhận hàng trả → cộng inventory cho từng item, ghi audit log */
    ReturnEntryResponse confirm(Integer returnId, Integer staffId);
}
