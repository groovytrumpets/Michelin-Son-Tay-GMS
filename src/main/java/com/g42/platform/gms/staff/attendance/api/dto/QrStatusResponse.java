package com.g42.platform.gms.staff.attendance.api.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QrStatusResponse {
    private Integer locationId;
    private String locationName;
    private String address;
    private boolean alreadyCheckedIn;
    private boolean alreadyCheckedOut;
    private Integer checkinId;
}
