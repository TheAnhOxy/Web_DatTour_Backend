package com.tour.core.service;

import com.tour.core.dto.request.TourSearchRequest;
import com.tour.core.dto.response.TourListResponse;

import java.util.List;

public interface TourSearchService {

    List<TourListResponse> searchTours(TourSearchRequest request, int page, int size);
}
