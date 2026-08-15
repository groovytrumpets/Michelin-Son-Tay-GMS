package com.g42.platform.gms.booking.customer.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "booking_config")
@Getter
@Setter
public class BookingConfigJpa {

    @Id
    @Column(name = "id")
    private Integer id = 1;

    /** Độ dài 1 block slot (phút), thay cho hằng số BASE_SLOT_MINUTES trước đây */
    @Column(name = "slot_duration_minutes", nullable = false)
    private Integer slotDurationMinutes = 30;

    /** Số giờ tối thiểu phải đặt trước, áp dụng cho customer */
    @Column(name = "min_lead_time_hours", nullable = false)
    private Integer minLeadTimeHours = 2;

    /** Số ngày tối đa cho phép đặt lịch trước */
    @Column(name = "days_ahead_horizon", nullable = false)
    private Integer daysAheadHorizon = 10;

    /** Sức chứa mặc định áp dụng khi sinh slot mới */
    @Column(name = "default_capacity", nullable = false)
    private Integer defaultCapacity = 3;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
