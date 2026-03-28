package com.g42.platform.gms.service_ticket_management.api.mapper;

import com.g42.platform.gms.service_ticket_management.api.dto.checkin.ServiceTicketResponse;
import com.g42.platform.gms.service_ticket_management.api.dto.manage.ServiceQueueResponse;
import com.g42.platform.gms.service_ticket_management.api.dto.manage.ServiceTicketListResponse;
import com.g42.platform.gms.service_ticket_management.domain.entity.ServiceTicket;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for converting ServiceTicket domain entity to ServiceTicketResponse DTO.
 * Follows the same pattern as BookingDtoMapper (Domain → DTO).
 */
@Mapper(componentModel = "spring")
public interface ServiceTicketDtoMapper {

    @Mapping(target = "photos", ignore = true)
    @Mapping(target = "warnings", ignore = true)
    ServiceTicketResponse toResponse(ServiceTicket domain);

    ServiceTicketListResponse toDto(ServiceTicket savedServiceTicket);

    ServiceQueueResponse toQueueDto(ServiceTicket serviceTicket);
}
