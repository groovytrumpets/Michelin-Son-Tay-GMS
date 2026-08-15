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

    public CheckPhoneResponse(Status status, boolean hasPin) {
        this.status = status;
        this.hasPin = hasPin;
    }
}
