package com.g42.platform.gms.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CheckPhoneResponse {


    public enum Status {
        NOT_REGISTERED, // Chưa có trong DB
        UNVERIFIED,     // Có trong DB (Status=INACTIVE), chưa có PIN
        ACTIVE,         // Có trong DB (Status=ACTIVE), đã có PIN
        LOCKED          // Có trong DB (Status=LOCKED)
    }

    private Status status;
    private boolean hasPin;
}