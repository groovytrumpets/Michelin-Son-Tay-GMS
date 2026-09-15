package com.g42.platform.gms.analytics.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GoogleAnalyticsErrorCode {
    NOT_CONFIGURED("GOOGLE_ANALYTICS_NOT_CONFIGURED", "Chưa cấu hình OAuth Client cho Google Analytics/Search Console"),
    NOT_CONNECTED("GOOGLE_ANALYTICS_NOT_CONNECTED", "Chưa kết nối tài khoản Google — vào Kết nối để cấp quyền"),
    PROPERTY_NOT_CONFIGURED("GA4_PROPERTY_NOT_CONFIGURED", "Chưa cấu hình GA4_PROPERTY_ID"),
    SITE_NOT_CONFIGURED("GSC_SITE_NOT_CONFIGURED", "Chưa cấu hình GSC_SITE_URL"),
    INVALID_STATE("GOOGLE_ANALYTICS_INVALID_STATE", "Phiên kết nối không hợp lệ hoặc đã hết hạn, vui lòng thử lại"),
    /**
     * Google trả invalid_grant: refresh_token đã hết hạn hoặc bị thu hồi (đổi mật khẩu Google,
     * gỡ quyền truy cập, hoặc OAuth Client còn ở chế độ Testing nên token chỉ sống 7 ngày).
     * Không tự sửa được — phải bấm Kết nối lại ở /google-insights.
     */
    RECONNECT_REQUIRED("GOOGLE_ANALYTICS_RECONNECT_REQUIRED",
            "Kết nối Google đã hết hạn hoặc bị thu hồi — vào trang Google Insights bấm Kết nối lại"),
    UPSTREAM_ERROR("GOOGLE_ANALYTICS_UPSTREAM_ERROR", "Google trả về lỗi, vui lòng thử lại sau");

    private final String code;
    private final String message;
}
