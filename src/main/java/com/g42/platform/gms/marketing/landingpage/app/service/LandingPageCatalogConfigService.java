package com.g42.platform.gms.marketing.landingpage.app.service;

import com.g42.platform.gms.marketing.landingpage.api.dto.LandingPageConfigDto;
import com.g42.platform.gms.marketing.landingpage.api.dto.LandingPageConfigUpdateRequest;
import com.g42.platform.gms.marketing.landingpage.api.dto.LandingPageSectionDto;
import com.g42.platform.gms.marketing.landingpage.api.dto.LandingPageSectionUpdateRequest;
import com.g42.platform.gms.marketing.landingpage.domain.LandingPageSection;
import com.g42.platform.gms.marketing.landingpage.infrastructure.entity.LandingPageSectionConfigJpa;
import com.g42.platform.gms.marketing.landingpage.infrastructure.entity.LandingPageSectionItemJpa;
import com.g42.platform.gms.marketing.landingpage.infrastructure.repository.LandingPageSectionConfigJpaRepo;
import com.g42.platform.gms.marketing.landingpage.infrastructure.repository.LandingPageSectionItemJpaRepo;
import com.g42.platform.gms.warehouse.infrastructure.entity.CatalogItemJpa;
import com.g42.platform.gms.warehouse.infrastructure.repository.CatalogItemJpaRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LandingPageCatalogConfigService {
    private final LandingPageSectionConfigJpaRepo sectionConfigRepo;
    private final LandingPageSectionItemJpaRepo sectionItemRepo;
    private final CatalogItemJpaRepo catalogItemRepo;

    public LandingPageCatalogConfigService(
            LandingPageSectionConfigJpaRepo sectionConfigRepo,
            LandingPageSectionItemJpaRepo sectionItemRepo,
            CatalogItemJpaRepo catalogItemRepo
    ) {
        this.sectionConfigRepo = sectionConfigRepo;
        this.sectionItemRepo = sectionItemRepo;
        this.catalogItemRepo = catalogItemRepo;
    }

    @Transactional(readOnly = true)
    public LandingPageConfigDto getConfiguration() {
        Map<LandingPageSection, Boolean> configuredBySection = sectionConfigRepo.findAll().stream()
                .collect(Collectors.toMap(
                        LandingPageSectionConfigJpa::getSectionCode,
                        row -> Boolean.TRUE.equals(row.getConfigured()),
                        (left, right) -> right,
                        () -> new EnumMap<>(LandingPageSection.class)
                ));
        Map<LandingPageSection, List<Integer>> itemIdsBySection = new EnumMap<>(LandingPageSection.class);
        sectionItemRepo.findAllByOrderBySectionCodeAscDisplayOrderAsc().forEach(row ->
                itemIdsBySection.computeIfAbsent(row.getSectionCode(), ignored -> new ArrayList<>())
                        .add(row.getCatalogItem().getItemId())
        );

        List<LandingPageSectionDto> sections = new ArrayList<>();
        for (LandingPageSection section : LandingPageSection.values()) {
            sections.add(new LandingPageSectionDto(
                    section,
                    section.getMaxItems(),
                    configuredBySection.getOrDefault(section, false),
                    List.copyOf(itemIdsBySection.getOrDefault(section, List.of()))
            ));
        }
        return new LandingPageConfigDto(sections);
    }

    @Transactional
    public LandingPageConfigDto updateConfiguration(LandingPageConfigUpdateRequest request) {
        if (request == null || request.getSections() == null) {
            throw new IllegalArgumentException("Danh sách khu vực hiển thị là bắt buộc.");
        }

        Map<LandingPageSection, LandingPageSectionUpdateRequest> updates = new EnumMap<>(LandingPageSection.class);
        for (LandingPageSectionUpdateRequest sectionRequest : request.getSections()) {
            if (sectionRequest == null || sectionRequest.getSection() == null) {
                throw new IllegalArgumentException("Khu vực hiển thị không hợp lệ.");
            }
            if (updates.put(sectionRequest.getSection(), sectionRequest) != null) {
                throw new IllegalArgumentException("Khu vực " + sectionRequest.getSection() + " bị lặp.");
            }
        }
        if (updates.size() != LandingPageSection.values().length) {
            throw new IllegalArgumentException("Cần gửi đầy đủ cấu hình của 5 khu vực landing page.");
        }

        Set<Integer> allRequestedIds = updates.values().stream()
                .flatMap(section -> safeIds(section).stream())
                .collect(Collectors.toSet());
        Map<Integer, CatalogItemJpa> catalogItems = catalogItemRepo.findAllById(allRequestedIds).stream()
                .collect(Collectors.toMap(CatalogItemJpa::getItemId, Function.identity()));

        for (LandingPageSection section : LandingPageSection.values()) {
            validateSection(section, safeIds(updates.get(section)), catalogItems);
        }

        for (LandingPageSection section : LandingPageSection.values()) {
            sectionItemRepo.deleteAllBySectionCode(section);
        }
        sectionItemRepo.flush();

        List<LandingPageSectionItemJpa> rows = new ArrayList<>();
        for (LandingPageSection section : LandingPageSection.values()) {
            List<Integer> itemIds = safeIds(updates.get(section));
            for (int index = 0; index < itemIds.size(); index++) {
                LandingPageSectionItemJpa row = new LandingPageSectionItemJpa();
                row.setSectionCode(section);
                row.setCatalogItem(catalogItems.get(itemIds.get(index)));
                row.setDisplayOrder(index + 1);
                rows.add(row);
            }

            LandingPageSectionConfigJpa config = sectionConfigRepo.findById(section).orElseGet(() -> {
                LandingPageSectionConfigJpa created = new LandingPageSectionConfigJpa();
                created.setSectionCode(section);
                return created;
            });
            config.setConfigured(true);
            sectionConfigRepo.save(config);
        }
        sectionItemRepo.saveAll(rows);
        return getConfiguration();
    }

    private List<Integer> safeIds(LandingPageSectionUpdateRequest request) {
        return request == null || request.getCatalogItemIds() == null ? List.of() : request.getCatalogItemIds();
    }

    private void validateSection(
            LandingPageSection section,
            List<Integer> itemIds,
            Map<Integer, CatalogItemJpa> catalogItems
    ) {
        if (itemIds.size() > section.getMaxItems()) {
            throw new IllegalArgumentException(
                    "Khu vực " + section + " chỉ cho phép tối đa " + section.getMaxItems() + " sản phẩm."
            );
        }
        Set<Integer> uniqueIds = new HashSet<>();
        for (Integer itemId : itemIds) {
            if (itemId == null || itemId <= 0 || !uniqueIds.add(itemId)) {
                throw new IllegalArgumentException("Danh sách sản phẩm của " + section + " có mã trống hoặc bị lặp.");
            }
            CatalogItemJpa item = catalogItems.get(itemId);
            if (item == null) {
                throw new IllegalArgumentException("Không tìm thấy sản phẩm #" + itemId + ".");
            }
            if (!Boolean.TRUE.equals(item.getIsActive())) {
                throw new IllegalArgumentException("Sản phẩm #" + itemId + " đang ngừng hoạt động.");
            }
            if (!section.accepts(item.getItemType())) {
                throw new IllegalArgumentException(
                        "Sản phẩm #" + itemId + " không đúng loại của khu vực " + section + "."
                );
            }
        }
    }
}
