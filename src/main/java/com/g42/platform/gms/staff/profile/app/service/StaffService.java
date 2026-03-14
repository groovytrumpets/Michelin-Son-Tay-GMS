package com.g42.platform.gms.staff.profile.app.service;

import com.g42.platform.gms.staff.profile.api.mapper.StaffAuthDtoMapper;
import com.g42.platform.gms.staff.profile.api.mapper.StaffProfileDtoMapper;
import com.g42.platform.gms.staff.profile.domain.repository.StaffRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StaffService {
    @Autowired
    StaffRepo staffRepo;
    @Autowired
    StaffAuthDtoMapper staffAuthDtoMapper;
    @Autowired
    StaffProfileDtoMapper staffProfileDtoMapper;


}
