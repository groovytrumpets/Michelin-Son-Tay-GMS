package com.g42.platform.gms.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatReceiptDto {
    private Integer conversationId;
    private Integer messageId;
    private String status; // read
    private Integer byStaffId;
}
