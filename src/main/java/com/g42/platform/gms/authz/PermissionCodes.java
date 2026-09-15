package com.g42.platform.gms.authz;

/**
 * Danh mục mã quyền — bản sao trong code của bảng {@code permission}.
 *
 * <p>Hằng số ở đây tồn tại vì {@code @PreAuthorize} là annotation: giá trị phải
 * là hằng biết được lúc biên dịch, không đọc từ DB được. Bảng {@code permission}
 * (changeset 038) và lớp này phải luôn khớp nhau — {@code PermissionCatalogCheck}
 * đối chiếu hai bên lúc khởi động và ghi cảnh báo nếu lệch.
 *
 * <p>Phần ĐỘNG không nằm ở đây mà ở bảng {@code role_permission}: vai trò nào
 * được cấp mã nào là do admin cấu hình ở /role-permission-config. Thêm hằng số
 * mới vào lớp này thì phải thêm cả changeset seed dòng tương ứng vào
 * {@code permission}, nếu không màn cấu hình sẽ không thấy quyền đó để bật.
 */
public final class PermissionCodes {

    private PermissionCodes() {
    }

    // ---- Khách hàng & Lịch hẹn ----
    public static final String CUSTOMER_VIEW = "CUSTOMER_VIEW";
    public static final String CUSTOMER_CREATE = "CUSTOMER_CREATE";
    public static final String CUSTOMER_EDIT = "CUSTOMER_EDIT";
    public static final String CUSTOMER_DELETE = "CUSTOMER_DELETE";
    public static final String CUSTOMER_MERGE = "CUSTOMER_MERGE";
    public static final String CUSTOMER_IMPORT = "CUSTOMER_IMPORT";

    public static final String VEHICLE_VIEW = "VEHICLE_VIEW";
    public static final String VEHICLE_CREATE = "VEHICLE_CREATE";
    public static final String VEHICLE_EDIT = "VEHICLE_EDIT";
    public static final String VEHICLE_DELETE = "VEHICLE_DELETE";

    public static final String BOOKING_VIEW = "BOOKING_VIEW";
    public static final String BOOKING_CREATE = "BOOKING_CREATE";
    public static final String BOOKING_EDIT = "BOOKING_EDIT";
    public static final String BOOKING_DELETE = "BOOKING_DELETE";

    public static final String PARTS_SALE_VIEW = "PARTS_SALE_VIEW";
    public static final String PARTS_SALE_CREATE = "PARTS_SALE_CREATE";
    public static final String PARTS_SALE_EDIT = "PARTS_SALE_EDIT";

    // ---- Dịch vụ & Xưởng ----
    public static final String SERVICE_TICKET_VIEW = "SERVICE_TICKET_VIEW";
    public static final String SERVICE_TICKET_CREATE = "SERVICE_TICKET_CREATE";
    public static final String SERVICE_TICKET_EDIT = "SERVICE_TICKET_EDIT";
    public static final String SERVICE_TICKET_DELETE = "SERVICE_TICKET_DELETE";
    public static final String SERVICE_TICKET_ASSIGN = "SERVICE_TICKET_ASSIGN";
    public static final String SERVICE_TICKET_PAYMENT = "SERVICE_TICKET_PAYMENT";

    public static final String TICKET_BACKFILL_VIEW = "TICKET_BACKFILL_VIEW";
    public static final String TICKET_BACKFILL_EDIT = "TICKET_BACKFILL_EDIT";

    public static final String TECHNICIAN_TASK_VIEW = "TECHNICIAN_TASK_VIEW";
    public static final String TECHNICIAN_TASK_EDIT = "TECHNICIAN_TASK_EDIT";

    public static final String SERVICE_CATALOG_VIEW = "SERVICE_CATALOG_VIEW";
    public static final String SERVICE_CATALOG_CREATE = "SERVICE_CATALOG_CREATE";
    public static final String SERVICE_CATALOG_EDIT = "SERVICE_CATALOG_EDIT";
    public static final String SERVICE_CATALOG_DELETE = "SERVICE_CATALOG_DELETE";

    // ---- Kho & Phụ tùng ----
    public static final String WAREHOUSE_VIEW = "WAREHOUSE_VIEW";
    public static final String WAREHOUSE_EDIT = "WAREHOUSE_EDIT";

    public static final String WAREHOUSE_CONFIG_VIEW = "WAREHOUSE_CONFIG_VIEW";
    public static final String WAREHOUSE_CONFIG_EDIT = "WAREHOUSE_CONFIG_EDIT";

    public static final String ITEM_VIEW = "ITEM_VIEW";
    public static final String ITEM_CREATE = "ITEM_CREATE";
    public static final String ITEM_EDIT = "ITEM_EDIT";
    public static final String ITEM_DELETE = "ITEM_DELETE";

    public static final String STOCK_ENTRY_VIEW = "STOCK_ENTRY_VIEW";
    public static final String STOCK_ENTRY_CREATE = "STOCK_ENTRY_CREATE";
    public static final String STOCK_ENTRY_EDIT = "STOCK_ENTRY_EDIT";
    public static final String STOCK_ENTRY_DELETE = "STOCK_ENTRY_DELETE";

    public static final String STOCK_ISSUE_VIEW = "STOCK_ISSUE_VIEW";
    public static final String STOCK_ISSUE_CREATE = "STOCK_ISSUE_CREATE";
    public static final String STOCK_ISSUE_EDIT = "STOCK_ISSUE_EDIT";
    public static final String STOCK_ISSUE_DELETE = "STOCK_ISSUE_DELETE";

