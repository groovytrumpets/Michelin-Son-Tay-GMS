package com.g42.platform.gms.booking.customer.infrastructure.implement;

import com.g42.platform.gms.auth.entity.CustomerProfile;
import com.g42.platform.gms.auth.repository.CustomerProfileRepository;
import com.g42.platform.gms.booking.customer.domain.entity.BookingRequest;
import com.g42.platform.gms.booking.customer.domain.enums.BookingRequestStatus;
import com.g42.platform.gms.booking.customer.domain.repository.BookingRequestRepository;
import com.g42.platform.gms.booking.customer.infrastructure.entity.BookingRequestJpaEntity;
import com.g42.platform.gms.booking.customer.infrastructure.repository.BookingRequestJpaRepository;
import com.g42.platform.gms.booking.customer.infrastructure.mapper.BookingRequestMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class BookingRequestRepositoryImpl implements BookingRequestRepository {
    
    private final BookingRequestJpaRepository jpaRepository;
    private final BookingRequestMapper mapper;
    private final CustomerProfileRepository customerRepository;
    
    @Override
    public BookingRequest save(BookingRequest domain) {
        BookingRequestJpaEntity jpa = mapper.toJpa(domain);
        
        if (domain.getCustomerId() != null) {
            CustomerProfile customer = customerRepository.findById(domain.getCustomerId())
                .orElse(null);
            jpa.setCustomer(customer);
        }
        
        BookingRequestJpaEntity saved = jpaRepository.save(jpa);
        BookingRequest result = mapper.toDomain(saved);
        result.initializeDefaults();
        return result;
    }
    
    @Override
    public Optional<BookingRequest> findById(Integer requestId) {
        return jpaRepository.findById(requestId)
            .map(mapper::toDomain)
            .map(request -> {
                request.initializeDefaults();
                return request;
            });
    }
    
    @Override
    public Optional<BookingRequest> findByIdAndStatus(Integer requestId, BookingRequestStatus status) {
        return jpaRepository.findByRequestIdAndStatus(requestId, status)
            .map(mapper::toDomain)
            .map(request -> {
                request.initializeDefaults();
                return request;
            });
    }
    
    @Override
    public List<BookingRequest> findByStatus(BookingRequestStatus status) {
        return jpaRepository.findByStatus(status)
            .stream()
            .map(mapper::toDomain)
            .peek(BookingRequest::initializeDefaults)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<BookingRequest> findByPhone(String phone) {
        return jpaRepository.findByPhone(phone)
            .stream()
            .map(mapper::toDomain)
            .peek(BookingRequest::initializeDefaults)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<BookingRequest> findByStatusOrderByCreatedAtDesc(BookingRequestStatus status) {
        return jpaRepository.findByStatusOrderByCreatedAtDesc(status)
            .stream()
            .map(mapper::toDomain)
            .peek(BookingRequest::initializeDefaults)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<BookingRequest> findExpiredPendingRequests(LocalDateTime now) {
        return jpaRepository.findExpiredPendingRequests(now)
            .stream()
            .map(mapper::toDomain)
            .peek(BookingRequest::initializeDefaults)
            .collect(Collectors.toList());
    }
    
    @Override
    public long countByClientIpAndStatusAndCreatedAtAfter(String clientIp, BookingRequestStatus status, LocalDateTime dateTime) {
        return jpaRepository.countByClientIpAndStatusAndCreatedAtAfter(clientIp, status, dateTime);
    }
    
    @Override
    public List<BookingRequest> findRejectedByIpSince(String ip, LocalDateTime since) {
        return jpaRepository.findRejectedByIpSince(ip, since)
            .stream()
            .map(mapper::toDomain)
            .peek(BookingRequest::initializeDefaults)
            .collect(Collectors.toList());
    }
}
