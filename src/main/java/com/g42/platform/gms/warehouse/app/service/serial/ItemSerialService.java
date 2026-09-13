package com.g42.platform.gms.warehouse.app.service.serial;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.g42.platform.gms.common.util.Qty;
import com.g42.platform.gms.warehouse.api.dto.serial.ItemSerialDto;
import com.g42.platform.gms.warehouse.api.dto.serial.RegisterSerialsRequest;
import com.g42.platform.gms.warehouse.app.service.catalog.ItemQuantityPolicy;
import com.g42.platform.gms.warehouse.domain.enums.SerialStatus;
import com.g42.platform.gms.warehouse.domain.enums.StockEntryStatus;
import com.g42.platform.gms.warehouse.domain.exception.WarehouseErrorCode;
import com.g42.platform.gms.warehouse.domain.exception.WarehouseException;
import com.g42.platform.gms.warehouse.infrastructure.entity.CatalogItemJpa;
import com.g42.platform.gms.warehouse.infrastructure.entity.ItemSerialJpa;
import com.g42.platform.gms.warehouse.infrastructure.entity.StockEntryItemJpa;
import com.g42.platform.gms.warehouse.infrastructure.entity.StockEntryJpa;
import com.g42.platform.gms.warehouse.infrastructure.entity.WarehouseJpa;
import com.g42.platform.gms.warehouse.infrastructure.repository.CatalogItemJpaRepo;
import com.g42.platform.gms.warehouse.infrastructure.repository.ItemSerialJpaRepo;
import com.g42.platform.gms.warehouse.infrastructure.repository.StockEntryItemJpaRepo;
import com.g42.platform.gms.warehouse.infrastructure.repository.StockEntryJpaRepo;
import com.g42.platform.gms.warehouse.infrastructure.repository.WarehouseJpaRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Số serial của hàng theo dõi từng chiếc.
 *
 * Serial là lớp định danh nằm TRÊN lô, không thay lô: tồn kho, giá vốn và FIFO vẫn
 * tính theo stock_entry_item như mọi sản phẩm khác, mỗi serial chỉ trỏ về lô chứa
 * nó. Nhờ vậy bất biến "tồn kho = tổng số còn lại của các lô" giữ nguyên, còn serial
 * chỉ thêm một ràng buộc: số serial còn trong kho của một lô không vượt quá số còn
 * lại của lô đó.
 *
 * Vòng đời: IN_STOCK → RESERVED (dòng báo giá chọn) → SOLD (phiếu xuất xác nhận);
 * hoàn hàng đưa SOLD về IN_STOCK hoặc DEFECTIVE.
 */
@Service
@RequiredArgsConstructor
public class ItemSerialService {

    private static final List<SerialStatus> ACTIVE = List.of(SerialStatus.IN_STOCK, SerialStatus.RESERVED);
    private static final int MAX_CODE_LENGTH = 100;

    private final ItemSerialJpaRepo serialRepo;
    private final CatalogItemJpaRepo catalogItemJpaRepo;
    private final StockEntryItemJpaRepo stockEntryItemJpaRepo;
    private final StockEntryJpaRepo stockEntryJpaRepo;
    private final WarehouseJpaRepo warehouseJpaRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ─── Đọc ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ItemSerialDto> list(Integer itemId, Integer warehouseId, List<SerialStatus> statuses) {
        List<SerialStatus> wanted = statuses == null || statuses.isEmpty() ? Arrays.asList(SerialStatus.values()) : statuses;
        List<ItemSerialJpa> rows = warehouseId != null
                ? serialRepo.findByItemIdAndWarehouseIdAndStatusInOrderBySerialIdAsc(itemId, warehouseId, wanted)
                : serialRepo.findByItemIdAndStatusInOrderBySerialIdAsc(itemId, wanted);
        return toDtos(rows);
    }

    @Transactional(readOnly = true)
    public List<ItemSerialDto> listByIssue(Integer issueId) {
        return toDtos(serialRepo.findByIssueIdOrderBySerialIdAsc(issueId));
    }

