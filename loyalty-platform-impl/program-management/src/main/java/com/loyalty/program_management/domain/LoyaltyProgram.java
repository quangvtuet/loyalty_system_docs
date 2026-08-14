package com.loyalty.program_management.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Top-level Loyalty Program — theo DD-04 Schema.
 * Lifecycle: DRAFT -> ACTIVE -> SUSPENDED -> DEACTIVATED
 */
@Entity
@Table(name = "loyalty_program")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltyProgram {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID programId;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(nullable = false, length = 64)
    private String currencyName;

    @Column(nullable = false, precision = 10, scale = 6)
    private BigDecimal costPerPoint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ProgramStatus status;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column
    private LocalDate endDate;

    @Column(columnDefinition = "TEXT")
    private String termsAndConditions;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
