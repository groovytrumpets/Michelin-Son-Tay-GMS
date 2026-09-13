package com.g42.platform.gms.warehouse.domain.enums;

/** Cách đếm số lượng của một sản phẩm. */
public enum MeasurementType {
    /** Đếm nguyên chiếc: cái, bộ, hộp — số lượng bắt buộc là số nguyên. */
    COUNT,
    /** Đo lường liên tục: lít, kg, mét — cho phép số lẻ. */
    MEASURE
}
