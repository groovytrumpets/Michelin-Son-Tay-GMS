package com.g42.platform.gms.service_ticket_management.api.mapper;

import com.g42.platform.gms.auth.entity.CustomerProfile;
import com.g42.platform.gms.auth.entity.StaffProfile;
import com.g42.platform.gms.booking.customer.domain.entity.Booking;
import com.g42.platform.gms.booking.customer.infrastructure.entity.CatalogItemJpaEntity;
import com.g42.platform.gms.service_ticket_management.api.dto.manage.ServiceTicketDetailResponse;
import com.g42.platform.gms.service_ticket_management.api.dto.technician.TechnicianTicketDetailResponse;
import com.g42.platform.gms.service_ticket_management.domain.entity.VehicleConditionPhoto;
import com.g42.platform.gms.vehicle.domain.entity.Vehicle;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * Mapper cho Service Ticket Detail Response (shared cho cả Manage và Technician view).
 */
@Mapper(componentModel = "spring")
public interface ServiceTicketDetailMapper {
    
    // === MANAGE VIEW (RECEPTIONIST) ===
    
    /**
     * Map CustomerProfile to CustomerInfo (Manage view).
     */
    @Mapping(target = "customerId", source = "customerId")
    @Mapping(target = "fullName", source = "fullName")
    @Mapping(target = "phone", source = "phone")
    @Mapping(target = "email", source = "email")
    ServiceTicketDetailResponse.CustomerInfo toManageCustomerInfo(CustomerProfile customer);
    
    /**
     * Map Vehicle to VehicleInfo (Manage view).
     */
    @Mapping(target = "vehicleId", source = "vehicleId")
    @Mapping(target = "licensePlate", source = "licensePlate")
    @Mapping(target = "make", source = "brand")
    @Mapping(target = "model", source = "model")
    @Mapping(target = "year", source = "manufactureYear")
    ServiceTicketDetailResponse.VehicleInfo toManageVehicleInfo(Vehicle vehicle);
    
    /**
     * Map Booking to BookingInfo (Manage view).
     */
    @Mapping(target = "bookingId", source = "bookingId")
    @Mapping(target = "bookingCode", source = "bookingCode")
    @Mapping(target = "scheduledDate", source = "scheduledDate")
    @Mapping(target = "scheduledTime", source = "scheduledTime")
    ServiceTicketDetailResponse.BookingInfo toManageBookingInfo(Booking booking);
    
    /**
     * Map CatalogItem to ServiceInfo (Manage view).
     */
    @Mapping(target = "serviceId", source = "itemId")
    @Mapping(target = "serviceName", source = "itemName")
    @Mapping(target = "category", source = "itemType")
    ServiceTicketDetailResponse.ServiceInfo toManageServiceInfo(CatalogItemJpaEntity catalogItem);
    
    /**
     * Map VehicleConditionPhoto to PhotoInfo (Manage view).
     */
    @Mapping(target = "photoId", source = "photoId")
    @Mapping(target = "category", expression = "java(photo.getCategory().name())")
    @Mapping(target = "photoUrl", source = "photoUrl")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "uploadedAt", source = "uploadedAt")
    ServiceTicketDetailResponse.PhotoInfo toManagePhotoInfo(VehicleConditionPhoto photo);
    
    /**
     * Map list of photos (Manage view).
     */
    List<ServiceTicketDetailResponse.PhotoInfo> toManagePhotoInfoList(List<VehicleConditionPhoto> photos);
    
    /**
     * Map list of services (Manage view).
     */
    List<ServiceTicketDetailResponse.ServiceInfo> toManageServiceInfoList(List<CatalogItemJpaEntity> catalogItems);
    
    // === TECHNICIAN VIEW ===
    
    /**
     * Map CustomerProfile to CustomerInfo (Technician view).
     */
    @Mapping(target = "customerId", source = "customerId")
    @Mapping(target = "fullName", source = "fullName")
    @Mapping(target = "phone", source = "phone")
    @Mapping(target = "email", source = "email")
    TechnicianTicketDetailResponse.CustomerInfo toTechnicianCustomerInfo(CustomerProfile customer);
    
    /**
     * Map Vehicle to VehicleInfo (Technician view).
     */
    @Mapping(target = "vehicleId", source = "vehicleId")
    @Mapping(target = "licensePlate", source = "licensePlate")
    @Mapping(target = "make", source = "brand")
    @Mapping(target = "model", source = "model")
    @Mapping(target = "year", source = "manufactureYear")
    TechnicianTicketDetailResponse.VehicleInfo toTechnicianVehicleInfo(Vehicle vehicle);
    
    /**
     * Map Booking to BookingInfo (Technician view).
     */
    @Mapping(target = "bookingId", source = "bookingId")
    @Mapping(target = "bookingCode", source = "bookingCode")
    @Mapping(target = "scheduledDate", source = "scheduledDate")
    @Mapping(target = "scheduledTime", source = "scheduledTime")
    TechnicianTicketDetailResponse.BookingInfo toTechnicianBookingInfo(Booking booking);
    
    /**
     * Map CatalogItem to ServiceInfo (Technician view).
     */
    @Mapping(target = "serviceId", source = "itemId")
    @Mapping(target = "serviceName", source = "itemName")
    @Mapping(target = "category", source = "itemType")
    TechnicianTicketDetailResponse.ServiceInfo toTechnicianServiceInfo(CatalogItemJpaEntity catalogItem);
    
    /**
     * Map VehicleConditionPhoto to PhotoInfo (Technician view).
     */
    @Mapping(target = "photoId", source = "photoId")
    @Mapping(target = "category", expression = "java(photo.getCategory().name())")
    @Mapping(target = "photoUrl", source = "photoUrl")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "uploadedAt", source = "uploadedAt")
    TechnicianTicketDetailResponse.PhotoInfo toTechnicianPhotoInfo(VehicleConditionPhoto photo);
    
    /**
     * Map list of photos (Technician view).
     */
    List<TechnicianTicketDetailResponse.PhotoInfo> toTechnicianPhotoInfoList(List<VehicleConditionPhoto> photos);
    
    /**
     * Map list of services (Technician view).
     */
    List<TechnicianTicketDetailResponse.ServiceInfo> toTechnicianServiceInfoList(List<CatalogItemJpaEntity> catalogItems);
}
