package com.g42.platform.gms.staff.profile.infrastructure;

import com.g42.platform.gms.staff.profile.api.dto.RoleDto;
import com.g42.platform.gms.staff.profile.api.dto.StaffCreateDto;
import com.g42.platform.gms.staff.profile.api.dto.StaffProfileDto;
import com.g42.platform.gms.staff.profile.api.dto.StaffUpdateDto;
import com.g42.platform.gms.staff.profile.domain.entity.Role;
import com.g42.platform.gms.staff.profile.domain.entity.StaffAuth;
import com.g42.platform.gms.staff.profile.domain.entity.StaffProfile;
import com.g42.platform.gms.staff.profile.domain.exception.StaffErrorCode;
import com.g42.platform.gms.staff.profile.domain.exception.StaffException;
import com.g42.platform.gms.staff.profile.domain.repository.StaffRepo;
import com.g42.platform.gms.staff.profile.infrastructure.entity.RoleJpa;
import com.g42.platform.gms.staff.profile.infrastructure.entity.StaffAuthJpa;
import com.g42.platform.gms.staff.profile.infrastructure.entity.StaffProfileJpa;
import com.g42.platform.gms.staff.profile.infrastructure.mapper.StaffProfileJpaMapper;
import com.g42.platform.gms.staff.profile.infrastructure.repository.RoleJpaRepo;
import com.g42.platform.gms.staff.profile.infrastructure.repository.StaffAuthJpaRepo;
import com.g42.platform.gms.staff.profile.infrastructure.repository.StaffProileJpaRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
@Repository
@RequiredArgsConstructor
public class StaffRepoImpl implements StaffRepo {
    private final StaffProileJpaRepo staffProfileJpaRepo;
    private final StaffAuthJpaRepo staffAuthJpaRepo;
    private final StaffProfileJpaMapper staffProfileJpaMapper;
    private final RoleJpaRepo roleJpaRepo;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Override
    public Page<StaffProfile> findAllWithFilter(String search, String status, List<Integer> roleIds, Pageable pageable) {
        Page<StaffProfileJpa> result = staffProfileJpaRepo.findAllWithFilter(
                search, status, roleIds, pageable);
        return result.map(staffProfileJpaMapper::toDomain);
    }

    @Override
    public StaffProfile findById(Integer staffId) {
        StaffProfileJpa staffProfileJpa = staffProfileJpaRepo.findByStaffId(staffId);
        return  staffProfileJpaMapper.toDomain(staffProfileJpa);
    }

    @Override
    public List<Role> getAllRoles() {
        List<RoleJpa> roleJpas = roleJpaRepo.findAll();
        return roleJpas.stream().map(staffProfileJpaMapper::toRoleDomain).toList();
    }

    @Override
    @Transactional
    public StaffProfile createStaff(StaffCreateDto staffCreateDto) {
        //todo: check exits phone and email
        if (staffAuthJpaRepo.existsByEmail(staffCreateDto.getEmail())) {
            throw new StaffException("Email đã tồn tại!", StaffErrorCode.DUPLICATE_EMAIL);
        }
        if (staffProfileJpaRepo.existsByPhone(staffCreateDto.getPhone())) {
            throw new StaffException("Phone đã tồn tại!", StaffErrorCode.DUPLICATE_PHONE);
        }
        if (staffCreateDto.getGoogleId() != null && staffAuthJpaRepo.existsByGoogleId(staffCreateDto.getGoogleId())) {
            throw new StaffException("Google ID đã tồn tại!", StaffErrorCode.DUPLICATE_EMAIL);
        }
        //todo: create new staff profile, get id
        StaffProfileJpa staffProfileJpa = new StaffProfileJpa();
        staffProfileJpa.setFullName(staffCreateDto.getFullName());
        staffProfileJpa.setPhone(staffCreateDto.getPhone());
        staffProfileJpa.setPosition(staffCreateDto.getPosition());
        staffProfileJpa.setAvatar(staffCreateDto.getAvatar());
        staffProfileJpa.setDob(staffCreateDto.getDob());
        //todo: assign roles for new staff
        if (staffCreateDto.getRoles() != null && !staffCreateDto.getRoles().isEmpty()) {
            List<Integer> roleIds = staffCreateDto.getRoles().stream().map(RoleDto::getRoleId).toList();
            List<RoleJpa> roles = roleJpaRepo.findAllById(roleIds);
            staffProfileJpa.setRoles(roles);

        }
        StaffProfileJpa savedProfile = staffProfileJpaRepo.save(staffProfileJpa);
        //todo: create new staff auth by staffProfileId
        StaffAuthJpa staffAuthJpa = new StaffAuthJpa();
        staffAuthJpa.setStaffProfile(savedProfile);
        staffAuthJpa.setPasswordHash(passwordEncoder.encode(staffCreateDto.getPassword()));
        staffAuthJpa.setEmail(staffCreateDto.getEmail());
        staffAuthJpa.setStatus(staffCreateDto.getStatus());
        staffAuthJpa.setGoogleId(staffCreateDto.getGoogleId());
        staffAuthJpa.setAuthProvider(staffCreateDto.getAuthProvider());
        StaffAuthJpa staffAuthSaved = staffAuthJpaRepo.save(staffAuthJpa);
        return  staffProfileJpaMapper.toDomain(savedProfile);
    }

    @Override
    public StaffProfile updateStaff(Integer staffId, StaffUpdateDto dto) {
        StaffProfileJpa profile = staffProfileJpaRepo.findByStaffId(staffId);
        if (profile == null) {throw new StaffException("StaffId không đúng", StaffErrorCode.INVALID_STAFF_ID);}
        if (dto.getFullName() != null) profile.setFullName(dto.getFullName());
        if (dto.getPhone() != null) profile.setPhone(dto.getPhone());
        if (dto.getPosition() != null) profile.setPosition(dto.getPosition());
        if (dto.getAvatar() != null) profile.setAvatar(dto.getAvatar());
        if (dto.getDob() != null) profile.setDob(dto.getDob());

        if (dto.getRoles() != null && !dto.getRoles().isEmpty()) {
            List<Integer> roleIds = dto.getRoles().stream().map(RoleDto::getRoleId).toList();
            List<RoleJpa> roles = roleJpaRepo.findAllById(roleIds);
            profile.setRoles(roles);
        }

        if (dto.getStatus() != null) {
            StaffAuthJpa auth = staffAuthJpaRepo.findByStaffAuthId(staffId);
            if (auth != null) {
                auth.setStatus(dto.getStatus());
                staffAuthJpaRepo.save(auth);
            }
        }
        return staffProfileJpaMapper.toDomain(staffProfileJpaRepo.save(profile));
    }
}
