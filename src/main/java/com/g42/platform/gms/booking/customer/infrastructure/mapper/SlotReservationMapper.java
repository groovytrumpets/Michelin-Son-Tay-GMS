package com.g42.platform.gms.booking.customer.infrastructure.mapper;

import com.g42.platform.gms.booking.customer.domain.entity.SlotReservation;
import com.g42.platform.gms.booking.customer.infrastructure.entity.BookingSlotReservationJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface SlotReservationMapper {

    @Mappings({
            @Mapping(target = "bookingId", source = "booking.bookingId")
    })
    SlotReservation toDomain(BookingSlotReservationJpaEntity jpa);

    @Mappings({
            @Mapping(target = "booking", ignore = true)
    })
    BookingSlotReservationJpaEntity toJpa(SlotReservation domain);
}
