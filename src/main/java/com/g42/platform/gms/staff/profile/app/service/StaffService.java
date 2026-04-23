package com.g42.platform.gms.staff.profile.app.service;

import com.g42.platform.gms.hikvision.client.HikvisionUserClient;
import com.g42.platform.gms.staff.profile.api.dto.RoleDto;
import com.g42.platform.gms.staff.profile.api.dto.StaffCreateDto;
import com.g42.platform.gms.staff.profile.api.dto.StaffProfileDto;
import com.g42.platform.gms.staff.profile.api.dto.StaffUpdateDto;
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
    @Autowired
    HikvisionUserClient hikvisionUserClient;


    public Page<StaffProfileDto> getListOfAllStaffProfile(int page, int size, Boolean isActive, String search, List<Integer> roleIds, String status) {
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

    public List<RoleDto> getListOfRoles() {
        return staffRepo.getAllRoles().stream().map(staffProfileDtoMapper::toDto).toList();
    }

    public StaffProfileDto createStaff(StaffCreateDto staffCreateDto) {
        StaffProfile staffProfile = staffRepo.createStaff(staffCreateDto);
        StaffProfileDto result = staffProfileDtoMapper.toStaffProfileDto(staffProfile);

        // Sau khi tạo nhân viên trong GMS, tạo luôn trên Hikvision (employeeNo = staffId, auto-generated)
        if (result.getEmployeeNo() != null && !result.getEmployeeNo().isBlank()) {
            hikvisionUserClient.createPerson(result.getEmployeeNo(), result.getFullName());
        }

        return result;
    }

    public StaffProfileDto updateStaff(Integer staffId, StaffUpdateDto staffProfileDto) {
        StaffProfile staffProfile = staffRepo.updateStaff(staffId, staffProfileDto);
        StaffProfileDto result = staffProfileDtoMapper.toStaffProfileDto(staffProfile);

        // Sync tên mới lên Hikvision — employeeNo = staffId (auto-generated)
        if (staffProfileDto.getFullName() != null) {
            String employeeNo = String.valueOf(staffId);
            hikvisionUserClient.updatePerson(employeeNo, staffProfileDto.getFullName());
        }

        return result;
    }

    public StaffProfileDto deleteStaff(Integer staffId) {
        StaffProfile staffProfile = staffRepo.deleteStaff(staffId);
        StaffProfileDto result = staffProfileDtoMapper.toStaffProfileDto(staffProfile);

        // Xóa person trên Hikvision — employeeNo = staffId
        hikvisionUserClient.deletePerson(String.valueOf(staffId));

        return result;
    }

    public StaffProfileDto lockStaff(Integer staffId) {
        StaffProfile staffProfile = staffRepo.lockStaff(staffId);
        return staffProfileDtoMapper.toStaffProfileDto(staffProfile);
    }
}
