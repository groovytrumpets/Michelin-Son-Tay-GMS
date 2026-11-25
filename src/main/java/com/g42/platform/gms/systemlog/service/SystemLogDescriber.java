package com.g42.platform.gms.systemlog.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Đổi mô tả thô trong system_log thành câu tiếng Việt cho người quản lý đọc.
 *
 * <p>Ba dạng mô tả đang có trong DB:
 * <ol>
 *   <li>Tự động, cũ: {@code PUT /api/warehouse/catalog-item/update/1306 (itemId=1306)} → tra bảng
 *       {@link #RULES} theo đường dẫn để ra "Sửa mặt hàng (danh mục & tồn kho)".</li>
 *   <li>Có ghi chú thay đổi (AuditNote): dòng đầu là tiêu đề, các dòng "• " là thay đổi
 *       trước → sau, dòng "API: " là đường dẫn.</li>
 *   <li>Ghi tay qua @Auditable: đã là tiếng Việt, giữ nguyên.</li>
 * </ol>
 * Tính lúc đọc nên áp dụng được cho cả nhật ký đã ghi từ trước.
 */
public final class SystemLogDescriber {

    /** Kết quả: câu tóm tắt, danh sách thay đổi (có thể rỗng), đường dẫn API gốc (có thể null). */
    public record Described(String summary, List<String> changes, String api, boolean noise) {
    }

    private static final Pattern RAW = Pattern.compile("^(GET|POST|PUT|PATCH|DELETE) (\\S+)(?: \\((.*)\\))?$", Pattern.DOTALL);

    private static final Map<String, String> TICKET_STATUS = Map.ofEntries(
            Map.entry("HOLDING", "Đang giữ hàng"), Map.entry("CREATED", "Mới tạo"),
            Map.entry("INSPECTING", "Đang kiểm tra"), Map.entry("PENDING", "Chờ xử lý"),
            Map.entry("INSPECTED", "Đã kiểm tra"), Map.entry("ESTIMATED", "Đã báo giá"),
            Map.entry("REPAIRING", "Đang sửa chữa"), Map.entry("CANCELLED", "Đã huỷ"),
            Map.entry("COMPLETED", "Hoàn thành"), Map.entry("PAID", "Đã thanh toán"));

    private static final Map<String, String> ESTIMATE_STATUS = Map.of(
            "DRAFT", "Nháp", "SENT", "Đã gửi khách", "APPROVED", "Khách đã duyệt",
            "REJECTED", "Khách từ chối", "ARCHIVED", "Lưu trữ", "CANCELLED", "Đã huỷ");

    private static final Map<String, String> ACTION_FALLBACK = Map.of(
            "POST", "Tạo / gửi", "PUT", "Cập nhật", "PATCH", "Cập nhật", "DELETE", "Xoá");

    private record Rule(String method, Pattern path, Function<Matcher, String> label) {
    }

    private static final List<Rule> RULES = new ArrayList<>();

    private static void rule(String method, String pathRegex, String label) {
        RULES.add(new Rule(method, Pattern.compile(pathRegex), m -> label));
    }

    private static void rule(String method, String pathRegex, Function<Matcher, String> label) {
        RULES.add(new Rule(method, Pattern.compile(pathRegex), label));
    }

    static {
        // Kho — mặt hàng
        rule("PUT", "/api/warehouse/catalog-item/update/\\d+", "Sửa mặt hàng (danh mục & tồn kho)");
        rule("PUT", "/api/warehouse/item-color/\\d+", "Lưu màu sắc mặt hàng");
        rule("POST", "/api/warehouse/catalog-item/create", "Tạo mặt hàng mới");
        rule("DELETE", "/api/warehouse/catalog-item/\\d+", "Xoá mặt hàng");
        rule("POST", "/api/staff/catalog-drafts", "Lưu nháp mặt hàng");
        rule("PUT", "/api/staff/catalog-drafts/\\d+", "Sửa nháp mặt hàng");
        rule("DELETE", "/api/staff/catalog-drafts/\\d+", "Xoá nháp mặt hàng");
        // Kho — phiếu
        rule("POST", "/api/warehouse/stock-entries(/with-attachment)?", "Tạo phiếu nhập kho");
        rule("PUT", "/api/warehouse/stock-entries/\\d+", "Sửa phiếu nhập kho");
        rule("POST", "/api/warehouse/stock-entries/\\d+/confirm", "Xác nhận phiếu nhập kho (cộng tồn)");
        rule("POST", "/api/warehouse/stock-entries/\\d+/cancel", "Huỷ phiếu nhập kho");
        rule("POST", "/api/warehouse/stock-entries/\\d+/attachments", "Đính kèm chứng từ vào phiếu nhập");
        rule("POST", "/api/warehouse/stock-issues/\\d+/confirm", "Xác nhận phiếu xuất kho (trừ tồn)");
        rule("POST", "/api/warehouse/stock-issues/\\d+/attachments", "Đính kèm chứng từ vào phiếu xuất");
        rule("POST", "/api/warehouse/allocations/\\d+/request-issue", "Yêu cầu xuất kho cho phiếu");
        rule("POST", "/api/warehouse/allocations/cancel", "Huỷ giữ hàng");
        rule("POST", "/api/warehouse/return-entries/request", "Tạo yêu cầu hoàn hàng");
        rule("POST", "/api/warehouse/return-entries/from-issue", "Tạo phiếu hoàn từ phiếu xuất");
        rule("POST", "/api/warehouse/return-entries/\\d+/confirm", "Xác nhận phiếu hoàn hàng");
        rule("POST", "/api/warehouse/return-entries/items/\\d+/attachments", "Đính kèm chứng từ hoàn hàng");
        // Kho — danh mục phụ
        rule("POST", "/api/warehouse/brand/create", "Thêm thương hiệu");
        rule("DELETE", "/api/warehouse/brand/\\d+", "Xoá thương hiệu");
        rule("POST", "/api/warehouse/product-line/create", "Thêm dòng sản phẩm");
        rule("DELETE", "/api/warehouse/product-line/\\d+", "Xoá dòng sản phẩm");
        rule("POST", "/api/(warehouse/item-category/create|admin/item-category)", "Thêm danh mục hàng");
        rule("PUT", "/api/admin/item-category/\\d+", "Sửa danh mục hàng");
        rule("DELETE", "/api/(warehouse|admin)/item-category/\\d+", "Xoá danh mục hàng");
        rule("POST", "/api/warehouse/units", "Thêm đơn vị tính");
        rule("POST", "/api/warehouse/specs-attribute/create", "Thêm loại thông số kỹ thuật");
        rule("POST", "/api/warehouse/specs/create", "Thêm thông số kỹ thuật cho mặt hàng");
        rule("POST", "/api/warehouse/warehouse", "Tạo kho mới");
        rule("PUT", "/api/warehouse/warehouse/\\d+", "Sửa thông tin kho");
        // Báo giá
        rule("POST", "/api/service-ticket/estimate/?", "Tạo báo giá");
        rule("PUT", "/api/service-ticket/estimate/\\d+/(DRAFT|SENT|APPROVED|REJECTED|ARCHIVED|CANCELLED)",
                m -> "Chuyển báo giá sang «" + ESTIMATE_STATUS.getOrDefault(m.group(1), m.group(1)) + "»");
        rule("PUT", "/api/service-ticket/estimate/\\d+", "Sửa báo giá");
        rule("POST", "/api/service-ticket/estimate/\\d+/stock-allocation", "Giữ hàng theo báo giá");
        rule("PUT", "/api/service-ticket/estimate/\\d+/stock-allocation/update", "Cập nhật hàng giữ theo báo giá");
        rule("PUT", "/api/service-ticket/estimate/\\d+/commissions", "Sửa hoa hồng trên báo giá");
        rule("PUT", "/api/service-ticket/estimate/\\d+/item", "Sửa một dòng báo giá");
        rule("POST", "/api/webhook/zalo/estimate/sent", "Gửi báo giá cho khách qua Zalo");
        // Phiếu dịch vụ
        rule("PUT", "/api/service-ticket/manage/\\d+/([A-Z_]+)",
                m -> "Chuyển phiếu sang «" + TICKET_STATUS.getOrDefault(m.group(1), m.group(1)) + "»");
        rule("PUT", "/api/service-ticket/manage/tickets/[^/]+", "Sửa thông tin phiếu dịch vụ");
        rule("PUT", "/api/service-ticket/manage/tickets/[^/]+/estimated-delivery", "Sửa giờ hẹn trả xe");
        rule("POST", "/api/service-ticket/technician/tickets/[^/]+/finish", "Kỹ thuật viên báo sửa xong");
        rule("POST", "/api/receptionist/check-in/lookup", "Tra cứu xe/khách khi tiếp nhận");
        rule("POST", "/api/receptionist/check-in/complete-all", "Hoàn tất tiếp nhận xe (check-in)");
        rule("POST", "/api/receptionist/check-in/vehicles/create", "Thêm xe mới khi tiếp nhận");
        rule("PUT", "/api/admin/safety-category/\\d+", "Sửa hạng mục kiểm tra an toàn");
        // Bán phụ tùng, thanh toán
        rule("POST", "/api/service-ticket/parts-sale/hold", "Bán phụ tùng: giữ hàng");
        rule("POST", "/api/service-ticket/parts-sale", "Tạo phiếu bán phụ tùng");
        rule("POST", "/api/service-ticket/parts-sale/\\d+/cancel", "Huỷ phiếu bán phụ tùng");
        rule("POST", "/api/service-ticket/parts-sale/\\d+/checkout", "Thu tiền phiếu bán phụ tùng");
        rule("POST", "/api/payment/create", "Tạo hoá đơn");
        rule("POST", "/api/payment/create/payment", "Ghi nhận khách trả tiền");
        rule("POST", "/api/payment/proofs/\\d+", "Tải chứng từ thanh toán");
        rule("DELETE", "/api/payment/proofs/\\d+/\\d+", "Xoá chứng từ thanh toán");
        // Đặt lịch
        rule("POST", "/api/booking/staff/create-direct", "Nhân viên tạo lịch hẹn");
        rule("POST", "/api/booking/guest/create", "Khách đặt lịch trên website");
        rule("POST", "/api/booking/manage/booking-request/[^/]+/confirm", "Xác nhận yêu cầu đặt lịch");
        rule("PUT", "/api/booking/manage/booking-request/[^/]+/spam", "Đánh dấu yêu cầu đặt lịch là spam");
        rule("PUT", "/api/booking/manage/booking-request/[^/]+/update", "Sửa yêu cầu đặt lịch");
        // Khách hàng
        rule("POST", "/api/admin/customer-import/validate", "Kiểm tra file Excel nhập khách hàng");
        rule("POST", "/api/admin/customer-merge/dismiss", "Bỏ qua gợi ý gộp hồ sơ khách");
        // Website
        rule("POST", "/api/service/create/\\d+", "Tạo trang giới thiệu dịch vụ cho mặt hàng");
        rule("PUT", "/api/service/update/\\d+", "Sửa trang giới thiệu dịch vụ");
        rule("DELETE", "/api/service/delete/\\d+", "Xoá trang giới thiệu dịch vụ");
        rule("PUT", "/api/service/combo/\\d+", "Sửa combo");
        rule("POST", "/api/admin/posts", "Tạo bài viết");
        rule("PUT", "/api/admin/posts/\\d+", "Sửa bài viết");
        rule("PATCH", "/api/admin/posts/\\d+/status", "Đổi trạng thái bài viết");
        rule("POST", "/api/admin/item-posts", "Tạo bài viết phụ tùng");
        rule("PUT", "/api/admin/item-posts/\\d+", "Sửa bài viết phụ tùng");
        rule("PATCH", "/api/admin/item-posts/\\d+/status", "Đổi trạng thái bài viết phụ tùng");
        rule("POST", "/api/public/(item-)?posts/[^/]+/view", "Khách xem bài viết");
        rule("POST", "/api/public/(item-)?posts/[^/]+/share", "Khách chia sẻ bài viết");
        rule("POST", "/api/admin/nav-menu/upload-image", "Tải ảnh lên");
        rule("POST", "/api/admin/nav-menu/[^/]+", "Thêm mục menu website");
        rule("PUT", "/api/admin/nav-menu/items/\\d+", "Sửa mục menu website");
        rule("DELETE", "/api/admin/nav-menu/items/\\d+", "Xoá mục menu website");
        rule("PUT", "/api/admin/nav-menu/[^/]+/reorder", "Sắp xếp lại menu website");
        rule("PUT", "/api/admin/site-header", "Sửa thanh đầu trang website");
        rule("PUT", "/api/admin/landing-page-catalog", "Sửa danh mục trên trang chủ");
        rule("POST", "/api/admin/sliders", "Thêm slider");
        rule("POST", "/api/admin/sliders/\\d+/items", "Thêm ảnh vào slider");
        rule("DELETE", "/api/admin/sliders/\\d+/items/\\d+", "Xoá ảnh khỏi slider");
        rule("POST", "/api/admin/recruitment/jobs", "Đăng tin tuyển dụng");
        rule("PUT", "/api/admin/recruitment/jobs/\\d+", "Sửa tin tuyển dụng");
        rule("PATCH", "/api/admin/recruitment/jobs/\\d+/status", "Đổi trạng thái tin tuyển dụng");
        rule("POST", "/api/public/recruitment/jobs/[^/]+/apply", "Ứng viên nộp hồ sơ");
        rule("POST", "/api/public/recruitment/upload-cv", "Ứng viên tải CV lên");
        rule("POST", "(/home/uploads/image|/api/admin/.+/upload-image|/api/admin/media-library/upload)", "Tải ảnh lên");
        rule("DELETE", "/api/admin/media-library", "Xoá ảnh trong thư viện");
        // Hệ thống, tiện ích
        rule("PUT", "/api/staff-notification/\\d+/isReaded", "Đánh dấu đã đọc thông báo");
        rule("PUT", "/api/staff/table-prefs/.+", "Lưu cách hiển thị cột bảng");
        rule("POST", "/api/push/subscriptions", "Bật thông báo đẩy trên thiết bị");
        rule("DELETE", "/api/push/subscriptions", "Tắt thông báo đẩy trên thiết bị");
        rule("POST", "/api/staff/dashboard/configs", "Tạo cấu hình bảng điều khiển");
        rule("PUT", "/api/staff/dashboard/configs/\\d+", "Sửa cấu hình bảng điều khiển");
        rule("POST", "/api/staff/dashboard/configs/\\d+/activate", "Đổi cấu hình bảng điều khiển đang dùng");
        rule("POST", "/api/dashboard/summary/refresh", "Tính lại số liệu bảng điều khiển");
        rule("POST", "/api/ai-assistant/chat", "Hỏi trợ lý AI");
        rule("POST", "/api/reports/data-export/otp", "Xuất báo cáo tổng hợp: gửi mã xác minh");
        rule("POST", "/api/reports/data-export/verify", "Xuất báo cáo tổng hợp: nhập mã xác minh");
        rule("POST", "/api/reports/data-export/download", "Tải file báo cáo tổng hợp");
        rule("POST", "/api/admin/data-backup/otp", "Sao lưu CSDL: gửi mã xác minh");
        rule("POST", "/api/admin/data-backup/verify", "Sao lưu CSDL: nhập mã xác minh");
        rule("POST", "/api/admin/data-backup/download", "Tải bản sao lưu CSDL");
        rule("POST", "/api/admin/analytics/google/disconnect", "Ngắt kết nối Google Analytics");
    }

    private SystemLogDescriber() {
    }

    public static Described describe(String description) {
        boolean noise = SystemLogOperation.isNoise(description);
        if (description == null || description.isBlank()) {
            return new Described("(không có mô tả)", List.of(), null, noise);
        }

        // Dạng có ghi chú thay đổi
        if (description.contains("\n" + AuditNote.CHANGE_PREFIX) || description.contains("\n" + AuditNote.API_PREFIX)) {
            String[] lines = description.split("\n");
            List<String> changes = new ArrayList<>();
            String api = null;
            for (int i = 1; i < lines.length; i++) {
                String line = lines[i];
                if (line.startsWith(AuditNote.CHANGE_PREFIX)) {
                    changes.add(line.substring(AuditNote.CHANGE_PREFIX.length()).trim());
                } else if (line.startsWith(AuditNote.API_PREFIX)) {
                    api = line.substring(AuditNote.API_PREFIX.length()).trim();
                } else if (!line.isBlank()) {
                    changes.add(line.trim());
                }
            }
            String summary = lines[0].isBlank() && api != null ? labelForApi(api) : lines[0].trim();
            return new Described(summary, changes, api, noise);
        }

        // Dạng tự động cũ: "PUT /api/... (itemId=1306)"
        Matcher raw = RAW.matcher(description.trim());
        if (raw.matches()) {
            String api = raw.group(1) + " " + raw.group(2);
            return new Described(labelForApi(api), List.of(), api, noise);
        }

        // Ghi tay qua @Auditable — đã là tiếng Việt
        return new Described(description.trim(), List.of(), null, noise);
    }

    /** "PUT /api/warehouse/catalog-item/update/1306" → "Sửa mặt hàng (danh mục & tồn kho)". */
    static String labelForApi(String api) {
        int space = api.indexOf(' ');
        if (space < 0) return api;
        String method = api.substring(0, space);
        String path = api.substring(space + 1);
        int query = path.indexOf('?');
        if (query >= 0) path = path.substring(0, query);
        if (path.length() > 1 && path.endsWith("/")) path = path.substring(0, path.length() - 1);
        for (Rule rule : RULES) {
            if (!rule.method().equals(method)) continue;
            Matcher m = rule.path().matcher(path);
            if (m.matches()) return rule.label().apply(m);
        }
        return ACTION_FALLBACK.getOrDefault(method, method) + " · " + path;
    }
}