    /** Số serial còn trong kho (IN_STOCK + RESERVED) theo từng lô. */
    @Transactional(readOnly = true)
    public Map<Integer, Long> countActiveByLots(Collection<Integer> lotIds) {
        if (lotIds == null || lotIds.isEmpty()) return Map.of();
        Map<Integer, Long> result = new HashMap<>();
        for (Object[] row : serialRepo.countByLots(lotIds, ACTIVE)) {
            result.put((Integer) row[0], ((Number) row[1]).longValue());
        }
        return result;
    }

    // ─── Khai báo serial ─────────────────────────────────────────────────────

    /** Khai báo serial cho hàng đã nằm sẵn trong một lô đã nhập. */
    @Transactional
    public List<ItemSerialDto> registerForLot(RegisterSerialsRequest request, Integer staffId) {
        CatalogItemJpa item = requireSerialItem(request.getItemId());
        StockEntryItemJpa lot = stockEntryItemJpaRepo.findById(request.getEntryItemId())
                .orElseThrow(() -> invalid("Không tìm thấy lô id=" + request.getEntryItemId()));
        if (!lot.getItemId().equals(item.getItemId())) {
            throw invalid("Lô này không thuộc sản phẩm " + item.getItemName());
        }
        StockEntryJpa entry = stockEntryJpaRepo.findById(lot.getEntryId())
                .orElseThrow(() -> invalid("Không tìm thấy phiếu nhập của lô"));
        if (entry.getStatus() != StockEntryStatus.CONFIRMED) {
            throw invalid("Phiếu nhập của lô chưa xác nhận — khai báo serial ngay trên phiếu nhập");
        }

        List<String> codes = normalizeCodes(request.getSerialCodes());
        long active = serialRepo.countByEntryItemIdAndStatusIn(lot.getEntryItemId(), ACTIVE);
        BigDecimal capacity = Qty.nz(lot.getRemainingQuantity()).subtract(BigDecimal.valueOf(active));
        if (BigDecimal.valueOf(codes.size()).compareTo(capacity) > 0) {
            throw invalid("Lô " + entry.getEntryCode() + " còn " + Qty.text(lot.getRemainingQuantity())
                    + " " + unitOf(item) + " và đã có " + active + " serial — chỉ khai báo thêm được "
                    + Qty.text(capacity.max(BigDecimal.ZERO)) + " serial");
        }
        ensureCodesUnused(item, codes);

        List<ItemSerialJpa> created = new ArrayList<>();
        for (String code : codes) {
            created.add(newSerial(item.getItemId(), entry.getWarehouseId(), lot.getEntryItemId(), code,
                    request.getAttributesJson(), staffId));
        }
        return toDtos(serialRepo.saveAll(created));
    }

    /**
     * Kiểm tra serial nhập kèm một dòng phiếu nhập còn nháp.
     * Được phép khai báo ít hơn số lượng (bổ sung sau), không được nhiều hơn.
     */
    public List<String> validateEntrySerials(Integer itemId, BigDecimal stockQuantity, List<String> rawCodes) {
        if (rawCodes == null || rawCodes.isEmpty()) return List.of();
        CatalogItemJpa item = requireSerialItem(itemId);
        List<String> codes = normalizeCodes(rawCodes);
        if (BigDecimal.valueOf(codes.size()).compareTo(Qty.nz(stockQuantity)) > 0) {
            throw invalid(item.getItemName() + ": nhập " + codes.size() + " serial nhưng số lượng chỉ có "
                    + Qty.text(stockQuantity));
        }
        ensureCodesUnused(item, codes);
        return codes;
    }

    /** Phiếu nhập được xác nhận: sinh serial từ danh sách nhập kèm của từng dòng. */
    @Transactional
    public void createForConfirmedEntryItem(Integer warehouseId, Integer entryItemId, Integer itemId,
                                            String serialCodesJson, Integer staffId) {
        List<String> codes = parseCodes(serialCodesJson);
        if (codes.isEmpty()) return;
        CatalogItemJpa item = requireSerialItem(itemId);
        codes = normalizeCodes(codes);
        ensureCodesUnused(item, codes);
        List<ItemSerialJpa> created = new ArrayList<>();
        for (String code : codes) {
            created.add(newSerial(itemId, warehouseId, entryItemId, code, null, staffId));
        }
        serialRepo.saveAll(created);
    }

