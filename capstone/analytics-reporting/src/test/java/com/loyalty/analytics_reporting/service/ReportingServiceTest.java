package com.loyalty.analytics_reporting.service;

import com.loyalty.analytics_reporting.repository.FactPointTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ReportingService — UC-LB-04 Generate point liability report.
 *
 * Spec-trace:
 * - testGetFinancialLiability                            → basic report correctness (pre-existing)
 * - testGetFinancialLiability_Fresh_ReturnsReport        → happy path UC-LB-04, I-11
 * - testGetFinancialLiability_StaleData_FlagsAsStale     → G6-A05, I-11, I-5 CON.4 (EXC-07)
 * - testGetFinancialLiability_OnlyReadsDataWarehouse     → I-9 isolation (EXC-08)
 * - testGetFinancialLiability_EmptyWarehouse_NotStale    → empty DW treated as fresh
 */
class ReportingServiceTest {

    @Mock
    private FactPointTransactionRepository factRepository;

    @InjectMocks
    private ReportingService reportingService;

    private UUID programId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        programId = UUID.randomUUID();
    }

    /**
     * Pre-existing basic correctness test (retained unchanged).
     */
    @Test
    void testGetFinancialLiability() {
        when(factRepository.calculateTotalLiability(eq(programId), any()))
                .thenReturn(new BigDecimal("1250000.00"));
        when(factRepository.calculateUnspentPointsInAgeBucket(eq(programId), any(), any(), any()))
                .thenReturn(1000L)   // 0-3
                .thenReturn(2000L)   // 3-6
                .thenReturn(3000L)   // 6-12
                .thenReturn(4000L);  // >12
        when(factRepository.findMaxEarnDate(eq(programId)))
                .thenReturn(LocalDateTime.now().minusMinutes(2));

        ReportingService.LiabilityReport report = reportingService.getFinancialLiability(programId);

        assertEquals(new BigDecimal("1250000.00"), report.getTotalLiabilityUsd());
        assertEquals(1000L, report.getUnspent0to3Months());
        assertEquals(4000L, report.getUnspentOver12Months());
    }

    /**
     * UC-LB-04 happy path — fresh report.
     * lastRefreshedAt is within 10 minutes → stale=false, stalenessWarning=null.
     * Spec-trace: I-11
     */
    @Test
    void testGetFinancialLiability_Fresh_ReturnsReport() {
        // Arrange: warehouse was refreshed 5 minutes ago (within CON.4 limit)
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        stubRepositoryWithLastRefresh(fiveMinutesAgo);

        // Act
        ReportingService.LiabilityReport report = reportingService.getFinancialLiability(programId);

        // Assert: happy path — report is returned and NOT stale
        assertNotNull(report);
        assertFalse(report.isStale(), "Report must not be stale when DW refreshed < 10 minutes ago");
        assertNull(report.getStalenessWarning(), "No staleness warning expected for a fresh report");
        assertEquals(fiveMinutesAgo, report.getLastRefreshedAt());
        assertEquals(BigDecimal.valueOf(12345), report.getTotalLiabilityUsd());
    }

    /**
     * UC-LB-04 alt — stale data (CON.4 / EXC-07 / G6-A05).
     * lastRefreshedAt is more than 10 minutes ago → stale=true, stalenessWarning populated.
     * Report is returned but must NOT be presented as current.
     * Spec-trace: G6-A05, I-11, I-5 CON.4
     */
    @Test
    void testGetFinancialLiability_StaleData_FlagsAsStale() {
        // Arrange: warehouse was last refreshed 15 minutes ago — past the CON.4 10-minute limit
        LocalDateTime fifteenMinutesAgo = LocalDateTime.now().minusMinutes(15);
        stubRepositoryWithLastRefresh(fifteenMinutesAgo);

        // Act
        ReportingService.LiabilityReport report = reportingService.getFinancialLiability(programId);

        // Assert: CON.4 stale flag is set
        assertNotNull(report);
        assertTrue(report.isStale(),
                "Report must be stale when DW was last refreshed more than 10 minutes ago (CON.4 / EXC-07)");
        assertNotNull(report.getStalenessWarning(),
                "Staleness warning must be populated for Finance to identify stale data");
        assertTrue(report.getStalenessWarning().contains("CON.4"),
                "Warning must reference CON.4");
        assertEquals(fifteenMinutesAgo, report.getLastRefreshedAt());
        // Data is still returned — just flagged, not hidden (EXC-07: show staleness instead of hiding)
        assertNotNull(report.getTotalLiabilityUsd());
    }

    /**
     * I-9 isolation — Analytics & Reporting Service reads ONLY from the Data Warehouse.
     * Verifies only FactPointTransactionRepository is used (CON.2 / EXC-08).
     * No Earning DB, Tiering DB, Redemption DB, or Program Mgmt DB is accessed.
     * Spec-trace: I-9
     */
    @Test
    void testGetFinancialLiability_OnlyReadsDataWarehouse() {
        LocalDateTime recentRefresh = LocalDateTime.now().minusMinutes(2);
        stubRepositoryWithLastRefresh(recentRefresh);

        reportingService.getFinancialLiability(programId);

        // The only repository in this service is factRepository (Data Warehouse).
        // Verifying all fact interactions confirms I-9 compliance.
        verify(factRepository, atLeastOnce()).calculateTotalLiability(eq(programId), any());
        verify(factRepository, atLeastOnce()).findMaxEarnDate(eq(programId));
        // No cross-program data leakage
        verify(factRepository, never()).calculateTotalLiability(argThat(id -> !id.equals(programId)), any());
    }

    /**
     * Edge case — empty Data Warehouse (no facts yet).
     * findMaxEarnDate returns null → treated as refreshed now → stale=false.
     * Spec-trace: I-11 boundary
     */
    @Test
    void testGetFinancialLiability_EmptyWarehouse_NotStale() {
        when(factRepository.calculateTotalLiability(any(), any())).thenReturn(null);
        when(factRepository.calculateUnspentPointsInAgeBucket(any(), any(), any(), any())).thenReturn(null);
        when(factRepository.findMaxEarnDate(any())).thenReturn(null); // no facts yet

        ReportingService.LiabilityReport report = reportingService.getFinancialLiability(programId);

        assertNotNull(report);
        assertFalse(report.isStale(),
                "Empty warehouse has no earnDate — must not be considered stale");
        assertEquals(BigDecimal.ZERO, report.getTotalLiabilityUsd());
    }

    // ── Helper ───────────────────────────────────────────────────────

    private void stubRepositoryWithLastRefresh(LocalDateTime lastRefresh) {
        when(factRepository.calculateTotalLiability(eq(programId), any()))
                .thenReturn(BigDecimal.valueOf(12345));
        when(factRepository.calculateUnspentPointsInAgeBucket(eq(programId), any(), any(), any()))
                .thenReturn(1000L);
        when(factRepository.findMaxEarnDate(eq(programId)))
                .thenReturn(lastRefresh);
    }
}
