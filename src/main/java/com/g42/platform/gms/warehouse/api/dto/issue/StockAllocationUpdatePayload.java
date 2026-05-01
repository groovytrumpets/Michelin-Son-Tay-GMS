package com.g42.platform.gms.warehouse.api.dto.issue;
public record StockAllocationUpdatePayload(
        Integer estimateItemId,
        Integer allocationId,
        String newStatus
) {

}
