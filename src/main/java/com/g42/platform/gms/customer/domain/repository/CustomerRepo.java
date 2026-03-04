package com.g42.platform.gms.customer.domain.repository;

import com.g42.platform.gms.auth.entity.CustomerAuth;
import com.g42.platform.gms.auth.entity.CustomerProfile;
import com.g42.platform.gms.customer.api.dto.CustomerCreateDto;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRepo {
    CustomerProfile createNewCustomerProfile(CustomerCreateDto customerDto);

    CustomerAuth createNewCustomerAuth(CustomerProfile customerProfile);
}
