package com.g42.platform.gms.booking.customer.infrastructure.entity;

import com.g42.platform.gms.booking_management.infrastructure.entity.CatalogItemJpaEntity;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "booking_request_details")
@Data
public class BookingRequestDetailJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_detail_id")
    private Integer requestDetailId;

    @ManyToOne
    @JoinColumn(name = "request_id", nullable = false)
    private BookingRequestJpaEntity request;

    @ManyToOne
    @JoinColumn(name = "item_id", nullable = false)
    private CatalogItemJpaEntity item;
}
