package com.g42.platform.gms.promotion.infrastructure.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "promotion_customer", schema = "michelin_garage")
public class PromotionCustomerJpa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "promotion_customer_id", nullable = false)
    private Integer promotionCustomerId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "promotion_id", nullable = false)
    private PromotionJpa promotion;

    @NotNull
    @Column(name = "customer_id", nullable = false)
    private Integer customerProfileId;


}