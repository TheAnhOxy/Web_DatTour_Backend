package com.tour.core.service;

import com.tour.core.dto.request.TourRequest;
import com.tour.core.dto.response.TourDetailResponse;
import com.tour.core.dto.response.TourListResponse;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface TourService {

    Page<TourListResponse> getAllForCustomer(String keyword, Long categoryId, Boolean isHot, Long destinationId, int page, int size);

    Page<TourListResponse> getAllForAdmin(String status, Long categoryId, Boolean isHot, Long destinationId, int page, int size);

    Page<TourListResponse> searchForAdmin(String keyword, String status, Long categoryId, Boolean isHot, Long destinationId, int page, int size);

    TourDetailResponse getById(Long id);

    TourDetailResponse getBySlug(String slug);

    TourDetailResponse create(TourRequest request, List<MultipartFile> images);

    TourDetailResponse update(Long id, TourRequest request);

    void delete(Long id);

    TourDetailResponse toggleHot(Long id);

    void syncAllToElasticsearch();
}

