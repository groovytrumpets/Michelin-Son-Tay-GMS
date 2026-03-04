package com.g42.platform.gms.customer.infrastructure;

import com.g42.platform.gms.auth.entity.CustomerAuth;
import com.g42.platform.gms.auth.entity.CustomerProfile;
import com.g42.platform.gms.customer.api.dto.CustomerCreateDto;
import com.g42.platform.gms.customer.domain.repository.CustomerRepo;
import com.g42.platform.gms.customer.infrastructure.repository.CustomerAuthJpaRepo;
import com.g42.platform.gms.customer.infrastructure.repository.CustomerProfileJpaRepo;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class CustomerRepoImpl implements CustomerRepo {
    @Autowired
    CustomerAuthJpaRepo customerAuthJpaRepo;
    @Autowired
    CustomerProfileJpaRepo customerProfileJpaRepo;
    @Override
    public CustomerProfile createNewCustomerProfile(CustomerCreateDto customerDto) {
    //todo: create new acc based on customerDto
        return null;
    }

    @Override
    public CustomerAuth createNewCustomerAuth(CustomerProfile customerProfile) {
        return null;
    }
}
