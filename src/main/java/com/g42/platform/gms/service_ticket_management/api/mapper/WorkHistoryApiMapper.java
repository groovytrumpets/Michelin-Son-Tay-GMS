package com.g42.platform.gms.service_ticket_management.api.mapper;

import com.g42.platform.gms.auth.entity.CustomerProfile;
import com.g42.platform.gms.service_ticket_management.api.dto.work_history.WorkHistoryResponse;
import com.g42.platform.gms.service_ticket_management.domain.entity.ServiceTicket;
import com.g42.platform.gms.vehicle.domain.entity.Vehicle;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WorkHistoryApiMapper {

    @Mapping(target = "serviceTicketId", source = "ticket.serviceTicketId")
    @Mapping(target = "ticketCode", source = "ticket.ticketCode")
    @Mapping(target = "completedDate", expression = "java(ticket.getCompletedAt() != null ? ticket.getCompletedAt().toLocalDate() : null)")
    @Mapping(target = "licensePlate", source = "vehicle.licensePlate")
    @Mapping(target = "vehicleBrand", source = "vehicle.brand")
    @Mapping(target = "vehicleModel", source = "vehicle.model")
    @Mapping(target = "vehicleYear", source = "vehicle.manufactureYear")
    @Mapping(target = "customerName", source = "customer.fullName")
    @Mapping(target = "customerPhone", source = "customer.phone")
    @Mapping(target = "serviceType", source = "serviceType")
    @Mapping(target = "customerRequest", source = "ticket.customerRequest")
    @Mapping(target = "technicianNotes", source = "ticket.technicianNotes")
    WorkHistoryResponse toWorkHistoryResponse(
        ServiceTicket ticket,
        Vehicle vehicle,
        CustomerProfile customer,
        String serviceType
    );
}
