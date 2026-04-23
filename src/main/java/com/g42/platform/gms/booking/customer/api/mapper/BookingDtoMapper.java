package com.g42.platform.gms.booking.customer.api.mapper;

import com.g42.platform.gms.booking.customer.api.dto.BookingResponse;
import com.g42.platform.gms.booking.customer.api.dto.BookingResponse.ProgressStep;
import com.g42.platform.gms.booking.customer.api.dto.ServiceItemDto;
import com.g42.platform.gms.booking.customer.domain.entity.Booking;
import com.g42.platform.gms.booking.customer.domain.enums.BookingStatus;
import com.g42.platform.gms.booking.customer.infrastructure.entity.CatalogItemJpaEntity;
import com.g42.platform.gms.catalog.infrastructure.repository.CatalogItemRepository;
import com.g42.platform.gms.estimation.api.internal.EstimateInternalApi;
import com.g42.platform.gms.estimation.domain.entity.Estimate;
import com.g42.platform.gms.service_ticket_management.domain.enums.TicketStatus;
import com.g42.platform.gms.service_ticket_management.infrastructure.entity.ServiceTicketJpa;
import com.g42.platform.gms.service_ticket_management.infrastructure.repository.ServiceTicketRepository;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring")
public abstract class BookingDtoMapper {

    @Autowired
    protected CatalogItemRepository catalogItemRepository;

    @Autowired
    protected ServiceTicketRepository serviceTicketRepository;

    @Autowired
    protected EstimateInternalApi estimateInternalApi;

    @Mapping(target = "customerName", ignore = true)
    @Mapping(target = "phone", ignore = true)
    @Mapping(target = "serviceIds", source = "catalogItemIds")
    @Mapping(target = "services", ignore = true)
    @Mapping(target = "totalEstimatedTime", ignore = true)
    @Mapping(target = "progressSteps", ignore = true)
    @Mapping(target = "technicianNotes", ignore = true)
    @Mapping(target = "ticketStatus", ignore = true)
    public abstract BookingResponse toResponse(Booking domain);

    @AfterMapping
    protected void populateServices(@MappingTarget BookingResponse response, Booking domain) {
        List<Integer> catalogItemIds = domain.getCatalogItemIds();

        if (catalogItemIds == null || catalogItemIds.isEmpty()) {
            response.setServices(new ArrayList<>());
            response.setTotalEstimatedTime(0);
        } else {
            List<CatalogItemJpaEntity> items = catalogItemRepository.findAllById(catalogItemIds);
            List<ServiceItemDto> serviceDtos = new ArrayList<>();
            int totalTime = 0;
            for (CatalogItemJpaEntity item : items) {
                ServiceItemDto dto = mapToServiceDto(item);
                serviceDtos.add(dto);
                if (dto.getEstimateTime() != null && dto.getEstimateTime() > 0) {
                    totalTime += dto.getEstimateTime();
                }
            }
            response.setServices(serviceDtos);
            response.setTotalEstimatedTime(totalTime);
        }

        // Populate progress steps và technician notes từ service ticket
        if (domain.getBookingId() != null) {
            serviceTicketRepository.findByBookingId(domain.getBookingId()).ifPresent(ticket -> {
                response.setTicketStatus(ticket.getTicketStatus() != null ? ticket.getTicketStatus().name() : null);
                response.setTechnicianNotes(ticket.getTechnicianNotes());
                response.setProgressSteps(buildProgressSteps(domain.getStatus(), ticket.getTicketStatus()));
                Estimate latestEstimate = estimateInternalApi.findLatestByServiceTicketId(ticket.getServiceTicketId());
                if (latestEstimate != null) {
                    response.setEstimateId(latestEstimate.getId());
                }
            });
        }

        // Nếu chưa có service ticket, vẫn build progress từ booking status
        if (response.getProgressSteps() == null) {
            response.setProgressSteps(buildProgressSteps(domain.getStatus(), null));
        }
    }

    /**
     * Build danh sách 4 bước tiến trình.
     *
     * Luồng Booking:  PENDING → CONFIRMED → DONE (check-in hoàn tất, ticket được tạo)
     * Luồng Ticket:   CREATED/DRAFT → IN_PROGRESS → COMPLETED
     *
     * Bước 1 "Đã đặt lịch"    : luôn COMPLETED
     * Bước 2 "Đã xác nhận"    : COMPLETED khi booking CONFIRMED hoặc DONE
     * Bước 3 "Đang thực hiện" : ACTIVE khi booking=DONE + ticket=IN_PROGRESS
     *                           COMPLETED khi ticket=COMPLETED
     *                           PENDING còn lại
     * Bước 4 "Hoàn thành"     : COMPLETED khi ticket=COMPLETED
     */
    private List<ProgressStep> buildProgressSteps(BookingStatus bookingStatus, TicketStatus ticketStatus) {
        List<ProgressStep> steps = new ArrayList<>();

        // Bước 1: luôn COMPLETED
        steps.add(step("Đã đặt lịch", "COMPLETED"));

        // Bước 2: CONFIRMED hoặc DONE
        boolean confirmed = bookingStatus == BookingStatus.CONFIRMED
                || bookingStatus == BookingStatus.DONE;
        steps.add(step("Đã xác nhận", confirmed ? "COMPLETED" : "PENDING"));

        // Bước 3: booking phải DONE (check-in xong) mới bắt đầu luồng ticket
        boolean checkedIn = bookingStatus == BookingStatus.DONE;
        boolean serviceCompleted = ticketStatus == TicketStatus.COMPLETED;
        boolean serviceInProgress = checkedIn && ticketStatus == TicketStatus.REPAIRING;
        String step3;
        if (serviceCompleted) {
            step3 = "COMPLETED";
        } else if (serviceInProgress) {
            step3 = "ACTIVE";
        } else if (checkedIn) {
            // Đã check-in nhưng ticket chưa IN_PROGRESS (CREATED/DRAFT)
            step3 = "ACTIVE";
        } else {
            step3 = "PENDING";
        }
        steps.add(step("Đang thực hiện", step3));

        // Bước 4: chỉ COMPLETED khi ticket COMPLETED
        steps.add(step("Hoàn thành", serviceCompleted ? "COMPLETED" : "PENDING"));

        return steps;
    }

    private ProgressStep step(String label, String status) {
        ProgressStep s = new ProgressStep();
        s.setLabel(label);
        s.setStatus(status);
        return s;
    }

    private ServiceItemDto mapToServiceDto(CatalogItemJpaEntity item) {
        ServiceItemDto dto = new ServiceItemDto();
        dto.setItemId(item.getItemId());
        dto.setItemName(item.getItemName());
        dto.setItemType(item.getItemType());
        if (item.getServiceService() != null) {
            dto.setEstimateTime(item.getServiceService().getEstimateTime());
        }
        return dto;
    }
}

