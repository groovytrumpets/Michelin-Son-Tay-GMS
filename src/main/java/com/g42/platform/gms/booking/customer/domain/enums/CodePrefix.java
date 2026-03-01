package com.g42.platform.gms.booking.customer.domain.enums;

public enum CodePrefix {
    BOOKING("BK"),
    REQUEST("RQ");

    private final String prefix;

    CodePrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }

    @Override
    public String toString() {
        return prefix;
    }
}
