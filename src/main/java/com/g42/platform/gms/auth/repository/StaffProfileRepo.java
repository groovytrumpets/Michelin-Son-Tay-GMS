package com.g42.platform.gms.auth.repository;

import com.g42.platform.gms.auth.entity.Role;
import com.g42.platform.gms.auth.entity.StaffProfile;
import com.g42.platform.gms.auth.entity.StaffRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StaffProfileRepo extends JpaRepository<StaffProfile,Integer> {

    StaffProfile searchByPhone(String phone);

    StaffProfile getStaffProfileByStaffauth_StaffAuthId(Integer staffAuthId);

    @Query("SELECT sp FROM StaffProfile sp JOIN sp.staffRoles sr JOIN sr.role r WHERE r.roleCode = :roleCode")
    List<StaffProfile> findByRoleCode(@Param("roleCode") String roleCode);

    Optional<StaffProfile> findByEmployeeNo(String employeeNo);

    StaffProfile findByPhone(String phone);
    @Query("SELECT sp FROM StaffProfile sp WHERE sp.phone LIKE %:phone%")
    StaffProfile findByPhoneEquals(@Param("phone")String phone);

    // Dùng cho danh bạ chat nội bộ (GET /api/chat/contacts?search=)
    List<StaffProfile> findByFullNameContainingIgnoreCase(String fullName);

    /** [staffId, fullName, phone, position] của nhân viên chưa nghỉ việc — ô chọn quản lý kho. */
    @Query("SELECT sp.staffId, sp.fullName, sp.phone, sp.position FROM StaffProfile sp "
            + "WHERE sp.isResigned IS NULL OR sp.isResigned = false ORDER BY sp.fullName")
    List<Object[]> findActiveStaffBrief();

    /** [staffId, fullName] theo danh sách id — hiển thị tên quản lý kho. */
    @Query("SELECT sp.staffId, sp.fullName FROM StaffProfile sp WHERE sp.staffId IN :ids")
    List<Object[]> findNamesByIds(@Param("ids") java.util.Collection<Integer> ids);
}
