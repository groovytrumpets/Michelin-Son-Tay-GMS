package com.g42.platform.gms.booking_management.application.port;

import com.g42.platform.gms.booking_management.application.command.CreateCustomerCommand;
import com.g42.platform.gms.booking_management.application.dto.CustomerData;

import java.util.Optional;

public interface CustomerGateway {
    Optional<CustomerData> findByPhone(String phone);
    Optional<CustomerData> findById(Integer id);

    CustomerData create(String fullName, String phone, String firstBookingAt    );
    Integer getOrCreateCustomer(CreateCustomerCommand command);
}
