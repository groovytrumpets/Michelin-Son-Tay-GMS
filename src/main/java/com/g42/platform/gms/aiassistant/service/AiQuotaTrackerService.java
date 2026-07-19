package com.g42.platform.gms.aiassistant.service;

import com.g42.platform.gms.aiassistant.dto.AiQuotaDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Đếm số token/request đã dùng trong ngày để hiển thị mức sử dụng cho người dùng —
 * bộ đếm NỘI BỘ, in-memory (reset khi restart app hoặc sang ngày mới theo giờ hệ thống),
 * dùng CHUNG cho cả 2 kênh (staff + khách hàng) vì cả hai cùng gọi chung 1 API key Gemini.
 * Đây KHÔNG phải quota thật còn lại từ Google — chỉ là ngưỡng tham khảo tự cấu hình
 * (gemini.quota.daily-token-limit / daily-request-limit) để vẽ progress bar cho người dùng.
 */
@Component
public class AiQuotaTrackerService {

    @Value("${gemini.quota.daily-token-limit:1000000}")
    private long dailyTokenLimit;

    @Value("${gemini.quota.daily-request-limit:1500}")
    private long dailyRequestLimit;

    private volatile LocalDate trackedDay = LocalDate.now();
    private final AtomicLong tokensUsedToday = new AtomicLong(0);
    private final AtomicLong requestsUsedToday = new AtomicLong(0);

    public synchronized void record(long totalTokens) {
        rolloverIfNewDay();
        tokensUsedToday.addAndGet(Math.max(totalTokens, 0));
        requestsUsedToday.incrementAndGet();
    }

    public synchronized AiQuotaDto snapshot() {
        rolloverIfNewDay();
        long tokensUsed = tokensUsedToday.get();
        long requestsUsed = requestsUsedToday.get();
        return new AiQuotaDto(
                trackedDay.toString(),
                requestsUsed,
                dailyRequestLimit,
                Math.max(dailyRequestLimit - requestsUsed, 0),
                tokensUsed,
                dailyTokenLimit,
                Math.max(dailyTokenLimit - tokensUsed, 0)
        );
    }

    private void rolloverIfNewDay() {
        LocalDate today = LocalDate.now();
        if (!today.equals(trackedDay)) {
            trackedDay = today;
            tokensUsedToday.set(0);
            requestsUsedToday.set(0);
        }
    }
}