    // ─── Báo giá giữ serial ──────────────────────────────────────────────────

    /** Một dòng báo giá cần đồng bộ serial. */
    public record EstimateSerialLine(Integer estimateItemId, Integer itemId, Integer warehouseId,
                                     Integer entryItemId, BigDecimal quantity, List<Integer> serialIds,
                                     boolean removed) {
    }

    /**
     * Đồng bộ serial đang giữ với danh sách dòng của MỘT bản báo giá.
     * Serial được chọn → RESERVED cho dòng; serial dòng đó từng giữ mà nay bỏ chọn → IN_STOCK.
     *
     * supersededItemIds: dòng của các bản báo giá cũ hơn cùng phiếu. Bản mới thay thế
     * bản cũ nên được lấy lại serial bản cũ đang giữ, và serial bản cũ giữ mà bản mới
     * không chọn nữa thì nhả về kho.
     */
    @Transactional
    public void syncEstimateReservations(List<EstimateSerialLine> lines, Set<Integer> supersededItemIds) {
        if (lines == null || lines.isEmpty()) return;
        Set<Integer> superseded = supersededItemIds == null ? Set.of() : supersededItemIds;
        Set<Integer> lineIds = lines.stream().map(EstimateSerialLine::estimateItemId)
                .filter(Objects::nonNull).collect(Collectors.toCollection(HashSet::new));
        Map<Integer, Set<Integer>> keepByLine = new HashMap<>();

        for (EstimateSerialLine line : lines) {
            if (line.estimateItemId() == null || line.removed() || line.itemId() == null) continue;
            List<Integer> wanted = line.serialIds() == null ? List.of() : line.serialIds().stream()
                    .filter(Objects::nonNull).distinct().toList();
            if (wanted.isEmpty()) continue;

            CatalogItemJpa item = requireSerialItem(line.itemId());
            if (BigDecimal.valueOf(wanted.size()).compareTo(Qty.nz(line.quantity())) != 0) {
                throw invalid(item.getItemName() + ": đã chọn " + wanted.size() + " serial nhưng số lượng là "
                        + Qty.text(line.quantity()));
            }

            List<ItemSerialJpa> serials = serialRepo.lockByIds(wanted);
            if (serials.size() != wanted.size()) {
                throw invalid(item.getItemName() + ": có serial không còn tồn tại, hãy chọn lại");
            }
            for (ItemSerialJpa serial : serials) {
                if (!serial.getItemId().equals(line.itemId())) {
                    throw invalid("Serial " + serial.getSerialCode() + " không thuộc sản phẩm " + item.getItemName());
                }
                if (line.warehouseId() != null && !serial.getWarehouseId().equals(line.warehouseId())) {
                    throw invalid("Serial " + serial.getSerialCode() + " không nằm trong kho đã chọn");
                }
                if (line.entryItemId() != null && line.entryItemId() > 0
                        && !serial.getEntryItemId().equals(line.entryItemId())) {
                    throw invalid("Serial " + serial.getSerialCode() + " không thuộc lô đã chọn của dòng");
                }
                boolean heldByThisLine = serial.getStatus() == SerialStatus.RESERVED
                        && (line.estimateItemId().equals(serial.getEstimateItemId())
                            || superseded.contains(serial.getEstimateItemId()));
                if (serial.getStatus() != SerialStatus.IN_STOCK && !heldByThisLine) {
                    throw invalid("Serial " + serial.getSerialCode() + " đang "
                            + (serial.getStatus() == SerialStatus.RESERVED ? "được giữ cho báo giá khác" : "không còn trong kho"));
                }
                serial.setStatus(SerialStatus.RESERVED);
                serial.setEstimateItemId(line.estimateItemId());
            }
            serialRepo.saveAll(serials);
            keepByLine.put(line.estimateItemId(), new HashSet<>(wanted));
        }

        Set<Integer> holders = new HashSet<>(lineIds);
        holders.addAll(superseded);
        List<ItemSerialJpa> previouslyHeld = serialRepo.findByEstimateItemIdIn(holders);
        List<ItemSerialJpa> toRelease = previouslyHeld.stream()
                .filter(s -> s.getStatus() == SerialStatus.RESERVED)
                .filter(s -> !keepByLine.getOrDefault(s.getEstimateItemId(), Set.of()).contains(s.getSerialId()))
                .filter(s -> keepByLine.values().stream().noneMatch(kept -> kept.contains(s.getSerialId())))
                .toList();
        release(toRelease);
    }

