package com.g42.platform.gms.notification.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TokenRes {
    private String access_token;
    private String refresh_token;
    private Long expires_in;

}
