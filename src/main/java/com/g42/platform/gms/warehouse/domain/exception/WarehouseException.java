package com.g42.platform.gms.warehouse.domain.exception;

import lombok.Getter;

@Getter
public class WarehouseException extends RuntimeException {
        private final WarehouseErrorCode code;
    public WarehouseException(String message, WarehouseErrorCode code) {
        super(message);
        this.code = code;
    }
}
