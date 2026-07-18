package com.g42.platform.gms.customer.infrastructure.entity;

import com.g42.platform.gms.customer.domain.enums.CustomerRank;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "customer_points")
@Getter
@Setter
public class CustomerPointsJpa {

    @Id
    @Column(name = "customer_id")
    private Integer customerId;

    @Column(name = "total_points")
    private Integer totalPoints = 0;

    @Column(name = "lifetime_points")
    private Integer lifetimePoints = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_rank", length = 20)
    private CustomerRank currentRank = CustomerRank.BRONZE;

    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt;

    @Column(name = "points_reset_year")
    private Integer pointsResetYear;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
