package com.g42.platform.gms.customer.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "point_config")
@Getter
@Setter
public class PointConfigJpa {

    @Id
    @Column(name = "id")
    private Integer id = 1;

    @Column(name = "points_per_1000_vnd")
    private Integer pointsPer1000Vnd = 1;

    @Column(name = "bonus_points_per_service")
    private Integer bonusPointsPerService = 10;

    @Column(name = "points_per_referral")
    private Integer pointsPerReferral = 50;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
