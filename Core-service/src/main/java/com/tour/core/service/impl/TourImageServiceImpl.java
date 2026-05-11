package com.tour.core.service.impl;

import com.tour.core.dto.request.TourImageRequest;
import com.tour.core.dto.response.TourImageResponse;
import com.tour.core.entity.Tour;
import com.tour.core.entity.TourImage;
import com.tour.core.exception.InvalidDataException;
import com.tour.core.exception.ResourceNotFoundException;
import com.tour.core.repository.TourImageRepository;
import com.tour.core.repository.TourRepository;
import com.tour.core.service.TourImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TourImageServiceImpl implements TourImageService {

    private final TourImageRepository tourImageRepository;
    private final TourRepository tourRepository;
    private final ModelMapper modelMapper;

    @Override
    @Cacheable(value = "tourImages", key = "#tourId")
    @Transactional(readOnly = true)
    public List<TourImageResponse> getByTourId(Long tourId) {
        if (!tourRepository.existsById(tourId)) {
            throw new ResourceNotFoundException("Tour không tồn tại: " + tourId);
        }

        return tourImageRepository.findByTourIdOrderBySortOrderAsc(tourId)
                .stream()
                .map(image -> modelMapper.map(image, TourImageResponse.class))
                .toList();
    }

    @Override
    @CacheEvict(value = "tourImages", key = "#tourId")
    @Transactional
    public TourImageResponse addImage(Long tourId, TourImageRequest request) {
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new ResourceNotFoundException("Tour không tồn tại: " + tourId));

        if (Boolean.TRUE.equals(request.getIsCover())) {
            List<TourImage> existingImages = tourImageRepository.findByTourIdOrderBySortOrderAsc(tourId);
            existingImages.forEach(image -> image.setIsCover(false));
            tourImageRepository.saveAll(existingImages);
            log.info("Reset isCover=false cho {} ảnh cũ của tour {}", existingImages.size(), tourId);
        }

        int nextOrder = tourImageRepository.findByTourIdOrderBySortOrderAsc(tourId).size() + 1;

        TourImage image = TourImage.builder()
                .tour(tour)
                .imageUrl(request.getImageUrl())
                .isCover(Boolean.TRUE.equals(request.getIsCover()))
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : nextOrder)
                .build();

        TourImage savedImage = tourImageRepository.save(image);
        log.info("Thêm ảnh id={} cho tour id={}, isCover={}", savedImage.getId(), tourId, savedImage.getIsCover());
        return modelMapper.map(savedImage, TourImageResponse.class);
    }

    @Override
    @CacheEvict(value = "tourImages", key = "#tourId")
    @Transactional
    public void deleteImage(Long tourId, Long imageId) {
        TourImage image = tourImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Ảnh không tồn tại: " + imageId));

        if (!image.getTour().getId().equals(tourId)) {
            throw new InvalidDataException("Ảnh id=" + imageId + " không thuộc tour id=" + tourId);
        }

        tourImageRepository.delete(image);
        log.info("Xóa ảnh id={} khỏi tour id={}", imageId, tourId);
    }

    @Override
    @CacheEvict(value = "tourImages", key = "#tourId")
    @Transactional
    public TourImageResponse setCover(Long tourId, Long imageId) {
        List<TourImage> allImages = tourImageRepository.findByTourIdOrderBySortOrderAsc(tourId);

        boolean exists = allImages.stream().anyMatch(image -> image.getId().equals(imageId));
        if (!exists) {
            throw new ResourceNotFoundException("Ảnh id=" + imageId + " không tồn tại trong tour id=" + tourId);
        }

        allImages.forEach(image -> image.setIsCover(image.getId().equals(imageId)));
        tourImageRepository.saveAll(allImages);

        TourImage coverImage = allImages.stream()
                .filter(image -> image.getId().equals(imageId))
                .findFirst()
                .get();

        log.info("Đặt ảnh id={} làm bìa cho tour id={}", imageId, tourId);
        return modelMapper.map(coverImage, TourImageResponse.class);
    }

    @Override
    @CacheEvict(value = "tourImages", key = "#tourId")
    @Transactional
    public List<TourImageResponse> reorder(Long tourId, List<Long> imageIds) {
        if (!tourRepository.existsById(tourId)) {
            throw new ResourceNotFoundException("Tour không tồn tại: " + tourId);
        }

        List<TourImage> allImages = tourImageRepository.findByTourIdOrderBySortOrderAsc(tourId);
        Map<Long, TourImage> imageMap = allImages.stream()
                .collect(Collectors.toMap(TourImage::getId, image -> image));

        for (Long id : imageIds) {
            if (!imageMap.containsKey(id)) {
                throw new InvalidDataException("Ảnh id=" + id + " không thuộc tour id=" + tourId);
            }
        }

        for (int i = 0; i < imageIds.size(); i++) {
            TourImage image = imageMap.get(imageIds.get(i));
            if (image != null) {
                image.setSortOrder(i + 1);
            }
        }

        tourImageRepository.saveAll(allImages);
        log.info("Sắp xếp lại {} ảnh cho tour id={}", imageIds.size(), tourId);
        return getByTourId(tourId);
    }
}
