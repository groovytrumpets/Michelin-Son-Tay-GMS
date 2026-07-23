package com.g42.platform.gms.marketing.slider.infrastructure.entity;

import com.g42.platform.gms.promotion.infrastructure.entity.PromotionJpa;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "slider_item", schema = "michelin_garage")
public class SliderItemJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slider_id", nullable = false)
    private SliderJpa slider;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id")
    private PromotionJpa promotion;

    @Column(name = "image_url", length = 500, nullable = false)
    private String imageUrl;

    @Column(name = "target_url", length = 500)
    private String targetUrl;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "subtitle", length = 200)
    private String subtitle;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @ColumnDefault("1")
    @Column(name = "is_active")
    private Boolean isActive;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
