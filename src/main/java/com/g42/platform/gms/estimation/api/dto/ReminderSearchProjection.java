package com.g42.platform.gms.estimation.api.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public interface ReminderSearchProjection {
    Integer getReminderId();
    Integer getCustomerId();
    String getCustomerName();
    String getCustomerPhone();
    Integer getVehicleId();
    String getLicensePlate();
    String getTicketCode();
    LocalDate getReminderDate();
    LocalTime getReminderTime();
    String getStatus();
    String getNote();
}
