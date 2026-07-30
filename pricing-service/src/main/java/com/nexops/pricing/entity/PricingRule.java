package com.nexops.pricing.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pricing_rules")
@Data
@NoArgsConstructor
public class PricingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long productId;
    private String productName;

    @Column(precision = 10, scale = 2)
    private BigDecimal previousPrice;

    @Column(precision = 10, scale = 2)
    private BigDecimal newPrice;

    private String reason;
    private Integer demandCount;

    private LocalDateTime decidedAt;

    @PrePersist
    @PreUpdate
    void stamp() {
        decidedAt = LocalDateTime.now();
    }
}
