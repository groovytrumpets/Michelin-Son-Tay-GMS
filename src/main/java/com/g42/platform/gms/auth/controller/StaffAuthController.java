package com.g42.platform.gms.auth.controller;

import com.g42.platform.gms.auth.dto.LoginRequest;
import com.g42.platform.gms.auth.dto.StaffAuthDto;
import com.g42.platform.gms.auth.service.StaffAuthService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@RequestMapping("/api/auth/staff-auth")
public class StaffAuthController {
    @Autowired
    private final StaffAuthService staffAuthService;
    private final AuthenticationManager authenticationManager;



//    @GetMapping
//    public Iterable<StaffAuthDto> getAllStaffAuth(){
//        return staffAuthService.getAllStaffAuth();
//    }

//    @GetMapping("/{id}")
//    public ResponseEntity<StaffAuthDto> getStaftAuthById(@PathVariable int id){
//        return staffAuthService.getStaffAuthById(id);
//    }
    @PostMapping("/login")
    public String login(@RequestBody LoginRequest loginRequest){
//        System.out.println("PHONE = " + loginRequest.getPhone());
//        System.out.println("PIN   = " + loginRequest.getPin());

        return staffAuthService.verifyStaffAuth(loginRequest);
    }

}
