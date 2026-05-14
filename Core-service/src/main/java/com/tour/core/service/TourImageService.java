package com.tour.core.service;

import com.tour.core.dto.request.TourImageRequest;
import com.tour.core.dto.response.TourImageResponse;

import java.util.List;

public interface TourImageService {

    List<TourImageResponse> getByTourId(Long tourId);

    TourImageResponse addImage(Long tourId, TourImageRequest request);

    void deleteImage(Long tourId, Long imageId);

    TourImageResponse setCover(Long tourId, Long imageId);

    List<TourImageResponse> reorder(Long tourId, List<Long> imageIds);
}
