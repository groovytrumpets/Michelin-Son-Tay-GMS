package com.g42.platform.gms.warehouse.api.dto.request;

import com.g42.platform.gms.warehouse.api.dto.issue.CreateStockIssueRequest;
import lombok.Data;

import java.util.List;

/** Cập nhật phiếu xuất kho — chỉ khi DRAFT */
@Data
public class UpdateStockIssueRequest {
    private String issueReason;
    
    private String receiverName;
    private String receiverPhone;
    private String licensePlate;

    /** Nếu truyền thì replace toàn bộ items */
    private List<CreateStockIssueRequest.IssueItemRequest> items;
}
