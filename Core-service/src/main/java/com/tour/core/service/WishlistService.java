package com.tour.core.service;

import com.tour.core.dto.request.WishlistRequest;
import com.tour.core.dto.response.WishlistResponse;

import java.util.List;

public interface WishlistService {
    List<WishlistResponse> getByUserId(Long userId);

    WishlistResponse add(Long userId, WishlistRequest request);

    void remove(Long wishlistId, Long userId);

    boolean check(Long userId, Long tourId);
}
