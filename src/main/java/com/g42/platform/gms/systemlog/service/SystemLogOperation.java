package com.g42.platform.gms.systemlog.service;

import java.util.List;

/**
 * Nhóm thao tác cho bộ lọc "Thao tác" ở trang Nhật ký hệ thống.
 *
 * <p>Nhật ký tự động chỉ lưu đường dẫn API trong description, nên mỗi nhóm khớp
 * bằng LIKE trên đoạn đường dẫn (đúng cho cả mô tả cũ "PUT /api/..." lẫn mô tả mới
 * có dòng cuối "API: PUT /api/..."), hoặc theo module/action với nhật ký ghi tay.
 */
public enum SystemLogOperation {
    CATALOG_EDIT("Sửa mặt hàng / tồn kho / giá", List.of(
            "/api/warehouse/catalog-item/update/", "/api/warehouse/item-color/"), List.of(), List.of()),
    CATALOG_CREATE("Tạo mặt hàng", List.of("/api/warehouse/catalog-item/create"), List.of(), List.of()),
    CATALOG_DELETE("Xoá mặt hàng", List.of("DELETE /api/warehouse/catalog-item/"), List.of(), List.of()),
    STOCK_ENTRY("Phiếu nhập kho", List.of("/api/warehouse/stock-entries"), List.of(), List.of()),
    STOCK_ISSUE("Xuất kho / giữ hàng", List.of(
            "/api/warehouse/stock-issues", "/api/warehouse/allocations", "/stock-allocation"), List.of(), List.of()),
    STOCK_RETURN("Hoàn hàng", List.of("/api/warehouse/return-entries"), List.of(), List.of()),
    WAREHOUSE_SETUP("Thương hiệu, danh mục hàng, đơn vị, kho", List.of(
            "/api/warehouse/brand/", "/api/warehouse/product-line/", "/api/warehouse/item-category",
            "/api/admin/item-category", "/api/warehouse/units", "/api/warehouse/specs",
            "/api/warehouse/warehouse"), List.of(), List.of()),
    ESTIMATE("Báo giá", List.of("/api/service-ticket/estimate", "/api/webhook/zalo/estimate"),
            List.of("ESTIMATION"), List.of()),
    SERVICE_TICKET("Phiếu dịch vụ (tiếp nhận, trạng thái, KTV)", List.of(
            "/api/service-ticket/manage", "/api/receptionist/check-in", "/api/service-ticket/technician"),
            List.of("SERVICE_TICKET"), List.of()),
    PARTS_SALE("Bán phụ tùng", List.of("/api/service-ticket/parts-sale"), List.of(), List.of()),
    PAYMENT("Thanh toán / hoá đơn", List.of("/api/payment"), List.of("BILLING"), List.of()),
    BOOKING("Đặt lịch", List.of("/api/booking"), List.of("BOOKING"), List.of()),
    CUSTOMER("Khách hàng / xe / gọi chăm sóc", List.of("/api/admin/customer", "/api/admin/vehicles"),
            List.of("CUSTOMER", "CUSTOMERIMPORT", "CUSTOMERMERGE", "VEHICLE"), List.of()),
    LOGIN("Đăng nhập", List.of(), List.of("AUTH"), List.of("LOGIN", "LOGIN_FAILED")),
    STAFF("Nhân viên / phân quyền / xưởng", List.of(), List.of("STAFF", "ROLE_PERMISSION", "BRANCH"),
            List.of("PERMISSION")),
    DOCUMENT_TEMPLATE("Biểu mẫu chứng từ", List.of(), List.of("DOCUMENT_TEMPLATE"), List.of()),
    WEBSITE("Website (bài viết, menu, trang chủ)", List.of(
            "/api/admin/posts", "/api/admin/item-posts", "/api/admin/nav-menu", "/api/admin/sliders",
            "/api/admin/site-header", "/api/admin/landing-page", "/api/service/", "/api/admin/media-library",
            "/api/admin/recruitment"), List.of(), List.of()),
    DATA_EXPORT("Sao lưu / xuất dữ liệu", List.of("/api/admin/data-backup", "/api/reports/data-export"),
            List.of("DATA_BACKUP", "DATASAFETY", "REPORT_EXPORT", "REPORT"), List.of("EXPORT"));

    /**
     * Thao tác vặt, ẩn mặc định: không đổi dữ liệu nghiệp vụ nhưng chiếm phần lớn nhật ký
     * (riêng "đã đọc thông báo" hơn 3.000 dòng trên production tháng 11/2026).
     */
    public static final List<String> NOISE_FRAGMENTS = List.of(
            "/api/staff-notification/",
            "/api/staff/table-prefs/",
            "/api/push/subscriptions",
            "/api/public/posts/",
            "/api/public/item-posts/",
            "/api/receptionist/check-in/lookup",
            "/home/uploads/image",
            "/upload-image",
            "/api/admin/media-library/upload",
            "/api/admin/customer-import/validate",
            "/api/public/recruitment/upload-cv"
    );

    private final String label;
    private final List<String> pathFragments;
    private final List<String> modules;
    private final List<String> actions;

    SystemLogOperation(String label, List<String> pathFragments, List<String> modules, List<String> actions) {
        this.label = label;
        this.pathFragments = pathFragments;
        this.modules = modules;
        this.actions = actions;
    }

    public String label() {
        return label;
    }

    public List<String> pathFragments() {
        return pathFragments;
    }

    public List<String> modules() {
        return modules;
    }

    public List<String> actions() {
        return actions;
    }

    public static SystemLogOperation parse(String code) {
        if (code == null || code.isBlank()) return null;
        try {
            return SystemLogOperation.valueOf(code.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static boolean isNoise(String description) {
        if (description == null) return false;
        for (String fragment : NOISE_FRAGMENTS) {
            if (description.contains(fragment)) return true;
        }
        return false;
    }
}
