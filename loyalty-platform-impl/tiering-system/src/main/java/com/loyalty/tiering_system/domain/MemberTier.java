package com.loyalty.tiering_system.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Member Tier — trạng thái tier hiện tại của member.
 * Mutable snapshot, 1 record duy nhất per member per program.
 * Mapping với bảng member_tier theo schema Section 3.2.
 */
@Entity
@Table(name = "member_tier",
        uniqueConstraints = @UniqueConstraint(name = "uq_member_program_tier", columnNames = {"memberId", "programId"}),
        indexes = @Index(name = "idx_member_tier_status", columnList = "programId, status, gracePeriodEnd"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberTier {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID memberTierId;

    @Column(nullable = false)
    private String memberId;

    @Column(nullable = false)
    private String programId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TierName currentTier;

    @Enumerated(EnumType.STRING)
    @Column
    private TierName previousTier;

    @Column(nullable = false)
    private LocalDateTime effectiveFrom;

    @Column(nullable = false)
    private LocalDate tierPeriodEnd;

    @Column
    private LocalDateTime gracePeriodEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TierStatus status;

    @Column(nullable = false)
    private Long cumulativeQp;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
