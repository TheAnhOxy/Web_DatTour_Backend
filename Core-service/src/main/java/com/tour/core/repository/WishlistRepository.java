package com.tour.core.repository;

import com.tour.core.entity.Wishlist;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    List<Wishlist> findByUserId(Long userId);

    Optional<Wishlist> findByUserIdAndTourId(Long userId, Long tourId);

    boolean existsByUserIdAndTourId(Long userId, Long tourId);

    @Query("""
        SELECT w.tour.id, w.tour.title, w.tour.slug, COUNT(w)
        FROM Wishlist w
        GROUP BY w.tour.id, w.tour.title, w.tour.slug
        ORDER BY COUNT(w) DESC
        """)
    List<Object[]> findMostWishlistedTours(Pageable pageable);

    @Query("SELECT COUNT(w) FROM Wishlist w")
    Long countTotal();
}
