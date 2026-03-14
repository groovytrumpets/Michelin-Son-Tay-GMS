package com.g42.platform.gms.service_ticket_management.api.mapper;

import com.g42.platform.gms.service_ticket_management.api.dto.safety.*;
import com.g42.platform.gms.service_ticket_management.domain.entity.SafetyInspection;
import com.g42.platform.gms.service_ticket_management.domain.entity.SafetyInspectionItem;
import com.g42.platform.gms.service_ticket_management.domain.entity.SafetyInspectionTire;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface SafetyInspectionMapper {

    // Request mappings (DTO -> Domain)
    @Mappings({
            @Mapping(target = "inspectionId", ignore = true),
            @Mapping(target = "technicianId", ignore = true),
            @Mapping(target = "inspectionStatus", ignore = true),
            @Mapping(target = "createdAt", ignore = true),
            @Mapping(target = "updatedAt", ignore = true)
    })
    SafetyInspection toDomain(SafetyInspectionRequest request);

    @Mappings({
            @Mapping(target = "tireId", ignore = true),
            @Mapping(target = "inspectionId", ignore = true)
    })
    SafetyInspectionTire toDomain(TireDataRequest request);

    @Mappings({
            @Mapping(target = "itemId", ignore = true),
            @Mapping(target = "inspectionId", ignore = true),
            @Mapping(target = "categoryName", ignore = true)  // Not in request, will be populated from database
    })
    SafetyInspectionItem toDomain(InspectionItemRequest request);

    // Response mappings (Domain -> DTO)
    SafetyInspectionResponse toResponse(SafetyInspection domain);

    TireDataResponse toResponse(SafetyInspectionTire domain);
    
    // Map domain to response - categoryName is already populated in domain
    InspectionItemResponse toResponse(SafetyInspectionItem domain);

    // List mappings
    List<SafetyInspectionTire> tiresToDomain(List<TireDataRequest> requests);
    List<SafetyInspectionItem> itemsToDomain(List<InspectionItemRequest> requests);
    List<TireDataResponse> tiresToResponse(List<SafetyInspectionTire> domain);
    List<InspectionItemResponse> itemsToResponse(List<SafetyInspectionItem> domain);
}