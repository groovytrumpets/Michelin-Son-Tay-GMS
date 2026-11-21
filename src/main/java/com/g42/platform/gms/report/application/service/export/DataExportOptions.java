package com.g42.platform.gms.report.application.service.export;

import java.time.LocalDate;
import java.util.Set;

/**
 * Lựa chọn của một lần xuất báo cáo tổng hợp, đã chuẩn hoá (from ≤ to, có ít nhất một loại).
 *
 * @param basis                mốc ngày chọn phiếu vào kỳ
 * @param branchId             null = mọi xưởng (tồn kho không theo xưởng — kho dùng chung)
 * @param activeCustomersOnly  true = chỉ khách có phiếu / lượt sổ cũ trong kỳ; false = cả danh bạ
 * @param includeLegacy        kèm sổ dịch vụ cũ (tab riêng, tiền luôn tách khỏi doanh thu hoá đơn)
 */
public record DataExportOptions(
        LocalDate from,
        LocalDate to,
        Set<DataExportType> types,
        DateBasis basis,
        Integer branchId,
        boolean activeCustomersOnly,
        boolean includeLegacy) {

    public enum DateBasis {
        /** Phiếu có ngày tiếp nhận trong kỳ — giống /customer-report. Hoá đơn chưa thu vẫn tính doanh thu. */
        RECEIVED("Ngày tiếp nhận phiếu"),
        /** Phiếu có hoá đơn ĐÃ THU trong kỳ (theo paid_at) — khớp sổ quỹ, chỉ gồm phiếu đã thu. */
        PAID("Ngày thu tiền");

        private final String label;

        DateBasis(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    public boolean has(DataExportType type) {
        return types.contains(type);
    }
}
