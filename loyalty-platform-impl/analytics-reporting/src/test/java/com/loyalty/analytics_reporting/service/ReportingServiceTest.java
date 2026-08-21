package com.loyalty.analytics_reporting.service;

import com.loyalty.analytics_reporting.repository.FactPointTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class ReportingServiceTest {

    @Mock
    private FactPointTransactionRepository factRepository;

    @InjectMocks
    private ReportingService reportingService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetFinancialLiability() {
        UUID programId = UUID.randomUUID();

        when(factRepository.calculateTotalLiability(eq(programId), any()))
                .thenReturn(new BigDecimal("1250000.00"));
        
        when(factRepository.calculateUnspentPointsInAgeBucket(eq(programId), any(), any(), any()))
                .thenReturn(1000L)   // 0-3
                .thenReturn(2000L)   // 3-6
                .thenReturn(3000L)   // 6-12
                .thenReturn(4000L);  // >12

        ReportingService.LiabilityReport report = reportingService.getFinancialLiability(programId);

        assertEquals(new BigDecimal("1250000.00"), report.getTotalLiabilityUsd());
        assertEquals(1000L, report.getUnspent0to3Months());
        assertEquals(4000L, report.getUnspentOver12Months());
    }
}
