package com.g42.platform.gms.warehouse.domain.repository;

import com.g42.platform.gms.warehouse.domain.entity.Brand;
import com.g42.platform.gms.warehouse.domain.entity.ProductLine;
import com.g42.platform.gms.warehouse.domain.entity.SpecAttribute;
import com.g42.platform.gms.warehouse.domain.entity.Specification;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CatalogItemRepo {
    List<Brand> getAllBrands();

    List<Specification> getAllSpecs();

    List<ProductLine> getAllProductLines();

    List<SpecAttribute> getAllSpecAttibutes();
}
