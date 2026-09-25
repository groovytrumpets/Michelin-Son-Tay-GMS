package com.g42.platform.gms.branch.service;

import com.g42.platform.gms.branch.dto.BranchDto;
import com.g42.platform.gms.branch.entity.Branch;
import com.g42.platform.gms.branch.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class BranchService {

    private static final Pattern CODE_PATTERN = Pattern.compile("[A-Z0-9_-]{1,20}");

    private final BranchRepository branchRepository;
    private final BranchDirectory branchDirectory;

    @Transactional(readOnly = true)
    public List<BranchDto> listActive() {
        return branchRepository.findAllByOrderBySortOrderAscBranchIdAsc().stream()
                .filter(b -> Boolean.TRUE.equals(b.getIsActive()))
                .map(b -> toDto(b, null))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BranchDto> listAll() {
        return branchRepository.findAllByOrderBySortOrderAscBranchIdAsc().stream()
                .map(b -> toDto(b, branchRepository.countTickets(b.getBranchId())))
                .toList();
    }

    @Transactional
    public BranchDto create(BranchDto dto) {
        Branch branch = new Branch();
        applyEditable(branch, dto, null);
        boolean first = branchRepository.count() == 0;
        branch.setIsActive(true);
        branch.setIsDefault(false);
        Branch saved = branchRepository.save(branch);
        if (first || Boolean.TRUE.equals(dto.getIsDefault())) {
            branchRepository.markOnlyDefault(saved.getBranchId());
            saved.setIsDefault(true);
        }
        refreshAfterCommit();
        return toDto(saved, 0L);
    }

    @Transactional
    public BranchDto update(Integer branchId, BranchDto dto) {
        Branch branch = load(branchId);
        applyEditable(branch, dto, branchId);

        if (dto.getIsActive() != null && !dto.getIsActive().equals(branch.getIsActive())) {
            if (!dto.getIsActive() && Boolean.TRUE.equals(branch.getIsDefault())) {
                throw bad("Không ẩn được xưởng mặc định. Hãy đặt xưởng khác làm mặc định trước.");
            }
            branch.setIsActive(dto.getIsActive());
        }
        Branch saved = branchRepository.save(branch);
        if (Boolean.TRUE.equals(dto.getIsDefault()) && !Boolean.TRUE.equals(saved.getIsDefault())) {
            makeDefault(saved);
        }
        refreshAfterCommit();
        return toDto(saved, branchRepository.countTickets(branchId));
    }

    @Transactional
    public BranchDto setDefault(Integer branchId) {
        Branch branch = load(branchId);
        makeDefault(branch);
        refreshAfterCommit();
        return toDto(branch, branchRepository.countTickets(branchId));
    }

    /** Chỉ xoá được xưởng tạo nhầm, chưa có lịch hẹn / phiếu nào; còn lại thì ẩn. */
    @Transactional
    public void delete(Integer branchId) {
        Branch branch = load(branchId);
        if (Boolean.TRUE.equals(branch.getIsDefault())) {
            throw bad("Không xoá được xưởng mặc định.");
        }
        if (branchRepository.countTickets(branchId) > 0 || branchRepository.countBookings(branchId) > 0) {
            throw bad("Xưởng đã có lịch hẹn / phiếu nên không xoá được, chỉ ẩn được.");
        }
        branchRepository.delete(branch);
        refreshAfterCommit();
    }

    private void makeDefault(Branch branch) {
        if (!Boolean.TRUE.equals(branch.getIsActive())) {
            throw bad("Xưởng đang ẩn không đặt làm mặc định được.");
        }
        branchRepository.markOnlyDefault(branch.getBranchId());
        branch.setIsDefault(true);
    }

    private void applyEditable(Branch branch, BranchDto dto, Integer selfId) {
        if (dto.getBranchName() != null || selfId == null) {
            String name = trimToNull(dto.getBranchName());
            if (name == null) {
                throw bad("Vui lòng nhập tên xưởng.");
            }
            branch.setBranchName(name);
        }
        if (dto.getBranchCode() != null || selfId == null) {
            String code = trimToNull(dto.getBranchCode());
            code = code == null ? null : code.toUpperCase(Locale.ROOT);
            if (code == null || !CODE_PATTERN.matcher(code).matches()) {
                throw bad("Mã xưởng chỉ gồm chữ không dấu, số, gạch ngang (tối đa 20 ký tự), VD ST, HN1.");
            }
            boolean taken = selfId == null
                    ? branchRepository.existsByBranchCodeIgnoreCase(code)
                    : branchRepository.existsByBranchCodeIgnoreCaseAndBranchIdNot(code, selfId);
            if (taken) {
                throw bad("Mã xưởng " + code + " đã được dùng.");
            }
            branch.setBranchCode(code);
        }
        if (dto.getAddress() != null) branch.setAddress(trimToNull(dto.getAddress()));
        if (dto.getPhone() != null) branch.setPhone(trimToNull(dto.getPhone()));
        if (dto.getSortOrder() != null) branch.setSortOrder(dto.getSortOrder());
    }

    private Branch load(Integer branchId) {
        return branchRepository.findById(branchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy xưởng " + branchId));
    }

    private void refreshAfterCommit() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    branchDirectory.invalidate();
                }
            });
        } else {
            branchDirectory.invalidate();
        }
    }

    private static BranchDto toDto(Branch b, Long ticketCount) {
        return BranchDto.builder()
                .branchId(b.getBranchId())
                .branchCode(b.getBranchCode())
                .branchName(b.getBranchName())
                .address(b.getAddress())
                .phone(b.getPhone())
                .isDefault(b.getIsDefault())
                .isActive(b.getIsActive())
                .sortOrder(b.getSortOrder())
                .ticketCount(ticketCount)
                .build();
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
