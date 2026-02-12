package com.g42.platform.gms.booking.customer.infrastructure.mapper;

import com.g42.platform.gms.booking.customer.domain.entity.BookingRequestDetail;
import com.g42.platform.gms.booking.customer.infrastructure.entity.BookingRequestDetailJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface BookingRequestDetailMapper {

    @Mapping(target = "requestId",
            expression = "java(jpa.getRequest() != null ? jpa.getRequest().getRequestId() : null)")
    @Mapping(target = "itemId",
            expression = "java(jpa.getItem() != null ? jpa.getItem().getItemId() : null)")
    BookingRequestDetail toDomain(BookingRequestDetailJpaEntity jpa);

    @Mappings({
            @Mapping(target = "request", ignore = true),
            @Mapping(target = "item", ignore = true)
    })
    BookingRequestDetailJpaEntity toJpa(BookingRequestDetail domain);
}
