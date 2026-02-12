package com.g42.platform.gms.marketing.service_catalog.infrastructure.entity;

import com.g42.platform.gms.marketing.service_catalog.domain.enums.ServiceStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "service")
public class ServiceJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long serviceId;
    @Column(columnDefinition = "VARCHAR(100)")
    private String title;
    @Column(columnDefinition = "TEXT")
    private String shortDescription;
    @Column(columnDefinition = "TEXT")
    private String fullDescription;
    private boolean showPrice;
    @Column(columnDefinition = "VARCHAR(100)")
    private String displayPrice;
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private ServiceStatus status;
    @Column(columnDefinition = "DATETIME")
    private LocalDateTime displayFrom;
    @Column(columnDefinition = "DATETIME")
    private LocalDateTime displayTo;
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
    @Column
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    private String mediaThumbnail;

    @OneToMany(mappedBy = "service", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ServiceMediaJpaEntity> media = new ArrayList<>();
}
