package com.g42.platform.gms.warehouse.domain.enums;

/**
 * Loại phiếu xuất kho:
 * - SERVICE_TICKET: xuất theo phiếu dịch vụ (lắp tại xưởng hoặc theo phiếu đặt lịch)
 * - RETAIL: bán lẻ trực tiếp (khách mua mang về, không kèm dịch vụ)
 * - WHOLESALE: bán buôn (đại lý, garage khác, số lượng lớn)
 */
public enum IssueType {
    SERVICE_TICKET,
    RETAIL,
    WHOLESALE
}
