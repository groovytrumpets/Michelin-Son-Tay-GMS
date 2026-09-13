package com.g42.platform.gms.warehouse.app.service.catalog;

import com.g42.platform.gms.common.util.Qty;
import com.g42.platform.gms.warehouse.domain.entity.CatalogItem;
import com.g42.platform.gms.warehouse.domain.enums.MeasurementType;
import com.g42.platform.gms.warehouse.domain.exception.WarehouseErrorCode;
import com.g42.platform.gms.warehouse.domain.exception.WarehouseException;
import com.g42.platform.gms.warehouse.infrastructure.entity.CatalogItemJpa;
import com.g42.platform.gms.warehouse.infrastructure.repository.CatalogItemJpaRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Luật số lượng theo cấu hình đo lường của sản phẩm.
 *
 * Mọi chỗ ghi số lượng (báo giá, nhập kho, xuất kho, hoàn hàng, sửa tồn tay) đều đi
 * qua đây để hàng đếm (cái, hộp) không bao giờ nhận số lẻ, còn hàng đo (lít, kg)
 * không nhận quá số chữ số thập phân đã khai báo.
 */
@Component
@RequiredArgsConstructor
public class ItemQuantityPolicy {

    public static final int MAX_DECIMAL_SCALE = 3;

    private final CatalogItemJpaRepo catalogItemJpaRepo;

    /** Cấu hình tối thiểu để kiểm tra số lượng — dựng được từ JPA hoặc domain. */
    public record Rules(String itemName, String unit, String measurementType, Integer decimalScale,
                        String packagingUnit, BigDecimal conversionFactor, Boolean sellByPackageOnly) {

        public static Rules of(CatalogItemJpa item) {
            return item == null ? null : new Rules(item.getItemName(), item.getUnit(), item.getMeasurementType(),
                    item.getDecimalScale(), item.getPackagingUnit(), item.getConversionFactor(), item.getSellByPackageOnly());
        }

        public static Rules of(CatalogItem item) {
            return item == null ? null : new Rules(item.getItemName(), item.getUnit(), item.getMeasurementType(),
                    item.getDecimalScale(), item.getPackagingUnit(), item.getConversionFactor(), item.getSellByPackageOnly());
        }

        public int scale() {
            if (measurementOf(measurementType) == MeasurementType.COUNT) return 0;
            return decimalScale == null ? 0 : Math.max(0, Math.min(MAX_DECIMAL_SCALE, decimalScale));
        }

        public BigDecimal factor() {
            return conversionFactor == null || conversionFactor.signum() <= 0 ? BigDecimal.ONE : conversionFactor;
        }
    }

    /** Dữ liệu cấu hình đã chuẩn hoá, dùng chung cho lúc tạo và sửa sản phẩm. */
    public record MeasurementConfig(
            String measurementType,
            Integer decimalScale,
            String packagingUnit,
            BigDecimal conversionFactor,
            Boolean sellByPackageOnly,
            Boolean tracksLot,
            Boolean tracksSerial) {
    }

    public static MeasurementType measurementOf(String raw) {
        if (raw == null || raw.isBlank()) return MeasurementType.COUNT;
        try {
            return MeasurementType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return MeasurementType.COUNT;
        }
    }

    public static boolean tracksSerial(CatalogItemJpa item) {
        return item != null && Boolean.TRUE.equals(item.getTracksSerial());
    }

