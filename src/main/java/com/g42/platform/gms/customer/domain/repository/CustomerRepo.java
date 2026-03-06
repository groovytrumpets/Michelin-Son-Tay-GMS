package com.g42.platform.gms.customer.domain.repository;


import com.g42.platform.gms.customer.api.dto.CustomerCreateDto;
import com.g42.platform.gms.customer.domain.entity.CustomerAuth;
import com.g42.platform.gms.customer.domain.entity.CustomerProfile;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface CustomerRepo {
    CustomerProfile createNewCustomerProfile(CustomerCreateDto customerDto);

    CustomerAuth createNewCustomerAuth(CustomerCreateDto customerCreateDto,CustomerProfile customerProfile);

    Page<CustomerProfile> getListOfCustomers(int page,int size, LocalDate date, Boolean isGuest, String search);
}
