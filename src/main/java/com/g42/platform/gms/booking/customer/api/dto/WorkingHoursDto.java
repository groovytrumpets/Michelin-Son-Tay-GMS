package com.g42.platform.gms.booking.customer.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

/** dayOfWeek: 1 = Thứ 2 ... 7 = Chủ nhật (ISO-8601, khớp LocalDate.getDayOfWeek().getValue()) */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkingHoursDto {
    private Integer dayOfWeek;
    private Boolean isOpen;
    private LocalTime openTime;
    private LocalTime closeTime;
    private LocalTime breakStart;
    private LocalTime breakEnd;
}
