package com.tour.core.entity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name="price_configs")
@Data
public class PriceConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="departure_id")
    private Departure departure;

    private BigDecimal adultPrice;
    @Column(name="child_10_14_price")
    private BigDecimal child1014Price;

    @Column(name="child_4_9_price")
    private BigDecimal child49Price;
    private BigDecimal babyPrice;
}