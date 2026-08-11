package com.g42.platform.gms.marketing.news.app;

import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * Sinh đoạn định danh cho URL từ tiêu đề tiếng Việt.
 *
 * <p>URL sạch không kèm id là điểm cộng khi chia sẻ trên mạng xã hội và khi
 * Google hiển thị đường dẫn trong kết quả tìm kiếm, nên tính duy nhất được bảo
 * đảm bằng hậu tố số thay vì nhét khoá chính vào đường dẫn.
 */
public final class SlugGenerator {

    private static final int MAX_LENGTH = 200;

    private SlugGenerator() {
    }

    /**
     * Chuyển "Kinh nghiệm chọn lốp Michelin cho xe gầm cao"
     * thành "kinh-nghiem-chon-lop-michelin-cho-xe-gam-cao".
     */
    public static String toSlug(String raw) {
        if (raw == null) return "";
        String text = raw.trim();
        if (text.isEmpty()) return "";

        // đ/Đ không phải là chữ cái có dấu phụ nên Normalizer không tách được,
        // phải thay tay trước khi bỏ dấu.
        text = text.replace('đ', 'd').replace('Đ', 'D');

        String noAccent = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        String slug = noAccent.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");

        if (slug.length() > MAX_LENGTH) {
            slug = slug.substring(0, MAX_LENGTH).replaceAll("-+$", "");
        }
        return slug;
    }

    /**
     * Sinh slug chưa bị dùng. {@code isTaken} trả về true nghĩa là slug đã có
     * chủ, khi đó nối thêm {@code -2}, {@code -3}… cho tới khi trống.
     */
    public static String toUniqueSlug(String raw, String fallback, Predicate<String> isTaken) {
        String base = toSlug(raw);
        if (base.isEmpty()) base = toSlug(fallback);
        if (base.isEmpty()) base = "bai-viet";

        if (!isTaken.test(base)) return base;

        for (int suffix = 2; suffix < 1000; suffix++) {
            String candidate = trimForSuffix(base, suffix) + "-" + suffix;
            if (!isTaken.test(candidate)) return candidate;
        }
        // Trường hợp gần như không xảy ra; mốc thời gian bảo đảm không đụng nhau.
        return trimForSuffix(base, 0) + "-" + System.currentTimeMillis();
    }

    private static String trimForSuffix(String base, int suffix) {
        int room = MAX_LENGTH - (String.valueOf(suffix).length() + 1);
        if (base.length() <= room) return base;
        return base.substring(0, room).replaceAll("-+$", "");
    }
}
