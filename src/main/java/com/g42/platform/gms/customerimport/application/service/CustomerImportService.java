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
    /** Chỉ để hiện tên người đã chạy lô nhập trên màn xem chi tiết. */
    @Autowired private com.g42.platform.gms.staff.profile.infrastructure.repository.StaffProileJpaRepo staffProfileRepo;

    /* =============================== API ================================== */

    /** Chạy thử toàn bộ luồng, không ghi gì vào cơ sở dữ liệu. */
    @Transactional(readOnly = true)
    public CustomerImportReport validate(CustomerImportRequest request) {
        return process(request, true, null);
    }

    /**
     * Ghi thật. Toàn bộ lô nằm trong một giao dịch: lỗi giữa chừng thì không còn dấu vết.
     *
     * Có replaceBatchId nghĩa là người dùng mở một lô đã ghi ra sửa rồi nhập lại: gỡ lô
     * cũ trước, ghi lô mới sau, cùng một giao dịch để không bao giờ tồn tại hai bản.
     */
    @Transactional
    public CustomerImportReport commit(CustomerImportRequest request, Integer staffId) {
        Integer replaceBatchId = request.getReplaceBatchId();
        if (replaceBatchId == null) {
            return process(request, false, staffId);
        }

        ImportBatchJpa previous = importBatchRepo.findById(replaceBatchId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lô nhập số " + replaceBatchId));
        if (!ImportBatchJpa.STATUS_ROLLED_BACK.equals(previous.getStatus())) {
            rollback(replaceBatchId);
        }

        CustomerImportReport report = process(request, false, staffId);

        previous = importBatchRepo.findById(replaceBatchId).orElse(previous);
        previous.setReplacedByBatchId(report.getBatchId());
        importBatchRepo.save(previous);
        report.add(ImportIssueDto.warning(null, "batch",
                "Đã gỡ lô #" + replaceBatchId + " và ghi lại thành lô #" + report.getBatchId() + "."));
        return report;
    }

    /**
     * Nội dung một lô để mở lại lên bảng mà sửa. Ưu tiên nguyên văn đã gửi lần trước;
     * lô ghi trước khi có cột rows_json thì dựng lại từ dữ liệu đã ghi.
     */
    @Transactional(readOnly = true)
    public ImportBatchRowsDto batchRows(Integer batchId) {
        ImportBatchJpa batch = importBatchRepo.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lô nhập số " + batchId));

        ImportBatchRowsDto dto = new ImportBatchRowsDto();
        dto.setBatchId(batch.getImportBatchId());
        dto.setFileName(batch.getFileName());
        dto.setSheetName(batch.getSheetName());
        dto.setNote(batch.getNote());
        dto.setStatus(batch.getStatus());
        dto.setPlateConflictPolicy(batch.getPlateConflictPolicy());

        List<ImportRowDto> stored = readStoredRows(batch);
        if (stored != null) {
            dto.setRows(stored);
            return dto;
        }

        dto.setReconstructed(true);
        dto.setRows(reconstructRows(batchId));
        return dto;
    }

    /**
     * Lô đó đã đưa vào hệ thống những khách nào, mỗi khách có xe gì và những lượt nào.
     *
     * Dựng từ dữ liệu thật đang có để bấm vào là mở đúng hồ sơ khách. Lô đã hoàn tác
     * không còn lượt nào nên dựng lại từ nội dung file đã lưu, chỉ để xem lại.
     */
    @Transactional(readOnly = true)
    public ImportBatchDetailDto batchDetail(Integer batchId) {
        ImportBatchJpa batch = importBatchRepo.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lô nhập số " + batchId));

        ImportBatchDetailDto dto = new ImportBatchDetailDto();
        dto.setBatchId(batch.getImportBatchId());
        dto.setFileName(batch.getFileName());
        dto.setSheetName(batch.getSheetName());
        dto.setNote(batch.getNote());
        dto.setStatus(batch.getStatus());
        dto.setPlateConflictPolicy(batch.getPlateConflictPolicy());
        dto.setImportedAt(batch.getImportedAt());
        dto.setImportedBy(batch.getImportedBy());
        dto.setReplacedByBatchId(batch.getReplacedByBatchId());
        if (batch.getImportedBy() != null) {
            staffProfileRepo.findById(batch.getImportedBy())
                    .ifPresent(staff -> dto.setImportedByName(staff.getFullName()));
        }

        BatchCounts counts = readCounts(batch);
        dto.setCustomersCreated(counts.customersCreated);
        dto.setCustomersMerged(counts.customersMerged);
        dto.setVehiclesCreated(counts.vehiclesCreated);
        dto.setVisitsCreated(counts.visitsCreated);
        dto.setVisitItemsCreated(counts.visitItemsCreated);
        dto.setSkippedRows(counts.skippedRows);

        List<LegacyVisitJpa> visits = new ArrayList<>(legacyVisitRepo.findByImportBatchId(batchId));
        dto.setVisitsRemaining(visits.size());

        List<ImportRowDto> storedRows = readStoredRows(batch);

        if (visits.isEmpty()) {
            // Lô đã hoàn tác: không còn gì trong hệ thống, chỉ xem lại được nội dung file
            dto.setFromStoredRows(true);
            if (storedRows != null) dto.setCustomers(groupStoredRows(storedRows));
            return dto;
        }

        dto.setCustomers(groupVisits(visits, counts));
        if (storedRows != null) dto.setRowsWithoutVisit(findRowsWithoutVisit(storedRows, visits));
        return dto;
    }

    /** Gom lượt theo khách, kèm xe và các dòng dịch vụ của từng lượt. */
    private List<ImportBatchDetailDto.Customer> groupVisits(List<LegacyVisitJpa> visits, BatchCounts counts) {
        visits.sort(Comparator.comparing(
                LegacyVisitJpa::getVisitedAt, Comparator.nullsLast(Comparator.reverseOrder())));

        Map<Integer, List<LegacyVisitItemJpa>> itemsByVisit = new HashMap<>();
        List<Integer> visitIds = visits.stream().map(LegacyVisitJpa::getLegacyVisitId).toList();
        for (LegacyVisitItemJpa item : legacyVisitItemRepo.findByLegacyVisitIdInOrderByLineNoAsc(visitIds)) {
            itemsByVisit.computeIfAbsent(item.getLegacyVisitId(), k -> new ArrayList<>()).add(item);
        }

        Set<Integer> createdCustomers = new HashSet<>(counts.createdCustomerIds);
        Set<Integer> createdVehicles = new HashSet<>(counts.createdVehicleIds);
        Map<Integer, Vehicle> vehicleCache = new HashMap<>();
        Map<Integer, ImportBatchDetailDto.Customer> byCustomer = new LinkedHashMap<>();

        for (LegacyVisitJpa visit : visits) {
            ImportBatchDetailDto.Customer customer = byCustomer.computeIfAbsent(visit.getCustomerId(), id -> {
                ImportBatchDetailDto.Customer created = new ImportBatchDetailDto.Customer();
                created.setCustomerId(id);
                created.setCreatedByBatch(createdCustomers.contains(id));
                CustomerProfileJpa profile = customerProfileRepo.findByCustomerId(id);
                if (profile != null) {
                    created.setFullName(profile.getFullName());
                    created.setPhone(profile.getPhone());
                    created.setEmail(profile.getEmail());
                    created.setCustomerCode(profile.getCustomerCode());
                }
                return created;
            });

            ImportBatchDetailDto.Visit visitDto = new ImportBatchDetailDto.Visit();
            visitDto.setLegacyVisitId(visit.getLegacyVisitId());
            visitDto.setSourceRowNo(visit.getSourceRowNo());
            visitDto.setLegacyTicketCode(visit.getLegacyTicketCode());
            visitDto.setVisitedAt(visit.getVisitedAt());
            visitDto.setDeliveredAt(visit.getDeliveredAt());
            visitDto.setOdometer(visit.getOdometer());
            visitDto.setCustomerNote(visit.getCustomerNote());
            visitDto.setServicesText(visit.getServicesText());
            visitDto.setTotalAmount(visit.getTotalAmount());
            visitDto.setDiscountAmount(visit.getDiscountAmount());
            visitDto.setAmountMismatch(Boolean.TRUE.equals(visit.getAmountMismatch()));

            if (visit.getVehicleId() != null) {
                Vehicle vehicle = vehicleCache.computeIfAbsent(
                        visit.getVehicleId(), id -> vehicleRepo.findById(id).orElse(null));
                if (vehicle != null) {
                    visitDto.setLicensePlate(vehicle.getLicensePlate());
                    boolean known = customer.getVehicles().stream()
                            .anyMatch(v -> Objects.equals(v.getVehicleId(), vehicle.getVehicleId()));
                    if (!known) {
                        ImportBatchDetailDto.Vehicle vehicleDto = new ImportBatchDetailDto.Vehicle();
                        vehicleDto.setVehicleId(vehicle.getVehicleId());
                        vehicleDto.setLicensePlate(vehicle.getLicensePlate());
                        vehicleDto.setBrand(vehicle.getBrand());
                        vehicleDto.setModel(vehicle.getModel());
                        vehicleDto.setManufactureYear(vehicle.getManufactureYear());
                        vehicleDto.setCreatedByBatch(createdVehicles.contains(vehicle.getVehicleId()));
                        customer.getVehicles().add(vehicleDto);
                    }
                }
            }

            for (LegacyVisitItemJpa item : itemsByVisit.getOrDefault(visit.getLegacyVisitId(), List.of())) {
                ImportBatchDetailDto.Item itemDto = new ImportBatchDetailDto.Item();
                itemDto.setCategory(item.getCategory());
                itemDto.setItemName(item.getItemName());
                itemDto.setQuantity(item.getQuantity());
                itemDto.setUnitPrice(item.getUnitPrice());
                itemDto.setAmount(item.getAmount());
                visitDto.getItems().add(itemDto);
            }

            customer.getVisits().add(visitDto);
            customer.setVisitCount(customer.getVisits().size());
            if (visit.getTotalAmount() != null) {
                customer.setTotalAmount(customer.getTotalAmount() == null
                        ? visit.getTotalAmount()
                        : customer.getTotalAmount().add(visit.getTotalAmount()));
            }
            if (visit.getVisitedAt() != null
                    && (customer.getLastVisitedAt() == null || visit.getVisitedAt().isAfter(customer.getLastVisitedAt()))) {
                customer.setLastVisitedAt(visit.getVisitedAt());
            }
        }

        return new ArrayList<>(byCustomer.values());
    }

    /** Lô đã hoàn tác: gom theo số điện thoại trong file, chỉ để xem lại nội dung. */
    private List<ImportBatchDetailDto.Customer> groupStoredRows(List<ImportRowDto> rows) {
        Map<String, ImportBatchDetailDto.Customer> byPhone = new LinkedHashMap<>();

        for (ImportRowDto row : rows) {
            String phone = ImportNormalizer.normalizePhone(row.getPhone());
            String plateKey = ImportNormalizer.normalizePlate(row.getLicensePlate());
            String key = phone != null ? phone
                    : plateKey != null ? "plate:" + plateKey
                    : "row:" + (row.getSourceRowNo() == null ? row.hashCode() : row.getSourceRowNo());

            ImportBatchDetailDto.Customer customer = byPhone.computeIfAbsent(key, k -> {
                ImportBatchDetailDto.Customer created = new ImportBatchDetailDto.Customer();
                created.setFullName(ImportNormalizer.trimToNull(row.getFullName()));
                created.setPhone(ImportNormalizer.trimToNull(row.getPhone()));
                created.setEmail(ImportNormalizer.trimToNull(row.getEmail()));
                return created;
            });
            if (customer.getFullName() == null) {
                customer.setFullName(ImportNormalizer.trimToNull(row.getFullName()));
            }

            String plate = ImportNormalizer.trimToNull(row.getLicensePlate());
            if (plate != null && customer.getVehicles().stream()
                    .noneMatch(v -> plate.equalsIgnoreCase(v.getLicensePlate()))) {
                ImportBatchDetailDto.Vehicle vehicleDto = new ImportBatchDetailDto.Vehicle();
                vehicleDto.setLicensePlate(plate);
                vehicleDto.setBrand(ImportNormalizer.trimToNull(row.getBrand()));
                vehicleDto.setModel(ImportNormalizer.trimToNull(row.getModel()));
                vehicleDto.setManufactureYear(row.getManufactureYear());
                customer.getVehicles().add(vehicleDto);
            }

            ImportBatchDetailDto.Visit visitDto = new ImportBatchDetailDto.Visit();
            visitDto.setSourceRowNo(row.getSourceRowNo());
            visitDto.setLegacyTicketCode(row.getLegacyTicketCode());
            LocalDate visitedDate = ImportNormalizer.parseDate(row.getVisitedDate());
            if (visitedDate != null) visitDto.setVisitedAt(LocalDateTime.of(visitedDate, DEFAULT_VISIT_TIME));
            visitDto.setLicensePlate(plate);
            visitDto.setOdometer(row.getOdometer());
            visitDto.setCustomerNote(row.getCustomerNote());
            visitDto.setTotalAmount(row.getTotalAmount());
            for (ImportItemDto item : row.getItems() == null ? List.<ImportItemDto>of() : row.getItems()) {
                ImportBatchDetailDto.Item itemDto = new ImportBatchDetailDto.Item();
                itemDto.setCategory(item.getCategory());
                itemDto.setItemName(displayName(item));
                itemDto.setQuantity(item.getQuantity());
                itemDto.setUnitPrice(item.getUnitPrice());
                itemDto.setAmount(item.getAmount());
                visitDto.getItems().add(itemDto);
            }

            customer.getVisits().add(visitDto);
            customer.setVisitCount(customer.getVisits().size());
            if (row.getTotalAmount() != null) {
                customer.setTotalAmount(customer.getTotalAmount() == null
                        ? row.getTotalAmount()
                        : customer.getTotalAmount().add(row.getTotalAmount()));
            }
            if (visitDto.getVisitedAt() != null
                    && (customer.getLastVisitedAt() == null || visitDto.getVisitedAt().isAfter(customer.getLastVisitedAt()))) {
                customer.setLastVisitedAt(visitDto.getVisitedAt());
            }
        }

        return new ArrayList<>(byPhone.values());
    }

    /** Dòng có trong file nhưng không thành lượt nào: trùng, thiếu ngày hoặc bị loại. */
    private List<ImportBatchDetailDto.MissingRow> findRowsWithoutVisit(List<ImportRowDto> rows,
                                                                      List<LegacyVisitJpa> visits) {
        Set<Integer> imported = new HashSet<>();
        for (LegacyVisitJpa visit : visits) {
            if (visit.getSourceRowNo() != null) imported.add(visit.getSourceRowNo());
        }

        List<ImportBatchDetailDto.MissingRow> missing = new ArrayList<>();
        for (ImportRowDto row : rows) {
            if (row.getSourceRowNo() == null || imported.contains(row.getSourceRowNo())) continue;
            ImportBatchDetailDto.MissingRow item = new ImportBatchDetailDto.MissingRow();
            item.setSourceRowNo(row.getSourceRowNo());
            item.setFullName(ImportNormalizer.trimToNull(row.getFullName()));
            item.setPhone(ImportNormalizer.trimToNull(row.getPhone()));
            item.setLicensePlate(ImportNormalizer.trimToNull(row.getLicensePlate()));
            item.setVisitedDate(ImportNormalizer.trimToNull(row.getVisitedDate()));
            missing.add(item);
        }
        return missing;
    }

    private List<ImportRowDto> readStoredRows(ImportBatchJpa batch) {
        String json = batch.getRowsJson();
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json,
                    new com.fasterxml.jackson.core.type.TypeReference<List<ImportRowDto>>() {});
        } catch (Exception e) {
            log.warn("Không đọc được nội dung gốc của lô {}: {}", batch.getImportBatchId(), e.getMessage());
            return null;
        }
    }

    /**
     * Dựng lại các dòng từ những gì đã ghi. Chỉ khôi phục được lượt thật sự vào hệ
     * thống — dòng bị bỏ qua vì trùng hoặc thiếu ngày thì không còn dấu vết để dựng.
     */
    private List<ImportRowDto> reconstructRows(Integer batchId) {
        List<LegacyVisitJpa> visits = new ArrayList<>(legacyVisitRepo.findByImportBatchId(batchId));
        if (visits.isEmpty()) return List.of();
        visits.sort(Comparator.comparing(
                LegacyVisitJpa::getSourceRowNo, Comparator.nullsLast(Comparator.naturalOrder())));

        List<Integer> visitIds = visits.stream().map(LegacyVisitJpa::getLegacyVisitId).toList();
        Map<Integer, List<LegacyVisitItemJpa>> itemsByVisit = new HashMap<>();
        for (LegacyVisitItemJpa item : legacyVisitItemRepo.findByLegacyVisitIdInOrderByLineNoAsc(visitIds)) {
            itemsByVisit.computeIfAbsent(item.getLegacyVisitId(), k -> new ArrayList<>()).add(item);
        }

        Map<Integer, CustomerProfileJpa> customerCache = new HashMap<>();
        Map<Integer, Vehicle> vehicleCache = new HashMap<>();
        List<ImportRowDto> rows = new ArrayList<>();

        for (LegacyVisitJpa visit : visits) {
            ImportRowDto row = new ImportRowDto();
            row.setSourceRowNo(visit.getSourceRowNo());
            row.setLegacyTicketCode(visit.getLegacyTicketCode());

            CustomerProfileJpa customer = customerCache.computeIfAbsent(
                    visit.getCustomerId(), customerProfileRepo::findByCustomerId);
            if (customer != null) {
                row.setFullName(customer.getFullName());
                row.setPhone(customer.getPhone());
                row.setEmail(customer.getEmail());
            }

            if (visit.getVehicleId() != null) {
                Vehicle vehicle = vehicleCache.computeIfAbsent(
                        visit.getVehicleId(), id -> vehicleRepo.findById(id).orElse(null));
                if (vehicle != null) {
                    row.setLicensePlate(vehicle.getLicensePlate());
                    row.setBrand(vehicle.getBrand());
                    row.setModel(vehicle.getModel());
                    row.setManufactureYear(vehicle.getManufactureYear());
                }
            }

            row.setOdometer(visit.getOdometer());
            if (visit.getVisitedAt() != null) row.setVisitedDate(visit.getVisitedAt().toLocalDate().toString());
            if (visit.getDeliveredAt() != null) row.setDeliveredDate(visit.getDeliveredAt().toLocalDate().toString());
            row.setCustomerNote(visit.getCustomerNote());
            row.setTotalAmount(visit.getTotalAmount());
            row.setCalled(Boolean.TRUE.equals(visit.getCalled()));
            row.setCallSuccess(Boolean.TRUE.equals(visit.getCallSuccess()));
            row.setCallNote(visit.getCallNote());
            row.setRawJson(visit.getRawJson());

            List<ImportItemDto> items = new ArrayList<>();
            for (LegacyVisitItemJpa item : itemsByVisit.getOrDefault(visit.getLegacyVisitId(), List.of())) {
                items.add(new ImportItemDto(item.getCategory(), item.getItemName(),
                        item.getQuantity(), item.getUnitPrice(), item.getAmount()));
            }
            // Chiết khấu lúc nhập bị tách khỏi các dòng dịch vụ, trả lại thành một dòng
            // để lần nhập lại không đánh rơi phần giảm giá.
            BigDecimal discount = visit.getDiscountAmount();
            if (discount != null && discount.signum() != 0) {
                items.add(new ImportItemDto("Giảm giá", "Giảm giá", null, null, discount));
            }
            row.setItems(items);

            rows.add(row);
        }
        return rows;
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
        if (phone == null && plateKey == null) {
            throw new RowRejected("licensePlate",
                    "Phiếu phải có ít nhất một trong hai: biển số hoặc số điện thoại, để xác định khách. "
                            + "Có biển số thì bỏ trống số điện thoại cũng được, và ngược lại. Bổ sung một trong hai rồi nhập lại.");
        }

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

        // Khách không có SĐT thì định danh theo biển số; "plate:" + khoá biển số là chuỗi
        // ổn định giữa bước kiểm tra thử và bước ghi (customerId lúc thử là id giả âm).
        String customerRef = phone != null ? phone : "plate:" + plateKey;
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
                if (plateKey != null) state.customerIdByPlateKey.putIfAbsent(plateKey, known);
                return known;
            }
            CustomerProfileJpa existing = customerProfileRepo.findByPhone(phone);
            if (existing != null) {
                mergeBlankFields(existing, row, report, dryRun);
                report.setCustomersMerged(report.getCustomersMerged() + 1);
                state.customerIdByPhone.put(phone, existing.getCustomerId());
                if (plateKey != null) state.customerIdByPlateKey.putIfAbsent(plateKey, existing.getCustomerId());
                return existing.getCustomerId();
            }
            Integer created = createCustomer(row, phone, report, dryRun, state);
            report.setCustomersCreated(report.getCustomersCreated() + 1);
            state.customerIdByPhone.put(phone, created);
            if (plateKey != null) state.customerIdByPlateKey.putIfAbsent(plateKey, created);
            return created;
        }

        // Không có số điện thoại: định danh khách theo BIỂN SỐ, đúng cách SĐT định danh ở nhánh
        // trên. Cùng một biển số là cùng một khách; tên khác trong file chỉ cảnh báo qua
        // mergeBlankFields chứ không ghi đè — giống hệt quy tắc trùng tên khi gộp theo SĐT.
        Integer knownByPlate = state.customerIdByPlateKey.get(plateKey);
        if (knownByPlate != null) {
            report.setCustomersMerged(report.getCustomersMerged() + 1);
            return knownByPlate;
        }

        Optional<Vehicle> existingVehicle = findVehicleByPlateKey(plateKey, state);
        if (existingVehicle.isPresent() && existingVehicle.get().getCustomer() != null) {
            Integer ownerId = existingVehicle.get().getCustomer().getCustomerId();
            CustomerProfileJpa owner = customerProfileRepo.findByCustomerId(ownerId);
            if (owner != null) {
                mergeBlankFields(owner, row, report, dryRun);
                report.setCustomersMerged(report.getCustomersMerged() + 1);
                report.add(ImportIssueDto.warning(row.getSourceRowNo(), "phone",
                        "Dòng không có số điện thoại — gộp vào chủ xe hiện có của biển số " + row.getLicensePlate() + "."));
                state.customerIdByPlateKey.put(plateKey, ownerId);
                return ownerId;
            }
        }

        Integer created = createCustomer(row, null, report, dryRun, state);
        report.setCustomersCreated(report.getCustomersCreated() + 1);
        report.add(ImportIssueDto.warning(row.getSourceRowNo(), "phone",
                "Dòng không có số điện thoại — tạo khách mới và định danh theo biển số " + row.getLicensePlate()
                        + ". Bổ sung số điện thoại trong hồ sơ khách khi có."));
        state.customerIdByPlateKey.put(plateKey, created);
        return created;
    }

    private Integer createCustomer(ImportRowDto row, String phone, CustomerImportReport report,
                                   boolean dryRun, RunState state) {
        if (dryRun) {
            // Id giả chỉ dùng để đếm trong phiên chạy thử, không bao giờ chạm cơ sở dữ liệu.
            // Bộ đếm riêng để khách nhận theo SĐT và khách nhận theo biển số không đụng id nhau.
            return -(++state.dryRunCustomerSeq);
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

    /* ========================= Sửa tay từng lượt ========================== */

    /**
     * Sửa một lượt đã nhập, ngay trên hồ sơ khách.
     *
     * Dùng lại đúng bộ quy tắc của luồng nhập file — tách giảm giá, dựng lại chuỗi dịch
     * vụ, đánh dấu lệch tiền, tính lại khoá chống trùng — để một lượt sửa tay và một
     * lượt nhập từ file không bao giờ khác chuẩn nhau.
     */
    @Transactional
    public void updateLegacyVisit(Integer legacyVisitId, LegacyVisitUpdateDto dto) {
        LegacyVisitJpa visit = legacyVisitRepo.findById(legacyVisitId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lượt dịch vụ số " + legacyVisitId));

        LocalDate visitedDate = ImportNormalizer.parseDate(dto.getVisitedDate());
        if (visitedDate == null) {
            throw new IllegalArgumentException("Ngày vào xưởng không hợp lệ. Nhập theo dạng yyyy-MM-dd.");
        }

        ImportRowDto probe = new ImportRowDto();
        probe.setItems(dto.getItems());
        VisitLines lines = splitLines(probe);

        // Khoá chống trùng phải tính lại: ngày, mã phiếu và các dòng dịch vụ đều đổi được
        CustomerProfileJpa profile = customerProfileRepo.findByCustomerId(visit.getCustomerId());
        String phone = profile == null ? null : ImportNormalizer.normalizePhone(profile.getPhone());
        String customerRef = phone != null ? phone : "cust:" + visit.getCustomerId();
        String dedupeKey = ImportNormalizer.dedupeKey(customerRef, visitedDate,
                dto.getLegacyTicketCode(), lines.itemNames);
        if (!dedupeKey.equals(visit.getDedupeKey()) && legacyVisitRepo.existsByDedupeKey(dedupeKey)) {
            throw new IllegalArgumentException(
                    "Khách này đã có một lượt y hệt: cùng ngày, cùng mã phiếu và cùng các dòng dịch vụ. "
                            + "Sửa cho khác đi, hoặc xoá bớt lượt thừa.");
        }

        // Sổ cũ chỉ ghi ngày. Lượt nào có giờ thật thì giữ nguyên giờ đó.
        LocalTime keepTime = Boolean.TRUE.equals(visit.getHasTime()) && visit.getVisitedAt() != null
                ? visit.getVisitedAt().toLocalTime()
                : DEFAULT_VISIT_TIME;
        visit.setVisitedAt(LocalDateTime.of(visitedDate, keepTime));

        LocalDate delivered = ImportNormalizer.parseDate(dto.getDeliveredDate());
        visit.setDeliveredAt(delivered == null ? null : LocalDateTime.of(delivered, DEFAULT_VISIT_TIME));

        visit.setLegacyTicketCode(ImportNormalizer.trimToNull(dto.getLegacyTicketCode()));
        visit.setOdometer(dto.getOdometer());
        visit.setCustomerNote(ImportNormalizer.trimToNull(dto.getCustomerNote()));
        visit.setCallNote(ImportNormalizer.trimToNull(dto.getCallNote()));
        visit.setTotalAmount(dto.getTotalAmount());
        visit.setDiscountAmount(lines.discount);
        visit.setServicesText(lines.servicesText);
        visit.setAmountMismatch(isAmountMismatch(dto.getTotalAmount(), lines.itemsTotal));
        visit.setDedupeKey(dedupeKey);
        legacyVisitRepo.save(visit);

        // Ghi lại toàn bộ dòng dịch vụ: sửa tại chỗ từng dòng sẽ lệch số thứ tự khi
        // người dùng chèn hoặc bỏ dòng giữa chừng.
        legacyVisitItemRepo.deleteAll(legacyVisitItemRepo.findByLegacyVisitIdOrderByLineNoAsc(legacyVisitId));
        legacyVisitItemRepo.flush();

        short lineNo = 1;
        List<LegacyVisitItemJpa> entities = new ArrayList<>();
        for (ImportItemDto item : lines.items) {
            LegacyVisitItemJpa entity = new LegacyVisitItemJpa();
            entity.setLegacyVisitId(legacyVisitId);
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

    /**
     * Xoá hẳn một lượt cũ. Chỉ xoá đúng lượt đó — khách và xe giữ nguyên, vì một lượt
     * ghi nhầm không có nghĩa là khách không tồn tại.
     */
    @Transactional
    public void deleteLegacyVisit(Integer legacyVisitId) {
        LegacyVisitJpa visit = legacyVisitRepo.findById(legacyVisitId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lượt dịch vụ số " + legacyVisitId));
        legacyVisitItemRepo.deleteAll(legacyVisitItemRepo.findByLegacyVisitIdOrderByLineNoAsc(legacyVisitId));
        legacyVisitRepo.delete(visit);
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
        if (request.getPlateConflictPolicy() != null) {
            batch.setPlateConflictPolicy(request.getPlateConflictPolicy().name());
        }
        batch.setRowsJson(writeRows(request.getRows()));
        return importBatchRepo.save(batch);
    }

    /** Giữ nguyên văn các dòng đã gửi để mở lô ra sửa lại được, kể cả dòng bị bỏ qua. */
    private String writeRows(List<ImportRowDto> rows) {
        if (rows == null || rows.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(rows);
        } catch (Exception e) {
            log.warn("Không lưu được nội dung gốc của lô nhập: {}", e.getMessage());
            return null;
        }
    }

    private void preloadExistingDedupeKeys(List<ImportRowDto> rows, RunState state) {
        if (rows.isEmpty()) return;
        Set<String> candidates = new HashSet<>();
        for (ImportRowDto row : rows) {
            // Cùng công thức customerRef với processRow: SĐT nếu có, không thì "plate:" + khoá biển số.
            String phone = ImportNormalizer.normalizePhone(row.getPhone());
            String customerRef;
            if (phone != null) {
                customerRef = phone;
            } else {
                String plateKey = ImportNormalizer.normalizePlate(row.getLicensePlate());
                if (plateKey == null) continue;
                customerRef = "plate:" + plateKey;
            }
            LocalDate date = ImportNormalizer.parseDate(row.getVisitedDate());
            if (date == null) date = ImportNormalizer.parseDate(row.getDeliveredDate());
            if (date == null) continue;
            VisitLines lines = splitLines(row);
            candidates.add(ImportNormalizer.dedupeKey(customerRef, date, row.getLegacyTicketCode(), lines.itemNames));
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
        /** Khách nhận diện theo biển số (dòng không có SĐT). Cùng biển số = cùng một khách. */
        final Map<String, Integer> customerIdByPlateKey = new HashMap<>();
        int dryRunCustomerSeq = 0;
        final Map<String, Integer> vehicleIdByPlate = new HashMap<>();
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
