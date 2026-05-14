package com.tour.core.service;

import com.tour.core.dto.DepartureResponseBookingDTO;
import com.tour.core.dto.request.DepartureRequest;
import com.tour.core.dto.response.DepartureResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface DepartureService {
    List<DepartureResponse> getByTourId(Long tourId);

    // Customer-facing: only OPEN departures
    List<DepartureResponse> getOpenByTourId(Long tourId);

    Page<DepartureResponse> getByTourId(Long tourId, String status, int page, int size);

    DepartureResponse getById(Long id);
    DepartureResponseBookingDTO getDepartureDetails(Long id);
    DepartureResponse create(DepartureRequest request);

    DepartureResponse update(Long id, DepartureRequest request);

    void delete(Long id);

    DepartureResponse updateStatus(Long id, String status);
}
