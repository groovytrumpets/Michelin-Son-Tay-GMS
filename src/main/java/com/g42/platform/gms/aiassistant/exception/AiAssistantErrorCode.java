package com.g42.platform.gms.aiassistant.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AiAssistantErrorCode {
    NOT_CONFIGURED("AI_NOT_CONFIGURED", "Trợ lý AI chưa được cấu hình"),
    UPSTREAM_ERROR("AI_UPSTREAM_ERROR", "Trợ lý AI đang gặp sự cố, vui lòng thử lại sau"),
    EMPTY_RESPONSE("AI_EMPTY_RESPONSE", "Trợ lý AI không trả về nội dung"),
    RATE_LIMITED("AI_RATE_LIMITED", "Bạn đang gửi quá nhiều yêu cầu, vui lòng thử lại sau ít phút"),
    /** Google báo quá tải (503) hoặc không trả lời kịp ở mọi model — lỗi phía Google, thử lại sau là được. */
    OVERLOADED("AI_OVERLOADED", "Máy chủ AI của Google đang quá tải nên chưa trả lời được. Vui lòng thử lại sau ít phút"),
    /** Mọi model đều trả 429: key đã dùng hết hạn mức Google cấp (theo phút hoặc theo ngày). */
    QUOTA_EXHAUSTED("AI_QUOTA_EXHAUSTED", "Trợ lý AI đã dùng hết lượt Google cấp cho hôm nay. Vui lòng thử lại sau");

    private final String code;
    private final String message;
}
