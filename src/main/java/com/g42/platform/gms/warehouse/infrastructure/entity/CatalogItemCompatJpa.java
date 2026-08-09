package com.g42.platform.gms.warehouse.infrastructure.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Một dòng khai báo xe tương thích của vật tư - hàng hóa.
 * Để trống brandId nghĩa là áp dụng cho mọi hãng; để trống modelId nghĩa là mọi
 * dòng của hãng đó. yearFrom/yearTo giới hạn theo đời xe, để trống là không giới hạn.
 */
@Entity(name = "CatalogItemCompat")
@Table(name = "catalog_item_compat")
@Getter
@Setter
public class CatalogItemCompatJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "compat_id")
    private Integer compatId;

    @NotNull
    @Column(name = "item_id", nullable = false)
    private Integer itemId;

    @Column(name = "brand_id")
    private Integer brandId;

    @Column(name = "model_id")
    private Integer modelId;

    @Column(name = "year_from")
    private Integer yearFrom;

    @Column(name = "year_to")
    private Integer yearTo;
}
