package com.tour.core.service.impl;

import com.tour.core.dto.request.CategoryRequest;
import com.tour.core.dto.response.CategoryResponse;
import com.tour.core.entity.TourCategory;
import com.tour.core.exception.InvalidDataException;
import com.tour.core.exception.ResourceNotFoundException;
import com.tour.core.repository.TourCategoryRepository;
import com.tour.core.service.TourCategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import org.springframework.data.domain.PageRequest;

@Service
@RequiredArgsConstructor
@Slf4j
public class TourCategoryServiceImpl implements TourCategoryService {

    private final TourCategoryRepository tourCategoryRepository;
    private final ModelMapper modelMapper;

    @Override
    @Cacheable(value = "categories", key = "'all'")
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAll() {
        return tourCategoryRepository.findAll()
                .stream()
                .map(category -> modelMapper.map(category, CategoryResponse.class))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getById(Long id) {
        TourCategory category = tourCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Danh mục không tồn tại: " + id));
        return modelMapper.map(category, CategoryResponse.class);
    }

    @Override
    @CacheEvict(value = "categories", allEntries = true)
    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        if (tourCategoryRepository.existsByName(request.getName())) {
            throw new InvalidDataException("Tên đã tồn tại");
        }

        TourCategory category = modelMapper.map(request, TourCategory.class);
        TourCategory savedCategory = tourCategoryRepository.save(category);
        log.info("Created tour category - id={}, name={}, by={}", savedCategory.getId(), savedCategory.getName(), getCurrentUser());
        return modelMapper.map(savedCategory, CategoryResponse.class);
    }

    @Override
    @CacheEvict(value = "categories", allEntries = true)
    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        TourCategory category = tourCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Danh mục không tồn tại: " + id));

        if (!category.getName().equals(request.getName()) && tourCategoryRepository.existsByName(request.getName())) {
            throw new InvalidDataException("Tên đã tồn tại");
        }

        modelMapper.map(request, category);
        TourCategory savedCategory = tourCategoryRepository.save(category);
        log.info("Updated tour category - id={}, name={}, by={}", savedCategory.getId(), savedCategory.getName(), getCurrentUser());
        return modelMapper.map(savedCategory, CategoryResponse.class);
    }

    @Override
    @CacheEvict(value = "categories", allEntries = true)
    @Transactional
    public void delete(Long id) {
        TourCategory category = tourCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Danh mục không tồn tại: " + id));
        tourCategoryRepository.delete(category);
        log.info("Deleted tour category - id={}, name={}, by={}", category.getId(), category.getName(), getCurrentUser());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getTopCategories(int limit) {
        List<TourCategory> top = tourCategoryRepository.findTopCategories(PageRequest.of(0, Math.max(1, limit)));
        return top.stream().map(c -> modelMapper.map(c, CategoryResponse.class)).toList();
    }

    private String getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "UNKNOWN";
    }
}