    /** Nhả serial đang giữ cho các dòng báo giá (huỷ giữ hàng, bỏ dòng...). */
    @Transactional
    public void releaseByEstimateItems(Collection<Integer> estimateItemIds) {
        if (estimateItemIds == null || estimateItemIds.isEmpty()) return;
        List<ItemSerialJpa> held = serialRepo.findByEstimateItemIdIn(
                estimateItemIds.stream().filter(Objects::nonNull).collect(Collectors.toSet()));
        release(held.stream().filter(s -> s.getStatus() == SerialStatus.RESERVED).toList());
    }

    // ─── Xuất kho ────────────────────────────────────────────────────────────

    /** Dòng phiếu xuất tối giản để service không phụ thuộc domain của phiếu xuất. */
    public record IssueLine(Integer itemId, Integer entryItemId, BigDecimal quantity) {
    }

    /**
     * Phiếu xuất xác nhận: đánh dấu SOLD đúng số serial của từng lô.
     * Ưu tiên serial các dòng báo giá của phiếu đã chọn; thiếu thì lấy serial còn trong kho
     * của lô theo thứ tự khai báo.
     */
    @Transactional
    public void consumeForIssue(Integer issueId, List<IssueLine> lines, Collection<Integer> estimateItemIds) {
        if (lines == null || lines.isEmpty()) return;
        Set<Integer> itemIds = lines.stream().map(IssueLine::itemId).collect(Collectors.toSet());
        Map<Integer, CatalogItemJpa> serialItems = catalogItemJpaRepo.findAllById(itemIds).stream()
                .filter(ItemQuantityPolicy::tracksSerial)
                .collect(Collectors.toMap(CatalogItemJpa::getItemId, i -> i));
        if (serialItems.isEmpty()) return;

        List<ItemSerialJpa> reservedForIssue = estimateItemIds == null || estimateItemIds.isEmpty()
                ? new ArrayList<>()
                : new ArrayList<>(serialRepo.findByEstimateItemIdIn(new HashSet<>(estimateItemIds)).stream()
                        .filter(s -> s.getStatus() == SerialStatus.RESERVED)
                        .toList());

        for (IssueLine line : lines) {
            CatalogItemJpa item = serialItems.get(line.itemId());
            if (item == null) continue;
            if (line.entryItemId() == null || line.entryItemId() <= 0) {
                throw invalid(item.getItemName() + ": không đủ lô hàng để xuất theo serial");
            }
            int needed = Qty.nz(line.quantity()).intValueExact();

            List<ItemSerialJpa> picked = new ArrayList<>();
            Iterator<ItemSerialJpa> it = reservedForIssue.iterator();
            while (it.hasNext() && picked.size() < needed) {
                ItemSerialJpa s = it.next();
                if (s.getItemId().equals(line.itemId()) && s.getEntryItemId().equals(line.entryItemId())) {
                    picked.add(s);
                    it.remove();
                }
            }
            if (picked.size() < needed) {
                for (ItemSerialJpa s : serialRepo.lockByLotAndStatus(line.entryItemId(), SerialStatus.IN_STOCK)) {
                    if (picked.size() >= needed) break;
                    picked.add(s);
                }
            }
            if (picked.size() < needed) {
                String lotCode = stockEntryItemJpaRepo.findById(line.entryItemId())
                        .flatMap(l -> stockEntryJpaRepo.findById(l.getEntryId()))
                        .map(StockEntryJpa::getEntryCode).orElse("#" + line.entryItemId());
                throw invalid(item.getItemName() + ": lô " + lotCode + " cần xuất " + needed + " chiếc nhưng chỉ có "
                        + picked.size() + " serial sẵn sàng — khai báo serial cho lô trước khi xuất kho");
            }
            for (ItemSerialJpa s : picked) {
                s.setStatus(SerialStatus.SOLD);
                s.setIssueId(issueId);
                s.setEstimateItemId(null);
            }
            serialRepo.saveAll(picked);
        }
    }

