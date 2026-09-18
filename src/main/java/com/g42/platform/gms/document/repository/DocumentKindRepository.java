package com.g42.platform.gms.document.repository;

import com.g42.platform.gms.document.entity.DataSourceType;
import com.g42.platform.gms.document.entity.DocumentKind;
import com.g42.platform.gms.document.entity.DocumentStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentKindRepository extends JpaRepository<DocumentKind, Integer> {

    List<DocumentKind> findAllByOrderBySortOrderAscNameAsc();

    List<DocumentKind> findByActiveTrueOrderBySortOrderAscNameAsc();

    List<DocumentKind> findByActiveTrueAndDataSourceAndStageOrderBySortOrderAscNameAsc(
            DataSourceType dataSource, DocumentStage stage);

    List<DocumentKind> findByActiveTrueAndDataSourceOrderBySortOrderAscNameAsc(DataSourceType dataSource);

    Optional<DocumentKind> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);
}
