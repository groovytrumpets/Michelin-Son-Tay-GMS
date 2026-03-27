package com.g42.platform.gms.warehouse.domain.repository;

import com.g42.platform.gms.warehouse.domain.entity.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CatalogItemRepo {
    List<Brand> getAllBrands();

    List<Specification> getAllSpecs();

    List<ProductLine> getAllProductLines();

    List<SpecAttribute> getAllSpecAttibutes();

    Brand createBrand(Brand brand);

    Brand getBrandById(Integer brandId);

    CatalogItem createCatalog(CatalogItem domain);
}
