package com.g42.platform.gms.service_ticket_management.infrastructure.mapper;

import com.g42.platform.gms.service_ticket_management.domain.entity.SafetyInspection;
import com.g42.platform.gms.service_ticket_management.domain.entity.SafetyInspectionItem;
import com.g42.platform.gms.service_ticket_management.domain.entity.SafetyInspectionTire;
import com.g42.platform.gms.service_ticket_management.infrastructure.entity.SafetyInspectionJpa;
import com.g42.platform.gms.service_ticket_management.infrastructure.entity.SafetyInspectionItemJpa;
import com.g42.platform.gms.service_ticket_management.infrastructure.entity.SafetyInspectionTireJpa;
import com.g42.platform.gms.service_ticket_management.infrastructure.projection.SafetyInspectionItemWithCategory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface SafetyInspectionInfraMapper {

    // Main entity mappings
    @Mappings({
            @Mapping(target = "tires", ignore = true),
            @Mapping(target = "items", ignore = true)
    })
    SafetyInspection toDomain(SafetyInspectionJpa jpa);

    @Mappings({
            @Mapping(target = "tires", ignore = true),
            @Mapping(target = "items", ignore = true)
    })
    SafetyInspectionJpa toJpa(SafetyInspection domain);

    // Tire entity mappings
    SafetyInspectionTire toDomain(SafetyInspectionTireJpa jpa);
    SafetyInspectionTireJpa toJpa(SafetyInspectionTire domain);
    List<SafetyInspectionTire> tiresToDomain(List<SafetyInspectionTireJpa> jpaList);
    List<SafetyInspectionTireJpa> tiresToJpa(List<SafetyInspectionTire> domainList);

    // Item entity mappings
    @Mapping(target = "categoryName", ignore = true)  // Will be populated from JOIN query or service layer
    SafetyInspectionItem toDomain(SafetyInspectionItemJpa jpa);
    
    // When mapping domain -> JPA, categoryName will be ignored automatically (not in JPA entity)
    SafetyInspectionItemJpa toJpa(SafetyInspectionItem domain);
    
    // List mappings will use the single-item methods above
    List<SafetyInspectionItem> itemsToDomain(List<SafetyInspectionItemJpa> jpaList);
    List<SafetyInspectionItemJpa> itemsToJpa(List<SafetyInspectionItem> domainList);
    
    // Projection mapping - converts projection with categoryName to domain
    SafetyInspectionItem projectionToDomain(SafetyInspectionItemWithCategory projection);
    List<SafetyInspectionItem> projectionsToItems(List<SafetyInspectionItemWithCategory> projections);
}