package com.g42.platform.gms.booking.customer.infrastructure.mapper;

import com.g42.platform.gms.booking.customer.domain.entity.Booking;
import com.g42.platform.gms.booking.customer.infrastructure.entity.BookingJpaEntity;
import com.g42.platform.gms.booking.customer.infrastructure.entity.CatalogItemJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface BookingMapper {

    @Mappings({
            @Mapping(target = "customerId", source = "customer.customerId"),
            @Mapping(target = "serviceIds", expression = "java(mapServiceIds(jpa.getServices()))")
    })
    Booking toDomain(BookingJpaEntity jpa);

    @Mappings({
            @Mapping(target = "customer", ignore = true),
            @Mapping(target = "services", ignore = true),
            @Mapping(target = "bookingId", source = "bookingId")
    })
    BookingJpaEntity toJpa(Booking domain);

    // Helper method for MapStruct
    default List<Integer> mapServiceIds(List<CatalogItemJpaEntity> services) {
        if (services == null) {
            return null;
        }
        return services.stream()
                .map(service -> service.getItemId())
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