    // ─── Hoàn hàng ───────────────────────────────────────────────────────────

    /**
     * Khách trả hàng: đưa serial đã bán ở phiếu xuất gốc về kho (hoặc kho hàng lỗi).
     * @return danh sách serial đã hoàn, để lưu lại trên dòng phiếu hoàn
     */
    @Transactional
    public List<Integer> restoreForReturn(Integer sourceIssueId, Integer itemId, Integer entryItemId,
                                          BigDecimal quantity,
                                          Integer targetWarehouseId, boolean defective) {
        CatalogItemJpa item = catalogItemJpaRepo.findById(itemId).orElse(null);
        if (!ItemQuantityPolicy.tracksSerial(item) || sourceIssueId == null || entryItemId == null) {
            return List.of();
        }
        int needed = Qty.nz(quantity).intValueExact();
        List<ItemSerialJpa> sold = serialRepo.lockSoldByIssueAndLot(sourceIssueId, entryItemId, SerialStatus.SOLD);
        List<ItemSerialJpa> picked = sold.size() > needed ? sold.subList(0, needed) : sold;
        if (picked.size() < needed) {
            throw invalid(item.getItemName() + ": phiếu xuất gốc chỉ còn " + picked.size()
                    + " serial chưa hoàn, không đủ " + needed);
        }
        List<ItemSerialJpa> restored = picked;
        for (ItemSerialJpa s : restored) {
            s.setStatus(defective ? SerialStatus.DEFECTIVE : SerialStatus.IN_STOCK);
            s.setIssueId(null);
            if (targetWarehouseId != null) s.setWarehouseId(targetWarehouseId);
        }
        serialRepo.saveAll(restored);
        return restored.stream().map(ItemSerialJpa::getSerialId).toList();
    }

    // ─── Hỗ trợ ──────────────────────────────────────────────────────────────

    /** Mã serial theo đúng thứ tự id truyền vào. */
    @Transactional(readOnly = true)
    public List<String> codesOf(List<Integer> serialIds) {
        if (serialIds == null || serialIds.isEmpty()) return List.of();
        Map<Integer, String> codeById = serialRepo.findAllById(serialIds).stream()
                .collect(Collectors.toMap(ItemSerialJpa::getSerialId, ItemSerialJpa::getSerialCode));
        return serialIds.stream().map(codeById::get).filter(Objects::nonNull).toList();
    }

    public List<Integer> parseIds(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            List<Integer> ids = objectMapper.readValue(json, new TypeReference<List<Integer>>() {});
            return ids == null ? List.of() : ids.stream().filter(Objects::nonNull).toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    public String toJson(Collection<?> values) {
        if (values == null || values.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(values);
        } catch (Exception e) {
            return null;
        }
    }

    public List<String> parseCodes(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            List<String> codes = objectMapper.readValue(json, new TypeReference<List<String>>() {});
            return codes == null ? List.of() : codes;
        } catch (Exception e) {
            return List.of();
        }
    }

    private void release(List<ItemSerialJpa> serials) {
        if (serials.isEmpty()) return;
        for (ItemSerialJpa s : serials) {
            s.setStatus(SerialStatus.IN_STOCK);
            s.setEstimateItemId(null);
        }
        serialRepo.saveAll(serials);
    }

    private CatalogItemJpa requireSerialItem(Integer itemId) {
        CatalogItemJpa item = itemId == null ? null : catalogItemJpaRepo.findById(itemId).orElse(null);
        if (item == null) throw invalid("Không tìm thấy sản phẩm id=" + itemId);
        if (!ItemQuantityPolicy.tracksSerial(item)) {
            throw invalid(item.getItemName() + " không bật theo dõi số serial");
        }
        return item;
    }

    private List<String> normalizeCodes(List<String> raw) {
        LinkedHashSet<String> codes = new LinkedHashSet<>();
        List<String> duplicates = new ArrayList<>();
        for (String value : raw == null ? List.<String>of() : raw) {
            String code = value == null ? "" : value.trim();
            if (code.isEmpty()) continue;
            if (code.length() > MAX_CODE_LENGTH) throw invalid("Serial quá dài (tối đa " + MAX_CODE_LENGTH + " ký tự): " + code);
            if (!codes.add(code)) duplicates.add(code);
        }
        if (!duplicates.isEmpty()) throw invalid("Serial bị nhập trùng: " + String.join(", ", duplicates));
        if (codes.isEmpty()) throw invalid("Chưa nhập serial nào");
        return new ArrayList<>(codes);
    }

