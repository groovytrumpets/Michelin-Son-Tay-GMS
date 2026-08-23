package com.g42.platform.gms.customerimport.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.g42.platform.gms.auth.entity.CustomerStatus;
import com.g42.platform.gms.customer.infrastructure.entity.CustomerAuthJpa;
import com.g42.platform.gms.customer.infrastructure.entity.CustomerProfileJpa;
import com.g42.platform.gms.customer.infrastructure.repository.CustomerAuthJpaRepo;
import com.g42.platform.gms.customer.infrastructure.repository.CustomerProfileJpaRepo;
import com.g42.platform.gms.customerimport.api.dto.*;
import com.g42.platform.gms.customerimport.infrastructure.entity.ImportBatchJpa;
import com.g42.platform.gms.customerimport.infrastructure.entity.LegacyVisitItemJpa;
import com.g42.platform.gms.customerimport.infrastructure.entity.LegacyVisitJpa;
import com.g42.platform.gms.customerimport.infrastructure.repository.ImportBatchRepository;
import com.g42.platform.gms.customerimport.infrastructure.repository.LegacyVisitItemRepository;
import com.g42.platform.gms.customerimport.infrastructure.repository.LegacyVisitRepository;
import com.g42.platform.gms.vehicle.entity.Vehicle;
import com.g42.platform.gms.vehicle.entity.VehicleBrand;
import com.g42.platform.gms.vehicle.repository.VehicleBrandRepository;
import com.g42.platform.gms.vehicle.repository.VehicleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * Nhập khách hàng, xe và lịch sử dịch vụ từ sổ Excel cũ.
 *
 * Chạy hai pha: {@link #validate} không ghi gì và trả về đúng những gì {@link #commit}
 * sẽ làm, nên người dùng sửa hết lỗi rồi mới ghi. Hai pha dùng chung một hàm xử lý để
 * không thể lệch nhau — kiểm tra thử báo hợp lệ thì lúc ghi phải hợp lệ.
 *
 * Quy tắc bất di bất dịch: KHÔNG BAO GIỜ ghi đè dữ liệu đang có. Khách đã tồn tại thì
 * chỉ điền vào ô đang trống. Sổ cũ thiếu tên ở gần một phần mười số dòng, nhập kiểu ghi
 * đè sẽ xoá trắng tên khách mà không ai biết.
 */
@Service
public class CustomerImportService {

    private static final Logger log = LoggerFactory.getLogger(CustomerImportService.class);

    /** Sổ cũ chỉ ghi ngày. Giờ này là do hệ thống điền, cờ hasTime = false đánh dấu điều đó. */
    private static final LocalTime DEFAULT_VISIT_TIME = LocalTime.of(8, 0);

    /** Hạng mục này là chiết khấu, không phải dịch vụ — tách sang discountAmount. */
    private static final String DISCOUNT_CATEGORY = "giam gia";

    /** Lệch quá ngưỡng này giữa tổng phiếu và tổng các dòng thì đánh dấu để rà lại. */
    private static final BigDecimal AMOUNT_TOLERANCE = new BigDecimal("1");

    @Autowired private CustomerProfileJpaRepo customerProfileRepo;
    @Autowired private CustomerAuthJpaRepo customerAuthRepo;
    /** Chỉ để lấy bản ghi khách đang được quản lý khi gán chủ xe (Vehicle tham chiếu entity này). */
    @Autowired private com.g42.platform.gms.auth.repository.CustomerProfileRepository customerOwnerRepo;
    @Autowired private VehicleRepository vehicleRepo;
    @Autowired private VehicleBrandRepository vehicleBrandRepo;
    @Autowired private LegacyVisitRepository legacyVisitRepo;
    @Autowired private LegacyVisitItemRepository legacyVisitItemRepo;
    @Autowired private ImportBatchRepository importBatchRepo;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private ObjectMapper objectMapper;

    /* =============================== API ================================== */

    /** Chạy thử toàn bộ luồng, không ghi gì vào cơ sở dữ liệu. */
    @Transactional(readOnly = true)
    public CustomerImportReport validate(CustomerImportRequest request) {
        return process(request, true, null);
    }

    /** Ghi thật. Toàn bộ lô nằm trong một giao dịch: lỗi giữa chừng thì không còn dấu vết. */
    @Transactional
    public CustomerImportReport commit(CustomerImportRequest request, Integer staffId) {
        return process(request, false, staffId);
    }

    /* ============================== Xử lý ================================= */

    private CustomerImportReport process(CustomerImportRequest request, boolean dryRun, Integer staffId) {
        CustomerImportReport report = new CustomerImportReport();
        report.setDryRun(dryRun);

        List<ImportRowDto> rows = request.getRows() == null ? List.of() : request.getRows();
        report.setTotalRows(rows.size());

        ImportBatchJpa batch = dryRun ? null : createBatch(request, staffId);
        if (batch != null) report.setBatchId(batch.getImportBatchId());

        RunState state = new RunState();
        state.policy = request.getPlateConflictPolicy() == null
                ? CustomerImportRequest.PlateConflictPolicy.KEEP_OWNER
                : request.getPlateConflictPolicy();

        // Hỏi một lần cho cả file thay vì mỗi dòng một truy vấn
        preloadExistingDedupeKeys(rows, state);

        for (ImportRowDto row : rows) {
            try {
                processRow(row, state, report, dryRun, batch);
            } catch (RowRejected rejected) {
                report.setSkippedRows(report.getSkippedRows() + 1);
                report.add(ImportIssueDto.error(row.getSourceRowNo(), rejected.field, rejected.getMessage()));
            }
        }

        if (batch != null) {
            batch.setCountsJson(writeCounts(report, state));
            importBatchRepo.save(batch);
        }
        return report;
    }

    private void processRow(ImportRowDto row, RunState state, CustomerImportReport report,
                            boolean dryRun, ImportBatchJpa batch) {
        Integer rowNo = row.getSourceRowNo();

        String phone = ImportNormalizer.normalizePhone(row.getPhone());
        if (phone != null && !ImportNormalizer.isValidPhone(phone)) {
            throw new RowRejected("phone",
                    "Số điện thoại \"" + row.getPhone() + "\" không đúng dạng 10 số bắt đầu bằng 0. Sửa trong file rồi nhập lại.");
        }

        String plateKey = ImportNormalizer.normalizePlate(row.getLicensePlate());

        Integer customerId = resolveCustomer(row, phone, plateKey, state, report, dryRun);
        Integer vehicleId = resolveVehicle(row, plateKey, customerId, state, report, dryRun);

        applyDoNotContact(row, customerId, state, report, dryRun);

        LocalDate visitedDate = ImportNormalizer.parseDate(row.getVisitedDate());
        if (visitedDate == null) visitedDate = ImportNormalizer.parseDate(row.getDeliveredDate());
        if (visitedDate == null) {
            report.add(ImportIssueDto.warning(rowNo, "visitedDate",
                    "Không có ngày nhận xe lẫn ngày giao xe. Khách và xe vẫn được tạo, nhưng lượt này không tính vào lịch sử đến xưởng."));
            return;
        }

        VisitLines lines = splitLines(row);

        String customerRef = phone != null ? phone : "cust:" + customerId;
        String dedupeKey = ImportNormalizer.dedupeKey(customerRef, visitedDate,
                row.getLegacyTicketCode(), lines.itemNames);

        if (state.existingDedupeKeys.contains(dedupeKey) || !state.seenDedupeKeys.add(dedupeKey)) {
            report.setVisitsDuplicate(report.getVisitsDuplicate() + 1);
            report.add(ImportIssueDto.warning(rowNo, "legacyTicketCode",
                    "Lượt này đã có trong hệ thống hoặc trùng với một dòng khác trong file — bỏ qua để không đếm hai lần."));
            return;
        }

        boolean mismatch = isAmountMismatch(row.getTotalAmount(), lines.itemsTotal);
        if (mismatch) {
            report.add(ImportIssueDto.warning(rowNo, "totalAmount",
                    "Tổng tiền trong sổ (" + row.getTotalAmount() + ") lệch tổng các dòng dịch vụ ("
                            + lines.itemsTotal + "). Vẫn nhập nguyên trạng và đánh dấu để rà lại."));
        }

        report.setVisitsCreated(report.getVisitsCreated() + 1);
        report.setVisitItemsCreated(report.getVisitItemsCreated() + lines.items.size());
        if (dryRun) return;

        LegacyVisitJpa visit = new LegacyVisitJpa();
        visit.setCustomerId(customerId);
        visit.setVehicleId(vehicleId);
        visit.setVisitedAt(LocalDateTime.of(visitedDate, DEFAULT_VISIT_TIME));
        visit.setHasTime(false);
        LocalDate delivered = ImportNormalizer.parseDate(row.getDeliveredDate());
        if (delivered != null) visit.setDeliveredAt(LocalDateTime.of(delivered, DEFAULT_VISIT_TIME));
        visit.setLegacyTicketCode(ImportNormalizer.trimToNull(row.getLegacyTicketCode()));
        visit.setOdometer(row.getOdometer());
        visit.setCustomerNote(ImportNormalizer.trimToNull(row.getCustomerNote()));
        visit.setServicesText(lines.servicesText);
        visit.setTotalAmount(row.getTotalAmount());
        visit.setDiscountAmount(lines.discount);
        visit.setAmountMismatch(mismatch);
        visit.setCalled(Boolean.TRUE.equals(row.getCalled()));
        visit.setCallSuccess(Boolean.TRUE.equals(row.getCallSuccess()));
        visit.setCallNote(ImportNormalizer.trimToNull(row.getCallNote()));
        visit.setImportBatchId(batch.getImportBatchId());
        visit.setSourceRowNo(rowNo);
        visit.setRawJson(row.getRawJson());
        visit.setDedupeKey(dedupeKey);
        LegacyVisitJpa saved = legacyVisitRepo.save(visit);

        short lineNo = 1;
        List<LegacyVisitItemJpa> entities = new ArrayList<>();
        for (ImportItemDto item : lines.items) {
            LegacyVisitItemJpa entity = new LegacyVisitItemJpa();
            entity.setLegacyVisitId(saved.getLegacyVisitId());
            entity.setLineNo(lineNo++);
            entity.setCategory(ImportNormalizer.trimToNull(item.getCategory()));
            entity.setItemName(displayName(item));
            entity.setQuantity(item.getQuantity());
            entity.setUnitPrice(item.getUnitPrice());
            entity.setAmount(item.getAmount());
            entities.add(entity);
        }
        legacyVisitItemRepo.saveAll(entities);
    }

    /* ============================ Khách hàng ============================== */

    private Integer resolveCustomer(ImportRowDto row, String phone, String plateKey, RunState state,
                                    CustomerImportReport report, boolean dryRun) {
        if (phone != null) {
            Integer known = state.customerIdByPhone.get(phone);
            if (known != null) {
                report.setCustomersMerged(report.getCustomersMerged() + 1);
                return known;
            }
            CustomerProfileJpa existing = customerProfileRepo.findByPhone(phone);
            if (existing != null) {
                mergeBlankFields(existing, row, report, dryRun);
                report.setCustomersMerged(report.getCustomersMerged() + 1);
                state.customerIdByPhone.put(phone, existing.getCustomerId());
                return existing.getCustomerId();
            }
            Integer created = createCustomer(row, phone, report, dryRun, state);
            report.setCustomersCreated(report.getCustomersCreated() + 1);
            state.customerIdByPhone.put(phone, created);
            return created;
        }

        // Không có số điện thoại thì thử tra chủ xe qua biển số
        if (plateKey != null) {
            Integer ownerId = findVehicleOwner(plateKey, state);
            if (ownerId != null) {
                report.setCustomersMerged(report.getCustomersMerged() + 1);
                report.add(ImportIssueDto.warning(row.getSourceRowNo(), "phone",
                        "Dòng không có số điện thoại — đã gắn vào chủ xe của biển số " + row.getLicensePlate() + "."));
                return ownerId;
            }
        }

        throw new RowRejected("phone",
                "Không có cả số điện thoại lẫn biển số nên không xác định được khách. Bổ sung một trong hai rồi nhập lại.");
    }

    private Integer createCustomer(ImportRowDto row, String phone, CustomerImportReport report,
                                   boolean dryRun, RunState state) {
        if (dryRun) {
            // Id giả chỉ dùng để đếm trong phiên chạy thử, không bao giờ chạm cơ sở dữ liệu
            return -(state.customerIdByPhone.size() + 1);
        }

        CustomerProfileJpa profile = new CustomerProfileJpa();
        profile.setPhone(phone);
        profile.setFullName(ImportNormalizer.trimToNull(row.getFullName()));
        profile.setEmail(resolveEmail(row, null, report));
        profile.setCreatedAt(LocalDateTime.now());
        CustomerProfileJpa saved = customerProfileRepo.save(profile);

        if (saved.getCustomerCode() == null || saved.getCustomerCode().isBlank()) {
            saved.setCustomerCode(String.format("KH%05d", saved.getCustomerId()));
            saved = customerProfileRepo.save(saved);
        }

        // Tài khoản tạo sẵn nhưng KHÔNG đăng nhập được: khách phải xác thực OTP Zalo
        // hoặc nhờ nhân viên kích hoạt. PIN 6 số cuối kèm cờ bắt đổi ở lần đăng nhập đầu.
        CustomerAuthJpa auth = new CustomerAuthJpa();
        auth.setCustomerId(saved.getCustomerId());
        auth.setStatus(CustomerStatus.INACTIVE);
        auth.setFailedAttemptCount(0);
        auth.setCreatedAt(LocalDateTime.now());
        auth.setMustChangePin(true);
        String pin = ImportNormalizer.defaultPin(phone);
        if (pin != null) auth.setPinHash(passwordEncoder.encode(pin));
        customerAuthRepo.save(auth);

        state.createdCustomerIds.add(saved.getCustomerId());
        return saved.getCustomerId();
    }

    /**
     * Chỉ điền vào ô đang trống. Giá trị khác trong file thì báo cảnh báo để người dùng
     * tự quyết, tuyệt đối không tự ghi đè.
     */
    private void mergeBlankFields(CustomerProfileJpa existing, ImportRowDto row,
                                  CustomerImportReport report, boolean dryRun) {
        boolean changed = false;

        String name = ImportNormalizer.trimToNull(row.getFullName());
        if (name != null) {
            if (ImportNormalizer.trimToNull(existing.getFullName()) == null) {
                existing.setFullName(name);
                changed = true;
            } else if (!name.equalsIgnoreCase(existing.getFullName())) {
                report.add(ImportIssueDto.warning(row.getSourceRowNo(), "fullName",
                        "Khách đã có tên \"" + existing.getFullName() + "\" trong hệ thống, giữ nguyên và bỏ qua tên \"" + name + "\" trong file."));
            }
        }

        if (ImportNormalizer.trimToNull(existing.getEmail()) == null) {
            String email = resolveEmail(row, existing.getCustomerId(), report);
            if (email != null) {
                existing.setEmail(email);
                changed = true;
            }
        }

        if (changed && !dryRun) customerProfileRepo.save(existing);
    }

    /** Email là định danh đăng nhập thứ hai nên phải là duy nhất; trùng thì bỏ qua chứ không làm vỡ cả lô. */
    private String resolveEmail(ImportRowDto row, Integer selfId, CustomerImportReport report) {
        String email = ImportNormalizer.trimToNull(row.getEmail());
        if (email == null) return null;

        Optional<CustomerProfileJpa> owner = customerProfileRepo.findByEmailIgnoreCase(email);
        if (owner.isPresent() && !owner.get().getCustomerId().equals(selfId)) {
            report.add(ImportIssueDto.warning(row.getSourceRowNo(), "email",
                    "Email " + email + " đã thuộc về khách khác nên không gán cho khách này."));
            return null;
        }
        return email;
    }

    private void applyDoNotContact(ImportRowDto row, Integer customerId, RunState state,
                                   CustomerImportReport report, boolean dryRun) {
        if (!ImportNormalizer.isDoNotContactNote(row.getCallNote())) return;
        if (!state.doNotContactIds.add(customerId)) return;

        report.setCustomersMarkedDoNotContact(report.getCustomersMarkedDoNotContact() + 1);
        report.add(ImportIssueDto.warning(row.getSourceRowNo(), "callNote",
                "Ghi chú \"" + row.getCallNote() + "\" — đánh dấu không gọi khách này nữa."));

        if (dryRun || customerId == null || customerId < 0) return;
        CustomerProfileJpa profile = customerProfileRepo.findByCustomerId(customerId);
        if (profile != null) {
            profile.setDoNotContact(true);
            customerProfileRepo.save(profile);
        }
    }

    /* ================================ Xe ================================== */

    private Integer resolveVehicle(ImportRowDto row, String plateKey, Integer customerId, RunState state,
                                   CustomerImportReport report, boolean dryRun) {
        if (plateKey == null) return null;

        if (!ImportNormalizer.looksLikePlate(plateKey)) {
            report.add(ImportIssueDto.warning(row.getSourceRowNo(), "licensePlate",
                    "Biển số \"" + row.getLicensePlate() + "\" không đúng dạng biển Việt Nam. Vẫn nhập nguyên trạng."));
        }

        Integer known = state.vehicleIdByPlate.get(plateKey);
        if (known != null) {
            report.setVehiclesLinked(report.getVehiclesLinked() + 1);
            return known;
        }

        Optional<Vehicle> existing = findVehicleByPlateKey(plateKey, state);
        if (existing.isPresent()) {
            Vehicle vehicle = existing.get();
            Integer ownerId = vehicle.getCustomer() == null ? null : vehicle.getCustomer().getCustomerId();
            Integer resolved = handlePlateConflict(row, vehicle, ownerId, customerId, state, report, dryRun);
            state.vehicleIdByPlate.put(plateKey, resolved);
            report.setVehiclesLinked(report.getVehiclesLinked() + 1);
            return resolved;
        }

        report.setVehiclesCreated(report.getVehiclesCreated() + 1);
        if (dryRun) {
            Integer placeholder = -(state.vehicleIdByPlate.size() + 1);
            state.vehicleIdByPlate.put(plateKey, placeholder);
            return null;
        }

        Vehicle vehicle = new Vehicle();
        vehicle.setLicensePlate(ImportNormalizer.trimToNull(row.getLicensePlate()));
        vehicle.setBrand(resolveBrand(row, report));
        vehicle.setModel(ImportNormalizer.trimToNull(row.getModel()));
        vehicle.setManufactureYear(row.getManufactureYear());
        customerOwnerRepo.findById(customerId).ifPresent(vehicle::setCustomer);
        Vehicle savedVehicle = vehicleRepo.save(vehicle);
        state.createdVehicleIds.add(savedVehicle.getVehicleId());
        state.vehicleIdByPlate.put(plateKey, savedVehicle.getVehicleId());
        state.vehiclesByPlateKey.put(plateKey, savedVehicle);
        return savedVehicle.getVehicleId();
    }

    private Integer handlePlateConflict(ImportRowDto row, Vehicle vehicle, Integer ownerId, Integer customerId,
                                        RunState state, CustomerImportReport report, boolean dryRun) {
        if (ownerId == null || ownerId.equals(customerId)) return vehicle.getVehicleId();

        String message = "Biển số " + row.getLicensePlate() + " đang thuộc về khách khác (mã " + ownerId + ").";
        switch (state.policy) {
            case TRANSFER -> {
                report.add(ImportIssueDto.warning(row.getSourceRowNo(), "licensePlate",
                        message + " Đã chuyển xe sang cho khách trong file theo lựa chọn của bạn."));
                if (!dryRun && customerId != null && customerId > 0) {
                    customerOwnerRepo.findById(customerId).ifPresent(owner -> {
                        vehicle.setCustomer(owner);
                        vehicleRepo.save(vehicle);
                    });
                }
                return vehicle.getVehicleId();
            }
            case SKIP_VEHICLE -> {
                report.add(ImportIssueDto.warning(row.getSourceRowNo(), "licensePlate",
                        message + " Lượt này được ghi cho khách nhưng không gắn xe."));
                return null;
            }
            default -> {
                report.add(ImportIssueDto.warning(row.getSourceRowNo(), "licensePlate",
                        message + " Giữ nguyên chủ xe hiện tại, lượt vẫn gắn vào xe này."));
                return vehicle.getVehicleId();
            }
        }
    }

    private Integer findVehicleOwner(String plateKey, RunState state) {
        Integer cached = state.vehicleOwnerByPlate.get(plateKey);
        if (cached != null) return cached;
        Optional<Vehicle> vehicle = findVehicleByPlateKey(plateKey, state);
        if (vehicle.isEmpty() || vehicle.get().getCustomer() == null) return null;
        Integer ownerId = vehicle.get().getCustomer().getCustomerId();
        state.vehicleOwnerByPlate.put(plateKey, ownerId);
        return ownerId;
    }

    /**
     * Biển số trong hệ thống lưu nguyên văn nên "30K-86694" và "30K86694" là hai bản
     * ghi khác nhau nếu chỉ so chuỗi. Đối chiếu theo khoá đã bỏ dấu để không tạo trùng
     * xe — riêng trong sổ cũ đã có bốn cặp lệch nhau đúng vì lý do này.
     *
     * Bảng xe được nạp một lần cho cả lô: so khớp phải chuẩn hoá nên không đẩy được
     * xuống câu truy vấn, mà mỗi dòng một lần quét bảng thì quá tốn.
     */
    private Optional<Vehicle> findVehicleByPlateKey(String plateKey, RunState state) {
        if (!state.vehiclesLoaded) {
            for (Vehicle vehicle : vehicleRepo.findAll()) {
                String key = ImportNormalizer.normalizePlate(vehicle.getLicensePlate());
                if (key != null) state.vehiclesByPlateKey.putIfAbsent(key, vehicle);
            }
            state.vehiclesLoaded = true;
        }
        return Optional.ofNullable(state.vehiclesByPlateKey.get(plateKey));
    }

    /** Gộp lỗi chính tả rồi đối chiếu danh mục hãng xe; không nhận ra thì giữ nguyên văn. */
    private String resolveBrand(ImportRowDto row, CustomerImportReport report) {
        String raw = ImportNormalizer.trimToNull(row.getBrand());
        if (raw == null) return null;

        String canonical = ImportNormalizer.canonicalBrand(raw);
        if (canonical != null) {
            Optional<VehicleBrand> known = vehicleBrandRepo.findByNameIgnoreCase(canonical);
            if (known.isPresent()) return known.get().getName();
            return canonical;
        }

        Optional<VehicleBrand> exact = vehicleBrandRepo.findByNameIgnoreCase(raw);
        if (exact.isPresent()) return exact.get().getName();

        report.add(ImportIssueDto.warning(row.getSourceRowNo(), "brand",
                "Hãng xe \"" + raw + "\" không có trong danh mục. Giữ nguyên văn — sửa lại sau ở màn Cấu hình hãng xe nếu cần."));
        return raw;
    }

    /* ============================ Dòng dịch vụ ============================ */

    private VisitLines splitLines(ImportRowDto row) {
        VisitLines result = new VisitLines();
        List<ImportItemDto> items = row.getItems() == null ? List.of() : row.getItems();

        List<String> names = new ArrayList<>();
        List<BigDecimal> amounts = new ArrayList<>();
        for (ImportItemDto item : items) {
            if (isBlankLine(item)) continue;

            String categoryKey = ImportNormalizer.stripDiacritics(item.getCategory());
            if (categoryKey != null && categoryKey.contains(DISCOUNT_CATEGORY)) {
                if (item.getAmount() != null) {
                    result.discount = result.discount == null
                            ? item.getAmount()
                            : result.discount.add(item.getAmount());
                }
                continue;
            }

            result.items.add(item);
            names.add(displayName(item));
            amounts.add(item.getAmount());
        }

        result.itemNames = names;
        result.servicesText = names.isEmpty() ? null : String.join("; ", names);
        result.itemsTotal = ImportNormalizer.sum(amounts);
        return result;
    }

    /**
     * Sổ cũ bỏ trống ô Diễn giải ở khoảng bảy chục dòng, chỉ điền số lượng và giá.
     * Khi đó tên hạng mục chính là tên dịch vụ.
     */
    private String displayName(ImportItemDto item) {
        String name = ImportNormalizer.trimToNull(item.getItemName());
        if (name != null) return name;
        return ImportNormalizer.trimToNull(item.getCategory());
    }

    private boolean isBlankLine(ImportItemDto item) {
        if (item == null) return true;
        boolean hasText = ImportNormalizer.trimToNull(item.getItemName()) != null;
        boolean hasQty = item.getQuantity() != null && item.getQuantity().signum() != 0;
        boolean hasPrice = item.getUnitPrice() != null && item.getUnitPrice().signum() != 0;
        boolean hasAmount = item.getAmount() != null && item.getAmount().signum() != 0;
        return !hasText && !hasQty && !hasPrice && !hasAmount;
    }

    private boolean isAmountMismatch(BigDecimal declared, BigDecimal computed) {
        if (declared == null || computed == null) return false;
        if (computed.signum() == 0) return false;
        return declared.subtract(computed).abs().compareTo(AMOUNT_TOLERANCE) > 0;
    }

    /* ============================== Hoàn tác ============================== */

    /**
     * Gỡ nguyên một lô đã nhập.
     *
     * Chỉ xoá khách và xe do CHÍNH lô này tạo ra, và chỉ khi chúng không còn được
     * tham chiếu ở đâu khác. Khách đã có phiếu dịch vụ thật hoặc lượt cũ từ lô khác
     * thì giữ lại — hoàn tác không được phép làm mất dữ liệu ngoài phạm vi lô.
     */
    @Transactional
    public CustomerImportReport rollback(Integer batchId) {
        ImportBatchJpa batch = importBatchRepo.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lô nhập số " + batchId));

        CustomerImportReport report = new CustomerImportReport();
        report.setBatchId(batchId);

        if (ImportBatchJpa.STATUS_ROLLED_BACK.equals(batch.getStatus())) {
            report.add(ImportIssueDto.warning(null, "batch", "Lô này đã được hoàn tác trước đó."));
            return report;
        }

        List<LegacyVisitJpa> visits = legacyVisitRepo.findByImportBatchId(batchId);
        List<Integer> visitIds = visits.stream().map(LegacyVisitJpa::getLegacyVisitId).toList();
        if (!visitIds.isEmpty()) {
            legacyVisitItemRepo.deleteAll(legacyVisitItemRepo.findByLegacyVisitIdInOrderByLineNoAsc(visitIds));
        }
        legacyVisitRepo.deleteAll(visits);
        legacyVisitRepo.flush();
        report.setVisitsCreated(visits.size());

        BatchCounts counts = readCounts(batch);

        int vehiclesRemoved = 0;
        for (Integer vehicleId : counts.createdVehicleIds) {
            if (legacyVisitRepo.countByVehicleId(vehicleId) > 0) continue;
            vehicleRepo.findById(vehicleId).ifPresent(vehicleRepo::delete);
            vehiclesRemoved++;
        }
        report.setVehiclesCreated(vehiclesRemoved);

        int customersRemoved = 0;
        for (Integer customerId : counts.createdCustomerIds) {
            if (legacyVisitRepo.countByCustomerId(customerId) > 0) {
                report.add(ImportIssueDto.warning(null, "customer",
                        "Giữ lại khách mã " + customerId + " vì còn lượt dịch vụ từ lô nhập khác."));
                continue;
            }
            if (!vehicleRepo.findByCustomer_CustomerId(customerId).isEmpty()) {
                report.add(ImportIssueDto.warning(null, "customer",
                        "Giữ lại khách mã " + customerId + " vì vẫn còn xe gắn với khách."));
                continue;
            }
            CustomerAuthJpa auth = customerAuthRepo.findByCustomerId(customerId);
            if (auth != null) customerAuthRepo.delete(auth);
            CustomerProfileJpa profile = customerProfileRepo.findByCustomerId(customerId);
            if (profile != null) customerProfileRepo.delete(profile);
            customersRemoved++;
        }
        report.setCustomersCreated(customersRemoved);

        batch.setStatus(ImportBatchJpa.STATUS_ROLLED_BACK);
        importBatchRepo.save(batch);
        return report;
    }

    @Transactional(readOnly = true)
    public List<ImportBatchJpa> listBatches() {
        return importBatchRepo.findAllByOrderByImportedAtDesc();
    }

    /* ============================== Nội bộ ================================ */

    private ImportBatchJpa createBatch(CustomerImportRequest request, Integer staffId) {
        ImportBatchJpa batch = new ImportBatchJpa();
        batch.setFileName(ImportNormalizer.trimToNull(request.getFileName()));
        batch.setSheetName(ImportNormalizer.trimToNull(request.getSheetName()));
        batch.setNote(ImportNormalizer.trimToNull(request.getNote()));
        batch.setImportedBy(staffId);
        batch.setStatus(ImportBatchJpa.STATUS_COMMITTED);
        return importBatchRepo.save(batch);
    }

    private void preloadExistingDedupeKeys(List<ImportRowDto> rows, RunState state) {
        if (rows.isEmpty()) return;
        Set<String> candidates = new HashSet<>();
        for (ImportRowDto row : rows) {
            String phone = ImportNormalizer.normalizePhone(row.getPhone());
            if (phone == null) continue;
            LocalDate date = ImportNormalizer.parseDate(row.getVisitedDate());
            if (date == null) date = ImportNormalizer.parseDate(row.getDeliveredDate());
            if (date == null) continue;
            VisitLines lines = splitLines(row);
            candidates.add(ImportNormalizer.dedupeKey(phone, date, row.getLegacyTicketCode(), lines.itemNames));
        }
        if (candidates.isEmpty()) return;
        state.existingDedupeKeys.addAll(legacyVisitRepo.findExistingDedupeKeys(candidates));
    }

    private String writeCounts(CustomerImportReport report, RunState state) {
        BatchCounts counts = new BatchCounts();
        counts.customersCreated = report.getCustomersCreated();
        counts.customersMerged = report.getCustomersMerged();
        counts.vehiclesCreated = report.getVehiclesCreated();
        counts.visitsCreated = report.getVisitsCreated();
        counts.visitItemsCreated = report.getVisitItemsCreated();
        counts.skippedRows = report.getSkippedRows();
        counts.createdCustomerIds = new ArrayList<>(state.createdCustomerIds);
        counts.createdVehicleIds = new ArrayList<>(state.createdVehicleIds);
        try {
            return objectMapper.writeValueAsString(counts);
        } catch (Exception e) {
            log.warn("Không ghi được thống kê lô nhập: {}", e.getMessage());
            return null;
        }
    }

    private BatchCounts readCounts(ImportBatchJpa batch) {
        if (batch.getCountsJson() == null || batch.getCountsJson().isBlank()) return new BatchCounts();
        try {
            return objectMapper.readValue(batch.getCountsJson(), BatchCounts.class);
        } catch (Exception e) {
            log.warn("Không đọc được thống kê lô nhập {}: {}", batch.getImportBatchId(), e.getMessage());
            return new BatchCounts();
        }
    }

    /** Trạng thái trong một lần chạy — để hai dòng cùng số điện thoại không tạo hai khách. */
    private static final class RunState {
        CustomerImportRequest.PlateConflictPolicy policy = CustomerImportRequest.PlateConflictPolicy.KEEP_OWNER;
        final Map<String, Integer> customerIdByPhone = new HashMap<>();
        final Map<String, Integer> vehicleIdByPlate = new HashMap<>();
        final Map<String, Integer> vehicleOwnerByPlate = new HashMap<>();
        final Map<String, Vehicle> vehiclesByPlateKey = new HashMap<>();
        boolean vehiclesLoaded = false;
        final Set<String> seenDedupeKeys = new HashSet<>();
        final Set<String> existingDedupeKeys = new HashSet<>();
        final Set<Integer> doNotContactIds = new HashSet<>();
        final List<Integer> createdCustomerIds = new ArrayList<>();
        final List<Integer> createdVehicleIds = new ArrayList<>();
    }

    private static final class VisitLines {
        final List<ImportItemDto> items = new ArrayList<>();
        List<String> itemNames = new ArrayList<>();
        String servicesText;
        BigDecimal itemsTotal = BigDecimal.ZERO;
        BigDecimal discount;
    }

    /** Nội dung của cột counts_json. */
    public static final class BatchCounts {
        public int customersCreated;
        public int customersMerged;
        public int vehiclesCreated;
        public int visitsCreated;
        public int visitItemsCreated;
        public int skippedRows;
        public List<Integer> createdCustomerIds = new ArrayList<>();
        public List<Integer> createdVehicleIds = new ArrayList<>();
    }

    /** Dòng không ghi được. Ném ra để thoát sớm, người gọi biến thành một lỗi trong báo cáo. */
    private static final class RowRejected extends RuntimeException {
        final String field;

        RowRejected(String field, String message) {
            super(message);
            this.field = field;
        }
    }
}
