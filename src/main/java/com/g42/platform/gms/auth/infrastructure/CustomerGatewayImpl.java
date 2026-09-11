package com.g42.platform.gms.auth.infrastructure;

import com.g42.platform.gms.auth.entity.CustomerProfile;
import com.g42.platform.gms.auth.repository.CustomerProfileRepository;
import com.g42.platform.gms.booking_management.api.dto.CustomerDto;
import com.g42.platform.gms.booking_management.application.command.CreateCustomerCommand;
import com.g42.platform.gms.booking_management.application.dto.CustomerData;
import com.g42.platform.gms.booking_management.application.port.CustomerGateway;
import com.g42.platform.gms.customer.api.mapper.CustomerDtoMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
@Component
@AllArgsConstructor
public class CustomerGatewayImpl implements CustomerGateway {
    private final CustomerProfileRepository customerProfileRepository;
    private final CustomerDtoMapper customerDtoMapper;

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
        return customerProfileRepository.findByPhone(command.phone()).map(cus -> {
            if (cus.getReferrerId() == null && command.referrerPhone() != null && !command.referrerPhone().isBlank()) {
                customerProfileRepository.findByPhone(command.referrerPhone()).ifPresent(referrer -> {
                    cus.setReferrerId(referrer.getCustomerId());
                    customerProfileRepository.save(cus);
                });
            }
            return cus.getCustomerId();
        }).orElseGet(() -> {
            CustomerProfile cus = new CustomerProfile();
            cus.setFullName(command.fullName());
            cus.setPhone(command.phone());
            cus.setFirstBookingAt(command.firstBookingAt());
            // customer_type và notification_channel là NOT NULL dưới DB — không set thì Hibernate
            // insert NULL và văng ConstraintViolation.
            cus.setCustomerType(com.g42.platform.gms.customer.domain.enums.CustomerType.INDIVIDUAL);
            cus.setNotificationChannel(com.g42.platform.gms.notification.domain.NotificationChannel.ZALO);
            cus.setIsDealer(false);
            cus.setIsCompany(false);
            
            if (command.referrerPhone() != null && !command.referrerPhone().isBlank()) {
                customerProfileRepository.findByPhone(command.referrerPhone()).ifPresent(referrer -> {
                    cus.setReferrerId(referrer.getCustomerId());
                });
            }
            
            CustomerProfile saved = customerProfileRepository.save(cus);
            return saved.getCustomerId();
        });
    }

    @Override
    public CustomerDto findCusDtoById(Integer customerId) {
        CustomerProfile customerProfile = customerProfileRepository.findById(customerId).orElse(null);
        return customerDtoMapper.toCustomerDto(customerProfile);
    }
}
