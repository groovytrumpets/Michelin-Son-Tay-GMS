package com.g42.platform.gms.estimation.api.internal;

import com.g42.platform.gms.estimation.api.dto.UsedEstimateItemDto;
import com.g42.platform.gms.estimation.domain.entity.Estimate;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

public interface EstimateInternalApi {
    List<Estimate> findAllByServiceTicketId(List<Integer> ticketIds);

    /**
     * Lấy các dòng phụ tùng/dịch vụ (chưa bị xóa) thuộc bản estimate mới nhất của mỗi serviceTicketId,
     * dùng để tổng hợp lịch sử phụ tùng/dịch vụ đã sử dụng theo khách hàng.
     */
    List<UsedEstimateItemDto> findUsedItemsHistoryByServiceTicketIds(List<Integer> ticketIds);

    Estimate findLatestByServiceTicketId(Integer serviceTicketId);

    Estimate findById(Integer estimateId);

    void linkEstimateToServiceTicket(Integer estimateId, Integer serviceTicketId);

    void updateBookingToRemindById(Integer reminderId, Integer bookingId);

    Integer releaseEstimate(Integer allocationId, Integer quantity, Integer staffId);

    void calculateAndLockGrossProfit(Integer serviceTicketId);
//
//    void validatePromotion(Map<Integer, Integer> returnAllocationMap);
}
