package com.g42.platform.gms.marketing.service_catalog.infrastructure.entity;

import com.g42.platform.gms.marketing.service_catalog.domain.enums.MediaType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "service_media")
public class ServiceMediaJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long serviceMediaId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MediaType mediaType;
    private String mediaUrl;
    private String mediaThumbnail;
    private String mediaDescription;
    @Column(nullable = false)
    private int displayOrder = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceJpaEntity service;
}
