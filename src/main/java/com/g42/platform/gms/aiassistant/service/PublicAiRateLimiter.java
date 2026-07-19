package com.g42.platform.gms.aiassistant.service;

import com.g42.platform.gms.aiassistant.exception.AiAssistantErrorCode;
import com.g42.platform.gms.aiassistant.exception.AiAssistantException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiter đơn giản (in-memory, sliding window) cho endpoint AI công khai
 * (không yêu cầu đăng nhập) — chặn lạm dụng vì quota free tier của Gemini rất hạn chế.
 * Không cần Redis/DB: mỗi instance backend tự quản lý bộ đếm riêng, đủ dùng cho quy mô hiện tại.
 */
@Component
public class PublicAiRateLimiter {

    private static final int MAX_REQUESTS_PER_WINDOW = 6;
    private static final long WINDOW_MILLIS = 60_000L;

    private final ConcurrentHashMap<String, Deque<Long>> requestTimestampsByIp = new ConcurrentHashMap<>();

    public void checkAndRecord(String clientIp) {
        String key = clientIp == null || clientIp.isBlank() ? "unknown" : clientIp;
        Deque<Long> timestamps = requestTimestampsByIp.computeIfAbsent(key, k -> new ArrayDeque<>());

        synchronized (timestamps) {
            long now = Instant.now().toEpochMilli();
            while (!timestamps.isEmpty() && now - timestamps.peekFirst() > WINDOW_MILLIS) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= MAX_REQUESTS_PER_WINDOW) {
                throw new AiAssistantException(AiAssistantErrorCode.RATE_LIMITED);
            }
            timestamps.addLast(now);
        }
    }
}
