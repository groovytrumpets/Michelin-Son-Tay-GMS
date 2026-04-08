package com.g42.platform.gms.warehouse.app.service.allocation;

import com.g42.platform.gms.warehouse.api.dto.response.StockAllocationResult;
import com.g42.platform.gms.warehouse.app.service.dto.StockShortageInfo;

import java.util.List;

public interface StockAllocationService {

    /**
     * Reserve hàng theo estimateId.
     * Trả về danh sách shortage nếu có item thiếu hàng (không tạo allocation cho item đó).
     */
    List<StockShortageInfo> reserve(Integer estimateId, Integer staffId);

    /** Commit khi ServiceTicket PAID → trừ current_quantity, sinh StockIssue */
    void commit(Integer serviceTicketId, Integer staffId);

    /** Release khi ServiceTicket CANCELLED → giảm reserved_quantity */
    void release(Integer serviceTicketId, Integer staffId);

    /** Cập nhật số lượng allocation (khi ticket chưa hoàn thành) */
    StockAllocationResult updateAllocation(Integer allocationId, int newQuantity, Integer staffId);
}
