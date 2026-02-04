package com.g42.platform.gms.marketing.service_catalog.domain.entity;

import com.g42.platform.gms.marketing.service_catalog.domain.enums.MediaType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServiceMedia {
    private Long serviceMediaId;
    private int displayOrder;
    private String mediaDescription;
    private String urlThumbnail;
    private String url;
    private MediaType mediaType;
}
