package com.loyalty.tiering_system.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * QP Ledger — append-only ghi nhận mỗi lần accrual QP.
 * Mapping với bảng qp_ledger theo schema Section 3.2.
 */
@Entity
@Table(name = "qp_ledger", indexes = {
        @Index(name = "idx_qp_member_period", columnList = "memberId, programId, tierPeriodStart, tierPeriodEnd")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QpLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID qpId;

    @Column(nullable = false)
    private String memberId;

    @Column(nullable = false)
    private String programId;

    @Column(nullable = false)
    private String sourceEventId;

    @Column(nullable = false)
    private Long qpAmount;

    @Column(nullable = false)
    private LocalDate tierPeriodStart;

    @Column(nullable = false)
    private LocalDate tierPeriodEnd;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime accrualDate;
}
