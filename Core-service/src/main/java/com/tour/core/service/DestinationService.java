package com.tour.core.service;

import com.tour.core.dto.request.DestinationRequest;
import com.tour.core.dto.response.DestinationResponse;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

public interface DestinationService {

    Page<DestinationResponse> getAll(int page, int size);

    DestinationResponse getById(Long id);

    Page<DestinationResponse> search(String keyword, int page, int size);

    DestinationResponse create(DestinationRequest request, MultipartFile file);

    DestinationResponse update(Long id, DestinationRequest request, MultipartFile file);

    void delete(Long id);
}
