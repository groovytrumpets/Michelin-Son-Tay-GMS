package com.g42.platform.gms.warehouse.app.service.commission;

import com.g42.platform.gms.warehouse.api.dto.response.CommissionReportResponse;

import java.util.List;

public interface CommissionService {

    /**
     * Tính và ghi hoa hồng sau khi StockIssue được confirm.
     * @param issueId  ID phiếu xuất
     * @param staffId  ID nhân viên tư vấn
     */
    void processCommission(Integer issueId, Integer staffId);

    /** Báo cáo hoa hồng theo kỳ (MANAGER) */
    List<CommissionReportResponse> getCommissionReport(String periodMonth, Integer staffId);
}
