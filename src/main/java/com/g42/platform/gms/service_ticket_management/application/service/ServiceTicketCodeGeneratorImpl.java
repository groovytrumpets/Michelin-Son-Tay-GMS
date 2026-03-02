package com.g42.platform.gms.service_ticket_management.application.service;

import com.g42.platform.gms.common.enums.CodePrefix;
import com.g42.platform.gms.common.exception.CodeGenerationException;
import com.g42.platform.gms.common.service.RandomCodeGenerator;
import com.g42.platform.gms.service_ticket_management.infrastructure.repository.ServiceTicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Implementation of ServiceTicketCodeGenerator.
 * Delegates to common RandomCodeGenerator service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceTicketCodeGeneratorImpl implements ServiceTicketCodeGenerator {

    private final RandomCodeGenerator randomCodeGenerator;
    private final ServiceTicketRepository serviceTicketRepository;

    @Override
    @Transactional
    public String generateCode(LocalDate date, CodePrefix prefix) throws CodeGenerationException {
        return randomCodeGenerator.generateCode(
            date,
            prefix,
            serviceTicketRepository::existsByTicketCode
        );
    }
}
