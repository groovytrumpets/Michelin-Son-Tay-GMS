package com.g42.platform.gms.service_ticket_management.api.mapper;

import com.g42.platform.gms.service_ticket_management.api.dto.checkin.PhotoUploadResponse;
import com.g42.platform.gms.service_ticket_management.api.dto.checkin.ServiceTicketResponse;
import com.g42.platform.gms.service_ticket_management.domain.entity.VehicleConditionPhoto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PhotoResponseMapper {

    @Mapping(target = "message", constant = "Upload ảnh thành công")
    PhotoUploadResponse toUploadResponse(VehicleConditionPhoto photo);

    @Mapping(target = "category", expression = "java(photo.getCategory().name())")
    ServiceTicketResponse.PhotoInfo toPhotoInfo(VehicleConditionPhoto photo);

    List<ServiceTicketResponse.PhotoInfo> toPhotoInfoList(List<VehicleConditionPhoto> photos);
}
