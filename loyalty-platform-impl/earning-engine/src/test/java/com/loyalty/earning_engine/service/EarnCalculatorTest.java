package com.loyalty.earning_engine.service;

import com.loyalty.earning_engine.service.EarningLedgerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;




import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class EarnCalculatorTest {

    @Mock
    private EarningLedgerService ledgerService;

    @Mock
    private org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private EarnCalculator earnCalculator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testProcessEarn_Silver_BaseOnly() {
        earnCalculator.processEarn("user-1", 100, "txn-1", "SILVER", null, null);

        verify(ledgerService, times(1)).recordBaseEarn("user-1", 100, "txn-1");
    }

    @Test
    void testProcessEarn_Platinum_BaseOnly() {
        // Platinum tier gets 2.0x base multiplier
        earnCalculator.processEarn("user-1", 100, "txn-2", "PLATINUM", null, null);

        verify(ledgerService, times(1)).recordBaseEarn("user-1", 200, "txn-2");
    }

    @Test
    void testProcessEarn_Gold_WithDoubleBonus() {
        // Gold tier gets 1.5x base multiplier -> 150 points
        // Double bonus campaign means (2.0 - 1.0) * base = 1.0 * 150 = 150 bonus points
        earnCalculator.processEarn("user-1", 100, "txn-3", "GOLD", "DOUBLE_POINTS_AUG", null);

        verify(ledgerService, times(1)).recordBaseEarn("user-1", 150, "txn-3");
        verify(ledgerService, times(1)).recordBonusEarn("user-1", 150, "txn-3", "DOUBLE_POINTS_AUG");
    }
}
