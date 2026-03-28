package com.g42.platform.gms.warehouse.domain.repository;

import com.g42.platform.gms.warehouse.domain.entity.*;
import jakarta.validation.constraints.NotNull;
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

    ProductLine saveProductLine(ProductLine productLine);

    boolean exitBySku(String sku);

    ItemCategory saveItemCate(ItemCategory itemCategory);

    ProductLine getProductLineById(Integer productLineId);

    List<Specification> getListOfSpecsByItem(Integer itemId);

    ItemCategory getItemCategoryById(@NotNull Integer itemCategoryId);

    CatalogItem saveCatalogItem(CatalogItem catalogItem);

    boolean exitByCategoryCode(String categoryCode);

    Specification saveSpec(Specification specification);

    SpecAttribute saveSpecAttribute(SpecAttribute specAttribute);

    List<ItemCategory> getAllItemCategory();

    SpecAttribute getSpecAttributeById(Integer attributeId);

    CatalogItem getCatalogItemById(Integer itemId);
}
