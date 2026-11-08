package com.g42.platform.gms.notification.application.zalo;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * 50 sự kiện webhook Zalo gần nhất, giữ trong bộ nhớ (mất khi restart BE).
 * Chỉ để màn /zalo-oa-config trả lời câu "Zalo có đang bắn webhook về không?" —
 * dữ liệu nghiệp vụ (đánh giá phiếu) vẫn lưu vào bảng feedback như cũ.
 */
@Component
public class ZaloWebhookEventLog {

    private static final int MAX = 50;

    public record Event(Instant receivedAt, String eventName, String summary, boolean handled, String error) {}

    private final Deque<Event> events = new ArrayDeque<>();

    public synchronized void record(String eventName, String summary, boolean handled, String error) {
        events.addFirst(new Event(Instant.now(), eventName, summary, handled, error));
        while (events.size() > MAX) events.removeLast();
    }

    public synchronized List<Event> recent() {
        return new ArrayList<>(events);
    }
}