    private void ensureCodesUnused(CatalogItemJpa item, List<String> codes) {
        List<ItemSerialJpa> existing = serialRepo.findByItemIdAndSerialCodeIn(item.getItemId(), codes);
        if (!existing.isEmpty()) {
            throw invalid(item.getItemName() + ": serial đã tồn tại — "
                    + existing.stream().map(ItemSerialJpa::getSerialCode).collect(Collectors.joining(", ")));
        }
    }

    private ItemSerialJpa newSerial(Integer itemId, Integer warehouseId, Integer entryItemId, String code,
                                    String attributesJson, Integer staffId) {
        ItemSerialJpa serial = new ItemSerialJpa();
        serial.setItemId(itemId);
        serial.setWarehouseId(warehouseId);
        serial.setEntryItemId(entryItemId);
        serial.setSerialCode(code);
        serial.setStatus(SerialStatus.IN_STOCK);
        serial.setAttributesJson(attributesJson);
        serial.setCreatedBy(staffId);
        return serial;
    }

    private List<ItemSerialDto> toDtos(List<ItemSerialJpa> rows) {
        if (rows.isEmpty()) return List.of();
        Set<Integer> lotIds = rows.stream().map(ItemSerialJpa::getEntryItemId).collect(Collectors.toSet());
        Map<Integer, StockEntryItemJpa> lots = stockEntryItemJpaRepo.findAllById(lotIds).stream()
                .collect(Collectors.toMap(StockEntryItemJpa::getEntryItemId, l -> l));
        Set<Integer> entryIds = lots.values().stream().map(StockEntryItemJpa::getEntryId).collect(Collectors.toSet());
        Map<Integer, StockEntryJpa> entries = stockEntryJpaRepo.findAllById(entryIds).stream()
                .collect(Collectors.toMap(StockEntryJpa::getEntryId, e -> e));
        Set<Integer> warehouseIds = rows.stream().map(ItemSerialJpa::getWarehouseId).collect(Collectors.toSet());
        Map<Integer, String> warehouseNames = warehouseJpaRepo.findAllById(warehouseIds).stream()
                .collect(Collectors.toMap(WarehouseJpa::getWarehouseId,
                        w -> w.getWarehouseName() != null ? w.getWarehouseName() : String.valueOf(w.getWarehouseId())));

        List<ItemSerialDto> result = new ArrayList<>();
        for (ItemSerialJpa row : rows) {
            ItemSerialDto dto = new ItemSerialDto();
            dto.setSerialId(row.getSerialId());
            dto.setItemId(row.getItemId());
            dto.setWarehouseId(row.getWarehouseId());
            dto.setWarehouseName(warehouseNames.get(row.getWarehouseId()));
            dto.setEntryItemId(row.getEntryItemId());
            StockEntryItemJpa lot = lots.get(row.getEntryItemId());
            if (lot != null) {
                dto.setImportPrice(lot.getImportPrice());
                StockEntryJpa entry = entries.get(lot.getEntryId());
                if (entry != null) {
                    dto.setEntryCode(entry.getEntryCode());
                    dto.setEntryDate(entry.getEntryDate());
                }
            }
            dto.setSerialCode(row.getSerialCode());
            dto.setStatus(row.getStatus().name());
            dto.setEstimateItemId(row.getEstimateItemId());
            dto.setIssueId(row.getIssueId());
            dto.setAttributesJson(row.getAttributesJson());
            dto.setNotes(row.getNotes());
            dto.setCreatedAt(row.getCreatedAt());
            result.add(dto);
        }
        return result;
    }

    private static String unitOf(CatalogItemJpa item) {
        return item.getUnit() != null && !item.getUnit().isBlank() ? item.getUnit() : "chiếc";
    }

    private static WarehouseException invalid(String message) {
        return new WarehouseException(message, WarehouseErrorCode.INVALID_SERIAL);
    }
}
