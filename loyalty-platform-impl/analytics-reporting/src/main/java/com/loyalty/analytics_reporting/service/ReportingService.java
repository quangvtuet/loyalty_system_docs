package com.loyalty.analytics_reporting.service;

import com.loyalty.analytics_reporting.repository.FactPointTransactionRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for computing point liabilities and KPIs (DD-05, UC-LB-04).
 *
 * CON.4: Analytics reporting data must not exceed 10 minutes staleness.
 * When {@code lastRefreshedAt} is more than 10 minutes before now, {@code stale=true}
 * and {@code stalenessWarning} is populated. The figure is NOT presented as current (EXC-07).
 * Spec-trace: G6-A05 — tested by ReportingServiceTest.testGetFinancialLiability_StaleData_FlagsAsStale.
 *
 * I-9: This service reads ONLY from the Data Warehouse (FactPointTransactionRepository).
 * It never accesses Earning DB, Tiering DB, Redemption DB, or Program Mgmt DB (CON.2, EXC-08).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportingService {

    /** CON.4: maximum allowed data age before a report is flagged stale. */
    static final Duration MAX_FRESHNESS = Duration.ofMinutes(10);

    /** CON.4 warning message shown when data is stale (EXC-07). */
    static final String CON4_WARNING =
            "CON.4: Data Warehouse facts are more than 10 minutes old. " +
            "Do not treat this report as current.";

    private final FactPointTransactionRepository factRepository;

    /**
     * UC-LB-04 — Generate point liability report.
     *
     * Happy path: reads Data Warehouse facts and returns a fresh report (stale=false).
     * Alt (CON.4 / EXC-07 / G6-A05): when lastRefreshedAt > 10 min ago, returns report
     * with stale=true and stalenessWarning set. Finance is alerted by the flag.
     */
    @Transactional(readOnly = true)
    public LiabilityReport getFinancialLiability(UUID programId) {
        LocalDateTime now = LocalDateTime.now();

        BigDecimal totalLiability = factRepository.calculateTotalLiability(programId, now);
        if (totalLiability == null) {
            totalLiability = BigDecimal.ZERO;
        }

        // Aging Buckets (DD-05 §3.1)
        Long bucket0to3   = getUnspent(programId, now.minusDays(90), now, now);
        Long bucket3to6   = getUnspent(programId, now.minusDays(180), now.minusDays(90), now);
        Long bucket6to12  = getUnspent(programId, now.minusDays(365), now.minusDays(180), now);
        Long bucketOver12 = getUnspent(programId, LocalDateTime.of(1970, 1, 1, 0, 0), now.minusDays(365), now);

        // CON.4 — derive lastRefreshedAt from most recent fact earnDate
        LocalDateTime lastRefreshedAt = factRepository.findMaxEarnDate(programId);
        if (lastRefreshedAt == null) {
            // No facts yet — treat as if refreshed just now (empty warehouse is not stale)
            lastRefreshedAt = now;
        }

        boolean stale = Duration.between(lastRefreshedAt, now).compareTo(MAX_FRESHNESS) > 0;
        String stalenessWarning = stale ? CON4_WARNING : null;

        if (stale) {
            log.warn("[CON.4] Data Warehouse for program {} is stale. Last refresh: {}", programId, lastRefreshedAt);
        }

        return new LiabilityReport(
                totalLiability,
                bucket0to3,
                bucket3to6,
                bucket6to12,
                bucketOver12,
                lastRefreshedAt,
                stale,
                stalenessWarning
        );
    }

    private Long getUnspent(UUID programId, LocalDateTime from, LocalDateTime to, LocalDateTime now) {
        Long unspent = factRepository.calculateUnspentPointsInAgeBucket(programId, from, to, now);
        return unspent == null ? 0L : unspent;
    }

    /**
     * Immutable liability report DTO.
     *
     * {@code stale} and {@code stalenessWarning} implement CON.4 / EXC-07.
     */
    @Getter
    public static class LiabilityReport {
        private final BigDecimal totalLiabilityUsd;
        private final Long unspent0to3Months;
        private final Long unspent3to6Months;
        private final Long unspent6to12Months;
        private final Long unspentOver12Months;
        /** Timestamp of the most recent Data Warehouse fact row (derived from MAX earnDate). */
        private final LocalDateTime lastRefreshedAt;
        /** CON.4: true when lastRefreshedAt is more than 10 minutes ago. */
        private final boolean stale;
        /** CON.4 human-readable warning when stale=true; null otherwise. */
        private final String stalenessWarning;

        public LiabilityReport(BigDecimal totalLiabilityUsd, Long unspent0to3Months,
                                Long unspent3to6Months, Long unspent6to12Months,
                                Long unspentOver12Months, LocalDateTime lastRefreshedAt,
                                boolean stale, String stalenessWarning) {
            this.totalLiabilityUsd    = totalLiabilityUsd;
            this.unspent0to3Months    = unspent0to3Months;
            this.unspent3to6Months    = unspent3to6Months;
            this.unspent6to12Months   = unspent6to12Months;
            this.unspentOver12Months  = unspentOver12Months;
            this.lastRefreshedAt      = lastRefreshedAt;
            this.stale                = stale;
            this.stalenessWarning     = stalenessWarning;
        }
    }
}
