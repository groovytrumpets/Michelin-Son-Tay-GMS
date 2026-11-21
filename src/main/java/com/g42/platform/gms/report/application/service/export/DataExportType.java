package com.g42.platform.gms.report.application.service.export;

import java.util.List;

/**
 * Các loại dữ liệu xuất được ở màn /report-export. Mỗi loại ra một hoặc nhiều tab trong cùng
 * một file Excel; tab "Tổng quan" luôn có.
 *
 * <p>FE dựng danh sách ô tick từ chính enum này (GET /api/reports/data-export/overview) nên
 * thêm loại mới chỉ cần thêm ở đây + nhánh ghi sheet trong DataExportService.
 */
public enum DataExportType {

    CUSTOMERS("Thông tin khách hàng",
            "Toàn bộ hồ sơ khách (liên hệ, địa chỉ, nhóm, hạng, xe) kèm số phiếu và doanh thu trong kỳ.",
            List.of("Khách hàng", "Xe của khách"), false),

    PRODUCT_REVENUE("Doanh thu sản phẩm",
            "Doanh thu theo từng phụ tùng / dịch vụ từ các hoá đơn trong kỳ: số lượng bán, giảm giá, VAT, đã thu.",
            List.of("Doanh thu sản phẩm"), true),

    INVENTORY("Số lượng sản phẩm (tồn kho)",
            "Tồn kho hiện tại của mọi phụ tùng / vật tư theo kho, giá trị tồn theo lô, nhập - xuất trong kỳ.",
            List.of("Tồn kho sản phẩm"), false),

    TICKETS("Phiếu dịch vụ",
            "Mọi phiếu sửa chữa / bán phụ tùng trong kỳ, kèm chi tiết từng hạng mục và sổ dịch vụ cũ (nếu chọn).",
            List.of("Phiếu dịch vụ", "Chi tiết hạng mục", "Sổ dịch vụ cũ"), true),

    PROFIT("Biên lợi nhuận",
            "Doanh thu chưa VAT trừ giá vốn hàng xuất kho, tiền thuê ngoài và hoa hồng — theo phiếu và theo sản phẩm.",
            List.of("Biên LN theo phiếu", "Biên LN theo sản phẩm"), true);

    private final String label;
    private final String description;
    private final List<String> sheets;
    /** Khoảng thời gian có lọc loại này không (tồn kho và danh bạ thì luôn là hiện tại). */
    private final boolean rangeBound;

    DataExportType(String label, String description, List<String> sheets, boolean rangeBound) {
        this.label = label;
        this.description = description;
        this.sheets = sheets;
        this.rangeBound = rangeBound;
    }

    public String label() { return label; }
    public String description() { return description; }
    public List<String> sheets() { return sheets; }
    public boolean rangeBound() { return rangeBound; }
}
