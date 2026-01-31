package com.g42.platform.gms.auth.service;

import com.g42.platform.gms.auth.dto.LoginRequest;
import com.g42.platform.gms.auth.dto.StaffAuthDto;
import com.g42.platform.gms.auth.entity.Staffauth;
import com.g42.platform.gms.auth.mapper.StaffAuthMapper;
import com.g42.platform.gms.auth.repository.StaffAuthRepo;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class StaffAuthService {
    @Autowired
    private StaffAuthRepo staffAuthRepo;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private JWTService jwtService;
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

    public String verifyStaffAuth(LoginRequest loginRequest){
        try{
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getPhone(),
                        loginRequest.getPin()
                ));
        if (authentication.isAuthenticated()) {
            return jwtService.generateJWToken(loginRequest.getPhone());
        }
        }catch (BadCredentialsException e){
            System.out.println(e.getMessage());
        return  "LOGIN FAILED ";
        }
        return "LOGIN FAILED ";
    }


}
