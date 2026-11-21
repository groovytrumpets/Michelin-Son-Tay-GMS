package com.g42.platform.gms.report.api.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Thân POST /api/reports/data-export/download.
 *
 * @param exportToken   token nhận được từ /verify (đặt trong body chứ không phải header vì CORS
 *                      chỉ cho phép vài header cố định)
 * @param types         mã {@code DataExportType}; rỗng = lỗi
 * @param dateBasis     RECEIVED (mặc định) | PAID
 * @param branchId      null = mọi xưởng
 * @param customerScope ALL (mặc định) | ACTIVE — chỉ khách có phiếu / lượt sổ cũ trong kỳ
 * @param includeLegacy null = có
 */
public record DataExportRequest(
        String exportToken,
        LocalDate from,
        LocalDate to,
        List<String> types,
        String dateBasis,
        Integer branchId,
        String customerScope,
        Boolean includeLegacy) {
}
