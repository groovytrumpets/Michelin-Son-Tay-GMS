package com.g42.platform.gms.customermerge.infrastructure.liquibase;

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
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Changeset 039: gỡ MỌI ràng buộc duy nhất trên biển số xe.
 *
 * Một biển số được phép xuất hiện ở nhiều hồ sơ khách: vợ chồng / gia đình / công ty dùng
 * chung một xe thì mỗi người vẫn có hồ sơ riêng và đều tra được xe theo biển số.
 *
 * Phải dò tên index trong information_schema chứ không xoá theo tên cố định, vì mỗi cơ sở
 * dữ liệu mang một tên khác nhau: bản dựng từ Hibernate có "UKj5v3su3bdx4bvsk1t9dga4bsq",
 * bản đã chạy changeset 037 có thêm "uk_vehicle_plate_key". Xoá xong vẫn giữ index thường
 * để tra cứu theo biển số không bị quét cả bảng.
 */
public class DropVehiclePlateUniqueChange implements CustomTaskChange {

    private static final Set<String> PLATE_COLUMNS = Set.of("license_plate", "plate_key");

    private final List<String> droppedIndexes = new ArrayList<>();
    private final List<String> createdIndexes = new ArrayList<>();

    @Override
    public void execute(Database database) throws CustomChangeException {
        Connection con = ((JdbcConnection) database.getConnection()).getUnderlyingConnection();
        try {
            for (String indexName : findPlateUniqueIndexes(con)) {
                try (Statement st = con.createStatement()) {
                    st.executeUpdate("ALTER TABLE vehicle DROP INDEX `" + indexName + "`");
                }
                droppedIndexes.add(indexName);
            }
            ensureLookupIndex(con, "idx_vehicle_plate_key", "plate_key");
            ensureLookupIndex(con, "idx_vehicle_license_plate", "license_plate");
        } catch (SQLException e) {
            throw new CustomChangeException("Không gỡ được ràng buộc duy nhất của biển số xe: " + e.getMessage(), e);
        }
    }

    /** Các UNIQUE index (trừ khoá chính) có dính cột biển số. */
    private Set<String> findPlateUniqueIndexes(Connection con) throws SQLException {
        Set<String> names = new LinkedHashSet<>();
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT DISTINCT INDEX_NAME, COLUMN_NAME FROM information_schema.STATISTICS"
                        + " WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'vehicle'"
                        + " AND NON_UNIQUE = 0 AND INDEX_NAME <> 'PRIMARY'");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                if (PLATE_COLUMNS.contains(rs.getString(2).toLowerCase())) names.add(rs.getString(1));
            }
        }
        return names;
    }

    private void ensureLookupIndex(Connection con, String indexName, String column) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE()"
                        + " AND TABLE_NAME = 'vehicle' AND COLUMN_NAME = ?")) {
            ps.setString(1, column);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) return;
            }
        }
        try (Statement st = con.createStatement()) {
            st.executeUpdate("CREATE INDEX `" + indexName + "` ON vehicle (`" + column + "`)");
        }
        createdIndexes.add(indexName);
    }

    @Override
    public String getConfirmationMessage() {
        return "Biển số xe dùng chung được: đã gỡ " + droppedIndexes.size() + " ràng buộc duy nhất "
                + droppedIndexes + (createdIndexes.isEmpty() ? "" : ", thêm index tra cứu " + createdIndexes);
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
