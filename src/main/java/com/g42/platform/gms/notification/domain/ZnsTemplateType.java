package com.g42.platform.gms.notification.domain;

/**
 * Các loại tin ZNS hệ thống gửi. Mã mẫu mặc định là mã đã hard-code từ trước; quản lý có thể
 * đổi sang mẫu khác ở /zalo-oa-config (lưu trong zalo_oa_config.template_map).
 *
 * Đổi mẫu thì mẫu mới PHẢI có đúng các tham số ở {@link #params} — Zalo từ chối cả tin nếu
 * thiếu hoặc thừa tham số.
 */
public enum ZnsTemplateType {
    BOOKING_REQUESTED("546766", "Đã nhận yêu cầu đặt lịch",
            "customer_name, product_name, order_code, booking_status, booking_time, garage_location"),
    BOOKING_CONFIRM("562453", "Xác nhận lịch hẹn",
            "customer_name, service, booking_code, booking_time, location"),
    OTP("547094", "Mã OTP đăng nhập / xác minh", "otp"),
    FEEDBACK("547146", "Mời đánh giá phiếu dịch vụ (có nút chấm sao)", "customer_name, service_code"),
    ESTIMATE("574006", "Gửi báo giá", "customer_name, order_code, time, service_price, price, location");

    private final String defaultTemplateId;
    private final String label;
    private final String params;

    ZnsTemplateType(String defaultTemplateId, String label, String params) {
        this.defaultTemplateId = defaultTemplateId;
        this.label = label;
        this.params = params;
    }

    public String getDefaultTemplateId() { return defaultTemplateId; }
    public String getLabel() { return label; }
    public String getParams() { return params; }
}
