package com.g42.platform.gms.auth.api.internal;

import com.g42.platform.gms.auth.entity.CustomerProfile;

import java.util.List;

public interface CustomerInternalApi {

    List<CustomerProfile> findAllByIds(List<Integer> customerIds);

    CustomerProfile findById(Integer customerId);
}
