package com.g42.platform.gms.hikvision.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "hikvision.device")
public class HikvisionProperties {

    @NotBlank
    private String host;

    private int port = 80;

    @NotBlank
    private String username;

    @NotBlank
    private String password;

    private boolean syncEnabled = true;

    private int connectTimeoutSeconds = 10;

    private int socketTimeoutSeconds = 10;
}
