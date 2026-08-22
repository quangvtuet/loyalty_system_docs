package com.loyalty.redemption_engine.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Reward Catalog Item — theo FR-03-001.
 * min points_cost = 100 (FR-03-013).
 */
@Entity
@Table(name = "reward_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RewardItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID itemId;

    @Column(nullable = false)
    private String programId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String category; // VOUCHER, MERCHANDISE, CASH_BACK, TRAVEL

    @Column(nullable = false)
    private Long pointsCost; // minimum 100

    @Column(nullable = false)
    private BigDecimal currencyValue; // 100 pts = $1.00

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FulfillmentType fulfillmentType;

    @Column(nullable = false)
    private String minTierRequired; // SILVER, GOLD, PLATINUM

    @Column
    private Integer stockQuantity; // NULL = unlimited

    @Column(nullable = false)
    private String status; // DRAFT, ACTIVE, OUT_OF_STOCK, DISCONTINUED

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
