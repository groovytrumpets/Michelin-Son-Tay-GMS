package com.g42.platform.gms.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentDto {
    private String url;
    private String kind; // image | video | file
    private String name;
    private Long size;
    private String mimeType;
    private Integer width;
    private Integer height;
    private Integer durationSec;
    private String thumbnailUrl;
}
