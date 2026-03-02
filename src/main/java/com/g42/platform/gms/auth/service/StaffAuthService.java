package com.g42.platform.gms.auth.service;

import com.g42.platform.gms.auth.constant.AuthErrorCode;
import com.g42.platform.gms.auth.dto.AuthResponse;
import com.g42.platform.gms.auth.dto.LoginRequest;
import com.g42.platform.gms.auth.dto.StaffAuthDto;
import com.g42.platform.gms.auth.dto.StaffAuthResponse;
import com.g42.platform.gms.auth.entity.*;
import com.g42.platform.gms.auth.exception.AuthException;
import com.g42.platform.gms.auth.mapper.StaffAuthMapper;
import com.g42.platform.gms.auth.repository.StaffAuthRepo;
import com.g42.platform.gms.auth.repository.StaffProfileRepo;
import com.g42.platform.gms.auth.repository.StaffRoleRepository;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
public class StaffAuthService {
    @Autowired
    private StaffAuthRepo staffAuthRepo;
    @Autowired
    private StaffProfileRepo staffProfileRepo;
    private StaffRoleRepository staffRoleRepo;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private JWTService jwtService;
    private final StaffAuthMapper staffAuthMapper; //Mapper giúp biến entity thành dto
    private static final int MAX_LOGIN_ATTEMPTS = 10;

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
        StaffAuthDto staffAuthDto = staffAuthMapper.toDto((StaffAuth) staffAuth);
        if (staffAuthDto.getPasswordHash().equals(password)) {
            return staffAuthDto;
        }
        return null;
    }
    @Transactional(noRollbackFor = AuthException.class)
    public StaffAuthResponse verifyStaffAuth(LoginRequest loginRequest){

        try{
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getPhone(),
                        loginRequest.getPin()
                ));
            StaffPrincipal  staffPrincipal = (StaffPrincipal) authentication.getPrincipal();
            StaffAuth staffAuth = staffPrincipal.getStaffAuth();
        if (authentication.isAuthenticated()) {
            //todo: staffAccLocked
            if (!(staffPrincipal.isAccountNonLocked())){
                throw new AuthException(AuthErrorCode.ACCOUNT_LOCKED.name(), "Tài khoản đã bị khóa");
            }
            String token = jwtService.generateStaffJWToken(staffPrincipal.getAuthId());
            staffAuth.setFailedLoginCount(0);
            staffAuthRepo.save(staffAuth);
            System.err.println("STAFF LOGIN SUCCESS: ATTEMPT = "+staffAuth.getFailedLoginCount());
            //todo: staff have lot of roles
            StaffProfile staffProfile = staffProfileRepo.getStaffProfileByStaffauth_StaffAuthId(staffAuth.getStaffAuthId());
            List<String> roles = staffRoleRepo
                    .getStaffRoleByStaff_StaffId(staffProfile.getStaffId())
                    .stream()
                    .map(staffRole -> staffRole.getRole().getRoleCode())
                    .toList();
            return new StaffAuthResponse("LOGIN_SUCCESS", roles, token);
        }
        }catch (BadCredentialsException e){
            System.err.println(e.getMessage());

        }
            throw new AuthException(AuthErrorCode.USER_NOT_FOUND.name(), "Sai thông tin đăng nhập, tài khoản có thể bị khóa sau 10 lần thử");
    }


}
