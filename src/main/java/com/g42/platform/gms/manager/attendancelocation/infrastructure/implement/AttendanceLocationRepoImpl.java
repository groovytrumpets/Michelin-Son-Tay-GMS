package com.g42.platform.gms.manager.attendancelocation.infrastructure.implement;

import com.g42.platform.gms.manager.attendancelocation.domain.entity.AttendanceLocation;
import com.g42.platform.gms.manager.attendancelocation.domain.repository.AttendanceLocationRepo;
import com.g42.platform.gms.manager.attendancelocation.infrastructure.mapper.AttendanceLocationJpaMapper;
import com.g42.platform.gms.manager.attendancelocation.infrastructure.repository.AttendanceLocationJpaRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AttendanceLocationRepoImpl implements AttendanceLocationRepo {

    private final AttendanceLocationJpaRepo jpaRepo;
    private final AttendanceLocationJpaMapper mapper;

    @Override
    public List<AttendanceLocation> findAll() {
        return jpaRepo.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<AttendanceLocation> findById(Integer locationId) {
        return jpaRepo.findById(locationId).map(mapper::toDomain);
    }

    @Override
    public Optional<AttendanceLocation> findByQrToken(String qrToken) {
        return jpaRepo.findByQrToken(qrToken).map(mapper::toDomain);
    }

    @Override
    public boolean existsByQrToken(String qrToken) {
        return jpaRepo.existsByQrToken(qrToken);
    }

    @Override
    public AttendanceLocation save(AttendanceLocation location) {
        return mapper.toDomain(jpaRepo.save(mapper.toJpa(location)));
    }
}
