package com.g42.platform.gms.booking.customer.infrastructure.repository;

import com.g42.platform.gms.booking.customer.domain.entity.BookingRequestDetail;
import com.g42.platform.gms.booking.customer.domain.repository.BookingRequestDetailRepository;
import com.g42.platform.gms.booking.customer.infrastructure.entity.BookingRequestDetailJpaEntity;
import com.g42.platform.gms.booking.customer.infrastructure.entity.BookingRequestJpaEntity;
import com.g42.platform.gms.booking.customer.infrastructure.jpa.BookingRequestDetailJpaRepository;
import com.g42.platform.gms.booking.customer.infrastructure.jpa.BookingRequestJpaRepository;
import com.g42.platform.gms.booking.customer.infrastructure.mapper.BookingRequestDetailMapper;
import com.g42.platform.gms.catalog.entity.CatalogItem;
import com.g42.platform.gms.catalog.repository.CatalogItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class BookingRequestDetailRepositoryImpl implements BookingRequestDetailRepository {
    
    private final BookingRequestDetailJpaRepository jpaRepository;
    private final BookingRequestDetailMapper mapper;
    private final BookingRequestJpaRepository bookingRequestJpaRepository;
    private final CatalogItemRepository catalogItemRepository;
    
    @Override
    public BookingRequestDetail save(BookingRequestDetail domain) {
        BookingRequestDetailJpaEntity jpa = mapper.toJpa(domain);
        
        if (domain.getRequestId() != null) {
            BookingRequestJpaEntity request = bookingRequestJpaRepository.findById(domain.getRequestId())
                .orElseThrow(() -> new RuntimeException("BookingRequest not found: " + domain.getRequestId()));
            jpa.setRequest(request);
        }
        
        if (domain.getItemId() != null) {
            CatalogItem item = catalogItemRepository.findById(domain.getItemId())
                .orElseThrow(() -> new RuntimeException("CatalogItem not found: " + domain.getItemId()));
            jpa.setItem(item);
        }
        
        BookingRequestDetailJpaEntity saved = jpaRepository.save(jpa);
        return mapper.toDomain(saved);
    }
    
    @Override
    public List<BookingRequestDetail> saveAll(List<BookingRequestDetail> details) {
        List<BookingRequestDetailJpaEntity> jpaList = details.stream()
            .map(domain -> {
                BookingRequestDetailJpaEntity jpa = mapper.toJpa(domain);
                
                if (domain.getRequestId() != null) {
                    BookingRequestJpaEntity request = bookingRequestJpaRepository.findById(domain.getRequestId())
                        .orElseThrow(() -> new RuntimeException("BookingRequest not found: " + domain.getRequestId()));
                    jpa.setRequest(request);
                }
                
                if (domain.getItemId() != null) {
                    CatalogItem item = catalogItemRepository.findById(domain.getItemId())
                        .orElseThrow(() -> new RuntimeException("CatalogItem not found: " + domain.getItemId()));
                    jpa.setItem(item);
                }
                
                return jpa;
            })
            .collect(Collectors.toList());
        
        return jpaRepository.saveAll(jpaList)
            .stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<BookingRequestDetail> findByRequestId(Integer requestId) {
        return jpaRepository.findByRequest_RequestId(requestId)
            .stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }
    
    @Override
    public void deleteByRequestId(Integer requestId) {
        jpaRepository.deleteByRequest_RequestId(requestId);
    }
}
