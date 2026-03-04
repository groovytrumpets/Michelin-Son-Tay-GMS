package com.g42.platform.gms.booking.customer.api.mapper;

import com.g42.platform.gms.booking.customer.api.dto.BookingResponse;
import com.g42.platform.gms.booking.customer.api.dto.ServiceItemDto;
import com.g42.platform.gms.booking.customer.domain.entity.Booking;
import com.g42.platform.gms.booking.customer.infrastructure.entity.CatalogItemJpaEntity;
import com.g42.platform.gms.catalog.repository.CatalogItemRepository;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.AfterMapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring")
public abstract class BookingDtoMapper {

    @Autowired
    protected CatalogItemRepository catalogItemRepository;

    @Mapping(target = "customerName", ignore = true)
    @Mapping(target = "phone", ignore = true)
    @Mapping(target = "services", ignore = true)
    @Mapping(target = "totalEstimatedTime", ignore = true)
    public abstract BookingResponse toResponse(Booking domain);
    
    @AfterMapping
    protected void populateServices(@MappingTarget BookingResponse response, Booking domain) {
        List<Integer> serviceIds = domain.getServiceIds();
        
        if (serviceIds == null || serviceIds.isEmpty()) {
            response.setServices(new ArrayList<>());
            response.setTotalEstimatedTime(0);
            return;
        }
        
        List<CatalogItemJpaEntity> items = catalogItemRepository.findAllById(serviceIds);
        
        List<ServiceItemDto> serviceDtos = new ArrayList<>();
        int totalTime = 0;
        
        for (CatalogItemJpaEntity item : items) {
            ServiceItemDto dto = mapToServiceDto(item);
            serviceDtos.add(dto);
            
            if (dto.getEstimateTime() != null && dto.getEstimateTime() > 0) {
                totalTime = totalTime + dto.getEstimateTime();
            }
        }
        
        response.setServices(serviceDtos);
        response.setTotalEstimatedTime(totalTime);
    }
    
    private ServiceItemDto mapToServiceDto(CatalogItemJpaEntity item) {
        ServiceItemDto dto = new ServiceItemDto();
        dto.setItemId(item.getItemId());
        dto.setItemName(item.getItemName());
        dto.setItemType(item.getItemType());
        dto.setEstimatedPrice(item.getEstimatedPrice());
        
        if (item.getServiceService() != null) {
            dto.setEstimateTime(item.getServiceService().getEstimateTime());
        }
        
        return dto;
    }
}

