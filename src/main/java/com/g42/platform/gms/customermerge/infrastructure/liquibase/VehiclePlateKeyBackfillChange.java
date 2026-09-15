package com.g42.platform.gms.customermerge.infrastructure.liquibase;

import com.g42.platform.gms.customermerge.infrastructure.jdbc.MergeLogWriter;
import com.g42.platform.gms.customermerge.infrastructure.jdbc.RowSnapshots;
import com.g42.platform.gms.customermerge.infrastructure.jdbc.VehicleRowMerger;
import com.g42.platform.gms.vehicle.support.PlateKeys;
import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Changeset 037-5: điền vehicle.plate_key cho xe cũ, rồi gộp các dòng xe trùng biển số
 * mà KHÔNG gây mất thông tin — tức mọi dòng trong nhóm cùng một chủ, hoặc chưa có chủ.
 *
 * Nhóm trùng biển số giữa HAI chủ khác nhau thì để nguyên: hai hồ sơ khách đó có tên và
 * thông tin khác nhau, tự quyết giữ bên nào là đoán mò trên dữ liệu thật. Nhân viên xử lý
 * ở màn /customer-merge; changeset 037-6 sẽ tự thêm UNIQUE khi đã gộp hết.
 *
 * Chuẩn hoá bằng Java (PlateKeys) thay vì REGEXP_REPLACE của MySQL để khoá sinh ra ở đây
 * trùng khít với khoá ứng dụng tính lúc lưu xe.
 */
public class VehiclePlateKeyBackfillChange implements CustomTaskChange {

    private int backfilled;
    private int groupsMerged;
    private int groupsLeftForReview;

    @Override
    public void execute(Database database) throws CustomChangeException {
        Connection con = ((JdbcConnection) database.getConnection()).getUnderlyingConnection();
        try {
            backfill(con);
            mergeSameOwnerDuplicates(con);
        } catch (SQLException e) {
            throw new CustomChangeException("Không điền được plate_key / gộp xe trùng biển số: " + e.getMessage(), e);
        }
    }

    private void backfill(Connection con) throws SQLException {
        Map<Integer, String> keys = new LinkedHashMap<>();
        try (PreparedStatement ps = con.prepareStatement("SELECT vehicle_id, license_plate, plate_key FROM vehicle");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String key = PlateKeys.normalize(rs.getString(2));
                if (!Objects.equals(key, rs.getString(3))) keys.put(rs.getInt(1), key);
            }
        }
        try (PreparedStatement ps = con.prepareStatement("UPDATE vehicle SET plate_key = ? WHERE vehicle_id = ?")) {
            for (Map.Entry<Integer, String> entry : keys.entrySet()) {
                ps.setString(1, entry.getValue());
                ps.setInt(2, entry.getKey());
                ps.addBatch();
            }
            ps.executeBatch();
        }
        backfilled = keys.size();
    }

    private void mergeSameOwnerDuplicates(Connection con) throws SQLException {
        List<String> duplicatedKeys = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT plate_key FROM vehicle WHERE plate_key IS NOT NULL GROUP BY plate_key HAVING COUNT(*) > 1");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) duplicatedKeys.add(rs.getString(1));
        }

        for (String plateKey : duplicatedKeys) {
            List<Integer> vehicleIds = new ArrayList<>();
            List<Integer> ownerIds = new ArrayList<>();
            Integer ownedVehicleId = null;
            // Dòng giữ lại: ưu tiên dòng có chủ, rồi đến dòng cũ nhất
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT vehicle_id, customer_id FROM vehicle WHERE plate_key = ?"
                            + " ORDER BY customer_id IS NULL, vehicle_id")) {
                ps.setString(1, plateKey);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int vehicleId = rs.getInt(1);
                        int owner = rs.getInt(2);
                        boolean hasOwner = !rs.wasNull();
                        vehicleIds.add(vehicleId);
                        if (hasOwner) {
                            if (!ownerIds.contains(owner)) ownerIds.add(owner);
                            if (ownedVehicleId == null) ownedVehicleId = vehicleId;
                        }
                    }
                }
            }

            if (ownerIds.size() > 1) {
                groupsLeftForReview++;
                continue;
            }

            int keepId = ownedVehicleId != null ? ownedVehicleId : vehicleIds.get(0);
            List<Integer> drops = vehicleIds.stream().filter(id -> id != keepId).toList();
            String snapshot = RowSnapshots.toJson(Map.of(
                    "vehicles", RowSnapshots.read(con, "vehicle", "vehicle_id", vehicleIds)));

            VehicleRowMerger.Result result = VehicleRowMerger.merge(con, keepId, drops, null);
            String moved = MergeLogWriter.describe(result.rewrites());
            MergeLogWriter.insert(con, MergeLogWriter.TYPE_VEHICLE,
                    ownerIds.isEmpty() ? null : ownerIds.get(0), List.of(),
                    keepId, result.removedVehicleIds(), snapshot,
                    "Tự gộp khi nâng cấp (Liquibase 037-5): biển số " + plateKey + " có " + vehicleIds.size()
                            + " dòng xe cùng một chủ." + (moved.isEmpty() ? "" : " Đã chuyển: " + moved + "."),
                    null, null);
            groupsMerged++;
        }
    }

    @Override
    public String getConfirmationMessage() {
        return "plate_key: điền " + backfilled + " xe; gộp " + groupsMerged
                + " nhóm xe trùng biển số cùng chủ; còn " + groupsLeftForReview
                + " nhóm trùng biển số khác chủ cần gộp ở /customer-merge";
    }

    @Override
    public void setUp() {
    }

    @Override
    public void setFileOpener(ResourceAccessor resourceAccessor) {
    }

    @Override
    public ValidationErrors validate(Database database) {
        return new ValidationErrors();
    }
}
