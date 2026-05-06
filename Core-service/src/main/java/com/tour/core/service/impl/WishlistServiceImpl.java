package com.tour.core.service.impl;

import com.tour.core.dto.request.WishlistRequest;
import com.tour.core.dto.response.TourSummaryResponse;
import com.tour.core.dto.response.WishlistResponse;
import com.tour.core.entity.Tour;
import com.tour.core.entity.Wishlist;
import com.tour.core.exception.ForBiddenException;
import com.tour.core.exception.InvalidDataException;
import com.tour.core.exception.ResourceNotFoundException;
import com.tour.core.repository.TourRepository;
import com.tour.core.repository.WishlistRepository;
import com.tour.core.service.WishlistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;
    private final TourRepository tourRepository;
    private final ModelMapper modelMapper;
    private final CacheManager cacheManager;

    @Override
    @Transactional(readOnly = true)
    public List<WishlistResponse> getByUserId(Long userId) {
        List<Wishlist> list = wishlistRepository.findByUserId(userId);
        return list.stream().map(w -> toResponse(w)).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public WishlistResponse add(Long userId, WishlistRequest request) {
        Long tourId = request.getTourId();

        var tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new ResourceNotFoundException("Tour không tồn tại: " + tourId));

        if (wishlistRepository.existsByUserIdAndTourId(userId, tourId)) {
            throw new InvalidDataException("Tour đã trong wishlist");
        }

        Wishlist w = new Wishlist();
        w.setUserId(userId);
        w.setTour(tour);

        Wishlist saved = wishlistRepository.save(w);
        log.info("User {} - add wishlist - tourId={}", userId, tourId);
        // evict single check cache for this user-tour
        if (cacheManager != null) {
            var c = cacheManager.getCache("wishlistChecks");
            if (c != null) c.evict(userId + ":" + tourId);
        }
        return toResponse(saved);
    }

    @Override
    @Transactional
    @CacheEvict(value = "wishlists", key = "#userId")
    public void remove(Long wishlistId, Long userId) {
        Wishlist w = wishlistRepository.findById(wishlistId)
                .orElseThrow(() -> new ResourceNotFoundException("Wishlist không tồn tại: " + wishlistId));

        if (!w.getUserId().equals(userId)) {
            throw new ForBiddenException("Bạn không có quyền xóa mục wishlist này");
        }

        Long tourId = w.getTour() != null ? w.getTour().getId() : null;
        wishlistRepository.deleteById(wishlistId);
        log.info("User {} - remove wishlist - wishlistId={} tourId={}", userId, wishlistId, tourId);
        if (cacheManager != null && tourId != null) {
            var c = cacheManager.getCache("wishlistChecks");
            if (c != null) c.evict(userId + ":" + tourId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean check(Long userId, Long tourId) {
        return wishlistRepository.existsByUserIdAndTourId(userId, tourId);
    }

    private WishlistResponse toResponse(Wishlist w) {
        WishlistResponse resp = new WishlistResponse();
        resp.setId(w.getId());
        resp.setUserId(w.getUserId());
        resp.setCreatedAt(w.getCreatedAt());

        if (w.getTour() != null) {
            Tour t = w.getTour();
            TourSummaryResponse ts = modelMapper.map(t, TourSummaryResponse.class);
            // set cover image url from images
            if (t.getImages() != null && !t.getImages().isEmpty()) {
                var cover = t.getImages().stream().filter(i -> Boolean.TRUE.equals(i.getIsCover())).findFirst()
                        .orElse(t.getImages().get(0));
                ts.setCoverImageUrl(cover.getImageUrl());
            }
            resp.setTour(ts);
        }

        return resp;
    }
}
