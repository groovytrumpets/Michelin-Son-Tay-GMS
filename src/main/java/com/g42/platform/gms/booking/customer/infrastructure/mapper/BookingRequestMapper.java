package com.g42.platform.gms.booking.customer.infrastructure.mapper;

import com.g42.platform.gms.booking.customer.domain.entity.BookingRequest;
import com.g42.platform.gms.booking.customer.infrastructure.entity.BookingRequestDetailJpaEntity;
import com.g42.platform.gms.booking.customer.infrastructure.entity.BookingRequestJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface BookingRequestMapper {

    @Mappings({
            @Mapping(target = "customerId",
                    expression = "java(jpa.getCustomer() != null ? jpa.getCustomer().getCustomerId() : null)"),
            @Mapping(target = "confirmedBy",
                    expression = "java(jpa.getConfirmedBy() != null ? (int) jpa.getConfirmedBy().getStaffId() : null)"),
            @Mapping(target = "serviceIds",
                    expression = "java(mapServiceIds(jpa.getDetails()))")
    })
    BookingRequest toDomain(BookingRequestJpaEntity jpa);

    @Mappings({
            @Mapping(target = "customer", ignore = true),
            @Mapping(target = "confirmedBy", ignore = true),
            @Mapping(target = "details", ignore = true)
    })
    BookingRequestJpaEntity toJpa(BookingRequest domain);

    // ===== Helper methods cho MapStruct =====

    default List<Integer> mapServiceIds(List<BookingRequestDetailJpaEntity> details) {
        if (details == null) {
            return null;
        }
        return details.stream()
                .map(detail -> detail.getItem() != null ? detail.getItem().getItemId() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
