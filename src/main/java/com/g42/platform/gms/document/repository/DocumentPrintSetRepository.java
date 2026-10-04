package com.g42.platform.gms.document.repository;

import com.g42.platform.gms.document.entity.DocumentPrintSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentPrintSetRepository extends JpaRepository<DocumentPrintSet, String> {
}
