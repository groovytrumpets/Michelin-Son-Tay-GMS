package com.g42.platform.gms.customermerge.application.service;

import com.g42.platform.gms.customer.application.service.CustomerRankingService;
import com.g42.platform.gms.customer.domain.exception.CustomerErrorCode;
import com.g42.platform.gms.customer.domain.exception.CustomerException;
import com.g42.platform.gms.customermerge.api.dto.CustomerCompareDto;
import com.g42.platform.gms.customermerge.api.dto.CustomerMergeRequest;
import com.g42.platform.gms.customermerge.api.dto.DuplicateGroupDto;
import com.g42.platform.gms.customermerge.api.dto.MergeLogDto;
import com.g42.platform.gms.customermerge.api.dto.MergeResultDto;
import com.g42.platform.gms.customermerge.api.dto.VehicleMergeRequest;
import com.g42.platform.gms.customermerge.infrastructure.jdbc.MergeLogWriter;
import com.g42.platform.gms.customermerge.infrastructure.jdbc.ReferenceRewriter;
import com.g42.platform.gms.customermerge.infrastructure.jdbc.RowSnapshots;
import com.g42.platform.gms.customermerge.infrastructure.jdbc.VehicleRowMerger;
import com.g42.platform.gms.vehicle.support.PlateKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Gộp hồ sơ khách hàng trùng.
 *
 * Dùng cho hai tình huống: dữ liệu cũ bị tách đôi (người 2 số điện thoại thành 2 hồ sơ cùng
 * biển số), và nhân viên lỡ tạo trùng một khách. Gộp là thao tác không hoàn tác được bằng
 * nút bấm, nên mọi hồ sơ bị gộp đều được chụp nguyên văn vào customer_merge_log trước.
 *
 * Làm bằng JDBC thay vì JPA: phải chuyển tham chiếu trên hàng chục bảng, nhiều bảng không có
 * entity hay khoá ngoại (xem ReferenceRewriter), và cần kiểm soát đúng thứ tự cập nhật để
 * không vướng các ràng buộc duy nhất (phone, email, customer_code).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerMergeService {

    private static final int MAX_CUSTOMERS = 6;

    /** Bảng có customer_id nhưng xử lý riêng, không chuyển kiểu chung. */
    private static final Set<String> CUSTOMER_SPECIAL_TABLES = Set.of(
            "customer_profile", "customer_auth", "customer_points", "customer_phone",
            "customer_merge_log", "customer_duplicate_dismissal");
    private static final List<String> CUSTOMER_REF_COLUMNS = List.of("customer_id", "reporter_customer_id");

    /**
     * Các trường nhân viên được chọn giữ từ hồ sơ nào. Cột đi theo cụm (VD địa chỉ gồm cả
     * tỉnh/huyện/xã) được chọn cùng nhau để không ghép địa chỉ nửa nọ nửa kia.
     */
    private record FieldDef(String key, String label, List<String> columns, List<String> displayColumns) {
        FieldDef(String key, String label, List<String> columns) {
            this(key, label, columns, columns);
        }
    }

    private static final List<FieldDef> FIELDS = List.of(
            new FieldDef("fullName", "Họ tên", List.of("full_name")),
            new FieldDef("email", "Email", List.of("email")),
            new FieldDef("customerCode", "Mã khách hàng", List.of("customer_code")),
            new FieldDef("dob", "Ngày sinh", List.of("dob")),
            new FieldDef("gender", "Giới tính", List.of("gender")),
            new FieldDef("customerType", "Loại khách", List.of("customer_type", "is_dealer"), List.of("customer_type")),
            new FieldDef("company", "Doanh nghiệp", List.of("is_company", "company_name"), List.of("company_name")),
            new FieldDef("taxCode", "Mã số thuế", List.of("tax_code")),
            new FieldDef("address", "Địa chỉ",
                    List.of("address", "ward_id", "ward_name", "district_id", "district_name", "province_id", "province_name"),
                    List.of("address", "ward_name", "district_name", "province_name")),
            new FieldDef("identityCard", "CCCD/CMND", List.of("identity_card", "id_issue_date", "id_issue_place")),
            new FieldDef("customerGroup", "Nhóm khách hàng", List.of("customer_group_id")),
            new FieldDef("notificationChannel", "Kênh nhận thông báo", List.of("notification_channel")),
            new FieldDef("representative", "Người đại diện", List.of("representative_name", "rep_identity_card", "rep_position")),
            new FieldDef("contract", "Hợp đồng", List.of("contract_number", "contract_date")),
            new FieldDef("bankAccount", "Tài khoản ngân hàng", List.of("bank_account_info")),
            new FieldDef("location", "Toạ độ", List.of("latitude", "longitude")),
            new FieldDef("contact", "Người liên hệ khác", List.of("contact_name", "contact_phone", "contact_email", "contact_address")),
            new FieldDef("referrer", "Người giới thiệu", List.of("referrer_id")),
            new FieldDef("doNotContact", "Không liên hệ nữa", List.of("do_not_contact")),
            new FieldDef("avatar", "Ảnh đại diện", List.of("avatar")),
            new FieldDef("note", "Ghi chú", List.of("note"))
    );

    private final JdbcTemplate jdbc;
    private final CustomerRankingService rankingService;

    /* ============================ Gợi ý nhóm trùng ============================ */

    @Transactional(readOnly = true)
    public List<DuplicateGroupDto> listDuplicates() {
        Map<String, DuplicateGroupDto> groups = new LinkedHashMap<>();
        Map<String, LinkedHashMap<Integer, Boolean>> membersByPlate = new HashMap<>();

        for (Map<String, Object> row : jdbc.queryForList(
                "SELECT v.vehicle_id, v.license_plate, v.plate_key, v.brand, v.model, v.manufacture_year, v.customer_id"
                        + " FROM vehicle v JOIN (SELECT plate_key FROM vehicle WHERE plate_key IS NOT NULL"
                        + "   GROUP BY plate_key HAVING COUNT(*) > 1) d ON d.plate_key = v.plate_key"
                        + " ORDER BY v.plate_key, v.vehicle_id")) {
            String plateKey = (String) row.get("plate_key");
            DuplicateGroupDto group = groups.computeIfAbsent(plateKey,
                    k -> newGroup(k, DuplicateGroupDto.REASON_DUPLICATE_VEHICLE));
            addVehicle(group, row, "customer_id");
            Integer owner = toInt(row.get("customer_id"));
            if (owner != null) {
                // Cùng một khách mà có hai dòng xe cùng biển số là nhập trùng thật, không phải
                // xe dùng chung — đánh dấu để xếp lên đầu danh sách.
                if (membersByPlate.computeIfAbsent(plateKey, k -> new LinkedHashMap<>()).put(owner, true) != null) {
                    group.setReason(DuplicateGroupDto.REASON_SAME_OWNER_DUPLICATE);
                    group.setNeedsAttention(true);
                }
            }
        }

        try {
            for (Map<String, Object> row : jdbc.queryForList(
                    "SELECT DISTINCT v.vehicle_id, v.license_plate, v.plate_key, v.brand, v.model, v.manufacture_year,"
                            + " v.customer_id AS owner_id, lv.customer_id AS visit_customer_id"
                            + " FROM legacy_visit lv JOIN vehicle v ON v.vehicle_id = lv.vehicle_id"
                            + " WHERE v.plate_key IS NOT NULL AND v.customer_id IS NOT NULL"
                            + " AND lv.customer_id IS NOT NULL AND lv.customer_id <> v.customer_id")) {
                String plateKey = (String) row.get("plate_key");
                DuplicateGroupDto group = groups.computeIfAbsent(plateKey,
                        k -> newGroup(k, DuplicateGroupDto.REASON_SHARED_HISTORY));
                addVehicle(group, row, "owner_id");
                LinkedHashMap<Integer, Boolean> members = membersByPlate.computeIfAbsent(plateKey, k -> new LinkedHashMap<>());
                members.put(toInt(row.get("owner_id")), true);
                members.putIfAbsent(toInt(row.get("visit_customer_id")), false);
            }
        } catch (DataAccessException e) {
            log.warn("Skip shared legacy history duplicate detection: {}", e.getMessage());
        }

        Set<String> dismissed = new HashSet<>();
        try {
            dismissed.addAll(jdbc.queryForList("SELECT group_key FROM customer_duplicate_dismissal", String.class));
        } catch (DataAccessException e) {
            log.warn("Skip duplicate dismissals: {}", e.getMessage());
        }

        Set<Integer> allIds = membersByPlate.values().stream()
                .flatMap(m -> m.keySet().stream()).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Integer, DuplicateGroupDto.CustomerSummary> summaries = loadSummaries(allIds);

        List<DuplicateGroupDto> result = new ArrayList<>();
        for (DuplicateGroupDto group : groups.values()) {
            LinkedHashMap<Integer, Boolean> members = membersByPlate.getOrDefault(group.getPlateKey(), new LinkedHashMap<>());
            List<Integer> ids = members.keySet().stream().filter(Objects::nonNull).sorted().toList();
            group.setGroupKey(groupKey(group.getPlateKey(), ids));
            // Trùng biển số giờ là chuyện hợp lệ (xe dùng chung) nên nhóm nào cũng bỏ qua được
            if (dismissed.contains(group.getGroupKey())) continue;

            for (Map.Entry<Integer, Boolean> member : members.entrySet()) {
                DuplicateGroupDto.CustomerSummary base = summaries.get(member.getKey());
                if (base == null) continue;
                DuplicateGroupDto.CustomerSummary summary = copySummary(base);
                summary.setVehicleOwner(Boolean.TRUE.equals(member.getValue()));
                group.getCustomers().add(summary);
            }
            if (group.getCustomers().size() < 2 && !group.isNeedsAttention()) continue;
            result.add(group);
        }
        result.sort(Comparator.comparing((DuplicateGroupDto g) -> !g.isNeedsAttention())
                .thenComparing(DuplicateGroupDto::getPlateKey));
        return result;
    }

    @Transactional
    public void dismiss(String groupKey, String note, Integer staffId) {
        if (groupKey == null || groupKey.isBlank()) {
            throw invalid("Thiếu mã nhóm cần bỏ qua.");
        }
        jdbc.update("INSERT IGNORE INTO customer_duplicate_dismissal (group_key, note, dismissed_by_staff_id, dismissed_at)"
                + " VALUES (?, ?, ?, NOW())", groupKey.trim(), truncate(note, 500), staffId);
    }

    /* ================================ So sánh ================================= */

    @Transactional(readOnly = true)
    public CustomerCompareDto compare(List<Integer> customerIds) {
        List<Integer> ids = distinctIds(customerIds);
        if (ids.size() < 2) throw invalid("Cần chọn ít nhất 2 hồ sơ để so sánh.");
        if (ids.size() > MAX_CUSTOMERS) throw invalid("Chỉ so sánh tối đa " + MAX_CUSTOMERS + " hồ sơ một lần.");

        Map<Integer, Map<String, Object>> rows = loadProfileRows(ids);
        requireAllExist(ids, rows.keySet());

        Map<Integer, List<Map<String, Object>>> phones = groupBy(queryIn(
                "SELECT customer_id, phone, note FROM customer_phone WHERE customer_id IN (%s) ORDER BY customer_phone_id", ids));
        Map<Integer, Map<String, Object>> auths = firstBy(queryIn(
                "SELECT customer_id, status, pin_hash IS NOT NULL AS has_pin, last_login_at FROM customer_auth"
                        + " WHERE customer_id IN (%s) ORDER BY customer_auth_id", ids));
        Map<Integer, Map<String, Object>> points = firstBy(queryIn(
                "SELECT customer_id, total_points, lifetime_points FROM customer_points WHERE customer_id IN (%s)", ids));
        List<Map<String, Object>> vehicleRows = queryIn(
                "SELECT vehicle_id, license_plate, plate_key, brand, model, manufacture_year, customer_id FROM vehicle"
                        + " WHERE customer_id IN (%s) ORDER BY vehicle_id", ids);
        Map<Integer, List<Map<String, Object>>> vehicles = groupBy(vehicleRows);
        Map<Integer, Map<String, Integer>> counts = relatedCounts(ids);

        CustomerCompareDto dto = new CustomerCompareDto();
        for (Integer id : ids) {
            Map<String, Object> row = rows.get(id);
            CustomerCompareDto.Customer customer = new CustomerCompareDto.Customer();
            customer.setCustomerId(id);
            customer.setCustomerCode(str(row.get("customer_code")));
            customer.setFullName(str(row.get("full_name")));
            customer.setCreatedAt(str(row.get("created_at")));
            customer.setPrimaryPhone(str(row.get("phone")));
            for (Map<String, Object> phone : phones.getOrDefault(id, List.of())) {
                CustomerCompareDto.Phone p = new CustomerCompareDto.Phone();
                p.setPhone(str(phone.get("phone")));
                p.setNote(str(phone.get("note")));
                customer.getOtherPhones().add(p);
            }
            Map<String, Object> auth = auths.get(id);
            if (auth != null) {
                customer.setAccountStatus(str(auth.get("status")));
                customer.setHasPin(truthy(auth.get("has_pin")));
                customer.setLastLoginAt(str(auth.get("last_login_at")));
            }
            Map<String, Object> pts = points.get(id);
            if (pts != null) {
                customer.setTotalPoints(intOr0(pts.get("total_points")));
                customer.setLifetimePoints(intOr0(pts.get("lifetime_points")));
            }
            for (Map<String, Object> v : vehicles.getOrDefault(id, List.of())) {
                customer.getVehicles().add(vehicleRow(v, "customer_id"));
            }
            customer.setRelatedCounts(counts.getOrDefault(id, new LinkedHashMap<>()));
            dto.getCustomers().add(customer);
        }

        Map<Integer, String> groupNames = lookupNames("SELECT group_id AS id, name FROM customer_group WHERE group_id IN (%s)",
                rows.values().stream().map(r -> toInt(r.get("customer_group_id"))).filter(Objects::nonNull).toList());
        Map<Integer, String> referrerNames = lookupNames(
                "SELECT customer_id AS id, CONCAT(COALESCE(full_name, ''), ' ', COALESCE(phone, '')) AS name FROM customer_profile WHERE customer_id IN (%s)",
                rows.values().stream().map(r -> toInt(r.get("referrer_id"))).filter(Objects::nonNull).toList());

        for (FieldDef def : FIELDS) {
            if (def.columns().stream().noneMatch(rows.get(ids.get(0))::containsKey)) continue;
            CustomerCompareDto.Field field = new CustomerCompareDto.Field();
            field.setKey(def.key());
            field.setLabel(def.label());
            Set<String> distinct = new LinkedHashSet<>();
            for (Integer id : ids) {
                String display = display(def, rows.get(id), groupNames, referrerNames);
                field.getValues().put(String.valueOf(id), display);
                if (display != null) {
                    distinct.add(display.trim().toLowerCase());
                    if (field.getDefaultSourceCustomerId() == null) field.setDefaultSourceCustomerId(id);
                }
            }
            field.setDiffers(distinct.size() > 1);
            if (field.getDefaultSourceCustomerId() == null) field.setDefaultSourceCustomerId(ids.get(0));
            dto.getFields().add(field);
        }

        vehicleRows.stream()
                .filter(v -> v.get("plate_key") != null)
                .collect(Collectors.groupingBy(v -> (String) v.get("plate_key"), LinkedHashMap::new, Collectors.toList()))
                .forEach((plateKey, list) -> {
                    if (list.size() < 2) return;
                    CustomerCompareDto.PlateGroup group = new CustomerCompareDto.PlateGroup();
                    group.setPlateKey(plateKey);
                    list.forEach(v -> group.getVehicles().add(vehicleRow(v, "customer_id")));
                    dto.getDuplicatePlates().add(group);
                });
        return dto;
    }

    /* ================================= Gộp khách ================================ */

    @Transactional
    public MergeResultDto merge(CustomerMergeRequest request, Integer staffId) {
        Integer keepId = request.getKeepCustomerId();
        List<Integer> mergeIds = distinctIds(request.getMergeCustomerIds());
        if (keepId == null) throw invalid("Chưa chọn hồ sơ được giữ lại.");
        mergeIds = mergeIds.stream().filter(id -> !id.equals(keepId)).toList();
        if (mergeIds.isEmpty()) throw invalid("Chưa chọn hồ sơ nào để gộp vào.");
        List<Integer> all = new ArrayList<>();
        all.add(keepId);
        all.addAll(mergeIds);
        if (all.size() > MAX_CUSTOMERS) throw invalid("Chỉ gộp tối đa " + MAX_CUSTOMERS + " hồ sơ một lần.");

        // Khoá các hồ sơ trong suốt giao dịch để hai người không gộp chồng lên nhau
        requireAllExist(all, new HashSet<>(queryIn(
                "SELECT customer_id FROM customer_profile WHERE customer_id IN (%s) FOR UPDATE", all)
                .stream().map(r -> toInt(r.get("customer_id"))).toList()));

        Map<Integer, Map<String, Object>> rows = loadProfileRows(all);
        List<Map<String, Object>> phoneRows = queryIn(
                "SELECT customer_phone_id, customer_id, phone, note FROM customer_phone WHERE customer_id IN (%s)"
                        + " ORDER BY customer_phone_id", all);
        List<Map<String, Object>> authRows = queryIn("SELECT * FROM customer_auth WHERE customer_id IN (%s)", all);
        List<Map<String, Object>> pointRows = queryIn("SELECT * FROM customer_points WHERE customer_id IN (%s)", all);
        List<Map<String, Object>> vehicleRows = queryIn("SELECT * FROM vehicle WHERE customer_id IN (%s)", all);

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("keepCustomerId", keepId);
        snapshot.put("profiles", all.stream().map(id -> RowSnapshots.plainRow(rows.get(id))).toList());
        snapshot.put("phones", phoneRows.stream().map(r -> RowSnapshots.plainRow(r)).toList());
        snapshot.put("auths", authRows.stream().map(r -> RowSnapshots.plainRow(r, "pin_hash")).toList());
        snapshot.put("points", pointRows.stream().map(r -> RowSnapshots.plainRow(r)).toList());
        snapshot.put("vehicles", vehicleRows.stream().map(r -> RowSnapshots.plainRow(r)).toList());
        snapshot.put("fieldSources", request.getFieldSources());

        // ---- 1. Giá trị cuối cùng của hồ sơ giữ lại ----
        Map<String, Object> keepRow = rows.get(keepId);
        Map<String, Object> updates = new LinkedHashMap<>();
        boolean combineNotes = request.getCombineNotes() == null || request.getCombineNotes();
        Map<String, Integer> sources = request.getFieldSources() == null ? Map.of() : request.getFieldSources();
        for (FieldDef def : FIELDS) {
            if (def.key().equals("note") && combineNotes) {
                if (keepRow.containsKey("note")) updates.put("note", combinedNotes(all, rows));
                continue;
            }
            Integer source = sources.get(def.key());
            if (source != null && !all.contains(source)) {
                throw invalid("Trường \"" + def.label() + "\" chọn lấy từ hồ sơ " + source + " không nằm trong nhóm gộp.");
            }
            if (source == null) source = defaultSource(def, all, rows);
            for (String column : def.columns()) {
                if (keepRow.containsKey(column)) updates.put(column, rows.get(source).get(column));
            }
        }
        Integer referrer = toInt(updates.get("referrer_id"));
        if (referrer != null && all.contains(referrer)) updates.put("referrer_id", null);
        if (keepRow.containsKey("created_at")) updates.put("created_at", earliest(all, rows, "created_at"));
        if (keepRow.containsKey("first_booking_at")) updates.put("first_booking_at", earliest(all, rows, "first_booking_at"));

        // ---- 2. Số điện thoại: giữ TẤT CẢ, chỉ chọn số chính ----
        LinkedHashMap<String, String> phoneNotes = new LinkedHashMap<>();
        for (Integer id : all) {
            String primary = str(rows.get(id).get("phone"));
            if (primary != null) {
                phoneNotes.putIfAbsent(primary.trim(), id.equals(keepId) ? "Số chính cũ" : "Số chính của hồ sơ " + codeOf(rows.get(id)));
            }
            for (Map<String, Object> phone : phoneRows) {
                if (id.equals(toInt(phone.get("customer_id"))) && str(phone.get("phone")) != null) {
                    phoneNotes.putIfAbsent(str(phone.get("phone")).trim(), str(phone.get("note")));
                }
            }
        }
        String primaryPhone = choosePrimaryPhone(request.getPrimaryPhone(), keepRow, phoneNotes);
        updates.put("phone", primaryPhone);

        // ---- 3. Ghi: gỡ các giá trị duy nhất ở hồ sơ bị gộp trước, rồi mới ghi vào hồ sơ giữ lại ----
        jdbc.update("UPDATE customer_profile SET phone = NULL, email = NULL, customer_code = NULL WHERE customer_id IN ("
                + in(mergeIds) + ")", mergeIds.toArray());
        jdbc.update("DELETE FROM customer_phone WHERE customer_id IN (" + in(all) + ")", all.toArray());

        List<Object> args = new ArrayList<>(updates.values());
        args.add(keepId);
        jdbc.update("UPDATE customer_profile SET "
                + updates.keySet().stream().map(c -> ReferenceRewriter.quote(c) + " = ?").collect(Collectors.joining(", "))
                + " WHERE customer_id = ?", args.toArray());

        for (Map.Entry<String, String> phone : phoneNotes.entrySet()) {
            if (phone.getKey().equals(primaryPhone)) continue;
            jdbc.update("INSERT INTO customer_phone (customer_id, phone, note, created_at) VALUES (?, ?, ?, NOW())",
                    keepId, phone.getKey(), truncate(phone.getValue(), 100));
        }

        // ---- 4. Tài khoản đăng nhập: chỉ giữ một ----
        Integer accountFrom = request.getAccountFromCustomerId();
        if (accountFrom != null && !all.contains(accountFrom)) {
            throw invalid("Tài khoản chọn giữ lại không thuộc nhóm hồ sơ đang gộp.");
        }
        Map<String, Object> keptAuth = chooseAuth(authRows, accountFrom, keepId);
        if (keptAuth != null) {
            Object keptAuthId = keptAuth.get("customer_auth_id");
            jdbc.update("DELETE FROM customer_auth WHERE customer_id IN (" + in(all) + ") AND customer_auth_id <> ?",
                    concat(all, keptAuthId));
            jdbc.update("UPDATE customer_auth SET customer_id = ? WHERE customer_auth_id = ?", keepId, keptAuthId);
        }

        // ---- 5. Điểm tích luỹ: cộng dồn ----
        int mergedPoints = mergePoints(keepId, mergeIds, pointRows);

        // ---- 6. Người được giới thiệu bởi hồ sơ bị gộp → trỏ về hồ sơ giữ lại ----
        jdbc.update("UPDATE customer_profile SET referrer_id = ? WHERE referrer_id IN (" + in(mergeIds) + ")",
                concat(List.of(keepId), mergeIds.toArray()));
        jdbc.update("UPDATE customer_profile SET referrer_id = NULL WHERE customer_id = ? AND referrer_id = ?", keepId, keepId);

        // ---- 7. Mọi bảng còn lại có customer_id (phiếu, lịch hẹn, xe, sổ cũ, khuyến mãi...) ----
        final List<Integer> mergeIdsFinal = mergeIds;
        List<ReferenceRewriter.RewriteResult> rewrites = jdbc.execute((ConnectionCallback<List<ReferenceRewriter.RewriteResult>>) con -> {
            List<ReferenceRewriter.RewriteResult> results = new ArrayList<>();
            for (ReferenceRewriter.RefColumn ref : ReferenceRewriter.discover(con, CUSTOMER_REF_COLUMNS, CUSTOMER_SPECIAL_TABLES)) {
                ReferenceRewriter.RewriteResult result = ReferenceRewriter.rewrite(con, ref, keepId, mergeIdsFinal);
                if (result.moved() > 0 || result.droppedAsDuplicate() > 0) results.add(result);
            }
            return results;
        });

        // ---- 8. Xe cùng biển số giờ đã về chung một chủ → gộp thành một dòng ----
        Map<String, Integer> keepVehicleIds = request.getKeepVehicleIds() == null ? Map.of() : request.getKeepVehicleIds();
        List<Integer> removedVehicles = new ArrayList<>();
        List<ReferenceRewriter.RewriteResult> vehicleRewrites = new ArrayList<>();
        Map<String, List<Integer>> plates = new LinkedHashMap<>();
        for (Map<String, Object> v : jdbc.queryForList(
                "SELECT vehicle_id, plate_key FROM vehicle WHERE customer_id = ? AND plate_key IS NOT NULL ORDER BY vehicle_id", keepId)) {
            plates.computeIfAbsent((String) v.get("plate_key"), k -> new ArrayList<>()).add(toInt(v.get("vehicle_id")));
        }
        Set<Integer> keepOwnVehicles = vehicleRows.stream()
                .filter(v -> keepId.equals(toInt(v.get("customer_id"))))
                .map(v -> toInt(v.get("vehicle_id"))).collect(Collectors.toSet());
        for (Map.Entry<String, List<Integer>> plate : plates.entrySet()) {
            List<Integer> vehicleIds = plate.getValue();
            if (vehicleIds.size() < 2) continue;
            Integer chosen = keepVehicleIds.get(plate.getKey());
            if (chosen != null && !vehicleIds.contains(chosen)) {
                throw invalid("Xe chọn giữ cho biển số " + plate.getKey() + " không thuộc nhóm xe trùng.");
            }
            if (chosen == null) {
                chosen = vehicleIds.stream().filter(keepOwnVehicles::contains).findFirst().orElse(vehicleIds.get(0));
            }
            final int keepVehicle = chosen;
            VehicleRowMerger.Result result = jdbc.execute((ConnectionCallback<VehicleRowMerger.Result>) con ->
                    VehicleRowMerger.merge(con, keepVehicle, vehicleIds, null));
            removedVehicles.addAll(result.removedVehicleIds());
            vehicleRewrites.addAll(result.rewrites());
        }

        // ---- 9. Xoá hồ sơ bị gộp ----
        try {
            jdbc.update("DELETE FROM customer_profile WHERE customer_id IN (" + in(mergeIds) + ")", mergeIds.toArray());
        } catch (DataIntegrityViolationException e) {
            throw invalid("Không xoá được hồ sơ bị gộp vì còn dữ liệu trỏ tới mà hệ thống chưa biết cách chuyển: "
                    + rootMessage(e) + ". Chưa có gì bị thay đổi.");
        }

        if (mergedPoints > 0 || pointRows.stream().anyMatch(r -> !keepId.equals(toInt(r.get("customer_id"))))) {
            rankingService.adjustPoints(keepId, 0, "Gộp hồ sơ " + mergeIds.stream()
                    .map(id -> codeOf(rows.get(id))).collect(Collectors.joining(", ")) + " (+" + mergedPoints + " điểm)");
        }

        // ---- 10. Nhật ký ----
        StringBuilder summary = new StringBuilder("Gộp ")
                .append(mergeIds.stream().map(id -> describeCustomer(rows.get(id))).collect(Collectors.joining(", ")))
                .append(" vào ").append(describeCustomer(keepRow)).append(".");
        String moved = MergeLogWriter.describe(rewrites);
        if (!moved.isEmpty()) summary.append(" Đã chuyển: ").append(moved).append(".");
        if (phoneNotes.size() > 1) summary.append(" Số điện thoại: ").append(phoneNotes.size()).append(" số, số chính ").append(primaryPhone).append(".");
        if (mergedPoints > 0) summary.append(" Cộng ").append(mergedPoints).append(" điểm.");
        if (!removedVehicles.isEmpty()) {
            summary.append(" Gộp ").append(removedVehicles.size()).append(" dòng xe trùng biển số");
            String vehicleMoved = MergeLogWriter.describe(vehicleRewrites);
            summary.append(vehicleMoved.isEmpty() ? "." : " (chuyển " + vehicleMoved + ").");
        }

        int logId = jdbc.execute((ConnectionCallback<Integer>) con -> MergeLogWriter.insert(con,
                MergeLogWriter.TYPE_CUSTOMER, keepId, mergeIdsFinal, null, removedVehicles,
                RowSnapshots.toJson(snapshot), summary.toString(), request.getNote(), staffId));
        log.info("Customer merge #{}: {}", logId, summary);

        MergeResultDto result = new MergeResultDto();
        result.setMergeLogId(logId);
        result.setKeptCustomerId(keepId);
        result.setRemovedCustomerIds(mergeIds);
        result.setRemovedVehicleIds(removedVehicles);
        result.setSummary(summary.toString());
        return result;
    }

    /* ============================ Chỉ gộp xe (khác người) ============================ */

    @Transactional
    public MergeResultDto mergeVehicles(VehicleMergeRequest request, Integer staffId) {
        String plateKey = PlateKeys.normalize(request.getPlateKey());
        if (plateKey == null) throw invalid("Thiếu biển số cần gộp.");
        List<Map<String, Object>> vehicles = jdbc.queryForList(
                "SELECT * FROM vehicle WHERE plate_key = ? ORDER BY vehicle_id FOR UPDATE", plateKey);
        if (vehicles.size() < 2) throw invalid("Biển số " + plateKey + " không còn nhiều dòng xe để gộp.");
        List<Integer> vehicleIds = vehicles.stream().map(v -> toInt(v.get("vehicle_id"))).toList();
        Integer keepVehicleId = request.getKeepVehicleId() != null ? request.getKeepVehicleId() : vehicleIds.get(0);
        if (!vehicleIds.contains(keepVehicleId)) throw invalid("Xe chọn giữ lại không mang biển số " + plateKey + ".");

        Integer ownerId = request.getOwnerCustomerId();
        if (ownerId != null) {
            Integer exists = jdbc.queryForObject("SELECT COUNT(*) FROM customer_profile WHERE customer_id = ?", Integer.class, ownerId);
            if (exists == null || exists == 0) throw invalid("Không tìm thấy khách hàng " + ownerId + " để giao xe.");
        }

        String snapshot = RowSnapshots.toJson(Map.of("vehicles", vehicles.stream().map(r -> RowSnapshots.plainRow(r)).toList()));
        VehicleRowMerger.Result merged = jdbc.execute((ConnectionCallback<VehicleRowMerger.Result>) con ->
                VehicleRowMerger.merge(con, keepVehicleId, vehicleIds, ownerId));

        String moved = MergeLogWriter.describe(merged.rewrites());
        String summary = "Gộp " + vehicleIds.size() + " dòng xe biển số " + plateKey + " thành một (xe #" + keepVehicleId + ")"
                + (ownerId != null ? ", giao cho khách #" + ownerId : "") + "; các hồ sơ khách giữ nguyên."
                + (moved.isEmpty() ? "" : " Đã chuyển: " + moved + ".");
        Integer finalOwner = ownerId != null ? ownerId : vehicles.stream()
                .filter(v -> keepVehicleId.equals(toInt(v.get("vehicle_id"))))
                .map(v -> toInt(v.get("customer_id"))).findFirst().orElse(null);
        int logId = jdbc.execute((ConnectionCallback<Integer>) con -> MergeLogWriter.insert(con,
                MergeLogWriter.TYPE_VEHICLE, finalOwner, List.of(), keepVehicleId, merged.removedVehicleIds(),
                snapshot, summary, request.getNote(), staffId));

        MergeResultDto result = new MergeResultDto();
        result.setMergeLogId(logId);
        result.setKeptCustomerId(finalOwner);
        result.setKeptVehicleId(keepVehicleId);
        result.setRemovedVehicleIds(merged.removedVehicleIds());
        result.setSummary(summary);
        return result;
    }

    /* ================================= Lịch sử ================================= */

    @Transactional(readOnly = true)
    public List<MergeLogDto> history(int limit) {
        int size = Math.min(Math.max(limit, 1), 200);
        return jdbc.query(
                "SELECT l.merge_log_id, l.merge_type, l.kept_customer_id, l.merged_customer_ids, l.kept_vehicle_id,"
                        + " l.merged_vehicle_ids, l.summary, l.note, l.merged_by_staff_id, l.merged_at,"
                        + " cp.full_name AS kept_name, cp.customer_code AS kept_code, sp.full_name AS staff_name"
                        + " FROM customer_merge_log l"
                        + " LEFT JOIN customer_profile cp ON cp.customer_id = l.kept_customer_id"
                        + " LEFT JOIN staff_profile sp ON sp.staff_id = l.merged_by_staff_id"
                        + " ORDER BY l.merge_log_id DESC LIMIT ?",
                (rs, i) -> {
                    MergeLogDto dto = new MergeLogDto();
                    dto.setMergeLogId(rs.getInt("merge_log_id"));
                    dto.setMergeType(rs.getString("merge_type"));
                    dto.setKeptCustomerId((Integer) rs.getObject("kept_customer_id", Integer.class));
                    dto.setKeptCustomerName(rs.getString("kept_name"));
                    dto.setKeptCustomerCode(rs.getString("kept_code"));
                    dto.setMergedCustomerIds(rs.getString("merged_customer_ids"));
                    dto.setKeptVehicleId((Integer) rs.getObject("kept_vehicle_id", Integer.class));
                    dto.setMergedVehicleIds(rs.getString("merged_vehicle_ids"));
                    dto.setSummary(rs.getString("summary"));
                    dto.setNote(rs.getString("note"));
                    dto.setMergedByStaffId((Integer) rs.getObject("merged_by_staff_id", Integer.class));
                    dto.setMergedByStaffName(rs.getString("staff_name"));
                    dto.setMergedAt(str(rs.getObject("merged_at")));
                    return dto;
                }, size);
    }

    /* ================================= Nội bộ ================================= */

    private DuplicateGroupDto newGroup(String plateKey, String reason) {
        DuplicateGroupDto group = new DuplicateGroupDto();
        group.setPlateKey(plateKey);
        group.setReason(reason);
        group.setNeedsAttention(DuplicateGroupDto.REASON_SAME_OWNER_DUPLICATE.equals(reason));
        return group;
    }

    private void addVehicle(DuplicateGroupDto group, Map<String, Object> row, String ownerColumn) {
        Integer vehicleId = toInt(row.get("vehicle_id"));
        if (group.getVehicles().stream().anyMatch(v -> v.getVehicleId().equals(vehicleId))) return;
        group.getVehicles().add(vehicleRow(row, ownerColumn));
        String plate = str(row.get("license_plate"));
        if (plate != null && !group.getPlates().contains(plate)) group.getPlates().add(plate);
    }

    private DuplicateGroupDto.VehicleRow vehicleRow(Map<String, Object> row, String ownerColumn) {
        DuplicateGroupDto.VehicleRow v = new DuplicateGroupDto.VehicleRow();
        v.setVehicleId(toInt(row.get("vehicle_id")));
        v.setLicensePlate(str(row.get("license_plate")));
        v.setBrand(str(row.get("brand")));
        v.setModel(str(row.get("model")));
        v.setManufactureYear(toInt(row.get("manufacture_year")));
        v.setOwnerCustomerId(toInt(row.get(ownerColumn)));
        return v;
    }

    private Map<Integer, DuplicateGroupDto.CustomerSummary> loadSummaries(Collection<Integer> ids) {
        Map<Integer, DuplicateGroupDto.CustomerSummary> result = new HashMap<>();
        if (ids.isEmpty()) return result;
        List<Integer> list = new ArrayList<>(ids);
        for (Map<String, Object> row : queryIn("SELECT * FROM customer_profile WHERE customer_id IN (%s)", list)) {
            DuplicateGroupDto.CustomerSummary s = new DuplicateGroupDto.CustomerSummary();
            s.setCustomerId(toInt(row.get("customer_id")));
            s.setCustomerCode(str(row.get("customer_code")));
            s.setFullName(str(row.get("full_name")));
            s.setPhone(str(row.get("phone")));
            s.setEmail(str(row.get("email")));
            s.setAddress(joinNonBlank(row, List.of("address", "ward_name", "district_name", "province_name")));
            s.setCreatedAt(str(row.get("created_at")));
            result.put(s.getCustomerId(), s);
        }
        for (Map<String, Object> row : queryIn("SELECT customer_id, phone FROM customer_phone WHERE customer_id IN (%s) ORDER BY customer_phone_id", list)) {
            DuplicateGroupDto.CustomerSummary s = result.get(toInt(row.get("customer_id")));
            if (s != null) s.getOtherPhones().add(str(row.get("phone")));
        }
        for (Map<String, Object> row : queryIn("SELECT customer_id, status FROM customer_auth WHERE customer_id IN (%s)", list)) {
            DuplicateGroupDto.CustomerSummary s = result.get(toInt(row.get("customer_id")));
            if (s != null && s.getAccountStatus() == null) s.setAccountStatus(str(row.get("status")));
        }
        countInto(result, "service_ticket", list, DuplicateGroupDto.CustomerSummary::setServiceTicketCount);
        countInto(result, "booking", list, DuplicateGroupDto.CustomerSummary::setBookingCount);
        countInto(result, "legacy_visit", list, DuplicateGroupDto.CustomerSummary::setLegacyVisitCount);
        return result;
    }

    private void countInto(Map<Integer, DuplicateGroupDto.CustomerSummary> summaries, String table, List<Integer> ids,
                           java.util.function.ObjIntConsumer<DuplicateGroupDto.CustomerSummary> setter) {
        try {
            for (Map<String, Object> row : queryIn("SELECT customer_id, COUNT(*) AS cnt FROM " + table
                    + " WHERE customer_id IN (%s) GROUP BY customer_id", ids)) {
                DuplicateGroupDto.CustomerSummary s = summaries.get(toInt(row.get("customer_id")));
                if (s != null) setter.accept(s, intOr0(row.get("cnt")));
            }
        } catch (DataAccessException e) {
            log.warn("Skip count on {}: {}", table, e.getMessage());
        }
    }

    private static DuplicateGroupDto.CustomerSummary copySummary(DuplicateGroupDto.CustomerSummary base) {
        DuplicateGroupDto.CustomerSummary s = new DuplicateGroupDto.CustomerSummary();
        s.setCustomerId(base.getCustomerId());
        s.setCustomerCode(base.getCustomerCode());
        s.setFullName(base.getFullName());
        s.setPhone(base.getPhone());
        s.setOtherPhones(new ArrayList<>(base.getOtherPhones()));
        s.setEmail(base.getEmail());
        s.setAddress(base.getAddress());
        s.setCreatedAt(base.getCreatedAt());
        s.setAccountStatus(base.getAccountStatus());
        s.setServiceTicketCount(base.getServiceTicketCount());
        s.setBookingCount(base.getBookingCount());
        s.setLegacyVisitCount(base.getLegacyVisitCount());
        return s;
    }

    /** Số bản ghi mỗi hồ sơ đang giữ, theo từng loại — để nhân viên thấy gộp sẽ chuyển những gì. */
    private Map<Integer, Map<String, Integer>> relatedCounts(List<Integer> ids) {
        return jdbc.execute((ConnectionCallback<Map<Integer, Map<String, Integer>>>) con -> {
            Map<Integer, Map<String, Integer>> result = new HashMap<>();
            List<ReferenceRewriter.RefColumn> refs = ReferenceRewriter.discover(con, CUSTOMER_REF_COLUMNS, CUSTOMER_SPECIAL_TABLES);
            for (Integer id : ids) {
                Map<String, Integer> counts = new LinkedHashMap<>();
                for (ReferenceRewriter.RefColumn ref : refs) {
                    int count = ReferenceRewriter.count(con, ref, List.of(id));
                    if (count > 0) counts.merge(MergeLogWriter.labelOf(ref.table()), count, Integer::sum);
                }
                result.put(id, counts);
            }
            return result;
        });
    }

    private Map<Integer, Map<String, Object>> loadProfileRows(List<Integer> ids) {
        Map<Integer, Map<String, Object>> rows = new LinkedHashMap<>();
        for (Map<String, Object> row : queryIn("SELECT * FROM customer_profile WHERE customer_id IN (%s)", ids)) {
            rows.put(toInt(row.get("customer_id")), row);
        }
        return rows;
    }

    private List<Map<String, Object>> queryIn(String sqlWithPlaceholder, List<Integer> ids) {
        if (ids.isEmpty()) return List.of();
        return jdbc.queryForList(String.format(sqlWithPlaceholder, in(ids)), ids.toArray());
    }

    private Map<Integer, String> lookupNames(String sqlWithPlaceholder, List<Integer> ids) {
        Map<Integer, String> names = new HashMap<>();
        if (ids.isEmpty()) return names;
        try {
            for (Map<String, Object> row : queryIn(sqlWithPlaceholder, ids.stream().distinct().toList())) {
                names.put(toInt(row.get("id")), str(row.get("name")));
            }
        } catch (DataAccessException e) {
            log.warn("Skip name lookup: {}", e.getMessage());
        }
        return names;
    }

    private static Map<Integer, List<Map<String, Object>>> groupBy(List<Map<String, Object>> rows) {
        Map<Integer, List<Map<String, Object>>> map = new HashMap<>();
        for (Map<String, Object> row : rows) {
            map.computeIfAbsent(toInt(row.get("customer_id")), k -> new ArrayList<>()).add(row);
        }
        return map;
    }

    private static Map<Integer, Map<String, Object>> firstBy(List<Map<String, Object>> rows) {
        Map<Integer, Map<String, Object>> map = new HashMap<>();
        for (Map<String, Object> row : rows) map.putIfAbsent(toInt(row.get("customer_id")), row);
        return map;
    }

    private String display(FieldDef def, Map<String, Object> row, Map<Integer, String> groupNames,
                           Map<Integer, String> referrerNames) {
        if (def.key().equals("customerGroup")) {
            Integer groupId = toInt(row.get("customer_group_id"));
            return groupId == null ? null : groupNames.getOrDefault(groupId, "Nhóm #" + groupId);
        }
        if (def.key().equals("referrer")) {
            Integer referrerId = toInt(row.get("referrer_id"));
            return referrerId == null ? null : referrerNames.getOrDefault(referrerId, "Khách #" + referrerId).trim();
        }
        return joinNonBlank(row, def.displayColumns());
    }

    private static String joinNonBlank(Map<String, Object> row, List<String> columns) {
        List<String> parts = new ArrayList<>();
        for (String column : columns) {
            Object value = row.get(column);
            if (value instanceof Boolean b) {
                if (b) parts.add("Có");
                continue;
            }
            String text = str(value);
            if (text != null) parts.add(text);
        }
        return parts.isEmpty() ? null : String.join(" · ", parts);
    }

    /** Mặc định: hồ sơ giữ lại nếu có giá trị, không thì hồ sơ đầu tiên có giá trị. */
    private Integer defaultSource(FieldDef def, List<Integer> all, Map<Integer, Map<String, Object>> rows) {
        for (Integer id : all) {
            if (joinNonBlank(rows.get(id), def.columns()) != null) return id;
        }
        return all.get(0);
    }

    private static String combinedNotes(List<Integer> all, Map<Integer, Map<String, Object>> rows) {
        LinkedHashSet<String> notes = new LinkedHashSet<>();
        for (Integer id : all) {
            String note = str(rows.get(id).get("note"));
            if (note != null) notes.add(note.trim());
        }
        return notes.isEmpty() ? null : String.join("\n---\n", notes);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object earliest(List<Integer> all, Map<Integer, Map<String, Object>> rows, String column) {
        Object best = null;
        for (Integer id : all) {
            Object value = rows.get(id).get(column);
            if (value == null) continue;
            if (best == null || (value instanceof Comparable c && value.getClass() == best.getClass() && c.compareTo(best) < 0)) {
                best = value;
            }
        }
        return best;
    }

    private String choosePrimaryPhone(String requested, Map<String, Object> keepRow, LinkedHashMap<String, String> phones) {
        if (requested != null && !requested.isBlank()) {
            String wanted = requested.trim();
            if (!phones.containsKey(wanted)) {
                throw invalid("Số chính " + wanted + " không có trong các hồ sơ đang gộp.");
            }
            return wanted;
        }
        String keepPrimary = str(keepRow.get("phone"));
        if (keepPrimary != null) return keepPrimary.trim();
        return phones.isEmpty() ? null : phones.keySet().iterator().next();
    }

    /**
     * Tài khoản giữ lại: theo lựa chọn của nhân viên, không thì tài khoản dùng được nhất
     * (đang hoạt động + có PIN > có PIN > đăng nhập gần nhất), hoà thì của hồ sơ giữ lại.
     */
    private static Map<String, Object> chooseAuth(List<Map<String, Object>> authRows, Integer accountFrom, Integer keepId) {
        if (authRows.isEmpty()) return null;
        if (accountFrom != null) {
            Map<String, Object> picked = authRows.stream()
                    .filter(a -> accountFrom.equals(toInt(a.get("customer_id")))).findFirst().orElse(null);
            if (picked != null) return picked;
        }
        Comparator<Map<String, Object>> byUsable = Comparator
                .comparing((Map<String, Object> a) -> "ACTIVE".equals(str(a.get("status"))) && a.get("pin_hash") != null)
                .thenComparing(a -> a.get("pin_hash") != null)
                .thenComparing(a -> str(a.get("last_login_at")) == null ? "" : str(a.get("last_login_at")))
                .thenComparing(a -> keepId.equals(toInt(a.get("customer_id"))));
        return authRows.stream().max(byUsable).orElse(null);
    }

    /** Cộng điểm của các hồ sơ bị gộp vào hồ sơ giữ lại; trả về tổng điểm đã cộng. */
    private int mergePoints(Integer keepId, List<Integer> mergeIds, List<Map<String, Object>> pointRows) {
        List<Map<String, Object>> merged = pointRows.stream()
                .filter(r -> mergeIds.contains(toInt(r.get("customer_id")))).toList();
        if (merged.isEmpty()) return 0;
        int total = merged.stream().mapToInt(r -> intOr0(r.get("total_points"))).sum();
        int lifetime = merged.stream().mapToInt(r -> intOr0(r.get("lifetime_points"))).sum();
        boolean keepHasRow = pointRows.stream().anyMatch(r -> keepId.equals(toInt(r.get("customer_id"))));

        List<Integer> toDelete = new ArrayList<>(mergeIds);
        if (!keepHasRow) {
            Integer donor = toInt(merged.get(0).get("customer_id"));
            jdbc.update("UPDATE customer_points SET customer_id = ?, total_points = ?, lifetime_points = ? WHERE customer_id = ?",
                    keepId, total, lifetime, donor);
            toDelete.remove(donor);
        } else {
            jdbc.update("UPDATE customer_points SET total_points = total_points + ?, lifetime_points = lifetime_points + ?"
                    + " WHERE customer_id = ?", total, lifetime, keepId);
        }
        if (!toDelete.isEmpty()) {
            jdbc.update("DELETE FROM customer_points WHERE customer_id IN (" + in(toDelete) + ")", toDelete.toArray());
        }
        return total;
    }

    private static String describeCustomer(Map<String, Object> row) {
        String name = str(row.get("full_name"));
        return (name == null ? "(chưa có tên)" : name) + " [" + codeOf(row) + "]";
    }

    private static String codeOf(Map<String, Object> row) {
        String code = str(row.get("customer_code"));
        return code != null ? code : "#" + row.get("customer_id");
    }

    private static String groupKey(String plateKey, List<Integer> sortedIds) {
        return "plate:" + plateKey + "|" + sortedIds.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private static List<Integer> distinctIds(List<Integer> ids) {
        if (ids == null) return List.of();
        return ids.stream().filter(Objects::nonNull).distinct().toList();
    }

    private static void requireAllExist(Collection<Integer> ids, Collection<Integer> found) {
        List<Integer> missing = ids.stream().filter(id -> !found.contains(id)).toList();
        if (!missing.isEmpty()) {
            throw new CustomerException("Không tìm thấy hồ sơ khách hàng: " + missing
                    + ". Có thể hồ sơ vừa được gộp ở máy khác — tải lại trang.", CustomerErrorCode.INVALID_ID);
        }
    }

    private static CustomerException invalid(String message) {
        return new CustomerException(message, CustomerErrorCode.INVALID_CUSTOMER_PROFILE);
    }

    private static String in(Collection<?> values) {
        return ReferenceRewriter.placeholders(values.size());
    }

    private static Object[] concat(Collection<?> first, Object... rest) {
        List<Object> all = new ArrayList<>(first);
        all.addAll(List.of(rest));
        return all.toArray();
    }

    private static Integer toInt(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.intValue();
        try {
            return Integer.valueOf(value.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static int intOr0(Object value) {
        Integer i = toInt(value);
        return i == null ? 0 : i;
    }

    private static boolean truthy(Object value) {
        if (value instanceof Boolean b) return b;
        Integer i = toInt(value);
        return i != null && i != 0;
    }

    private static String str(Object value) {
        if (value == null) return null;
        String text = value.toString();
        return text.isBlank() ? null : text;
    }

    private static String truncate(String value, int max) {
        if (value == null || value.isBlank()) return null;
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }

    private static String rootMessage(Throwable e) {
        Throwable root = e;
        while (root.getCause() != null && root.getCause() != root) root = root.getCause();
        return root.getMessage();
    }
}
