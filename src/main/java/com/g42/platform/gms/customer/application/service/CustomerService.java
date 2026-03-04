package com.g42.platform.gms.customer.application.service;


import com.g42.platform.gms.customer.api.dto.CustomerCreateDto;
import com.g42.platform.gms.customer.domain.entity.CustomerAuth;
import com.g42.platform.gms.customer.domain.entity.CustomerProfile;
import com.g42.platform.gms.customer.domain.repository.CustomerRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class CustomerService {
    @Autowired
    CustomerRepo customerRepo;
    public CustomerCreateDto createNewCustomer(CustomerCreateDto customerDto) {
        CustomerProfile customerProfile = customerRepo.createNewCustomerProfile(customerDto);
        CustomerAuth customerAuth = customerRepo.createNewCustomerAuth(customerDto,customerProfile);
        return null;
    }

    public Page<CustomerProfile> getListOfAllCustomerProfile(int page, int size, LocalDate date, Boolean isGuest, String search) {
        return customerRepo.getListOfCustomers(page,size,date,isGuest,search);
    }
}
