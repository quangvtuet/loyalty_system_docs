package com.loyalty.analytics_reporting.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Fact: Point Movement Transactions (Star Schema)
 */
@Entity
@Table(name = "fact_point_transaction")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FactPointTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long pointFactId;

    @Column
    private Integer dateKey;

    @Column
    private Long memberKey;

    @Column
    private Long programKey;

    @Column(nullable = false, length = 32)
    private String tierName;

    @Column(nullable = false, length = 64)
    private String channel;

    @Column(nullable = false, length = 64)
    private String transactionType;

    @Column(nullable = false, length = 32)
    private String eventType; // 'EARN', 'BONUS', 'REDEEM', 'EXPIRED', 'ADJUST'

    @Column(nullable = false, length = 32)
    private String status;

    @Column(nullable = false)
    private Long pointsAmount;

    @Column(nullable = false)
    private Long pointsRemaining;

    @Column(nullable = false, precision = 14, scale = 4)
    private BigDecimal financialLiabilityUsd; // points_remaining * cost_per_point

    @Column(nullable = false)
    private LocalDateTime earnDate;

    @Column(nullable = false)
    private LocalDateTime expiryDate;
}
