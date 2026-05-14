package com.tour.core.repository;

import com.tour.core.entity.Promotion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    Optional<Promotion> findByCode(String code);

    boolean existsByCode(String code);

    Page<Promotion> findByIsActive(Boolean isActive, Pageable pageable);

    @Query("SELECT COUNT(p) FROM Promotion p WHERE p.isActive = true AND p.validTo >= CURRENT_TIMESTAMP")
    Long countActivePromotions();

    @Query("SELECT COUNT(p) FROM Promotion p WHERE p.validTo < CURRENT_TIMESTAMP")
    Long countExpiredPromotions();

    @Query("SELECT COUNT(p) FROM Promotion p WHERE p.usedCount >= p.usageLimit AND p.usageLimit > 0")
    Long countFullyUsedPromotions();

    @Query("SELECT p FROM Promotion p WHERE p.usageLimit > 0 ORDER BY p.usedCount DESC")
    List<Promotion> findTopUsedPromotions(Pageable pageable);
}
