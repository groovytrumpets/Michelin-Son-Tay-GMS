package com.g42.platform.gms.common.enums;

import lombok.Getter;

/**
 * Common code prefixes for different entity types.
 * Used by RandomCodeGenerator to generate unique codes.
 */
@Getter
public enum CodePrefix {
    BOOKING("MST"),          // Booking codes: MST_XXXXXX (Maintenance Service Ticket)
    REQUEST("MST"),           // Request codes: MST_XXXXXX
    SERVICE_TICKET("MST");    // Service Ticket codes: ST_XXXXXX (deprecated, use BOOKING instead)
    
    private final String prefix;
    
    CodePrefix(String prefix) {
        this.prefix = prefix;
    }
}
