package com.g42.platform.gms.auth.service;

import com.g42.platform.gms.auth.dto.StaffAuthDto;
import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.auth.entity.Staffauth;
import com.g42.platform.gms.auth.repository.StaffAuthRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class StaffAuthDetailsService implements UserDetailsService {

    @Autowired
    private StaffAuthRepo staffAuthRepo;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        Staffauth staffAuth = staffAuthRepo.searchByEmail(username);
        if(staffAuth == null) throw new UsernameNotFoundException(username+" not found!");
        return new StaffPrincipal(staffAuth);
    }
}
