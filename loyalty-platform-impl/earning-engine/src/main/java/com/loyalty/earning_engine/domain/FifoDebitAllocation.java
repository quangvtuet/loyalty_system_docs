package com.loyalty.earning_engine.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/** Exact source-batch allocation created by CT-13 debit. */
@Entity
@Table(name = "fifo_debit_allocation",
        uniqueConstraints = @UniqueConstraint(columnNames = {"order_id", "batch_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FifoDebitAllocation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private String orderId;

    @Column(name = "member_id", nullable = false)
    private String memberId;

    @Column(name = "program_id", nullable = false)
    private String programId;

    @Column(name = "batch_id", nullable = false)
    private UUID batchId;

    @Column(nullable = false)
    private Integer pointsDebited;

    @Column(nullable = false)
    private boolean restored;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
