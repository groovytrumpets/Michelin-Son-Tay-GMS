package com.g42.platform.gms.warehouse.api.dto.serial;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/** Khai báo serial cho hàng đã có trong một lô (tồn cũ trước khi bật theo dõi serial). */
@Data
public class RegisterSerialsRequest {
    @NotNull
    private Integer itemId;
    @NotNull
    private Integer entryItemId;
    @NotEmpty
    private List<String> serialCodes;
    private String attributesJson;
}
