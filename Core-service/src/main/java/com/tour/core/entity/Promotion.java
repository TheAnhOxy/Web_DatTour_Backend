package com.tour.core.entity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity
@Table(name="promotions")
@Data
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String code;

    private BigDecimal discountPercent;
    private BigDecimal maxDiscount;

    private Integer usageLimit;
    private Integer usedCount;

    private LocalDateTime validFrom;
    private LocalDateTime validTo;

    private Boolean isActive;
}