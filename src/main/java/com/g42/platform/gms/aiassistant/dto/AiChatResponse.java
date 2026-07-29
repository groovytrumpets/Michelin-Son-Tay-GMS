package com.g42.platform.gms.aiassistant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiChatResponse {
    private String reply;
    private AiUsageDto usage;
    private AiQuotaDto quota;
    private String usedModel;

    public AiChatResponse(String reply, AiUsageDto usage, AiQuotaDto quota) {
        this.reply = reply;
        this.usage = usage;
        this.quota = quota;
    }
}
