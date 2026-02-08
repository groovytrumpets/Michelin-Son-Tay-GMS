package com.g42.platform.gms.booking.customer.entity;

import com.g42.platform.gms.catalog.entity.CatalogItem;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "booking_request_details")
@Data
public class BookingRequestDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_detail_id")
    private Integer requestDetailId;

    @ManyToOne
    @JoinColumn(name = "request_id", nullable = false)
    private BookingRequest request;

    @ManyToOne
    @JoinColumn(name = "item_id", nullable = false)
    private CatalogItem item;
}
