package com.g42.platform.gms.estimation.infrastructure.entity;

import com.g42.platform.gms.booking.customer.infrastructure.entity.BookingJpaEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@Entity
@Table(name = "service_reminder", schema = "michelin_garage")
public class ServiceReminderJpa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reminder_id", nullable = false)
    private Integer reminderId;

    @NotNull
    @Column(name = "service_ticket_id", nullable = false)
    private Integer serviceTicketId;

    @NotNull
    @Column(name = "vehicle_id", nullable = false)
    private Integer vehicleId;

    @NotNull
    @Column(name = "customer_id", nullable = false)
    private Integer customerId;

    @Column(name = "staff_id")
    private Integer staffId;

    @Column(name = "reminder_date")
    private LocalDate reminderDate;

    @Column(name = "reminder_time")
    private LocalTime reminderTime;

    @Lob
    @Column(name = "note")
    private String note;

    @Lob
    @Column(name = "status")
    private String status;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    private Instant createdAt;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updated_at")
    private Instant updatedAt;
    @Lob
    @Column(name = "reason")
    private String reason;
    @Column(name = "booking_id")
    private Integer bookingId;
}