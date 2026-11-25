package com.g42.platform.gms.systemlog.service;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * Ghi chú dễ đọc cho dòng Nhật ký hệ thống của request hiện tại.
 *
 * <p>Mô tả tự động của AuditAspect chỉ là đường dẫn API ("PUT /api/warehouse/catalog-item/update/1306"),
 * người quản lý đọc không hiểu ai đã đổi cái gì. Service nghiệp vụ gọi {@link #headline} và
 * {@link #change} trong lúc xử lý; khi controller trả về thành công, AuditAspect lấy ghi chú này
 * làm mô tả thay cho đường dẫn API.
 *
 * <p>Ghi chú sống trong request attribute nên chỉ có tác dụng trên thread request; gọi từ job nền
 * hay thread async thì bị bỏ qua, không lỗi.
 *
 * <p>Định dạng mô tả lưu xuống system_log.description (FE tách theo dòng):
 * <pre>
 * Sửa mặt hàng «BUGI ...» (SKU 1885510060)
 * • Tồn kho CS1 TÂN PHÚC: 40 → 0
 * • Lô NK-20261104-6 (CS1 TÂN PHÚC) còn: 40 → 0
 * API: PUT /api/warehouse/catalog-item/update/1306
 * </pre>
 */
public final class AuditNote {

    public static final String CHANGE_PREFIX = "• ";
    public static final String API_PREFIX = "API: ";

    private static final String ATTR = AuditNote.class.getName();
    /** system_log.description là VARCHAR(1000); chừa chỗ cho dòng API. */
    private static final int MAX_LENGTH = 880;

    private String headline;
    private final List<String> changes = new ArrayList<>();
    private boolean skipped;

    private AuditNote() {
    }

    /**
     * Không ghi dòng nhật ký nào cho request này — dùng khi lần lưu không đổi gì
     * (ví dụ màn sửa mặt hàng luôn gọi kèm "lưu màu sắc" dù màu không đổi,
     * trước đây sinh hàng nghìn dòng nhiễu).
     */
    public static void skip() {
        AuditNote note = current(true);
        if (note != null) {
            note.skipped = true;
        }
    }

    /** true nếu service đã gọi {@link #skip()}; xoá cờ sau khi đọc. */
    public static boolean consumeSkip() {
        AuditNote note = current(false);
        if (note == null || !note.skipped) {
            return false;
        }
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            attrs.removeAttribute(ATTR, RequestAttributes.SCOPE_REQUEST);
        }
        return true;
    }

    /** Câu tóm tắt thao tác, ví dụ "Sửa mặt hàng «BUGI ...»". Gọi lại thì ghi đè. */
    public static void headline(String text) {
        AuditNote note = current(true);
        if (note != null && text != null && !text.isBlank()) {
            note.headline = text.trim();
        }
    }

    /** Thêm một dòng thay đổi, ví dụ "Tồn kho CS1: 40 → 0". */
    public static void change(String line) {
        AuditNote note = current(true);
        if (note != null && line != null && !line.isBlank()) {
            note.changes.add(line.trim());
        }
    }

    /**
     * Lấy và xoá ghi chú của request hiện tại; null nếu service không ghi gì.
     * Xoá luôn để một request gọi nhiều endpoint lồng nhau không bị dính ghi chú cũ.
     */
    public static String consume(String apiLine, String fallbackHeadline) {
        AuditNote note = current(false);
        if (note == null) {
            return null;
        }
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            attrs.removeAttribute(ATTR, RequestAttributes.SCOPE_REQUEST);
        }
        if (note.headline == null && note.changes.isEmpty()) {
            return null;
        }

        StringBuilder sb = new StringBuilder(note.headline != null ? note.headline : nullToEmpty(fallbackHeadline));
        int written = 0;
        for (String line : note.changes) {
            String next = "\n" + CHANGE_PREFIX + line;
            if (sb.length() + next.length() > MAX_LENGTH) {
                sb.append("\n").append(CHANGE_PREFIX).append("… và ")
                        .append(note.changes.size() - written).append(" thay đổi khác");
                break;
            }
            sb.append(next);
            written++;
        }
        if (apiLine != null && !apiLine.isBlank()) {
            sb.append("\n").append(API_PREFIX).append(apiLine);
        }
        return sb.toString();
    }

    private static AuditNote current(boolean create) {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        Object existing = attrs.getAttribute(ATTR, RequestAttributes.SCOPE_REQUEST);
        if (existing instanceof AuditNote note) {
            return note;
        }
        if (!create) {
            return null;
        }
        AuditNote note = new AuditNote();
        attrs.setAttribute(ATTR, note, RequestAttributes.SCOPE_REQUEST);
        return note;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
