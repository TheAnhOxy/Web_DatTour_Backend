package com.tour.core.repository;

import com.tour.core.entity.Tour;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TourRepository extends JpaRepository<Tour, Long> {

    Optional<Tour> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @Query("""
            SELECT t FROM Tour t
            WHERE (:status IS NULL OR t.status = :status)
              AND (:categoryId IS NULL OR t.category.id = :categoryId)
              AND (:isHot IS NULL OR t.isHot = :isHot)
            """)
    Page<Tour> findByFilters(
            @Param("status") String status,
            @Param("categoryId") Long categoryId,
            @Param("isHot") Boolean isHot,
            Pageable pageable
    );
}
