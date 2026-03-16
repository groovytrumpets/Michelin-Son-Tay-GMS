package com.g42.platform.gms.staff.profile.infrastructure;

import com.g42.platform.gms.staff.profile.domain.entity.StaffProfile;
import com.g42.platform.gms.staff.profile.domain.repository.StaffRepo;
import com.g42.platform.gms.staff.profile.infrastructure.entity.StaffProfileJpa;
import com.g42.platform.gms.staff.profile.infrastructure.mapper.StaffProfileJpaMapper;
import com.g42.platform.gms.staff.profile.infrastructure.repository.StaffAuthJpaRepo;
import com.g42.platform.gms.staff.profile.infrastructure.repository.StaffProileJpaRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
@RequiredArgsConstructor
public class StaffRepoImpl implements StaffRepo {
    private final StaffProileJpaRepo staffProfileJpaRepo;
    private final StaffAuthJpaRepo staffAuthJpaRepo;
    private final StaffProfileJpaMapper staffProfileJpaMapper;
    @Override
    public Page<StaffProfile> findAllWithFilter(String search, String status, List<Integer> roleIds, Pageable pageable) {
        Page<StaffProfileJpa> result = staffProfileJpaRepo.findAllWithFilter(
                search, status, roleIds, pageable);
        return result.map(staffProfileJpaMapper::toDomain);
    }
}
