package com.loyalty.earning_engine.service;

import com.loyalty.earning_engine.domain.PointTransaction;
import com.loyalty.earning_engine.repository.PointTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class EarnCalculatorTest {

    @Mock
    private PointTransactionRepository repository;

    @InjectMocks
    private EarnCalculator earnCalculator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testProcessEarn_Silver_BaseOnly() {
        earnCalculator.processEarn("user-1", 100, "txn-1", "SILVER", null);

        ArgumentCaptor<PointTransaction> captor = ArgumentCaptor.forClass(PointTransaction.class);
        verify(repository, times(1)).save(captor.capture());

        PointTransaction savedTxn = captor.getValue();
        assertEquals(100, savedTxn.getAmount());
    }

    @Test
    void testProcessEarn_Platinum_BaseOnly() {
        // Platinum tier gets 2.0x base multiplier
        earnCalculator.processEarn("user-1", 100, "txn-2", "PLATINUM", null);

        ArgumentCaptor<PointTransaction> captor = ArgumentCaptor.forClass(PointTransaction.class);
        verify(repository, times(1)).save(captor.capture());

        PointTransaction savedTxn = captor.getValue();
        assertEquals(200, savedTxn.getAmount());
    }

    @Test
    void testProcessEarn_Gold_WithDoubleBonus() {
        // Gold tier gets 1.5x base multiplier -> 150 points
        // Double bonus campaign means (2.0 - 1.0) * base = 1.0 * 150 = 150 bonus points
        earnCalculator.processEarn("user-1", 100, "txn-3", "GOLD", "DOUBLE_POINTS_AUG");

        ArgumentCaptor<PointTransaction> captor = ArgumentCaptor.forClass(PointTransaction.class);
        verify(repository, times(2)).save(captor.capture());

        List<PointTransaction> savedTxns = captor.getAllValues();
        assertEquals(150, savedTxns.get(0).getAmount()); // Base
        assertEquals(150, savedTxns.get(1).getAmount()); // Bonus
    }
}
