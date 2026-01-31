package com.g42.platform.gms.auth.service;

import com.g42.platform.gms.auth.dto.StaffAuthDto;
import com.g42.platform.gms.auth.entity.Staffauth;
import com.g42.platform.gms.auth.mapper.StaffAuthMapper;
import com.g42.platform.gms.auth.repository.StaffAuthRepo;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class StaffAuthService {
    @Autowired
    private StaffAuthRepo staffAuthRepo;
    private final StaffAuthMapper staffAuthMapper; //Mapper giúp biến entity thành dto

    public Iterable<StaffAuthDto> getAllStaffAuth() {
        return staffAuthRepo.findAll().stream().map(staffAuthMapper::toDto).toList();
    }

    public ResponseEntity<StaffAuthDto> getStaffAuthById(int id) {
        var staffAuth= staffAuthRepo.findById(id).orElse(null);
        if(staffAuth == null){
            return ResponseEntity.notFound().build(); //Cài 404 not found
        }
        return ResponseEntity.ok(staffAuthMapper.toDto(staffAuth)); //Mapper giúp biến entity thành dto
    }

    public StaffAuthDto AuthenticateStaff(String email, String password){
        var staffAuth = staffAuthRepo.searchByEmail(email);
        if(staffAuth == null){
            return staffAuthMapper.toDto(null);
        }
        StaffAuthDto staffAuthDto = staffAuthMapper.toDto((Staffauth) staffAuth);
        if (staffAuthDto.getPasswordHash().equals(password)) {
            return staffAuthDto;
        }
        return null;
    }


}
