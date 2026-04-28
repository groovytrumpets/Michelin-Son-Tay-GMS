package com.g42.platform.gms.estimation.domain.repository;

import com.g42.platform.gms.estimation.domain.entity.Estimate;
import org.springframework.stereotype.Repository;


import java.util.List;

@Repository
public interface EstimateRepository {
    List<Estimate> getListOfEstimateByServiceTiketCode(Integer serviceTicketId);

    Estimate save(Estimate estimate);

    Estimate findEstimateById(Integer estimateId);

    Estimate findEstimateByServiceIdAndLatestVerson(Integer serviceTicketId);

    int findLatestEstimate(Integer serviceTicketId);

    Integer findEstimateIdByVersionAndServiceTicket(Integer serviceTicketId, int latestEstimateVersion);

    void deleteOldGitItemsByEstimateId(Integer estimateId);
}
