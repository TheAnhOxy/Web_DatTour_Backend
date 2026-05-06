package com.tour.core.service;

import com.tour.core.dto.request.TransportationRequest;
import com.tour.core.dto.response.TransportationResponse;

import java.util.List;

public interface TransportationService {

    List<TransportationResponse> getAll();

    TransportationResponse getById(Long id);

    TransportationResponse create(TransportationRequest request);

    TransportationResponse update(Long id, TransportationRequest request);

    void delete(Long id);
}
