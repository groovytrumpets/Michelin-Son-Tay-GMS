package com.g42.platform.gms.auth.infrastructure;

import com.g42.platform.gms.auth.entity.CustomerProfile;
import com.g42.platform.gms.auth.repository.CustomerProfileRepository;
import com.g42.platform.gms.booking_management.application.command.CreateCustomerCommand;
import com.g42.platform.gms.booking_management.application.dto.CustomerData;
import com.g42.platform.gms.booking_management.application.port.CustomerGateway;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
@Component
@AllArgsConstructor
public class CustomerGatewayImpl implements CustomerGateway {
    private final CustomerProfileRepository customerProfileRepository;
    @Override
    public Optional<CustomerData> findByPhone(String phone) {
        return customerProfileRepository.findByPhone(phone).map(cus -> new CustomerData(
                cus.getCustomerId(),
                cus.getFullName(),
                cus.getPhone(),
                cus.getFirstBookingAt()));
    }

    @Override
    public Optional<CustomerData> findById(Integer id) {
        return customerProfileRepository.findById(id).map(cus -> new CustomerData(
                cus.getCustomerId(),
                cus.getFullName(),
                cus.getPhone(),
                cus.getFirstBookingAt()));
    }

    @Override
    public CustomerData create(String fullName, String phone, String firstBookingAt) {
        //todo: function create new customer
        CustomerProfile cus = new CustomerProfile();

        return new CustomerData(
                cus.getCustomerId(),
                cus.getFullName(),
                cus.getPhone(),
                cus.getFirstBookingAt());
    }

    @Override
    public Integer getOrCreateCustomer(CreateCustomerCommand command) {
        return customerProfileRepository.findByPhone(command.phone()).map(CustomerProfile::getCustomerId)
                .orElseGet(() -> {
                    CustomerProfile cus = new CustomerProfile();
                    cus.setFullName(command.fullName());
                    cus.setPhone(command.phone());
                    cus.setFirstBookingAt(command.firstBookingAt());
                    return customerProfileRepository.save(cus).getCustomerId();
                });
    }
}
