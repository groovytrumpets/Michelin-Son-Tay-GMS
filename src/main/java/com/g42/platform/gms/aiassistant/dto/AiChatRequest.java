package com.g42.platform.gms.aiassistant.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class AiChatRequest {
    @NotBlank
    private String message;
    private List<AiChatTurn> history;
    private String model;
}
