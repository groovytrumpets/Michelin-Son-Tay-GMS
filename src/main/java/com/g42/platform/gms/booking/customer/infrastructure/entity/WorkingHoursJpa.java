package com.g42.platform.gms.booking.customer.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Giờ hoạt động của xưởng theo từng ngày trong tuần.
 * dayOfWeek dùng số ISO-8601: 1 = Thứ 2 ... 7 = Chủ nhật (khớp LocalDate.getDayOfWeek().getValue()).
 */
@Entity
@Table(name = "working_hours_config")
@Getter
@Setter
public class WorkingHoursJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "day_of_week", nullable = false, unique = true)
    private Integer dayOfWeek;

    @Column(name = "is_open", nullable = false)
    private Boolean isOpen = true;

    @Column(name = "open_time")
    private LocalTime openTime;

    @Column(name = "close_time")
    private LocalTime closeTime;

    @Column(name = "break_start")
    private LocalTime breakStart;

    @Column(name = "break_end")
    private LocalTime breakEnd;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