    public static final String STOCK_RETURN_VIEW = "STOCK_RETURN_VIEW";
    public static final String STOCK_RETURN_CREATE = "STOCK_RETURN_CREATE";
    public static final String STOCK_RETURN_EDIT = "STOCK_RETURN_EDIT";
    public static final String STOCK_RETURN_DELETE = "STOCK_RETURN_DELETE";

    public static final String PRICING_VIEW = "PRICING_VIEW";
    public static final String PRICING_EDIT = "PRICING_EDIT";

    public static final String WAREHOUSE_REPORT_VIEW = "WAREHOUSE_REPORT_VIEW";

    // ---- Nhân sự ----
    public static final String STAFF_VIEW = "STAFF_VIEW";
    public static final String STAFF_CREATE = "STAFF_CREATE";
    public static final String STAFF_EDIT = "STAFF_EDIT";
    public static final String STAFF_DELETE = "STAFF_DELETE";

    public static final String EMPLOYEE_VIEW = "EMPLOYEE_VIEW";
    public static final String EMPLOYEE_EDIT = "EMPLOYEE_EDIT";

    public static final String ROLE_PERMISSION_VIEW = "ROLE_PERMISSION_VIEW";
    public static final String ROLE_PERMISSION_EDIT = "ROLE_PERMISSION_EDIT";

    public static final String STAFF_NOTIFICATION_VIEW = "STAFF_NOTIFICATION_VIEW";
    public static final String STAFF_NOTIFICATION_SEND = "STAFF_NOTIFICATION_SEND";

    // ---- Ca làm & Chấm công ----
    public static final String SHIFT_VIEW = "SHIFT_VIEW";
    public static final String SHIFT_EDIT = "SHIFT_EDIT";

    public static final String ATTENDANCE_VIEW = "ATTENDANCE_VIEW";
    public static final String ATTENDANCE_EDIT = "ATTENDANCE_EDIT";

    public static final String ATTENDANCE_LOCATION_VIEW = "ATTENDANCE_LOCATION_VIEW";
    public static final String ATTENDANCE_LOCATION_EDIT = "ATTENDANCE_LOCATION_EDIT";

    public static final String ATTENDANCE_REQUEST_VIEW = "ATTENDANCE_REQUEST_VIEW";
    public static final String ATTENDANCE_REQUEST_APPROVE = "ATTENDANCE_REQUEST_APPROVE";

    // ---- Marketing & CSKH ----
    public static final String PROMOTION_VIEW = "PROMOTION_VIEW";
    public static final String PROMOTION_CREATE = "PROMOTION_CREATE";
    public static final String PROMOTION_EDIT = "PROMOTION_EDIT";
    public static final String PROMOTION_DELETE = "PROMOTION_DELETE";

    public static final String POST_VIEW = "POST_VIEW";
    public static final String POST_CREATE = "POST_CREATE";
    public static final String POST_EDIT = "POST_EDIT";
    public static final String POST_DELETE = "POST_DELETE";
    public static final String POST_PUBLISH = "POST_PUBLISH";

    public static final String ITEM_POST_VIEW = "ITEM_POST_VIEW";
    public static final String ITEM_POST_CREATE = "ITEM_POST_CREATE";
    public static final String ITEM_POST_EDIT = "ITEM_POST_EDIT";
    public static final String ITEM_POST_DELETE = "ITEM_POST_DELETE";

    public static final String MEDIA_VIEW = "MEDIA_VIEW";
    public static final String MEDIA_UPLOAD = "MEDIA_UPLOAD";
    public static final String MEDIA_DELETE = "MEDIA_DELETE";

    public static final String SITE_CONFIG_VIEW = "SITE_CONFIG_VIEW";
    public static final String SITE_CONFIG_EDIT = "SITE_CONFIG_EDIT";

    public static final String RECRUITMENT_VIEW = "RECRUITMENT_VIEW";
    public static final String RECRUITMENT_EDIT = "RECRUITMENT_EDIT";

    public static final String FEEDBACK_VIEW = "FEEDBACK_VIEW";
    public static final String FEEDBACK_EDIT = "FEEDBACK_EDIT";

    public static final String CAMPAIGN_VIEW = "CAMPAIGN_VIEW";
    public static final String CAMPAIGN_SEND = "CAMPAIGN_SEND";

    public static final String LOYALTY_VIEW = "LOYALTY_VIEW";
    public static final String LOYALTY_EDIT = "LOYALTY_EDIT";

    // ---- Báo cáo & Tài chính ----
    public static final String REPORT_CUSTOMER_VIEW = "REPORT_CUSTOMER_VIEW";
    public static final String REVENUE_VIEW = "REVENUE_VIEW";
    public static final String KPI_VIEW = "KPI_VIEW";
    public static final String KPI_EDIT = "KPI_EDIT";
    public static final String GOOGLE_INSIGHTS_VIEW = "GOOGLE_INSIGHTS_VIEW";
    public static final String GOOGLE_INSIGHTS_CONFIG = "GOOGLE_INSIGHTS_CONFIG";

    // ---- Hệ thống ----
    public static final String MASTER_DATA_VIEW = "MASTER_DATA_VIEW";
    public static final String MASTER_DATA_EDIT = "MASTER_DATA_EDIT";
    public static final String SYSTEM_LOG_VIEW = "SYSTEM_LOG_VIEW";
    public static final String BACKEND_LOG_VIEW = "BACKEND_LOG_VIEW";
    public static final String BUG_REPORT_VIEW = "BUG_REPORT_VIEW";
    public static final String BUG_REPORT_EDIT = "BUG_REPORT_EDIT";
}
