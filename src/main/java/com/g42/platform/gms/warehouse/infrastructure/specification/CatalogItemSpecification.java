package com.g42.platform.gms.warehouse.infrastructure.specification;

import com.g42.platform.gms.warehouse.domain.enums.CatalogItemType;
import com.g42.platform.gms.warehouse.infrastructure.entity.CatalogItemJpa;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CatalogItemSpecification {
    public static Specification<CatalogItemJpa> filterCatalog(CatalogItemType itemType, Boolean isActive, Integer brandId, Integer productLineId, Integer categoryId, BigDecimal minPrice, BigDecimal maxPrice) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (itemType != null) predicates.add(cb.equal(root.get("itemType"), itemType));
            if (isActive != null) predicates.add(cb.equal(root.get("isActive"), isActive));
            if (brandId != null) predicates.add(cb.equal(root.get("brandId"), brandId));
            if (productLineId != null) predicates.add(cb.equal(root.get("productLineId"), productLineId));
            if (categoryId != null) predicates.add(cb.equal(root.get("itemCategoryId"), categoryId));
            if (minPrice != null) predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            if (maxPrice != null) predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
