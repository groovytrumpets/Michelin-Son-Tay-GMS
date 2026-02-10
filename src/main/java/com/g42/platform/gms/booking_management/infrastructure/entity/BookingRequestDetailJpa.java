package com.g42.platform.gms.booking_management.infrastructure.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "booking_request_details")
@Data
public class BookingRequestDetailJpa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_detail_id")
    private Integer requestDetailId;

    @ManyToOne
    @JoinColumn(name = "request_id", nullable = false)
    private BookingRequestJpa request;

    @ManyToOne
    @JoinColumn(name = "item_id", nullable = false)
    private CatalogItemJpa item;
}
