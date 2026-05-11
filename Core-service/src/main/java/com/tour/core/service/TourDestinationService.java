package com.tour.core.service;

import com.tour.core.dto.response.TourDetailResponse;

import java.util.List;

public interface TourDestinationService {

    TourDetailResponse addDestination(Long tourId, Long destinationId);

    TourDetailResponse removeDestination(Long tourId, Long destinationId);

    TourDetailResponse setDestinations(Long tourId, List<Long> destinationIds);
}
