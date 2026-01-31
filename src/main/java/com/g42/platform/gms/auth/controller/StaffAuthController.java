package com.g42.platform.gms.auth.controller;

import com.g42.platform.gms.auth.dto.LoginRequest;
import com.g42.platform.gms.auth.dto.StaffAuthDto;
import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.auth.entity.Staffauth;
import com.g42.platform.gms.auth.mapper.StaffAuthMapper;
import com.g42.platform.gms.auth.repository.StaffAuthRepo;
import com.g42.platform.gms.auth.service.StaffAuthService;
import com.g42.platform.gms.common.dto.ApiResponse;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@RequestMapping("/api/auth/staff-auth")
public class StaffAuthController {
    @Autowired
    private final StaffAuthService staffAuthService;
    private final AuthenticationManager authenticationManager;


    //@RequestMapping("/StaffAuth")
    @GetMapping
    public Iterable<StaffAuthDto> getAllStaffAuth(){
        return staffAuthService.getAllStaffAuth();
    }

    @GetMapping("/{id}")
    public ResponseEntity<StaffAuthDto> getStaftAuthById(@PathVariable int id){
        return staffAuthService.getStaffAuthById(id);
    }
    @PostMapping("/staff-login")
    public StaffAuthDto doLogin(
            @RequestParam("email") String email, @RequestParam("password") String password){
        StaffAuthDto staffAuthDto = staffAuthService.AuthenticateStaff(email, password);
        if (staffAuthDto == null) {
            return new StaffAuthDto((long) -1, (long) -1,"WRONG","WRONG");
        }
        System.out.println("login successful");
        return staffAuthDto;
    }
    @PostMapping("/login")
    public String login(@RequestBody LoginRequest loginRequest){
//        System.out.println("PHONE = " + loginRequest.getPhone());
//        System.out.println("PIN   = " + loginRequest.getPin());

        return staffAuthService.verifyStaffAuth(loginRequest);
    }

}
