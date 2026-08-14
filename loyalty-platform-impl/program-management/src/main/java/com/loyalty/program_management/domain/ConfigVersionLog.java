package com.loyalty.program_management.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * WORM Audit Log for configuration versioning (FR-04-004, FR-04-005).
 */
@Entity
@Table(name = "config_version_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfigVersionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID changeId;

    @Column(nullable = false, length = 64)
    private String entityName;

    @Column(nullable = false)
    private UUID entityId;

    @Column(nullable = false)
    private UUID operatorId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String previousValue;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private String newValue;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime changedAt;
}
