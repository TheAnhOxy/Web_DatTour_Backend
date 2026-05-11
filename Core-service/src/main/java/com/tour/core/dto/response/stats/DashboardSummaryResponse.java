package com.tour.core.dto.response.stats;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class DashboardSummaryResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private TourStatsResponse tourStats;
    private PromotionStatsResponse promotionStats;
    private WishlistStatsResponse wishlistStats;
    private List<DepartureStatsResponse> upcomingDepartures;
    private List<DepartureStatsResponse> nearlyFullDepartures;
    private LocalDateTime generatedAt;
}
