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

    @Column(name = "rank_silver_points")
    private Integer rankSilverPoints = 5000;

    @Column(name = "rank_gold_points")
    private Integer rankGoldPoints = 15000;

    @Column(name = "rank_platinum_points")
    private Integer rankPlatinumPoints = 30000;

    @Column(name = "rank_diamond_points")
    private Integer rankDiamondPoints = 50000;

    @Column(name = "dealer_level2_points")
    private Integer dealerLevel2Points = 50000;

    @Column(name = "dealer_level3_points")
    private Integer dealerLevel3Points = 150000;

    @Column(name = "dealer_level4_points")
    private Integer dealerLevel4Points = 300000;

    @Column(name = "dealer_level5_points")
    private Integer dealerLevel5Points = 500000;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
