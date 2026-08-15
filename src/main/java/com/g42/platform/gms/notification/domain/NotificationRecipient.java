package com.g42.platform.gms.notification.domain;

/**
 * Danh tính người nhận thông báo + kênh ưu tiên của họ.
 * Dùng bởi CustomerNotificationDispatcher để chọn gửi qua Zalo hay Email.
 */
public record NotificationRecipient(String phone, String email, NotificationChannel preferredChannel) {

    public static NotificationRecipient of(String phone, String email, NotificationChannel preferredChannel) {
        return new NotificationRecipient(phone, email, preferredChannel);
    }

    /** Dùng khi không có hồ sơ khách hàng (chưa có tài khoản) — luôn gửi qua Zalo bằng số điện thoại. */
    public static NotificationRecipient phoneOnly(String phone) {
        return new NotificationRecipient(phone, null, NotificationChannel.ZALO);
    }

    public boolean canUseEmail() {
        return email != null && !email.isBlank();
    }

    public boolean canUseZalo() {
        return phone != null && !phone.isBlank();
    }

    /** EMAIL chỉ thắng khi khách chọn EMAIL và có email hợp lệ; mọi trường hợp khác dùng Zalo. */
    public boolean shouldUseEmail() {
        return preferredChannel == NotificationChannel.EMAIL && canUseEmail();
    }
}
