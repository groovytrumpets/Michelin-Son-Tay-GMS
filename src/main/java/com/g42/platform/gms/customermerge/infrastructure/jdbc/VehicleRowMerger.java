package com.g42.platform.gms.customermerge.infrastructure.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Gộp nhiều dòng xe cùng biển số thành một: giữ lại một dòng, chuyển toàn bộ lịch sử
 * (phiếu, lịch hẹn, số km, sổ cũ...) của các dòng kia sang, rồi xoá các dòng thừa.
 *
 * Plain JDBC, chạy trong giao dịch của phía gọi — dùng chung cho changeset Liquibase 037-3
 * và màn gộp hồ sơ.
 */
public final class VehicleRowMerger {

    private VehicleRowMerger() {
    }

    public record Result(int keptVehicleId, List<Integer> removedVehicleIds,
                         List<ReferenceRewriter.RewriteResult> rewrites) {
    }

    /**
     * @param ownerCustomerId chủ xe sau khi gộp; null = giữ nguyên chủ của dòng được giữ
     */
    public static Result merge(Connection con, int keepVehicleId, Collection<Integer> dropVehicleIds,
                               Integer ownerCustomerId) throws SQLException {
        List<Integer> drops = dropVehicleIds.stream()
                .filter(id -> id != null && id != keepVehicleId).distinct().toList();
        if (drops.isEmpty()) {
            if (ownerCustomerId != null) setOwner(con, keepVehicleId, ownerCustomerId);
            return new Result(keepVehicleId, List.of(), List.of());
        }

        fillBlankDetails(con, keepVehicleId, drops);

        List<ReferenceRewriter.RewriteResult> rewrites = new ArrayList<>();
        for (ReferenceRewriter.RefColumn ref : ReferenceRewriter.discover(con, List.of("vehicle_id"), Set.of("vehicle"))) {
            ReferenceRewriter.RewriteResult result = ReferenceRewriter.rewrite(con, ref, keepVehicleId, drops);
            if (result.moved() > 0 || result.droppedAsDuplicate() > 0) rewrites.add(result);
        }

        try (PreparedStatement ps = con.prepareStatement(
                "DELETE FROM vehicle WHERE vehicle_id IN (" + ReferenceRewriter.placeholders(drops.size()) + ")")) {
            ReferenceRewriter.bind(ps, 1, drops);
            ps.executeUpdate();
        }

        if (ownerCustomerId != null) setOwner(con, keepVehicleId, ownerCustomerId);
        return new Result(keepVehicleId, drops, rewrites);
    }

    /** Dòng giữ lại thiếu hãng/dòng xe/năm sản xuất thì lấy từ dòng bị gộp đầu tiên có giá trị. */
    private static void fillBlankDetails(Connection con, int keepVehicleId, List<Integer> drops) throws SQLException {
        String brand = null;
        String model = null;
        Integer year = null;
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT brand, model, manufacture_year FROM vehicle WHERE vehicle_id IN ("
                        + ReferenceRewriter.placeholders(drops.size()) + ") ORDER BY vehicle_id")) {
            ReferenceRewriter.bind(ps, 1, drops);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (brand == null && !isBlank(rs.getString(1))) brand = rs.getString(1);
                    if (model == null && !isBlank(rs.getString(2))) model = rs.getString(2);
                    int y = rs.getInt(3);
                    if (year == null && !rs.wasNull()) year = y;
                }
            }
        }
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE vehicle SET"
                        + " brand = CASE WHEN brand IS NULL OR TRIM(brand) = '' THEN ? ELSE brand END,"
                        + " model = CASE WHEN model IS NULL OR TRIM(model) = '' THEN ? ELSE model END,"
                        + " manufacture_year = COALESCE(manufacture_year, ?)"
                        + " WHERE vehicle_id = ?")) {
            ps.setString(1, brand);
            ps.setString(2, model);
            ps.setObject(3, year);
            ps.setInt(4, keepVehicleId);
            ps.executeUpdate();
        }
    }

    private static void setOwner(Connection con, int vehicleId, int customerId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE vehicle SET customer_id = ? WHERE vehicle_id = ?")) {
            ps.setInt(1, customerId);
            ps.setInt(2, vehicleId);
            ps.executeUpdate();
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
