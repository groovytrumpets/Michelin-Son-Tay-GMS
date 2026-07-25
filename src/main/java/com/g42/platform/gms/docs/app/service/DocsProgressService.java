package com.g42.platform.gms.docs.app.service;

import com.g42.platform.gms.docs.api.dto.DocsProgressDto;
import com.g42.platform.gms.docs.api.dto.DocsProgressUpdateDto;
import com.g42.platform.gms.docs.api.dto.ManagerDocsReportDto;

import java.util.List;

public interface DocsProgressService {
    List<DocsProgressDto> getProgressByStaffId(Integer staffId);
    DocsProgressDto updateProgress(DocsProgressUpdateDto updateDto);
    List<ManagerDocsReportDto> getManagerReport(int totalSystemTopics);
}
