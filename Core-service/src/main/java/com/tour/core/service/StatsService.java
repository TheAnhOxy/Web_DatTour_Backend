package com.tour.core.service;

import com.tour.core.dto.response.stats.*;

import java.util.List;

public interface StatsService {
    DashboardSummaryResponse getDashboardSummary();

    TourStatsResponse getTourStats();

    PromotionStatsResponse getPromotionStats();

    WishlistStatsResponse getWishlistStats();

    List<DepartureStatsResponse> getUpcomingDepartures(int limit);

    List<DepartureStatsResponse> getNearlyFullDepartures();
}
