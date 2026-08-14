package com.loyalty.redemption_engine.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Redemption Order — theo FR-03-014.
 * Trạng thái: PENDING → IN_PROGRESS → FULFILLED/FAILED → REVERSED
 */
@Entity
@Table(name = "redemption_order",
        indexes = {
                @Index(name = "idx_order_member", columnList = "memberId, programId, createdAt DESC"),
                @Index(name = "idx_order_status", columnList = "status, createdAt ASC")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RedemptionOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID orderId;

    @Column(nullable = false)
    private String memberId;

    @Column(nullable = false)
    private String programId;

    @Column(nullable = false)
    private UUID rewardItemId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Long totalPointsDebited;

    @Column(nullable = false)
    private String memberTierAtOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column
    private String deliveryAddress;

    @Column
    private String failureReason;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
