package com.g42.platform.gms.service_ticket_management.api.mapper;

import com.g42.platform.gms.service_ticket_management.api.dto.checkin.PhotoUploadResponse;
import com.g42.platform.gms.service_ticket_management.api.dto.checkin.ServiceTicketResponse;
import com.g42.platform.gms.service_ticket_management.domain.enums.PhotoCategory;
import com.g42.platform.gms.service_ticket_management.infrastructure.entity.VehicleConditionPhotoJpa;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * Mapper cho Photo responses trong quy trình Check-in.
 */
@Mapper(componentModel = "spring")
public interface PhotoResponseMapper {
    
    /**
     * Map VehicleConditionPhotoJpa sang PhotoUploadResponse.
     */
    @Mapping(target = "photoId", source = "photoId")
    @Mapping(target = "photoUrl", source = "photoUrl")
    @Mapping(target = "category", source = "category")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "uploadedAt", source = "uploadedAt")
    @Mapping(target = "message", constant = "Upload ảnh thành công")
    PhotoUploadResponse toUploadResponse(VehicleConditionPhotoJpa photo);
    
    /**
     * Map VehicleConditionPhotoJpa sang PhotoInfo (cho ServiceTicketResponse).
     */
    @Mapping(target = "photoId", source = "photoId")
    @Mapping(target = "category", expression = "java(photo.getCategory().name())")
    @Mapping(target = "photoUrl", source = "photoUrl")
    @Mapping(target = "description", source = "description")
    ServiceTicketResponse.PhotoInfo toPhotoInfo(VehicleConditionPhotoJpa photo);
    
    /**
     * Map list of VehicleConditionPhotoJpa sang list of PhotoInfo.
     */
    List<ServiceTicketResponse.PhotoInfo> toPhotoInfoList(List<VehicleConditionPhotoJpa> photos);
}
