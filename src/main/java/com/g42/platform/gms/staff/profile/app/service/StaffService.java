package com.g42.platform.gms.staff.profile.app.service;

import com.g42.platform.gms.staff.profile.api.dto.StaffProfileDto;
import com.g42.platform.gms.staff.profile.api.mapper.StaffAuthDtoMapper;
import com.g42.platform.gms.staff.profile.api.mapper.StaffProfileDtoMapper;
import com.g42.platform.gms.staff.profile.domain.entity.StaffProfile;
import com.g42.platform.gms.staff.profile.domain.repository.StaffRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class StaffService {
    @Autowired
    StaffRepo staffRepo;
    @Autowired
    StaffAuthDtoMapper staffAuthDtoMapper;
    @Autowired
    StaffProfileDtoMapper staffProfileDtoMapper;


    public Page<StaffProfileDto> getListOfAllStaffProfile(int page, int size, Boolean isActive, String search, List<Integer> roleIds) {
        String status = isActive == null ? null : (isActive ? "ACTIVE" : "INACTIVE");
        Pageable pageable = PageRequest.of(page, size);
        Page<StaffProfile> result = staffRepo.findAllWithFilter(
                search, status, roleIds, pageable
        );
        return result.map(staffProfileDtoMapper::toStaffProfileDto);
    }

    public StaffProfileDto getStaffProfileById(Integer staffId) {
        StaffProfile staffProfile = staffRepo.findById(staffId);
        return staffProfileDtoMapper.toStaffProfileDto(staffProfile);
    }
}
