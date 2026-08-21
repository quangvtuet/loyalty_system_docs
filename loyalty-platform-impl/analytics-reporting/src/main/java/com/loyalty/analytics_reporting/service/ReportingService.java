package com.loyalty.analytics_reporting.service;

import com.loyalty.analytics_reporting.repository.FactPointTransactionRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for computing point liabilities and KPIs (DD-05).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportingService {

    private final FactPointTransactionRepository factRepository;

    @Transactional(readOnly = true)
    public LiabilityReport getFinancialLiability(UUID programId) {
        LocalDateTime now = LocalDateTime.now();
        
        BigDecimal totalLiability = factRepository.calculateTotalLiability(programId, now);
        if (totalLiability == null) {
            totalLiability = BigDecimal.ZERO;
        }

        // Aging Buckets
        Long bucket0to3 = getUnspent(programId, now.minusDays(90), now, now);
        Long bucket3to6 = getUnspent(programId, now.minusDays(180), now.minusDays(90), now);
        Long bucket6to12 = getUnspent(programId, now.minusDays(365), now.minusDays(180), now);
        Long bucketOver12 = getUnspent(programId, LocalDateTime.of(1970, 1, 1, 0, 0), now.minusDays(365), now);

        return new LiabilityReport(
                totalLiability,
                bucket0to3,
                bucket3to6,
                bucket6to12,
                bucketOver12
        );
    }

    private Long getUnspent(UUID programId, LocalDateTime from, LocalDateTime to, LocalDateTime now) {
        Long unspent = factRepository.calculateUnspentPointsInAgeBucket(programId, from, to, now);
        return unspent == null ? 0L : unspent;
    }

    @Data
    public static class LiabilityReport {
        private final BigDecimal totalLiabilityUsd;
        private final Long unspent0to3Months;
        private final Long unspent3to6Months;
        private final Long unspent6to12Months;
        private final Long unspentOver12Months;
    }
}
