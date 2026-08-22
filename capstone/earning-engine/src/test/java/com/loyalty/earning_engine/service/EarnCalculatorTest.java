package com.loyalty.earning_engine.service;

import com.loyalty.earning_engine.client.TieringClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.core.KafkaTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class EarnCalculatorTest {

    @Mock
    private EarningLedgerService ledgerService;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private TieringClient tieringClient;

    @InjectMocks
    private EarnCalculator earnCalculator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testProcessEarn_Silver_BaseOnly() {
        when(tieringClient.getMemberTier("user-1", "DEFAULT_PROG")).thenReturn("SILVER");

        earnCalculator.processEarn("user-1", 100, "txn-1", "SILVER", null, "DEFAULT_PROG");

        verify(ledgerService, times(1)).recordBaseEarn("user-1", 100, "txn-1");
    }

    @Test
    void testProcessEarn_Platinum_BaseOnly() {
        when(tieringClient.getMemberTier("user-1", "DEFAULT_PROG")).thenReturn("PLATINUM");

        // Platinum tier gets 2.0x base multiplier
        earnCalculator.processEarn("user-1", 100, "txn-2", "PLATINUM", null, "DEFAULT_PROG");

        verify(ledgerService, times(1)).recordBaseEarn("user-1", 200, "txn-2");
    }

    @Test
    void testProcessEarn_Gold_WithDoubleBonus() {
        when(tieringClient.getMemberTier("user-1", "DEFAULT_PROG")).thenReturn("GOLD");

        // Gold tier gets 1.5x base multiplier -> 150 points
        // Double bonus campaign means (2.0 - 1.0) * base = 1.0 * 150 = 150 bonus points
        earnCalculator.processEarn("user-1", 100, "txn-3", "GOLD", "DOUBLE_POINTS_AUG", "DEFAULT_PROG");

        verify(ledgerService, times(1)).recordBaseEarn("user-1", 150, "txn-3");
        verify(ledgerService, times(1)).recordBonusEarn("user-1", 150, "txn-3", "DOUBLE_POINTS_AUG");
    }
}
