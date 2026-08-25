package com.g42.platform.gms.marketing.landingpage.domain;

import com.g42.platform.gms.warehouse.domain.enums.CatalogItemType;

public enum LandingPageSection {
    FEATURED(6),
    SERVICE(6),
    PART(6),
    COMBO(6),
    EQUIPMENT(6);

    private final int maxItems;

    LandingPageSection(int maxItems) {
        this.maxItems = maxItems;
    }

    public int getMaxItems() {
        return maxItems;
    }

    public boolean accepts(CatalogItemType itemType) {
        if (itemType == null) return false;
        return switch (this) {
            case FEATURED -> itemType == CatalogItemType.SERVICE
                    || itemType == CatalogItemType.PART
                    || itemType == CatalogItemType.COMBO
                    || itemType == CatalogItemType.EQUIPMENT
                    || itemType == CatalogItemType.MACHINERY;
            case SERVICE -> itemType == CatalogItemType.SERVICE;
            case PART -> itemType == CatalogItemType.PART;
            case COMBO -> itemType == CatalogItemType.COMBO;
            case EQUIPMENT -> itemType == CatalogItemType.EQUIPMENT
                    || itemType == CatalogItemType.MACHINERY;
        };
    }
}
