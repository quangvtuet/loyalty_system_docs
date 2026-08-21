package com.loyalty.program_management.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Promotional Campaign Table — theo DD-04 Schema.
 * FR-04-012: Priority conflict resolution.
 */
@Entity
@Table(name = "campaign",
        indexes = {
                @Index(name = "idx_campaign_active", columnList = "programId, status, startDate, endDate, priority ASC")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID campaignId;

    @Column(nullable = false)
    private UUID programId;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal multiplier;

    @Column(nullable = false)
    private Long flatBonus;

    @Column(nullable = false)
    private Integer priority;

    @Column(nullable = false)
    private LocalDateTime startDate;

    @Column(nullable = false)
    private LocalDateTime endDate;

    @Column
    private Long pointsBudget;

    @Column(nullable = false)
    private Long pointsIssuedTotal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CampaignStatus status;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
