package com.loyalty.earning_engine.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "point_balance", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"memberId", "programId"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID balanceId;

    @Column(nullable = false)
    private String memberId;

    @Column(nullable = false)
    private String programId;

    @Column(nullable = false)
    private Long confirmedBalance;

    @Column(nullable = false)
    private Long pendingBalance;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
