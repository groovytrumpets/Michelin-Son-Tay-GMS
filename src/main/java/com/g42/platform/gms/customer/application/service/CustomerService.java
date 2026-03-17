package com.g42.platform.gms.customer.application.service;


import com.g42.platform.gms.customer.api.dto.CustomerCreateDto;
import com.g42.platform.gms.customer.api.dto.CustomerUpdateDto;
import com.g42.platform.gms.customer.api.mapper.CustomerDtoMapper;
import com.g42.platform.gms.customer.domain.entity.CustomerAuth;
import com.g42.platform.gms.customer.domain.entity.CustomerProfile;
import com.g42.platform.gms.customer.domain.exception.CustomerErrorCode;
import com.g42.platform.gms.customer.domain.exception.CustomerException;
import com.g42.platform.gms.customer.domain.repository.CustomerRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class CustomerService {
    @Autowired
    CustomerRepo customerRepo;
    @Autowired
    CustomerDtoMapper customerDtoMapper;
    private PasswordEncoder passwordEncoder;
    @Transactional
    public CustomerCreateDto createNewCustomer(CustomerCreateDto customerDto) {
        CustomerProfile customerProfile = customerRepo.createNewCustomerProfile(customerDto);
        CustomerAuth customerAuth = customerRepo.createNewCustomerAuth(customerDto,customerProfile);
        return customerDtoMapper.toCusCreateDto(customerProfile,customerAuth);
    }

    public Page<CustomerProfile> getListOfAllCustomerProfile(int page, int size, LocalDate date, Boolean isGuest, String search, String status) {
        return customerRepo.getListOfCustomers(page,size,date,isGuest,search,status);
    }
    @Transactional
    public CustomerCreateDto updateCustomer(Integer customerId, CustomerUpdateDto customerUpdateDto) {
        CustomerProfile customerProfile = customerRepo.findProflieById(customerId);
        CustomerAuth customerAuth = customerRepo.findAuthById(customerId);
        customerProfile.setFullName(customerUpdateDto.getFullName());
        customerProfile.setPhone(customerUpdateDto.getPhone());
        customerProfile.setEmail(customerUpdateDto.getEmail());
        customerProfile.setGender(customerUpdateDto.getGender());
        customerProfile.setAvatar(customerUpdateDto.getAvatar());
        customerAuth.setStatus(customerUpdateDto.getStatus());
        customerAuth.setLastLoginAt(customerUpdateDto.getLastLoginAt());
        if (!customerRepo.updateCustomer(customerId,customerProfile,customerAuth)){
            throw new CustomerException("Update fail!", CustomerErrorCode.INVALID_CUSTOMER_PROFILE);
        }
        return customerDtoMapper.toCusCreateDto(customerProfile,customerAuth);
    }
}
