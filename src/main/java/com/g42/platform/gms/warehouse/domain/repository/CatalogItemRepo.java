package com.g42.platform.gms.warehouse.domain.repository;

import com.g42.platform.gms.warehouse.api.dto.SpecificationRespondDto;
import com.g42.platform.gms.warehouse.domain.entity.*;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Repository
public interface CatalogItemRepo {
    List<Brand> getAllBrands();

    List<Specification> getAllSpecs();

    List<ProductLine> getAllProductLines();

    List<SpecAttribute> getAllSpecAttibutes();

    Brand createBrand(Brand brand);

    Brand getBrandById(Integer brandId);

    CatalogItem createCatalog(CatalogItem domain);

    ProductLine saveProductLine(ProductLine productLine);

    boolean exitBySku(String sku);

    WorkCategory saveItemCate(WorkCategory itemCategory);

    ProductLine getProductLineById(Integer productLineId);

    List<Specification> getListOfSpecsByItem(Integer itemId);

    WorkCategory getItemCategoryById(@NotNull Integer itemCategoryId);

    CatalogItem saveCatalogItem(CatalogItem catalogItem);

    boolean exitByCategoryCode(String categoryCode);

    Specification saveSpec(Specification specification);

    SpecAttribute saveSpecAttribute(SpecAttribute specAttribute);

    List<WorkCategory> getAllItemCategory();

    SpecAttribute getSpecAttributeById(Integer attributeId);

    CatalogItem getCatalogItemById(Integer itemId);

    Integer findCategoryCode(String categoryCode);

    Map<Integer, String> getAllBrandByIds(Set<Integer> brandIds);

    Map<Integer, String> findAllLinesByIds(Set<Integer> lineIds);

    List<SpecificationRespondDto> getAllSpecsByItemId(Integer catalogItemId);

    Map<Integer, String> findAllCatesByIds(Set<Integer> categoryIds);

    int findCategoryMaxOrder();
}
