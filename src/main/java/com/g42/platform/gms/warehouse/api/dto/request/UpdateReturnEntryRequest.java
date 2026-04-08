package com.g42.platform.gms.warehouse.api.dto.request;

import lombok.Data;

import java.util.List;

/** Cập nhật phiếu hoàn hàng — chỉ khi DRAFT */
@Data
public class UpdateReturnEntryRequest {
    private String returnReason;
    /** Nếu truyền thì replace toàn bộ items */
    private List<ReturnEntryItemRequest> items;
    private List<ReturnEntryItemRequest> exchangeItems;
}
