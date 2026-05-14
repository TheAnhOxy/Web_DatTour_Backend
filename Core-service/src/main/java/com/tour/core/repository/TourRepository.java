package com.tour.core.repository;

import com.tour.core.entity.Tour;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
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
            ORDER BY t.id DESC
            """)
    Page<Tour> findByFilters(
            @Param("status") String status,
            @Param("categoryId") Long categoryId,
            @Param("isHot") Boolean isHot,
            @Param("destinationId") Long destinationId,
            Pageable pageable
    );

    @Query(value = """
            SELECT DISTINCT t.* FROM tours t
            LEFT JOIN tour_destinations td ON t.id = td.tour_id
            WHERE (:status IS NULL OR t.status = :status)
              AND (:categoryId IS NULL OR t.category_id = :categoryId)
              AND (:isHot IS NULL OR t.is_hot = :isHot)
              AND (:destinationId IS NULL OR td.destination_id = :destinationId)
              AND (:keyword IS NULL OR :keyword = ''
                   OR LOWER(unaccent(t.title)) LIKE LOWER(unaccent(CONCAT('%', :keyword, '%')))
                   OR LOWER(unaccent(t.slug)) LIKE LOWER(unaccent(CONCAT('%', :keyword, '%')))
                   OR LOWER(unaccent(t.description)) LIKE LOWER(unaccent(CONCAT('%', :keyword, '%'))))
            ORDER BY t.id DESC
            """,
            countQuery = """
            SELECT COUNT(DISTINCT t.id) FROM tours t
            LEFT JOIN tour_destinations td ON t.id = td.tour_id
            WHERE (:status IS NULL OR t.status = :status)
              AND (:categoryId IS NULL OR t.category_id = :categoryId)
              AND (:isHot IS NULL OR t.is_hot = :isHot)
              AND (:destinationId IS NULL OR td.destination_id = :destinationId)
              AND (:keyword IS NULL OR :keyword = ''
                   OR LOWER(unaccent(t.title)) LIKE LOWER(unaccent(CONCAT('%', :keyword, '%')))
                   OR LOWER(unaccent(t.slug)) LIKE LOWER(unaccent(CONCAT('%', :keyword, '%')))
                   OR LOWER(unaccent(t.description)) LIKE LOWER(unaccent(CONCAT('%', :keyword, '%'))))
            """,
            nativeQuery = true)
    Page<Tour> searchForAdmin(
            @Param("keyword") String keyword,
            @Param("status") String status,
            @Param("categoryId") Long categoryId,
            @Param("isHot") Boolean isHot,
            @Param("destinationId") Long destinationId,
            Pageable pageable
    );

    @Query("SELECT COUNT(t) FROM Tour t WHERE t.status = :status")
    Long countByStatus(@Param("status") String status);

    @Query("SELECT COUNT(t) FROM Tour t WHERE t.isHot = true")
    Long countHotTours();

    @Query("SELECT c.name, COUNT(t) FROM Tour t JOIN t.category c GROUP BY c.name ORDER BY COUNT(t) DESC")
    List<Object[]> countToursByCategory();

    @Query("SELECT COUNT(t) FROM Tour t WHERE t.category.id = :categoryId")
    Long countByCategoryId(@Param("categoryId") Long categoryId);
}

