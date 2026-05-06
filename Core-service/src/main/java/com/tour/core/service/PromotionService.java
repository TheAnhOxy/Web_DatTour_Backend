package com.tour.core.service;

import com.tour.core.dto.request.PromotionRequest;
import com.tour.core.dto.response.PromotionResponse;
import com.tour.core.dto.response.PromotionValidateResponse;
import org.springframework.data.domain.Page;

public interface PromotionService {
    Page<PromotionResponse> getCustomerList(Boolean isActive, int page, int size);

    Page<PromotionResponse> getStaffList(int page, int size);

    PromotionResponse getById(Long id);

    PromotionValidateResponse validate(String code);

    PromotionResponse create(PromotionRequest request);

    PromotionResponse update(Long id, PromotionRequest request);

    void delete(Long id);

    PromotionResponse toggle(Long id);
}
