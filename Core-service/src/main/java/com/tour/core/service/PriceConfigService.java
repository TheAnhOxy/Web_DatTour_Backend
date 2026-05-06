package com.tour.core.service;

import com.tour.core.dto.request.PriceConfigRequest;
import com.tour.core.dto.response.PriceConfigResponse;

public interface PriceConfigService {
    PriceConfigResponse getByDepartureId(Long departureId);

    PriceConfigResponse upsert(Long departureId, PriceConfigRequest request);
}
