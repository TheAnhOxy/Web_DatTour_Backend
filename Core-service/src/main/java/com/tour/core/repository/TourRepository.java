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
            SELECT DISTINCT t FROM Tour t
            LEFT JOIN t.destinations d
            WHERE (:status IS NULL OR t.status = :status)
              AND (:categoryId IS NULL OR t.category.id = :categoryId)
              AND (:isHot IS NULL OR t.isHot = :isHot)
              AND (:destinationId IS NULL OR d.id = :destinationId)
            """)
    Page<Tour> findByFilters(
            @Param("status") String status,
            @Param("categoryId") Long categoryId,
            @Param("isHot") Boolean isHot,
            @Param("destinationId") Long destinationId,
            Pageable pageable
    );
}
