package com.tour.core.dto.response.stats;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@Builder
public class WishlistStatsResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long totalWishlists;
    private List<TourWishlistItem> mostWishlisted;
}
