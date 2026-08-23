package com.g42.platform.gms.customerimport.application.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Quy tắc làm sạch dữ liệu sổ cũ.
 *
 * Tách thành lớp riêng, không phụ thuộc Spring, để bước kiểm tra thử và bước ghi
 * dùng chung đúng một bộ quy tắc — nếu kiểm tra thử báo hợp lệ thì lúc ghi không
 * được phép hiểu khác đi.
 *
 * Các ngưỡng và bảng ánh xạ ở đây rút ra từ chính file sổ của xưởng, không phải
 * quy tắc chung chung.
 */
public final class ImportNormalizer {

    private ImportNormalizer() {
    }

    /* ============================ Số điện thoại ============================ */

    /**
     * Chuẩn hoá về dạng 10 số bắt đầu bằng 0.
     *
     * Sổ cũ có đủ các kiểu: thừa số 0 ở đầu ("00983898555"), viết theo mã quốc gia,
     * và chèn dấu cách hoặc dấu chấm. Không chuẩn hoá thì cùng một khách sẽ bị tách
     * thành nhiều hồ sơ, mà số điện thoại lại chính là khoá định danh.
     */
    public static String normalizePhone(String raw) {
        if (raw == null) return null;
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return null;

        if (digits.startsWith("84") && digits.length() >= 11) {
            digits = "0" + digits.substring(2);
        }
        // Gỡ số 0 thừa ở đầu, nhưng luôn giữ lại đúng một số 0
        while (digits.length() > 10 && digits.startsWith("00")) {
            digits = digits.substring(1);
        }
        return digits;
    }

    /** Số di động Việt Nam hợp lệ: 10 chữ số, bắt đầu bằng 0. */
    public static boolean isValidPhone(String normalized) {
        return normalized != null && normalized.matches("^0\\d{9}$");
    }

    /**
     * PIN khởi tạo cho khách nhập từ sổ cũ: 6 số cuối của số điện thoại.
     * Đây là mã đoán được nếu biết số điện thoại, nên tài khoản luôn được tạo ở
     * trạng thái chưa kích hoạt kèm cờ bắt đổi PIN ở lần đăng nhập đầu.
     */
    public static String defaultPin(String normalizedPhone) {
        if (normalizedPhone == null || normalizedPhone.length() < 6) return null;
        return normalizedPhone.substring(normalizedPhone.length() - 6);
    }

    /* ============================== Biển số =============================== */

    /** Khoá so khớp biển số: viết hoa, bỏ mọi ký tự không phải chữ và số. */
    public static String normalizePlate(String raw) {
        if (raw == null) return null;
        String cleaned = raw.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
        return cleaned.isEmpty() ? null : cleaned;
    }

    /** Biển số Việt Nam: 2 số đầu là mã tỉnh, tổng 7-9 ký tự sau khi bỏ dấu. */
    public static boolean looksLikePlate(String normalizedPlate) {
        return normalizedPlate != null && normalizedPlate.matches("^\\d{2}[A-Z]{1,2}\\d{4,6}$");
    }

    /* =============================== Hãng xe ============================== */

    /**
     * Lỗi chính tả quan sát được trong sổ. Cố ý CHỈ gộp các biến thể viết sai của
     * tên hãng — không suy từ tên dòng xe ra hãng, vì đoán sai sẽ ghi dữ liệu sai
     * mà người dùng không biết. Ô nào không khớp thì giữ nguyên văn và báo cảnh báo
     * để người nhập tự quyết.
     */
    private static final Map<String, String> BRAND_ALIASES = Map.ofEntries(
            Map.entry("hydai", "Hyundai"),
            Map.entry("hyndai", "Hyundai"),
            Map.entry("huyndai", "Hyundai"),
            Map.entry("hyunhdai", "Hyundai"),
            Map.entry("nisan", "Nissan"),
            Map.entry("misu", "Mitsubishi"),
            Map.entry("misa", "Mitsubishi"),
            Map.entry("mitshubishi", "Mitsubishi"),
            Map.entry("chevle", "Chevrolet"),
            Map.entry("chevrolette", "Chevrolet"),
            Map.entry("bmv", "BMW"),
            Map.entry("vinfat", "VinFast"),
            Map.entry("vinfast", "VinFast"),
            Map.entry("madza", "Mazda"),
            Map.entry("mada", "Mazda"),
            Map.entry("toyota", "Toyota"),
            Map.entry("honda", "Honda"),
            Map.entry("kia", "Kia"),
            Map.entry("ford", "Ford"),
            Map.entry("mazda", "Mazda"),
            Map.entry("hyundai", "Hyundai"),
            Map.entry("nissan", "Nissan"),
            Map.entry("mitsubishi", "Mitsubishi"),
            Map.entry("chevrolet", "Chevrolet"),
            Map.entry("bmw", "BMW"),
            Map.entry("mercedes", "Mercedes-Benz"),
            Map.entry("lexus", "Lexus"),
            Map.entry("suzuki", "Suzuki"),
            Map.entry("isuzu", "Isuzu"),
            Map.entry("peugeot", "Peugeot")
    );

