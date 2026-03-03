package com.g42.platform.gms.common.enums;

import lombok.Getter;

/**
 * Common code prefixes for different entity types.
 * Used by RandomCodeGenerator to generate unique codes.
 */
@Getter
public enum CodePrefix {
    BOOKING("BK"),           // Booking codes: BK_XXXXXX
    REQUEST("RQ"),           // Request codes: RQ_XXXXXX
    SERVICE_TICKET("ST");    // Service Ticket codes: ST_XXXXXX
    
    private final String prefix;
    
    CodePrefix(String prefix) {
        this.prefix = prefix;
    }
}
