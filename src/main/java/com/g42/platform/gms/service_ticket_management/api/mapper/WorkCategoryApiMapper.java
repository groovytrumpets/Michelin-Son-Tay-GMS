package com.g42.platform.gms.service_ticket_management.api.mapper;

import com.g42.platform.gms.service_ticket_management.api.dto.safety.WorkCategoryResponse;
import com.g42.platform.gms.service_ticket_management.domain.entity.WorkCategory;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface WorkCategoryApiMapper {
    
    WorkCategoryResponse toResponse(WorkCategory domain);
    
    List<WorkCategoryResponse> toResponseList(List<WorkCategory> domainList);
}