    /**
     * Trả về tên hãng đã gộp lỗi chính tả, hoặc null nếu không nhận ra.
     * Người gọi tự đối chiếu tiếp với danh mục vehicle_brand trong cơ sở dữ liệu.
     */
    public static String canonicalBrand(String raw) {
        String key = stripDiacritics(raw);
        if (key == null) return null;
        return BRAND_ALIASES.get(key);
    }

    /* ================== Ghi chú chăm sóc khách hàng ======================= */

    /**
     * Những kết quả gọi có nghĩa là không liên hệ được nữa. Gặp các ghi chú này thì
     * bật cờ do_not_contact để phân hệ nhắc lịch thôi đẩy khách vào danh sách gọi.
     */
    private static final Set<String> DO_NOT_CONTACT_MARKERS = Set.of(
            "so chet", "chan so", "sai so", "so khong dung", "khong lien lac duoc"
    );

    public static boolean isDoNotContactNote(String note) {
        String key = stripDiacritics(note);
        if (key == null) return false;
        return DO_NOT_CONTACT_MARKERS.stream().anyMatch(key::contains);
    }

    /* ================================ Ngày ================================ */

    /** Chỉ nhận yyyy-MM-dd; FE chịu trách nhiệm quy đổi serial Excel trước khi gửi. */
    public static LocalDate parseDate(String raw) {
        String value = trimToNull(raw);
        if (value == null) return null;
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /* ============================== Chống trùng ============================ */

    /**
     * Khoá chống nhập trùng.
     *
     * Không dùng khoá ghép trên nhiều cột vì MySQL cho phép trùng vô hạn khi một
     * cột trong khoá là NULL, mà mã phiếu trong sổ cũ thường bỏ trống. Ghép sẵn một
     * chuỗi không rỗng ở đây thì nhập lại đúng file lần hai chắc chắn ra 0 dòng mới.
     *
     * Có băm cả danh sách dịch vụ để hai lượt cùng khách cùng ngày mà nội dung khác
     * nhau vẫn được coi là hai lượt riêng — sổ cũ có trường hợp này.
     *
     * customerRef là số điện thoại chứ không phải customerId, để bước kiểm tra thử
     * (chưa có id vì khách chưa được tạo) sinh ra đúng cùng một khoá với bước ghi.
     */
    public static String dedupeKey(String customerRef, LocalDate visitedDate,
                                   String legacyTicketCode, List<String> itemNames) {
        String code = trimToNull(legacyTicketCode);
        StringBuilder sb = new StringBuilder();
        sb.append(customerRef).append('|')
                .append(visitedDate == null ? "" : visitedDate).append('|')
                .append(code == null ? "" : code).append('|')
                .append(shortHash(String.join(";", itemNames == null ? List.of() : itemNames)));
        String key = sb.toString();
        return key.length() <= 120 ? key : key.substring(0, 120);
    }

    private static String shortHash(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                hex.append(String.format("%02x", digest[i]));
            }
            return hex.toString();
        } catch (Exception e) {
            // Không có thuật toán băm thì vẫn phải ra một khoá ổn định
            return Integer.toHexString(value.hashCode());
        }
    }

    /* ============================== Tiện ích ============================== */

    public static String trimToNull(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** Bỏ dấu tiếng Việt và viết thường, dùng làm khoá so khớp mờ. */
    public static String stripDiacritics(String raw) {
        String value = trimToNull(raw);
        if (value == null) return null;
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace('đ', 'd').replace('Đ', 'D');
        return normalized.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    /** Cộng tiền bỏ qua giá trị rỗng, dùng để đối chiếu tổng phiếu với tổng các dòng. */
    public static BigDecimal sum(List<BigDecimal> values) {
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal v : values) {
            if (v != null) total = total.add(v);
        }
        return total;
    }
}
