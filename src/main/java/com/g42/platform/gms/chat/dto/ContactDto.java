package com.g42.platform.gms.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContactDto {
    private Integer staffId;
    private String fullName;
    private String avatarUrl;
    private List<String> role;
    private boolean online;
    private LocalDateTime lastSeenAt;
}
