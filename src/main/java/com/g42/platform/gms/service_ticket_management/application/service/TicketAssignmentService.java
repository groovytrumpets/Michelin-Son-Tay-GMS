package com.g42.platform.gms.service_ticket_management.application.service;

import com.g42.platform.gms.service_ticket_management.api.dto.assignment.AssignmentInfo;
import com.g42.platform.gms.service_ticket_management.api.dto.assignment.TicketAssignmentResponse;
import com.g42.platform.gms.service_ticket_management.api.mapper.assignment.TicketAssignmentDtoMapper;
import com.g42.platform.gms.service_ticket_management.domain.entity.ServiceTicket;
import com.g42.platform.gms.service_ticket_management.domain.entity.ServiceTicketAssignment;
import com.g42.platform.gms.service_ticket_management.domain.exception.CheckInException;
import com.g42.platform.gms.service_ticket_management.domain.repository.TicketAssignmentRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketAssignmentService {
    private final TicketAssignmentRepo ticketAssignmentRepo;
    private final TicketAssignmentDtoMapper ticketAssignmentDtoMapper;

    @Transactional(readOnly = true)
    public TicketAssignmentResponse getAssignments(Integer serviceTicketId) {
        //todo: validate service ticket available
        ServiceTicket serviceTicket = ticketAssignmentRepo.findById(serviceTicketId);
        if (serviceTicket == null) {
            new CheckInException("Không tìm thấy service ticket: " + serviceTicketId);
        }
        //todo: get all active assignment
        List<ServiceTicketAssignment> serviceTicketAssignments = ticketAssignmentRepo.findAssignByServiceIdAndStatus(serviceTicketId,"ACTIVE");
        //todo: map to respond
        TicketAssignmentResponse ticketAssignmentResponse = new TicketAssignmentResponse();
        ticketAssignmentResponse.setTicketId(serviceTicketId);

        List<AssignmentInfo> assignmentInfos = serviceTicketAssignments.stream().map(ticketAssignmentDtoMapper.)

    }
}
