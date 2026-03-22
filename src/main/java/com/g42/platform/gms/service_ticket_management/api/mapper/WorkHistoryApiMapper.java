package com.g42.platform.gms.service_ticket_management.api.mapper;

import com.g42.platform.gms.auth.entity.CustomerProfile;
import com.g42.platform.gms.service_ticket_management.api.dto.work_history.WorkHistoryResponse;
import com.g42.platform.gms.service_ticket_management.infrastructure.entity.ServiceTicketJpa;
import com.g42.platform.gms.vehicle.entity.Vehicle;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for converting entities to WorkHistoryResponse DTO.
 * Maps from ServiceTicketJpa + Vehicle + CustomerProfile to WorkHistoryResponse.
 */
@Mapper(componentModel = "spring")
public interface WorkHistoryApiMapper {
    
    /**
     * Map from ServiceTicketJpa, Vehicle, and CustomerProfile to WorkHistoryResponse.
     * 
     * @param ticket The service ticket entity
     * @param vehicle The vehicle entity
     * @param customer The customer profile entity
     * @param serviceType The resolved service type string
     * @return WorkHistoryResponse DTO
     */
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
        ServiceTicketJpa ticket,
        Vehicle vehicle,
        CustomerProfile customer,
        String serviceType
    );
}
