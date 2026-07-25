package com.g42.platform.gms.docs.app.service;

import com.g42.platform.gms.docs.api.dto.DocsProgressDto;
import com.g42.platform.gms.docs.api.dto.DocsProgressUpdateDto;
import com.g42.platform.gms.docs.api.dto.ManagerDocsReportDto;
import com.g42.platform.gms.docs.infrastructure.entity.StaffDocsProgressJpa;
import com.g42.platform.gms.docs.infrastructure.repository.StaffDocsProgressRepository;
import com.g42.platform.gms.staff.profile.infrastructure.entity.StaffProfileJpa;
import com.g42.platform.gms.staff.profile.infrastructure.repository.StaffProileJpaRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocsProgressServiceImpl implements DocsProgressService {

    private final StaffDocsProgressRepository progressRepository;
    private final StaffProileJpaRepo staffProfileRepository;

    @Override
    public List<DocsProgressDto> getProgressByStaffId(Integer staffId) {
        if (staffId == null) return Collections.emptyList();
        return progressRepository.findByStaffId(staffId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public DocsProgressDto updateProgress(DocsProgressUpdateDto updateDto) {
        if (updateDto == null || updateDto.getStaffId() == null || updateDto.getTopicId() == null) {
            throw new IllegalArgumentException("StaffId và TopicId không được để trống");
        }

        StaffDocsProgressJpa entity = progressRepository
                .findByStaffIdAndTopicId(updateDto.getStaffId(), updateDto.getTopicId())
                .orElseGet(() -> {
                    StaffDocsProgressJpa newEntity = new StaffDocsProgressJpa();
                    newEntity.setStaffId(updateDto.getStaffId());
                    newEntity.setTopicId(updateDto.getTopicId());
                    newEntity.setSectionId(updateDto.getSectionId() != null ? updateDto.getSectionId() : "1");
                    return newEntity;
                });

        entity.setStatus(updateDto.getStatus() != null ? updateDto.getStatus() : "IN_PROGRESS");
        if (updateDto.getScore() != null) {
            entity.setScore(updateDto.getScore());
        }
        if ("COMPLETED".equalsIgnoreCase(entity.getStatus())) {
            entity.setCompletedAt(LocalDateTime.now());
        }
        entity.setLastAccessedAt(LocalDateTime.now());

        StaffDocsProgressJpa saved = progressRepository.save(entity);
        return toDto(saved);
    }

    @Override
    public List<ManagerDocsReportDto> getManagerReport(int totalSystemTopics) {
        int targetTotalTopics = totalSystemTopics > 0 ? totalSystemTopics : 18;
        List<StaffProfileJpa> staffList = staffProfileRepository.findAll();

        Map<Integer, List<StaffDocsProgressJpa>> progressMap = progressRepository.findAll()
                .stream()
                .collect(Collectors.groupingBy(StaffDocsProgressJpa::getStaffId));

        List<ManagerDocsReportDto> report = new ArrayList<>();

        for (StaffProfileJpa staff : staffList) {
            List<StaffDocsProgressJpa> userProgress = progressMap.getOrDefault(staff.getStaffId(), Collections.emptyList());
            long completedCount = userProgress.stream()
                    .filter(p -> "COMPLETED".equalsIgnoreCase(p.getStatus()))
                    .count();

            LocalDateTime lastActive = userProgress.stream()
                    .map(StaffDocsProgressJpa::getLastAccessedAt)
                    .filter(Objects::nonNull)
                    .max(LocalDateTime::compareTo)
                    .orElse(null);

            double percentage = Math.min(100.0, Math.round(((double) completedCount / targetTotalTopics) * 100.0));

            report.add(ManagerDocsReportDto.builder()
                    .staffId(staff.getStaffId())
                    .fullName(staff.getFullName() != null ? staff.getFullName() : "Nhân viên #" + staff.getStaffId())
                    .employeeNo(staff.getEmployeeNo() != null ? staff.getEmployeeNo() : "NV" + staff.getStaffId())
                    .position(staff.getPosition() != null ? staff.getPosition() : "Nhân viên")
                    .avatar(staff.getAvatar())
                    .completedTopicsCount(completedCount)
                    .totalTopics(targetTotalTopics)
                    .completionPercentage(percentage)
                    .lastActiveAt(lastActive)
                    .build());
        }

        report.sort(Comparator.comparing(ManagerDocsReportDto::getCompletionPercentage).reversed());
        return report;
    }

    private DocsProgressDto toDto(StaffDocsProgressJpa entity) {
        return DocsProgressDto.builder()
                .id(entity.getId())
                .staffId(entity.getStaffId())
                .topicId(entity.getTopicId())
                .sectionId(entity.getSectionId())
                .status(entity.getStatus())
                .score(entity.getScore())
                .lastAccessedAt(entity.getLastAccessedAt())
                .completedAt(entity.getCompletedAt())
                .build();
    }
}
