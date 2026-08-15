package com.g42.platform.gms.auth.dto;

import com.g42.platform.gms.common.util.ContactMasking;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Các kênh nhận mã OTP mà một tài khoản dùng được — để màn quên mật khẩu chỉ hiện
 * lựa chọn thực sự khả dụng, kèm giá trị đã che bớt để người dùng nhận ra của mình.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContactChannelsResponse {

    private boolean hasEmail;
    private boolean hasPhone;
    private String maskedEmail;
    private String maskedPhone;

    public static ContactChannelsResponse of(String phone, String email) {
        return new ContactChannelsResponse(
                ContactMasking.hasText(email),
                ContactMasking.hasText(phone),
                ContactMasking.maskEmail(email),
                ContactMasking.maskPhone(phone)
        );
    }
}
