package com.g42.platform.gms.service_ticket_management.domain.exception;

import lombok.Getter;

@Getter
public class AssignmentException extends RuntimeException {
        private final AssignmentErrorCode code;
    public AssignmentException(String message, AssignmentErrorCode code) {
        super(message);
        this.code = code;
    }
}
