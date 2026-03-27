package com.g42.platform.gms.service_ticket_management.domain.repository;

import com.g42.platform.gms.service_ticket_management.domain.entity.VehicleConditionPhoto;

import java.util.List;

public interface VehicleConditionPhotoRepo {

    List<VehicleConditionPhoto> findByServiceTicketId(Integer serviceTicketId);

    VehicleConditionPhoto save(VehicleConditionPhoto photo);

    boolean existsByServiceTicketId(Integer serviceTicketId);
}
