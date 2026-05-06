
package com.tour.identity.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "membership_tiers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MembershipTier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long  id;

    @Column(name = "tier_name", unique = true, length = 30)
    private String tierName;

    @Column(name = "min_points")
    private Integer minPoints;

    @Column(name = "discount_rate", precision = 5, scale = 2)
    private BigDecimal discountRate;
}
