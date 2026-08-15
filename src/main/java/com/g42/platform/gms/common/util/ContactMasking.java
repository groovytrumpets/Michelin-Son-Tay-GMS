package com.g42.platform.gms.common.util;

/**
 * Che bớt thông tin liên hệ trước khi trả ra ngoài.
 *
 * Màn "quên mật khẩu" phải cho người dùng biết mã sẽ gửi tới đâu, nhưng các API đó
 * không yêu cầu đăng nhập — chỉ cần biết số điện thoại của một người là gọi được.
 * Trả nguyên email/SĐT sẽ biến chúng thành công cụ tra thông tin liên hệ của người khác,
 * nên chỉ trả bản đã che, đủ để chủ tài khoản tự nhận ra của mình.
 */
public final class ContactMasking {

    private ContactMasking() {
    }

    public static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /** nguyenvana@gmail.com -> ng***a@gmail.com */
    public static String maskEmail(String email) {
        if (!hasText(email)) {
            return null;
        }
        String value = email.trim();
        int at = value.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        String local = value.substring(0, at);
        String domain = value.substring(at);
        if (local.length() <= 2) {
            return local.charAt(0) + "***" + domain;
        }
        return local.charAt(0) + String.valueOf(local.charAt(1)) + "***"
                + local.charAt(local.length() - 1) + domain;
    }

    /** 0912345678 -> 091****678 */
    public static String maskPhone(String phone) {
        if (!hasText(phone)) {
            return null;
        }
        String value = phone.trim();
        if (value.length() <= 6) {
            return "***" + value.substring(value.length() - Math.min(2, value.length()));
        }
        return value.substring(0, 3) + "****" + value.substring(value.length() - 3);
    }
}
