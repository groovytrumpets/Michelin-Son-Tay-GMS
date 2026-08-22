package com.g42.platform.gms.marketing.landingpage.app.service;

import com.g42.platform.gms.marketing.landingpage.api.dto.LandingPageConfigUpdateRequest;
import com.g42.platform.gms.marketing.landingpage.api.dto.LandingPageSectionUpdateRequest;
import com.g42.platform.gms.marketing.landingpage.domain.LandingPageSection;
import com.g42.platform.gms.marketing.landingpage.infrastructure.entity.LandingPageSectionItemJpa;
import com.g42.platform.gms.marketing.landingpage.infrastructure.repository.LandingPageSectionConfigJpaRepo;
import com.g42.platform.gms.marketing.landingpage.infrastructure.repository.LandingPageSectionItemJpaRepo;
import com.g42.platform.gms.warehouse.domain.enums.CatalogItemType;
import com.g42.platform.gms.warehouse.infrastructure.entity.CatalogItemJpa;
import com.g42.platform.gms.warehouse.infrastructure.repository.CatalogItemJpaRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LandingPageCatalogConfigServiceTest {
    @Mock
    private LandingPageSectionConfigJpaRepo sectionConfigRepo;
    @Mock
    private LandingPageSectionItemJpaRepo sectionItemRepo;
    @Mock
    private CatalogItemJpaRepo catalogItemRepo;

    private LandingPageCatalogConfigService service;

    @BeforeEach
    void setUp() {
        service = new LandingPageCatalogConfigService(sectionConfigRepo, sectionItemRepo, catalogItemRepo);
    }

    @Test
    void savesEverySectionAndPreservesRequestedOrder() {
        CatalogItemJpa serviceItem = item(1, CatalogItemType.SERVICE);
        CatalogItemJpa partItem = item(2, CatalogItemType.PART);
        CatalogItemJpa comboItem = item(3, CatalogItemType.COMBO);
        CatalogItemJpa equipmentItem = item(4, CatalogItemType.EQUIPMENT);
        when(catalogItemRepo.findAllById(any())).thenReturn(List.of(serviceItem, partItem, comboItem, equipmentItem));
        when(sectionConfigRepo.findById(any())).thenReturn(Optional.empty());
        when(sectionConfigRepo.findAll()).thenReturn(List.of());
        when(sectionItemRepo.findAllByOrderBySectionCodeAscDisplayOrderAsc()).thenReturn(List.of());

        LandingPageConfigUpdateRequest request = requestWithAllSections();
        find(request, LandingPageSection.FEATURED).setCatalogItemIds(List.of(2, 1));
        find(request, LandingPageSection.SERVICE).setCatalogItemIds(List.of(1));
        find(request, LandingPageSection.PART).setCatalogItemIds(List.of(2));
        find(request, LandingPageSection.COMBO).setCatalogItemIds(List.of(3));
        find(request, LandingPageSection.EQUIPMENT).setCatalogItemIds(List.of(4));

        service.updateConfiguration(request);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<LandingPageSectionItemJpa>> rowsCaptor = ArgumentCaptor.forClass(List.class);
        verify(sectionItemRepo).saveAll(rowsCaptor.capture());
        verify(sectionItemRepo, times(5)).deleteAllBySectionCode(any());
        List<LandingPageSectionItemJpa> rows = rowsCaptor.getValue();
        assertEquals(6, rows.size());
        assertEquals(2, rows.get(0).getCatalogItem().getItemId());
        assertEquals(1, rows.get(0).getDisplayOrder());
        assertEquals(1, rows.get(1).getCatalogItem().getItemId());
        assertEquals(2, rows.get(1).getDisplayOrder());
    }

    @Test
    void rejectsAnItemPlacedInTheWrongTypedSection() {
        CatalogItemJpa partItem = item(2, CatalogItemType.PART);
        when(catalogItemRepo.findAllById(any())).thenReturn(List.of(partItem));
        LandingPageConfigUpdateRequest request = requestWithAllSections();
        find(request, LandingPageSection.SERVICE).setCatalogItemIds(List.of(2));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.updateConfiguration(request)
        );

        assertTrue(error.getMessage().contains("không đúng loại"));
    }

    @Test
    void rejectsMoreThanSixFeaturedItems() {
        when(catalogItemRepo.findAllById(any())).thenReturn(List.of());
        LandingPageConfigUpdateRequest request = requestWithAllSections();
        find(request, LandingPageSection.FEATURED).setCatalogItemIds(List.of(1, 2, 3, 4, 5, 6, 7));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.updateConfiguration(request)
        );

        assertTrue(error.getMessage().contains("tối đa 6"));
    }

    private LandingPageConfigUpdateRequest requestWithAllSections() {
        LandingPageConfigUpdateRequest request = new LandingPageConfigUpdateRequest();
        List<LandingPageSectionUpdateRequest> sections = new ArrayList<>();
        Arrays.stream(LandingPageSection.values()).forEach(section -> {
            LandingPageSectionUpdateRequest sectionRequest = new LandingPageSectionUpdateRequest();
            sectionRequest.setSection(section);
            sectionRequest.setCatalogItemIds(new ArrayList<>());
            sections.add(sectionRequest);
        });
        request.setSections(sections);
        return request;
    }

    private LandingPageSectionUpdateRequest find(
            LandingPageConfigUpdateRequest request,
            LandingPageSection section
    ) {
        return request.getSections().stream()
                .filter(candidate -> candidate.getSection() == section)
                .findFirst()
                .orElseThrow();
    }

    private CatalogItemJpa item(int id, CatalogItemType type) {
        CatalogItemJpa item = new CatalogItemJpa();
        item.setItemId(id);
        item.setItemType(type);
        item.setIsActive(true);
        return item;
    }
}
