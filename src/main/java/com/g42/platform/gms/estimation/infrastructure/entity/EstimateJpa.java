    package com.g42.platform.gms.estimation.infrastructure.entity;

    import com.g42.platform.gms.common.enums.EstimateEnum;
    import com.g42.platform.gms.estimation.domain.enums.EstimateTypeEnum;
    import com.g42.platform.gms.service_ticket_management.infrastructure.entity.ServiceTicketJpa;
    import jakarta.persistence.*;
    import jakarta.validation.constraints.NotNull;
    import lombok.Getter;
    import lombok.Setter;
    import org.hibernate.annotations.ColumnDefault;

    import java.math.BigDecimal;
    import java.time.Instant;

    @Getter
    @Setter
    @Entity
    @Table(name = "estimate", schema = "michelin_garage")
    public class EstimateJpa {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "estimate_id", nullable = false)
        private Integer id;

        @NotNull
        @Column(name = "service_ticket_id", nullable = false)
        private Integer serviceTicketId;
        @Enumerated(EnumType.STRING)
        @NotNull
        @Lob
        @Column(name = "estimate_type", nullable = false)
        private EstimateTypeEnum estimateType;

        @Enumerated(EnumType.STRING)
        @NotNull
        @ColumnDefault("'DRAFT'")
        @Lob
        @Column(name = "status", nullable = false)
        private EstimateEnum status;

        @ColumnDefault("CURRENT_TIMESTAMP")
        @Column(name = "created_at")
        private Instant createdAt;

        @Column(name = "approved_at")
        private Instant approvedAt;

        @NotNull
        @ColumnDefault("1")
        @Column(name = "version", nullable = false)
        private Integer version;

        @Column(name = "revised_from_id")
        private Integer revisedFromId;
        @Column(name = "total_price", precision = 12, scale = 2)
        private BigDecimal totalPrice;


    }