package com.g42.platform.gms.estimation.domain.repository;

import com.g42.platform.gms.estimation.domain.entity.Estimate;
import org.springframework.stereotype.Repository;


import java.util.List;


public interface EstimateRepository {
    List<Estimate> getListOfEstimateByServiceTiketCode(Integer serviceTicketId);

    Estimate save(Estimate estimate);

    Estimate findEstimateById(Integer estimateId);
}
