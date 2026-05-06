package com.tour.core.repository;

import com.tour.core.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    List<Wishlist> findByUserId(Long userId);

    Optional<Wishlist> findByUserIdAndTourId(Long userId, Long tourId);

    boolean existsByUserIdAndTourId(Long userId, Long tourId);
}
