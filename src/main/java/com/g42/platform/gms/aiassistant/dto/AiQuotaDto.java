package com.g42.platform.gms.aiassistant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Snapshot mức sử dụng AI trong ngày — đếm NỘI BỘ (in-memory ở backend), dùng để
 * vẽ thanh quota cho người dùng thấy. KHÔNG PHẢI số quota thật Google còn cấp cho
 * API key (Gemini không có API tra cứu quota còn lại cho free tier); tokenLimit/
 * requestLimit chỉ là ngưỡng tham khảo tự cấu hình (gemini.quota.*).
 */
@Data
@AllArgsConstructor
public class AiQuotaDto {
    private String date;
    private long requestsUsed;
    private long requestLimit;
    private long requestsRemaining;
    private long tokensUsed;
    private long tokenLimit;
    private long tokensRemaining;
}
