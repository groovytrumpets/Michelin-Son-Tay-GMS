package com.g42.platform.gms.marketing.service_catalog.domain.entity;

import com.g42.platform.gms.marketing.service_catalog.domain.enums.MediaType;

public class ServiceMedia {
    private Long serviceMediaId;
    private int displayOrder;
    private String mediaDescription;
    private String mediaThumbnail;
    private String mediaUrl;
    private MediaType mediaType;
}
