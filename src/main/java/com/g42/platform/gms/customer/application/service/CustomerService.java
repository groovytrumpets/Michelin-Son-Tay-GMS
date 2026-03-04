package com.g42.platform.gms.customer.application.service;

import com.g42.platform.gms.auth.entity.CustomerAuth;
import com.g42.platform.gms.auth.entity.CustomerProfile;
import com.g42.platform.gms.customer.api.dto.CustomerCreateDto;
import com.g42.platform.gms.customer.domain.repository.CustomerRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CustomerService {
    @Autowired
    CustomerRepo customerRepo;
    public CustomerCreateDto createNewCustomer(CustomerCreateDto customerDto) {
        CustomerProfile customerProfile = customerRepo.createNewCustomerProfile(customerDto);
        CustomerAuth customerAuth = customerRepo.createNewCustomerAuth(customerProfile);
        return null;
    }
}
