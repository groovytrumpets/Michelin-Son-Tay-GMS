package com.g42.platform.gms.service_ticket_management.infrastructure.implement;

import com.g42.platform.gms.service_ticket_management.domain.entity.VehicleConditionPhoto;
import com.g42.platform.gms.service_ticket_management.domain.repository.VehicleConditionPhotoRepo;
import com.g42.platform.gms.service_ticket_management.infrastructure.mapper.VehicleConditionPhotoMapper;
import com.g42.platform.gms.service_ticket_management.infrastructure.repository.VehicleConditionPhotoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class VehicleConditionPhotoRepoImpl implements VehicleConditionPhotoRepo {

    private final VehicleConditionPhotoRepository jpaRepo;
    private final VehicleConditionPhotoMapper mapper;

    @Override
    public List<VehicleConditionPhoto> findByServiceTicketId(Integer serviceTicketId) {
        return jpaRepo.findByServiceTicketId(serviceTicketId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public VehicleConditionPhoto save(VehicleConditionPhoto photo) {
        return mapper.toDomain(jpaRepo.save(mapper.toJpa(photo)));
    }

    @Override
    public boolean existsByServiceTicketId(Integer serviceTicketId) {
        return !jpaRepo.findByServiceTicketId(serviceTicketId).isEmpty();
    }
}
