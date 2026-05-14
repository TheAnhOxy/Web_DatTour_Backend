package com.tour.core.service.impl;

import com.tour.core.dto.response.stats.*;
import com.tour.core.entity.Departure;
import com.tour.core.entity.Promotion;
import com.tour.core.entity.TourImage;
import com.tour.core.repository.*;
import com.tour.core.service.StatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatsServiceImpl implements StatsService {

    private final TourRepository tourRepository;
    private final DepartureRepository departureRepository;
    private final PromotionRepository promotionRepository;
    private final WishlistRepository wishlistRepository;
    private final TourImageRepository tourImageRepository;

    @Override
    @Cacheable(value = "dashboard", key = "'summary'")
    @Transactional(readOnly = true)
    public DashboardSummaryResponse getDashboardSummary() {
        log.info("Load dashboard summary");
        return DashboardSummaryResponse.builder()
                .tourStats(getTourStats())
                .promotionStats(getPromotionStats())
                .wishlistStats(getWishlistStats())
                .upcomingDepartures(getUpcomingDepartures(5))
                .nearlyFullDepartures(getNearlyFullDepartures())
                .generatedAt(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public TourStatsResponse getTourStats() {
        Object[] slots = departureRepository.sumSlotsForOpenDepartures();
        Long totalSlots = 0L;
        Long booked = 0L;

        if (slots != null && slots.length >= 2) {
            if (slots[0] != null) {
                totalSlots = convertToLong(slots[0]);
            }
            if (slots[1] != null) {
                booked = convertToLong(slots[1]);
            }
        }

        Double occupancyRate = totalSlots > 0
                ? Math.round(booked * 100.0 / totalSlots * 100.0) / 100.0
                : 0.0;

        List<Object[]> categoryRaw = tourRepository.countToursByCategory();
        Map<String, Long> toursByCategory = new LinkedHashMap<>();
        categoryRaw.forEach(row -> {
            String categoryName = (String) row[0];
            Long count = convertToLong(row[1]);
            toursByCategory.put(categoryName, count);
        });

        return TourStatsResponse.builder()
                .totalTours(tourRepository.count())
                .activeTours(tourRepository.countByStatus("ACTIVE"))
                .inactiveTours(tourRepository.countByStatus("INACTIVE"))
                .hotTours(tourRepository.countHotTours())
                .toursByCategory(toursByCategory)
                .totalDepartures(departureRepository.count())
                .openDepartures(departureRepository.countByStatus("OPEN"))
                .totalSlots(totalSlots)
                .bookedSlots(booked)
                .availableSlots(totalSlots - booked)
                .occupancyRate(occupancyRate)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionStatsResponse getPromotionStats() {
        List<Promotion> top5 = promotionRepository.findTopUsedPromotions(PageRequest.of(0, 5));
        List<PromotionUsageItem> topUsedItems = top5.stream()
                .map(p -> PromotionUsageItem.builder()
                        .code(p.getCode())
                        .usedCount(p.getUsedCount())
                        .usageLimit(p.getUsageLimit())
                        .usageRate(p.getUsageLimit() > 0
                                ? Math.round(p.getUsedCount() * 100.0 / p.getUsageLimit() * 100.0) / 100.0
                                : 0.0)
                        .isActive(p.getIsActive())
                        .build())
                .toList();

        return PromotionStatsResponse.builder()
                .totalPromotions(promotionRepository.count())
                .activePromotions(promotionRepository.countActivePromotions())
                .expiredPromotions(promotionRepository.countExpiredPromotions())
                .fullyUsedPromotions(promotionRepository.countFullyUsedPromotions())
                .topUsed(topUsedItems)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public WishlistStatsResponse getWishlistStats() {
        List<Object[]> raw = wishlistRepository.findMostWishlistedTours(PageRequest.of(0, 10));
        List<TourWishlistItem> items = raw.stream()
                .map(row -> {
                    Long tourId = (Long) row[0];
                    String title = (String) row[1];
                    String slug = (String) row[2];
                    Long count = ((Number) row[3]).longValue();

                    String coverImageUrl = tourImageRepository.findByTourIdOrderBySortOrderAsc(tourId)
                            .stream()
                            .filter(img -> Boolean.TRUE.equals(img.getIsCover()))
                            .findFirst()
                            .map(TourImage::getImageUrl)
                            .orElse(null);

                    return TourWishlistItem.builder()
                            .tourId(tourId)
                            .tourTitle(title)
                            .tourSlug(slug)
                            .coverImageUrl(coverImageUrl)
                            .wishlistCount(count)
                            .build();
                })
                .toList();

        return WishlistStatsResponse.builder()
                .totalWishlists(wishlistRepository.countTotal())
                .mostWishlisted(items)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartureStatsResponse> getUpcomingDepartures(int limit) {
        return departureRepository.findUpcomingDepartures(PageRequest.of(0, limit))
                .stream()
                .map(this::toDepartureStats)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartureStatsResponse> getNearlyFullDepartures() {
        return departureRepository.findNearlyFullDepartures()
                .stream()
                .map(this::toDepartureStats)
                .toList();
    }

    private DepartureStatsResponse toDepartureStats(Departure d) {
        int availableSlots = d.getMaxSlots() - d.getBookedSlots();
        double occupancyRate = d.getMaxSlots() > 0
                ? Math.round(d.getBookedSlots() * 100.0 / d.getMaxSlots() * 10.0) / 10.0
                : 0.0;

        return DepartureStatsResponse.builder()
                .departureId(d.getId())
                .tourTitle(d.getTour().getTitle())
                .tourSlug(d.getTour().getSlug())
                .startDate(d.getStartDate())
                .endDate(d.getEndDate())
                .maxSlots(d.getMaxSlots())
                .bookedSlots(d.getBookedSlots())
                .availableSlots(availableSlots)
                .occupancyRate(occupancyRate)
                .status(d.getStatus())
                .build();
    }

    private Long convertToLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return 0L;
            }
        }
        return 0L;
    }
}
