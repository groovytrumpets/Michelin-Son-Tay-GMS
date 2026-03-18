package com.g42.platform.gms.estimation.infrastructure.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "tax_rule", schema = "michelin_garage")
public class TaxRuleJpa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idtax_rule", nullable = false)
    private Integer taxRuleId;

    @Size(max = 45)
    @NotNull
    @Column(name = "tax_code", nullable = false, length = 45)
    private String taxCode;

    @Size(max = 100)
    @Column(name = "tax_name", length = 100)
    private String taxName;

    @Column(name = "tax_rate", precision = 5, scale = 2)
    private BigDecimal taxRate;

    @Size(max = 45)
    @Column(name = "item_type", length = 45)
    private String itemType;

    @NotNull
    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @ColumnDefault("1")
    @Column(name = "is_active")
    private Byte isActive;


}