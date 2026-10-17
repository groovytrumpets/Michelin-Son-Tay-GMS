package com.g42.platform.gms.datasafety.application.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Danh sách các loại dữ liệu "xoá mềm" mà thùng rác biết đọc và khôi phục.
 *
 * <p>Mỗi module tự chọn cách xoá mềm riêng từ trước (is_active = 0, deleted_at,
 * customer_auth.status = 'DELETED', is_deleted = 1), nên không có cột chung nào để
 * quét. Chỉ đưa vào đây những bảng mà nút "Xoá" trên màn hình THẬT SỰ xoá mềm — đã rà
 * từng service ngày 2026-10-18. Cố ý KHÔNG đưa:
 * <ul>
 *   <li>warehouse, warehouse_pricing, fallback_pricing_config: is_active = 0 là "tắt kho" /
 *       "giá cũ bị thay", không phải bị xoá;</li>
 *   <li>vehicle_model, product_unit...: nút xoá xoá cứng, không còn gì để khôi phục;</li>
 *   <li>staff_profile.is_resigned: nghỉ việc là trạng thái nhân sự, không phải xoá.</li>
 * </ul>
 *
 * <p>Mọi đoạn SQL ở đây là hằng số trong code — người dùng chỉ chọn {@code key} và
 * gõ từ khoá tìm kiếm (luôn đi qua tham số {@code ?}), nên không có đường nào chèn SQL.
 *
 * <p>Thêm loại mới: khai báo một {@link TrashType} ở {@link #TYPES}. Nếu khôi phục cần
 * hơn một câu UPDATE (kiểm tra trùng, đồng bộ bảng khác) thì để {@code restoreSql} null
 * và xử lý riêng trong {@link DataTrashService#restore}.
 */
public final class TrashRegistry {

    private TrashRegistry() {
    }

    public static final String GROUP_CATALOG = "Kho & sản phẩm";
    public static final String GROUP_CONTENT = "Nội dung website";
    public static final String GROUP_CUSTOMER = "Khách hàng & xe";
    public static final String GROUP_OPERATION = "Vận hành & nhân sự";

    /**
     * @param fromSql       mệnh đề FROM (kèm JOIN) — bí danh bảng chính luôn là {@code t}
     * @param deletedWhere  điều kiện "đang nằm trong thùng rác"
     * @param idExpr        khoá chính, cũng là tham số của restoreSql
     * @param titleExpr     tên hiển thị
     * @param subtitleExpr  dòng phụ (mã, nhóm...), có thể null
     * @param imageExpr     ảnh đại diện, có thể null
     * @param deletedAtExpr thời điểm xoá — chỉ các bảng dùng deleted_at mới biết
     * @param searchExpr    biểu thức được so khớp với từ khoá tìm kiếm
     * @param restoreSql    UPDATE một dòng theo {@code ?} = id; null = xử lý riêng
     * @param restoreNote   lưu ý hiện cạnh nút khôi phục, có thể null
     */
    public record TrashType(
            String key,
            String group,
            String label,
            String fromSql,
            String deletedWhere,
            String idExpr,
            String titleExpr,
            String subtitleExpr,
            String imageExpr,
            String deletedAtExpr,
            String searchExpr,
            String orderExpr,
            String restoreSql,
            String restoreNote) {}

    public static final String CATALOG_ITEM = "CATALOG_ITEM";

    private static final List<TrashType> TYPES = List.of(
            new TrashType(CATALOG_ITEM, GROUP_CATALOG, "Sản phẩm & dịch vụ",
                    "FROM catalog_item t LEFT JOIN item_category c ON c.item_category_id = t.item_category_id",
                    "t.is_active = 0",
                    "t.item_id",
                    "t.item_name",
                    "CONCAT_WS(' · ', t.sku, c.category_name, t.item_type)",
                    // Mục kho ít khi có image_url riêng; ảnh thật thường nằm ở bài viết sản phẩm đi kèm.
                    "COALESCE(NULLIF(t.image_url, ''), (SELECT ip.thumbnail_url FROM item_post ip "
                            + "WHERE ip.catalog_item_id = t.item_id AND ip.thumbnail_url IS NOT NULL "
                            + "ORDER BY ip.item_post_id DESC LIMIT 1))",
                    null,
                    "CONCAT_WS(' ', t.item_name, t.sku, t.part_number, t.barcode)",
                    "t.item_id DESC",
                    null,
                    "Bị chặn nếu SKU đã được một mục đang dùng lấy lại. Đường dẫn (slug) có thể đã nhường cho mục mới."),
            new TrashType("ITEM_CATEGORY", GROUP_CATALOG, "Danh mục hàng hoá",
                    "FROM item_category t",
                    "t.is_active = 0",
                    "t.item_category_id",
                    "t.category_name",
                    "CONCAT_WS(' · ', t.category_code, t.category_type)",
                    null, null,
                    "CONCAT_WS(' ', t.category_name, t.category_code)",
                    "t.display_order, t.item_category_id",
                    "UPDATE item_category SET is_active = 1 WHERE item_category_id = ? AND is_active = 0",
                    null),
            new TrashType("PART_BRAND", GROUP_CATALOG, "Thương hiệu phụ tùng",
                    "FROM brand t",
                    "t.is_active = 0",
                    "t.brand_id",
                    "t.brand_name",
                    null,
                    "t.logo_url", null,
                    "t.brand_name",
                    "t.brand_name",
                    "UPDATE brand SET is_active = 1 WHERE brand_id = ? AND is_active = 0",
                    null),
            new TrashType("PRODUCT_LINE", GROUP_CATALOG, "Dòng sản phẩm",
                    "FROM product_line t LEFT JOIN brand b ON b.brand_id = t.brand_id",
                    "t.is_active = 0",
                    "t.product_line_id",
                    "t.line_name",
                    "b.brand_name",
                    "b.logo_url", null,
                    "CONCAT_WS(' ', t.line_name, b.brand_name)",
                    "t.line_name",
                    "UPDATE product_line SET is_active = 1 WHERE product_line_id = ? AND is_active = 0",
                    null),
            new TrashType("SERVICE_RULE", GROUP_CATALOG, "Gợi ý dịch vụ theo km",
                    "FROM service_rule t",
                    "t.is_active = 0",
                    "t.rule_id",
                    "COALESCE(NULLIF(t.reason, ''), CONCAT('Quy tắc #', t.rule_id))",
                    "CONCAT_WS(' · ', t.vehicle_type_pattern, CONCAT(t.km_threshold, ' km'))",
                    null, null,
                    "CONCAT_WS(' ', t.reason, t.vehicle_type_pattern)",
                    "t.rule_id DESC",
                    "UPDATE service_rule SET is_active = 1, updated_at = NOW() WHERE rule_id = ? AND is_active = 0",
                    null),

            new TrashType("POST", GROUP_CONTENT, "Tin tức",
                    "FROM post t",
                    "t.deleted_at IS NOT NULL",
                    "t.post_id",
                    "t.title",
                    "CONCAT_WS(' · ', t.status, t.slug)",
                    "t.thumbnail_url",
                    "t.deleted_at",
                    "CONCAT_WS(' ', t.title, t.slug)",
                    "t.deleted_at DESC",
                    "UPDATE post SET deleted_at = NULL, updated_at = NOW() WHERE post_id = ? AND deleted_at IS NOT NULL",
                    "Bài quay lại đúng trạng thái trước khi xoá (đã đăng thì hiện lại ngay trên trang khách)."),
            new TrashType("ITEM_POST", GROUP_CONTENT, "Bài viết sản phẩm",
                    "FROM item_post t",
                    "t.deleted_at IS NOT NULL",
                    "t.item_post_id",
                    "t.title",
                    "CONCAT_WS(' · ', t.status, t.slug)",
                    "t.thumbnail_url",
                    "t.deleted_at",
                    "CONCAT_WS(' ', t.title, t.slug)",
                    "t.deleted_at DESC",
                    "UPDATE item_post SET deleted_at = NULL, updated_at = NOW() WHERE item_post_id = ? AND deleted_at IS NOT NULL",
                    "Bài quay lại đúng trạng thái trước khi xoá (đã đăng thì hiện lại ngay trên trang khách)."),
            new TrashType("JOB_POSITION", GROUP_CONTENT, "Tin tuyển dụng",
                    "FROM job_position t",
                    "t.deleted_at IS NOT NULL",
                    "t.job_id",
                    "t.title",
                    "CONCAT_WS(' · ', t.department, t.status)",
                    "t.thumbnail_url",
                    "t.deleted_at",
                    "CONCAT_WS(' ', t.title, t.department, t.slug)",
                    "t.deleted_at DESC",
                    "UPDATE job_position SET deleted_at = NULL, updated_at = NOW() WHERE job_id = ? AND deleted_at IS NOT NULL",
                    null),

            new TrashType("CUSTOMER", GROUP_CUSTOMER, "Khách hàng",
                    "FROM customer_auth a JOIN customer_profile t ON t.customer_id = a.customer_id",
                    "a.status = 'DELETED'",
                    "t.customer_id",
                    "COALESCE(NULLIF(t.full_name, ''), NULLIF(t.company_name, ''), CONCAT('Khách #', t.customer_id))",
                    "CONCAT_WS(' · ', t.customer_code, t.company_name, t.address)",
                    "t.avatar", null,
                    "CONCAT_WS(' ', t.full_name, t.company_name, t.customer_code, t.address)",
                    "t.customer_id DESC",
                    // Chưa từng đặt PIN thì quay về "chưa kích hoạt" như lúc mới nhập, có PIN thì đăng nhập lại được.
                    "UPDATE customer_auth SET status = IF(pin_hash IS NULL OR pin_hash = '', 'INACTIVE', 'ACTIVE') "
                            + "WHERE customer_id = ? AND status = 'DELETED'",
                    "Lúc xoá, SĐT và email đã được gỡ khỏi hồ sơ để nhường cho khách khác — khôi phục xong cần nhập lại ở Danh bạ khách hàng."),
            new TrashType("CUSTOMER_GROUP", GROUP_CUSTOMER, "Nhóm khách hàng",
                    "FROM customer_group t",
                    "t.active = 0",
                    "t.group_id",
                    "t.name",
                    "CONCAT_WS(' · ', t.code, t.note)",
                    null, null,
                    "CONCAT_WS(' ', t.name, t.code)",
                    "t.name",
                    "UPDATE customer_group SET active = 1 WHERE group_id = ? AND active = 0",
                    null),
            new TrashType("VEHICLE_BRAND", GROUP_CUSTOMER, "Hãng xe",
                    "FROM vehicle_brand t",
                    "t.is_active = 0",
                    "t.brand_id",
                    "t.name",
                    "t.country",
                    "t.logo_url", null,
                    "CONCAT_WS(' ', t.name, t.country)",
                    "t.sort_order, t.name",
                    "UPDATE vehicle_brand SET is_active = 1 WHERE brand_id = ? AND is_active = 0",
                    null),

            new TrashType("SERVICE_TICKET", GROUP_OPERATION, "Phiếu dịch vụ",
                    "FROM service_ticket t LEFT JOIN customer_profile cp ON cp.customer_id = t.customer_id "
                            + "LEFT JOIN vehicle v ON v.vehicle_id = t.vehicle_id",
                    "t.is_deleted = 1",
                    "t.service_ticket_id",
                    "COALESCE(t.ticket_code, CONCAT('Phiếu #', t.service_ticket_id))",
                    "CONCAT_WS(' · ', v.license_plate, COALESCE(cp.full_name, t.walk_in_name), t.ticket_status)",
                    null, null,
                    "CONCAT_WS(' ', t.ticket_code, v.license_plate, cp.full_name, t.walk_in_name)",
                    "t.service_ticket_id DESC",
                    "UPDATE service_ticket SET is_deleted = 0, updated_at = NOW() WHERE service_ticket_id = ? AND is_deleted = 1",
                    null),
            new TrashType("WORK_SHIFT", GROUP_OPERATION, "Ca làm việc",
                    "FROM work_shift t",
                    "t.is_active = 0",
                    "t.shift_id",
                    "t.shift_name",
                    "CONCAT(TIME_FORMAT(t.start_time, '%H:%i'), ' – ', TIME_FORMAT(t.end_time, '%H:%i'))",
                    null, null,
                    "t.shift_name",
                    "t.start_time",
                    "UPDATE work_shift SET is_active = 1 WHERE shift_id = ? AND is_active = 0",
                    null),
            new TrashType("ATTENDANCE_LOCATION", GROUP_OPERATION, "Địa điểm chấm công",
                    "FROM attendance_location t",
                    "t.is_active = 0",
                    "t.location_id",
                    "t.location_name",
                    "t.address",
                    null, null,
                    "CONCAT_WS(' ', t.location_name, t.address)",
                    "t.location_name",
                    "UPDATE attendance_location SET is_active = 1 WHERE location_id = ? AND is_active = 0",
                    null),
            new TrashType("SAFETY_CATEGORY", GROUP_OPERATION, "Đầu mục kiểm tra an toàn",
                    "FROM work_category t",
                    "t.is_active = 0",
                    "t.idwork_category",
                    "t.category_name",
                    "t.category_code",
                    null, null,
                    "CONCAT_WS(' ', t.category_name, t.category_code)",
                    "t.display_order, t.idwork_category",
                    "UPDATE work_category SET is_active = 1 WHERE idwork_category = ? AND is_active = 0",
                    null)
    );

    private static final Map<String, TrashType> BY_KEY;

    static {
        Map<String, TrashType> map = new LinkedHashMap<>();
        for (TrashType type : TYPES) map.put(type.key(), type);
        BY_KEY = Map.copyOf(map);
    }

    public static List<TrashType> all() {
        return TYPES;
    }

    public static TrashType find(String key) {
        return key == null ? null : BY_KEY.get(key.trim().toUpperCase());
    }
}
