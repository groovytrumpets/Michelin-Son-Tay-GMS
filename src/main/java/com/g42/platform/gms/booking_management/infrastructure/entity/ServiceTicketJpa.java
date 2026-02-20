package com.g42.platform.gms.booking_management.infrastructure.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "service_ticket", schema = "michelin_garage")
public class ServiceTicketJpa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "service_ticket_id", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private BookingJpa booking;

    @NotNull
    @Lob
    @Column(name = "status", nullable = false)
    private String status;

    @Lob
    @Column(name = "customer_request")
    private String customerRequest;

    @Lob
    @Column(name = "technician_notes")
    private String technicianNotes;

    @Column(name = "received_at")
    private Instant receivedAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    private Instant createdAt;

    @ColumnDefault("0")
    @Column(name = "is_deleted")
    private Boolean isDeleted;


}