package com.tour.core.service;

import com.tour.core.dto.request.CategoryRequest;
import com.tour.core.dto.response.CategoryResponse;

import java.util.List;

public interface TourCategoryService {

    List<CategoryResponse> getAll();

    CategoryResponse getById(Long id);

    CategoryResponse create(CategoryRequest request);

    CategoryResponse update(Long id, CategoryRequest request);

    void delete(Long id);

    List<CategoryResponse> getTopCategories(int limit);
}