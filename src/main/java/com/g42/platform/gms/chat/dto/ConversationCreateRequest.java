package com.g42.platform.gms.chat.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class ConversationCreateRequest {
    @NotEmpty
    private List<Integer> participantIds;
    private String type = "direct"; // direct | group
    private String title;
}
