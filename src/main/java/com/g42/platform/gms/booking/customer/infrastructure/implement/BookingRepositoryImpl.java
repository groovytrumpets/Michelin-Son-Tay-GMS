package com.g42.platform.gms.booking.customer.infrastructure.implement;

import com.g42.platform.gms.auth.entity.CustomerProfile;
import com.g42.platform.gms.auth.repository.CustomerProfileRepository;
import com.g42.platform.gms.booking.customer.domain.entity.Booking;
import com.g42.platform.gms.booking.customer.domain.repository.BookingRepository;
import com.g42.platform.gms.booking.customer.infrastructure.entity.BookingJpaEntity;
import com.g42.platform.gms.booking.customer.infrastructure.repository.BookingJpaRepository;
import com.g42.platform.gms.booking.customer.infrastructure.mapper.BookingMapper;
import com.g42.platform.gms.catalog.entity.CatalogItem;
import com.g42.platform.gms.catalog.repository.CatalogItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class BookingRepositoryImpl implements BookingRepository {
    
    private final BookingJpaRepository jpaRepository;
    private final BookingMapper mapper;
    private final CustomerProfileRepository customerRepository;
    private final CatalogItemRepository catalogItemRepository;
    
    @Override
    public Booking save(Booking domain) {
        BookingJpaEntity jpa = mapper.toJpa(domain);
        
        if (domain.getCustomerId() != null) {
            CustomerProfile customer = customerRepository.findById(domain.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found: " + domain.getCustomerId()));
            jpa.setCustomer(customer);
        }
        
        if (domain.getServiceIds() != null && !domain.getServiceIds().isEmpty()) {
            List<CatalogItem> services = catalogItemRepository.findAllById(domain.getServiceIds());
            jpa.setServices(services);
        }
        
        BookingJpaEntity saved = jpaRepository.save(jpa);
        Booking result = mapper.toDomain(saved);
        result.initializeDefaults();
        return result;
    }
    
    @Override
    public Optional<Booking> findById(Integer bookingId) {
        return jpaRepository.findById(bookingId)
            .map(mapper::toDomain)
            .map(booking -> {
                booking.initializeDefaults();
                return booking;
            });
    }
    
    @Override
    public Optional<Booking> findByIdAndCustomerId(Integer bookingId, Integer customerId) {
        return jpaRepository.findById(bookingId)
            .filter(jpa -> jpa.getCustomer() != null && jpa.getCustomer().getCustomerId().equals(customerId))
            .map(mapper::toDomain)
            .map(booking -> {
                booking.initializeDefaults();
                return booking;
            });
    }
    
    @Override
    public List<Booking> findByCustomerIdOrderByDateDesc(Integer customerId) {
        return jpaRepository.findByCustomer_CustomerIdOrderByScheduledDateDescScheduledTimeDesc(customerId)
            .stream()
            .map(mapper::toDomain)
            .peek(Booking::initializeDefaults)
            .collect(Collectors.toList());
    }
    
    @Override
    public void delete(Booking booking) {
        jpaRepository.findById(booking.getBookingId())
            .ifPresent(jpaRepository::delete);
    }
}
