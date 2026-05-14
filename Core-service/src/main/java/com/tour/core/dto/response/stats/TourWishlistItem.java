package com.tour.core.dto.response.stats;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@Builder
public class TourWishlistItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long tourId;
    private String tourTitle;
    private String tourSlug;
    private String coverImageUrl;
    private Long wishlistCount;
}
