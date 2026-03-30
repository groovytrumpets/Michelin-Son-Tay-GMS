package com.g42.platform.gms.service_ticket_management.api.dto.safety;

import com.g42.platform.gms.service_ticket_management.domain.enums.PressureUnit;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Structured tire input matching the physical inspection sheet:
 *
 * Actual data (per tire):
 *   frontLeft / frontRight / rearLeft / rearRight / spare → treadDepth + pressure riêng
 *
 * tireSpecification (size thực tế, theo trục):
 *   frontTireSpecification → FRONT_LEFT + FRONT_RIGHT
 *   rearTireSpecification  → REAR_LEFT + REAR_RIGHT
 *   SPARE → null (để xử lý logic riêng sau)
 *
 * recommendedTireSize (size lốp khuyến cáo): nhập 1 lần, áp cho tất cả bánh trừ spare
 *
 * recommendedPressure (áp suất khuyến cáo, theo trục):
 *   frontRecommendedPressure → FRONT_LEFT + FRONT_RIGHT
 *   rearRecommendedPressure  → REAR_LEFT + REAR_RIGHT
 *   spareRecommendedPressure → SPARE
 */
@Data
public class TireInputRequest {

    /** Size lốp thực tế trục trước — áp cho FRONT_LEFT + FRONT_RIGHT */
    private String frontTireSpecification;

    /** Size lốp thực tế trục sau — áp cho REAR_LEFT + REAR_RIGHT (SPARE = null) */
    private String rearTireSpecification;

    /** Size lốp khuyến cáo — nhập 1 lần, áp cho FRONT_LEFT, FRONT_RIGHT, REAR_LEFT, REAR_RIGHT */
    private String recommendedTireSize;

    private TireActualData frontLeft;
    private TireActualData frontRight;
    private TireActualData rearLeft;
    private TireActualData rearRight;
    private TireActualData spare;

    /** Áp suất khuyến cáo trục trước (kg/cm²) */
    private BigDecimal frontRecommendedPressure;

    /** Áp suất khuyến cáo trục sau (kg/cm²) — áp cho REAR_LEFT, REAR_RIGHT */
    private BigDecimal rearRecommendedPressure;

    /** Áp suất khuyến cáo bánh phụ (kg/cm²) */
    private BigDecimal spareRecommendedPressure;

    @Data
    public static class TireActualData {
        private BigDecimal treadDepth;
        private BigDecimal pressure;
        private PressureUnit pressureUnit;
    }
}
