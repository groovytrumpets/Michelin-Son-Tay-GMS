package com.g42.platform.gms.branch.repository;

import com.g42.platform.gms.branch.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BranchRepository extends JpaRepository<Branch, Integer> {

    List<Branch> findAllByOrderBySortOrderAscBranchIdAsc();

    boolean existsByBranchCodeIgnoreCase(String branchCode);

    boolean existsByBranchCodeIgnoreCaseAndBranchIdNot(String branchCode, Integer branchId);

    @Modifying
    @Query("update Branch b set b.isDefault = case when b.branchId = :branchId then true else false end")
    int markOnlyDefault(@Param("branchId") Integer branchId);

    // Hai bảng dưới không thuộc module này nên đếm bằng SQL thuần thay vì kéo entity của
    // module khác vào — chỉ cần biết xưởng đã có phiếu hay chưa trước khi cho xoá.
    @Query(value = "SELECT COUNT(*) FROM service_ticket WHERE branch_id = :branchId", nativeQuery = true)
    long countTickets(@Param("branchId") Integer branchId);

    @Query(value = "SELECT COUNT(*) FROM booking WHERE branch_id = :branchId", nativeQuery = true)
    long countBookings(@Param("branchId") Integer branchId);
}
