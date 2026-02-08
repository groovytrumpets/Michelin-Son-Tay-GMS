package com.g42.platform.gms.booking.customer.dto;

import com.g42.platform.gms.booking.customer.entity.Booking;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class BookingResponse {
    private Integer bookingId;
    private Integer customerId;
    private String customerName;
    private String phone;
    private LocalDate scheduledDate;
    private LocalTime scheduledTime;
    private String description;
    private String status;
    private Boolean isGuest;
    private List<Integer> serviceIds;
    private Integer vehicleId;
    
    public static BookingResponse from(Booking booking) {
        BookingResponse response = new BookingResponse();
        response.setBookingId(booking.getBookingId());
        if (booking.getCustomer() != null) {
            response.setCustomerId(booking.getCustomer().getCustomerId());
            response.setCustomerName(booking.getCustomer().getFullName());
            response.setPhone(booking.getCustomer().getPhone());
        }
        response.setScheduledDate(booking.getScheduledDate());
        response.setScheduledTime(booking.getScheduledTime());
        response.setDescription(booking.getDescription());
        if (booking.getStatus() != null) {
            response.setStatus(booking.getStatus().name());
        }
        response.setIsGuest(booking.getIsGuest());
        if (booking.getServices() != null) {
            response.setServiceIds(booking.getServices().stream()
                .map(service -> service.getItemId() != null ? service.getItemId() : null)
                .filter(id -> id != null)
                .collect(Collectors.toList()));
        }
        if (booking.getVehicle() != null) {
            response.setVehicleId(booking.getVehicle().getVehicleId());
        }
        return response;
    }
}
