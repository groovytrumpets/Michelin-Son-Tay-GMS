package com.g42.platform.gms.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CheckPhoneResponse {


    public enum Status {
        NOT_REGISTERED, // Chưa có trong DB
        UNVERIFIED,     // Có trong DB (Status=INACTIVE), chưa có PIN
        ACTIVE,         // Có trong DB (Status=ACTIVE), đã có PIN
        LOCKED          // Có trong DB (Status=LOCKED)
    }

    private Status status;
    private boolean hasPin;

    /**
     * Các kênh nhận mã OTP mà tài khoản này dùng được — để màn quên mật khẩu chỉ hiện
     * lựa chọn thực sự khả dụng.
     */
    private boolean hasEmail;
    private boolean hasPhone;

    /**
     * Email/SĐT đã che bớt để hiển thị ("ng***h@gmail.com", "098****321").
     * KHÔNG trả về giá trị đầy đủ: chỉ cần biết SĐT của khách là ai cũng gọi được API này,
     * nên trả nguyên email sẽ làm lộ thông tin liên hệ của người khác.
     */
    private String maskedEmail;
    private String maskedPhone;

    /**
     * Mọi số điện thoại của khách (số chính + số phụ, changeset 037), đã che bớt. Khách có từ
     * 2 số trở lên thì màn đăng nhập/kích hoạt bắt chọn 1 số: số nhận OTP, và là số của phiên
     * đăng nhập / số chính của tài khoản. Chỉ trả key để chọn, không trả số đầy đủ.
     */
    private java.util.List<PhoneOption> phones = new java.util.ArrayList<>();

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PhoneOption {
        /** "primary" cho số chính, id customer_phone cho số phụ. */
        private String key;
        private String maskedPhone;
        private boolean primary;
        /** Đúng số khách vừa gõ ở bước nhập định danh. */
        private boolean matched;
    }

    public CheckPhoneResponse(Status status, boolean hasPin) {
        this.status = status;
        this.hasPin = hasPin;
    }
}
