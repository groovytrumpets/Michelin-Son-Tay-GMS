package com.g42.platform.gms.attendancerequest.infrastructure.implement;

import com.g42.platform.gms.attendancerequest.domain.entity.AttendanceRequest;
import com.g42.platform.gms.attendancerequest.domain.repository.AttendanceRequestRepo;
import com.g42.platform.gms.attendancerequest.infrastructure.mapper.AttendanceRequestJpaMapper;
import com.g42.platform.gms.attendancerequest.infrastructure.repository.AttendanceRequestJpaRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AttendanceRequestRepoImpl implements AttendanceRequestRepo {

    private final AttendanceRequestJpaRepo jpaRepo;
    private final AttendanceRequestJpaMapper mapper;

    @Override
    public List<AttendanceRequest> findByStaffId(Integer staffId) {
        return jpaRepo.findByStaffIdOrderByCreatedAtDesc(staffId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<AttendanceRequest> findByFilters(String status, String requestType) {
        boolean hasStatus = StringUtils.hasText(status);
        boolean hasType = StringUtils.hasText(requestType);

        if (hasStatus && hasType) {
            return jpaRepo.findByStatusAndRequestTypeOrderByCreatedAtDesc(status, requestType).stream().map(mapper::toDomain).toList();
        }
        if (hasStatus) {
            return jpaRepo.findByStatusOrderByCreatedAtDesc(status).stream().map(mapper::toDomain).toList();
        }
        if (hasType) {
            return jpaRepo.findByRequestTypeOrderByCreatedAtDesc(requestType).stream().map(mapper::toDomain).toList();
        }
        return jpaRepo.findAllByOrderByCreatedAtDesc().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<AttendanceRequest> findById(Integer requestId) {
        return jpaRepo.findById(requestId).map(mapper::toDomain);
    }

    @Override
    public AttendanceRequest save(AttendanceRequest request) {
        return mapper.toDomain(jpaRepo.save(mapper.toJpa(request)));
    }

    @Override
    public void deleteById(Integer requestId) {
        jpaRepo.deleteById(requestId);
    }
}
