package com.g42.platform.gms.aiassistant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/** Số token của MỘT lượt chat, lấy từ usageMetadata trong response của Gemini. */
@Data
@AllArgsConstructor
public class AiUsageDto {
    private int promptTokens;
    private int responseTokens;
    private int totalTokens;
}
