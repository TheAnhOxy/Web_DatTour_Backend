package com.tour.core.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pricing_rules")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PricingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name="departure_id")
    private Departure departure;

    private String ruleName;
    private String ruleType;
    private String adjustmentType;

    private BigDecimal adjustmentValue;

    private Integer minDaysBefore;
    private Integer maxDaysBefore;

    private Integer minSlotsLeft;
    private Integer maxSlotsLeft;

    private LocalDateTime validFrom;
    private LocalDateTime validTo;

    private Integer priority;
    private Boolean isActive;
}