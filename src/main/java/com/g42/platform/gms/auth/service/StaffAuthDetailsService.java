package com.g42.platform.gms.auth.service;

import com.g42.platform.gms.auth.dto.StaffAuthDto;
import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.auth.entity.StaffProfile;
import com.g42.platform.gms.auth.entity.Staffauth;
import com.g42.platform.gms.auth.repository.StaffAuthRepo;
import com.g42.platform.gms.auth.repository.StaffProfileRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class StaffAuthDetailsService implements UserDetailsService {

    @Autowired
    private StaffAuthRepo staffAuthRepo;
    @Autowired
    private StaffProfileRepo staffProfileRepo;

    Staffauth staffAuth =null;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        //login via email
        if (username.contains("@")){
        staffAuth = staffAuthRepo.searchByEmail(username);
        }else {
            //login via phone - must join staffProfile
            StaffProfile staffProfile =staffProfileRepo.searchByPhone(username);
            if (staffProfile != null) {
                staffAuth = staffProfile.getStaffauth();
            }else  {
                throw new UsernameNotFoundException("Phone not found");
            }
        }
        if(staffAuth == null) throw new UsernameNotFoundException(username+" not found!");
        return new StaffPrincipal(staffAuth);
    }
}
