package com.g42.platform.gms.service_ticket_management.api.mapper;

import com.g42.platform.gms.auth.entity.CustomerProfile;
import com.g42.platform.gms.booking.customer.domain.entity.Booking;
import com.g42.platform.gms.service_ticket_management.api.dto.manage.ServiceTicketListResponse;
import com.g42.platform.gms.service_ticket_management.api.dto.technician.TechnicianTicketListResponse;
import com.g42.platform.gms.service_ticket_management.domain.entity.ServiceTicket;
import com.g42.platform.gms.vehicle.domain.entity.Vehicle;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ServiceTicketListMapper {

    @Mapping(target = "serviceTicketId", source = "ticket.serviceTicketId")
    @Mapping(target = "ticketCode", source = "ticket.ticketCode")
    @Mapping(target = "ticketStatus", source = "ticket.ticketStatus")
    @Mapping(target = "receivedAt", source = "ticket.receivedAt")
    @Mapping(target = "createdAt", source = "ticket.createdAt")
    @Mapping(target = "customerRequest", source = "ticket.customerRequest")
    @Mapping(target = "customerId", source = "customer.customerId")
    @Mapping(target = "customerName", source = "customer.fullName")
    @Mapping(target = "customerPhone", source = "customer.phone")
    @Mapping(target = "vehicleId", source = "vehicle.vehicleId")
    @Mapping(target = "licensePlate", source = "vehicle.licensePlate")
    @Mapping(target = "vehicleMake", source = "vehicle.brand")
    @Mapping(target = "vehicleModel", source = "vehicle.model")
    @Mapping(target = "bookingId", source = "booking.bookingId")
    @Mapping(target = "bookingCode", source = "booking.bookingCode")
    @Mapping(target = "scheduledDate", source = "booking.scheduledDate")
    @Mapping(target = "scheduledTime", source = "booking.scheduledTime")
    @Mapping(target = "serviceCategory", source = "booking.serviceCategory")
    @Mapping(target = "isGuest", source = "booking.isGuest")
    ServiceTicketListResponse toManageListResponse(ServiceTicket ticket,
                                                    CustomerProfile customer,
                                                    Vehicle vehicle,
                                                    Booking booking);

    @Mapping(target = "serviceTicketId", source = "ticket.serviceTicketId")
    @Mapping(target = "ticketCode", source = "ticket.ticketCode")
    @Mapping(target = "ticketStatus", source = "ticket.ticketStatus")
    @Mapping(target = "receivedAt", source = "ticket.receivedAt")
    @Mapping(target = "createdAt", source = "ticket.createdAt")
    @Mapping(target = "customerRequest", source = "ticket.customerRequest")
    @Mapping(target = "technicianNotes", source = "ticket.technicianNotes")
    @Mapping(target = "customerId", source = "customer.customerId")
    @Mapping(target = "customerName", source = "customer.fullName")
    @Mapping(target = "customerPhone", source = "customer.phone")
    @Mapping(target = "vehicleId", source = "vehicle.vehicleId")
    @Mapping(target = "licensePlate", source = "vehicle.licensePlate")
    @Mapping(target = "vehicleMake", source = "vehicle.brand")
    @Mapping(target = "vehicleModel", source = "vehicle.model")
    @Mapping(target = "bookingId", source = "booking.bookingId")
    @Mapping(target = "bookingCode", source = "booking.bookingCode")
    @Mapping(target = "scheduledDate", source = "booking.scheduledDate")
    @Mapping(target = "scheduledTime", source = "booking.scheduledTime")
    TechnicianTicketListResponse toTechnicianListResponse(ServiceTicket ticket,
                                                           CustomerProfile customer,
                                                           Vehicle vehicle,
                                                           Booking booking);
}
