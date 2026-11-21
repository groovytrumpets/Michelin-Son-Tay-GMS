package com.g42.platform.gms.report.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Báo cáo doanh thu theo khách hàng & theo sản phẩm (/sales-report).
 *
 * Dữ liệu gốc là từng dòng hàng của hoá đơn (dòng báo giá được chốt) trong kỳ; tiền mỗi dòng đã
 * được cân lại để cộng đúng bằng số tiền hoá đơn. Hai bộ lọc chéo:
 *  - {@code customers} gom theo khách, CHỈ áp bộ lọc sản phẩm (để còn chọn được khách khác);
 *  - {@code products} gom theo sản phẩm, CHỈ áp bộ lọc khách;
 *  - {@code summary} và {@code details} áp cả hai.
 *
 * Chọn một khách → {@code products} là các sản phẩm khách đó mua; chọn một sản phẩm →
 * {@code customers} là những khách đã mua sản phẩm đó.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesReportResponse {

    private Summary summary;
    private List<CustomerRow> customers;
    private List<ProductRow> products;
    /** Dòng chi tiết (khách × sản phẩm × hoá đơn), mới nhất trước, tối đa {@code Summary.detailLimit} dòng. */
    private List<DetailRow> details;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private LocalDate from;
        private LocalDate to;
        /** ALL | PAID | UNPAID (UNPAID gồm cả quá hạn). */
        private String status;
        private boolean legacyIncluded;

        /** Khách / sản phẩm đang lọc (null = tất cả) và tên hiển thị tương ứng. */
        private String customerKey;
        private String customerName;
        private String productKey;
        private String productName;

        private BigDecimal totalRevenue;
        private BigDecimal paidRevenue;
        private BigDecimal unpaidRevenue;
        /** Tiền thu mua linh kiện của khách (số âm, đã trừ trong tổng). */
        private BigDecimal tradeInAmount;
        /** Phần chênh giữa tổng hoá đơn và tổng dòng hàng đã chia lại vào các dòng. */
        private BigDecimal billAdjustment;
        private BigDecimal legacyRevenue;
        private BigDecimal quantity;

        private long invoiceCount;
        private long customerCount;
        private long productCount;

        private long detailCount;
        private int detailLimit;
        private boolean detailsTruncated;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerRow {
        /** {@code c:<customerId>} | {@code w:<sđt>} (khách lẻ có SĐT) | {@code wt:<ticketId>} | {@code c:none}. */
        private String customerKey;
        private Integer customerId;
        private String customerName;
        private String customerPhone;
        private boolean walkIn;
        private List<String> plates;
        private long invoiceCount;
        private long paidInvoiceCount;
        private long productCount;
        private BigDecimal revenue;
        private BigDecimal paidRevenue;
        private BigDecimal unpaidRevenue;
        private BigDecimal legacyRevenue;
        private LocalDate firstDate;
        private LocalDate lastDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductRow {
        /** {@code i:<itemId>} | {@code n:<tên>} (dòng gõ tay) | {@code t:<tên>} (thu mua) | {@code l:<tên>} (sổ cũ) | {@code x:...}. */
        private String productKey;
        private Integer itemId;
        private String sku;
        private String productName;
        /** SERVICE | PART | OTHER | TRADE_IN | LEGACY | ADJUSTMENT. */
        private String productType;
        private String categoryName;
        private String unit;
        private BigDecimal quantity;
        /** Tổng thành tiền các dòng như trên báo giá (trước khi cân theo hoá đơn). */
        private BigDecimal lineAmount;
        /** Giảm giá trên dòng (mã giảm giá / giảm tay). */
        private BigDecimal discountAmount;
        private BigDecimal revenue;
        private BigDecimal paidRevenue;
        private BigDecimal unpaidRevenue;
        private long invoiceCount;
        private long customerCount;
        private long giftCount;
        private LocalDate lastDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetailRow {
        private LocalDate date;
        private Integer serviceTicketId;
        private Integer legacyVisitId;
        private String ticketCode;
        /** SERVICE | PART (phiếu bán phụ tùng) | LEGACY. */
        private String ticketType;
        /** PAID | UNPAID | OVERDUE. */
        private String status;
        private String customerKey;
        private String customerName;
        private String customerPhone;
        private String licensePlate;
        private String productKey;
        private String productName;
        private String sku;
        private String productType;
        private String unit;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private BigDecimal lineAmount;
        private BigDecimal discountAmount;
        private BigDecimal revenue;
        private boolean gift;
    }
}