    /**
     * Kiểm tra và chuẩn hoá cấu hình đo lường người dùng khai báo.
     * Giá trị null được thay bằng mặc định an toàn (hành vi cũ: đếm, hệ số 1, theo lô).
     */
    public MeasurementConfig normalizeConfig(MeasurementConfig input) {
        if (input.measurementType() != null && !input.measurementType().isBlank()) {
            try {
                MeasurementType.valueOf(input.measurementType().trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw invalidConfig("Kiểu đo lường không hợp lệ: " + input.measurementType());
            }
        }
        MeasurementType type = measurementOf(input.measurementType());

        int scale;
        if (type == MeasurementType.COUNT) {
            scale = 0;
        } else {
            scale = input.decimalScale() == null ? 2 : input.decimalScale();
            if (scale < 0 || scale > MAX_DECIMAL_SCALE) {
                throw invalidConfig("Số chữ số thập phân phải từ 0 đến " + MAX_DECIMAL_SCALE);
            }
        }

        String packagingUnit = input.packagingUnit() == null || input.packagingUnit().isBlank()
                ? null : input.packagingUnit().trim();
        BigDecimal factor = input.conversionFactor() == null ? BigDecimal.ONE : input.conversionFactor();
        if (factor.signum() <= 0) {
            throw invalidConfig("Hệ số quy đổi phải lớn hơn 0");
        }
        if (factor.stripTrailingZeros().scale() > MAX_DECIMAL_SCALE) {
            throw invalidConfig("Hệ số quy đổi chỉ được tối đa " + MAX_DECIMAL_SCALE + " chữ số thập phân");
        }
        if (packagingUnit == null) {
            factor = BigDecimal.ONE;
        }
        if (type == MeasurementType.COUNT && !isWhole(factor)) {
            throw invalidConfig("Sản phẩm đếm theo chiếc thì hệ số quy đổi phải là số nguyên (hộp 10 cái = 10)");
        }

        boolean sellByPackageOnly = Boolean.TRUE.equals(input.sellByPackageOnly());
        if (sellByPackageOnly && (packagingUnit == null || factor.compareTo(BigDecimal.ONE) <= 0)) {
            throw invalidConfig("Muốn chỉ bán nguyên " + (packagingUnit == null ? "gói" : packagingUnit)
                    + " thì phải khai báo đơn vị đóng gói và hệ số quy đổi lớn hơn 1");
        }

        boolean tracksSerial = Boolean.TRUE.equals(input.tracksSerial());
        if (tracksSerial && type != MeasurementType.COUNT) {
            throw invalidConfig("Chỉ sản phẩm đếm theo chiếc mới theo dõi được số serial; hàng đo lường (lít, kg) hãy theo dõi theo lô");
        }
        boolean tracksLot = input.tracksLot() == null || Boolean.TRUE.equals(input.tracksLot());

        return new MeasurementConfig(type.name(), scale, packagingUnit, Qty.normalize(factor),
                sellByPackageOnly, tracksLot, tracksSerial);
    }

    /** Số lượng bán ra / giữ hàng: đúng số chữ số lẻ và đúng bội số đóng gói. */
    public void validateSaleQuantity(Integer itemId, BigDecimal quantity, String label) {
        if (itemId == null) return;
        validateSaleQuantity(rulesOf(itemId), quantity, label);
    }

    public void validateSaleQuantity(Rules rules, BigDecimal quantity, String label) {
        validateStockQuantity(rules, quantity, label);
        if (rules == null || !Boolean.TRUE.equals(rules.sellByPackageOnly())) return;
        BigDecimal factor = rules.factor();
        if (factor.compareTo(BigDecimal.ONE) > 0 && quantity.remainder(factor).signum() != 0) {
            throw new WarehouseException(prefix(label, rules) + "chỉ bán nguyên "
                    + (rules.packagingUnit() != null ? rules.packagingUnit() : "gói")
                    + " — số lượng phải là bội số của " + Qty.text(factor)
                    + " (đang nhập " + Qty.text(quantity) + ")", WarehouseErrorCode.INVALID_QUANTITY);
        }
    }

    /** Số lượng tồn / nhập kho: dương và đúng số chữ số lẻ của sản phẩm. */
    public void validateStockQuantity(Integer itemId, BigDecimal quantity, String label) {
        if (itemId == null) return;
        validateStockQuantity(rulesOf(itemId), quantity, label);
    }

    public void validateStockQuantity(Rules rules, BigDecimal quantity, String label) {
        if (quantity == null || quantity.signum() <= 0) {
            throw new WarehouseException(prefix(label, rules) + "số lượng phải lớn hơn 0", WarehouseErrorCode.INVALID_QUANTITY);
        }
        validateScale(rules, quantity, label);
    }

    /** Chỉ kiểm tra số chữ số lẻ (cho phép 0) — dùng khi sửa tay tồn kho/lô. */
    public void validateScale(Rules rules, BigDecimal quantity, String label) {
        if (!exceedsScale(rules, quantity)) return;
        int allowed = rules.scale();
        String unit = rules.unit() != null && !rules.unit().isBlank() ? " " + rules.unit() : " chiếc";
        String message = allowed == 0
                ? prefix(label, rules) + "sản phẩm đếm theo" + unit
                    + " nên số lượng phải là số nguyên (đang nhập " + Qty.text(quantity) + ")"
                : prefix(label, rules) + "số lượng chỉ được tối đa " + allowed + " chữ số thập phân (đang nhập "
                    + Qty.text(quantity) + ")";
        throw new WarehouseException(message, WarehouseErrorCode.INVALID_QUANTITY);
    }

    /** true nếu số lượng có nhiều chữ số lẻ hơn cấu hình cho phép. */
    public static boolean exceedsScale(Rules rules, BigDecimal quantity) {
        return quantity != null && rules != null && quantity.stripTrailingZeros().scale() > rules.scale();
    }

    public Rules rulesOf(Integer itemId) {
        return itemId == null ? null : catalogItemJpaRepo.findById(itemId).map(Rules::of).orElse(null);
    }

    /** Quy đổi số lượng theo đơn vị nhập sang đơn vị tồn. */
    public static BigDecimal toStockQuantity(BigDecimal inputQuantity, BigDecimal factor) {
        BigDecimal f = factor == null || factor.signum() <= 0 ? BigDecimal.ONE : factor;
        return Qty.normalize(Qty.nz(inputQuantity).multiply(f));
    }

    public static boolean isWhole(BigDecimal value) {
        return value != null && value.stripTrailingZeros().scale() <= 0;
    }

    private static String prefix(String label, Rules rules) {
        String name = label != null && !label.isBlank() ? label
                : rules != null && rules.itemName() != null ? rules.itemName() : null;
        return name == null ? "" : name + ": ";
    }

    private static WarehouseException invalidConfig(String message) {
        return new WarehouseException(message, WarehouseErrorCode.INVALID_MEASUREMENT_CONFIG);
    }
}
