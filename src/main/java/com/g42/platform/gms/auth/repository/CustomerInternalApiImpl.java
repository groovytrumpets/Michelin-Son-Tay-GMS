package com.g42.platform.gms.auth.repository;

import com.g42.platform.gms.auth.api.internal.CustomerInternalApi;
import com.g42.platform.gms.auth.entity.CustomerProfile;
import com.g42.platform.gms.customer.infrastructure.entity.CustomerProfileJpa;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerInternalApiImpl implements CustomerInternalApi {
    @Autowired
    private CustomerProfileRepository customerProfileRepository;

    @Override
    public List<CustomerProfile> findAllByIds(List<Integer> customerIds) {
        return customerProfileRepository.findAllById(customerIds);
    }

    @Override
    public CustomerProfile findById(Integer customerId) {
        return customerProfileRepository.findById(customerId).get();
    }

    @Override
    public String getCustomerPhoneByServiceTicketId(Integer serviceTicketId) {
        CustomerProfileJpa customerProfile = customerProfileRepository.getCustomerProfilesByServiceTicketId(serviceTicketId);
        return customerProfile.getPhone();
    }

    @Override
    public String getNameByServiceTicketId(Integer serviceTicketId) {
        CustomerProfileJpa customerProfile = customerProfileRepository.getCustomerProfilesByServiceTicketId(serviceTicketId);
        return customerProfile.getFullName();
    }
}
