package com.tour.core.service;

import com.tour.core.dto.request.TourRequest;
import com.tour.core.dto.response.TourDetailResponse;
import com.tour.core.dto.response.TourListResponse;
import org.springframework.data.domain.Page;

public interface TourService {

    Page<TourListResponse> getAllForCustomer(Long categoryId, Boolean isHot, Long destinationId, int page, int size);

    Page<TourListResponse> getAllForAdmin(String status, Long categoryId, Boolean isHot, Long destinationId, int page, int size);

    TourDetailResponse getById(Long id);

    TourDetailResponse getBySlug(String slug);

    TourDetailResponse create(TourRequest request);

    TourDetailResponse update(Long id, TourRequest request);

    void delete(Long id);

    TourDetailResponse toggleHot(Long id);
}
