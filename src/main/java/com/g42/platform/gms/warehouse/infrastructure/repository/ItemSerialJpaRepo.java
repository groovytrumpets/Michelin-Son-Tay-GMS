package com.g42.platform.gms.warehouse.infrastructure.repository;

import com.g42.platform.gms.warehouse.domain.enums.SerialStatus;
import com.g42.platform.gms.warehouse.infrastructure.entity.ItemSerialJpa;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ItemSerialJpaRepo extends JpaRepository<ItemSerialJpa, Integer> {

    List<ItemSerialJpa> findByItemIdAndSerialCodeIn(Integer itemId, Collection<String> serialCodes);

    List<ItemSerialJpa> findByItemIdAndWarehouseIdAndStatusInOrderBySerialIdAsc(
            Integer itemId, Integer warehouseId, Collection<SerialStatus> statuses);

    List<ItemSerialJpa> findByItemIdAndStatusInOrderBySerialIdAsc(Integer itemId, Collection<SerialStatus> statuses);

    List<ItemSerialJpa> findByEstimateItemIdIn(Collection<Integer> estimateItemIds);

    List<ItemSerialJpa> findByIssueIdOrderBySerialIdAsc(Integer issueId);

    long countByEntryItemIdAndStatusIn(Integer entryItemId, Collection<SerialStatus> statuses);

    boolean existsByItemIdAndStatus(Integer itemId, SerialStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ItemSerialJpa s WHERE s.serialId IN :ids")
    List<ItemSerialJpa> lockByIds(@Param("ids") Collection<Integer> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ItemSerialJpa s WHERE s.entryItemId = :entryItemId AND s.status = :status ORDER BY s.serialId ASC")
    List<ItemSerialJpa> lockByLotAndStatus(@Param("entryItemId") Integer entryItemId, @Param("status") SerialStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ItemSerialJpa s WHERE s.issueId = :issueId AND s.entryItemId = :entryItemId AND s.status = :status ORDER BY s.serialId ASC")
    List<ItemSerialJpa> lockSoldByIssueAndLot(@Param("issueId") Integer issueId,
                                            @Param("entryItemId") Integer entryItemId,
                                            @Param("status") SerialStatus status);

    /** Số serial còn giữ chỗ trong lô theo từng lô — hiển thị ở màn tồn kho. */
    @Query("SELECT s.entryItemId, COUNT(s) FROM ItemSerialJpa s WHERE s.entryItemId IN :lotIds AND s.status IN :statuses GROUP BY s.entryItemId")
    List<Object[]> countByLots(@Param("lotIds") Collection<Integer> lotIds, @Param("statuses") Collection<SerialStatus> statuses);
}
