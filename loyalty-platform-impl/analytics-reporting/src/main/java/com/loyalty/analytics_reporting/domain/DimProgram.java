package com.loyalty.analytics_reporting.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Dimension: Program (Star Schema)
 */
@Entity
@Table(name = "dim_program")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DimProgram {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long programKey;

    @Column(nullable = false, unique = true)
    private UUID programId;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(nullable = false, precision = 10, scale = 6)
    private BigDecimal costPerPoint;

    @Column(nullable = false, length = 32)
    private String status;
}
