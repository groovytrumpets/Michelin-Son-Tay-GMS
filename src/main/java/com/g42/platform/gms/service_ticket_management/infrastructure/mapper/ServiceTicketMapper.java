package com.g42.platform.gms.service_ticket_management.infrastructure.mapper;

import com.g42.platform.gms.service_ticket_management.domain.entity.ServiceTicket;
import com.g42.platform.gms.service_ticket_management.infrastructure.entity.ServiceTicketJpa;
import com.g42.platform.gms.service_ticket_management.infrastructure.entity.VehicleConditionPhotoJpa;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring")
public interface ServiceTicketMapper {

    @Mappings({
            @Mapping(target = "photoIds", expression = "java(mapPhotoIds(jpa.getConditionPhotos()))")
    })
    ServiceTicket toDomain(ServiceTicketJpa jpa);

    @Mappings({
            @Mapping(target = "conditionPhotos", ignore = true)
    })
    ServiceTicketJpa toJpa(ServiceTicket domain);

    // Helper method for MapStruct
    default List<Integer> mapPhotoIds(List<VehicleConditionPhotoJpa> photos) {
        if (photos == null) {
            return null;
        }
        
        List<Integer> photoIds = new ArrayList<>();
        for (VehicleConditionPhotoJpa photo : photos) {
            Integer photoId = photo.getPhotoId();
            if (photoId != null) {
                photoIds.add(photoId);
            }
        }
        
        return photoIds;
    }
}
