package com.g42.platform.gms.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessagePageDto {
    private List<MessageDto> messages;
    private boolean hasMore;
    private Integer nextCursor;
}
