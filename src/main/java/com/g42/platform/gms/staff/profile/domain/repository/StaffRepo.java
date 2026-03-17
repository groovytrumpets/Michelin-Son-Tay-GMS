package com.g42.platform.gms.staff.profile.domain.repository;

import com.g42.platform.gms.staff.profile.domain.entity.StaffProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StaffRepo {
    Page<StaffProfile> findAllWithFilter(String search, String status, List<Integer> roleIds, Pageable pageable);

    StaffProfile findById(Integer staffId);
}
