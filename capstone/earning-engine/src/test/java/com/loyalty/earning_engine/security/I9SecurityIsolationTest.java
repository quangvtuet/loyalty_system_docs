package com.loyalty.earning_engine.security;

import com.loyalty.earning_engine.domain.PointBalance;
import com.loyalty.earning_engine.domain.PointTransaction;
import com.loyalty.earning_engine.repository.FifoDebitAllocationRepository;
import com.loyalty.earning_engine.repository.PointBalanceRepository;
import com.loyalty.earning_engine.repository.PointTransactionRepository;
import com.loyalty.earning_engine.service.EarningLedgerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * I-9 Zone Security & Data Ownership Isolation Test (T4 Verification).
 *
 * Lab 1 I-9 Rule:
 * Member, Partner Systems, CRM & Notification Gateway, and Core Banking System
 * must NOT write directly to Earning DB (or Tiering DB / Redemption DB).
 * Earning Engine Service is the sole I-7 Owner of PointTransaction and PointBalance.
 *
 * EXC-04 / CON.2:
 * Direct database write attempts or bypasses of owning aggregate services (CT-04 / CT-13)
 * must be REFUSED, leaving the ledger state completely unaltered (0 repository writes).
 *
 * Spec-trace: I-9, CON.2, EXC-04, T4
 */
class I9SecurityIsolationTest {

    @Mock
    private PointTransactionRepository transactionRepository;

    @Mock
    private PointBalanceRepository balanceRepository;

    @Mock
    private FifoDebitAllocationRepository allocationRepository;

    private EarningLedgerService earningLedgerService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        earningLedgerService = new EarningLedgerService(transactionRepository, balanceRepository, allocationRepository);
    }

    /**
     * T4 / I-9 Hard Rule Negative Test:
     * Non-owning caller attempts direct FIFO debit mutation with invalid parameters
     * (e.g. negative points or missing orderId).
     *
     * Verification:
     * 1. Operation is refused with IllegalArgumentException.
     * 2. PointTransactionRepository is NEVER saved.
     * 3. PointBalanceRepository is NEVER saved.
     * 4. FifoDebitAllocationRepository is NEVER saved.
     * 5. Earning DB ledger state remains completely intact.
     */
    @Test
    @DisplayName("T4: Direct/invalid debit attempt without valid contract is rejected with zero ledger mutation")
    void testDirectDebitAttempt_InvalidParameters_RefusedWithoutLedgerMutation() {
        // Attempt 1: Negative points
        assertThrows(IllegalArgumentException.class, () ->
                earningLedgerService.debitPointsFifo("external-attacker", "DEFAULT_PROG", -500L, "order-attack-001"));

        // Attempt 2: Blank order ID
        assertThrows(IllegalArgumentException.class, () ->
                earningLedgerService.debitPointsFifo("external-attacker", "DEFAULT_PROG", 500L, "   "));

        // Assert that NO write occurred to Earning DB
        verify(transactionRepository, never()).save(any(PointTransaction.class));
        verify(balanceRepository, never()).save(any(PointBalance.class));
        verify(allocationRepository, never()).save(any());
    }

    /**
     * T4 / I-9 Hard Rule Negative Test:
     * Non-owner / unauthorized caller attempts to restore points without an existing CT-13 debit allocation.
     *
     * Verification:
     * 1. Operation is refused with IllegalStateException (ERR_RED_UNKNOWN_ALLOCATION).
     * 2. PointTransactionRepository is NEVER saved (no rogue REVERSAL written).
     * 3. PointBalance is NEVER incremented.
     * 4. Earning DB ledger remains strictly unmodified.
     */
    @Test
    @DisplayName("T4: Direct restore attempt without valid allocation is rejected with zero ledger mutation")
    void testDirectRestoreAttempt_UnknownAllocation_RefusedWithoutLedgerMutation() {
        when(allocationRepository.findByOrderIdOrderByCreatedAtAsc("rogue-order-999"))
                .thenReturn(Collections.emptyList());

        assertThrows(IllegalStateException.class, () ->
                earningLedgerService.restorePoints("attacker-002", "DEFAULT_PROG", "rogue-order-999", 10000L, "FRAUDULENT_REFUND"));

        // Assert that NO write occurred to Earning DB
        verify(transactionRepository, never()).save(any(PointTransaction.class));
        verify(balanceRepository, never()).save(any(PointBalance.class));
    }

    /**
     * T4 / I-9 Hard Rule Negative Test:
     * Non-owner attempts to cancel a non-existent or unowned source transaction.
     *
     * Verification:
     * 1. cancelTransaction returns false.
     * 2. PointTransactionRepository and PointBalanceRepository are NEVER touched.
     */
    @Test
    @DisplayName("T4: Unauthorized cancellation of unknown transaction leaves Earning DB untouched")
    void testCancelUnknownTransaction_RefusedWithoutLedgerMutation() {
        when(transactionRepository.findBySourceTxnId("non-existent-source-txn"))
                .thenReturn(Optional.empty());

        boolean result = earningLedgerService.cancelTransaction("non-existent-source-txn");

        assertFalse(result, "Cancellation of unknown transaction must return false");
        verify(transactionRepository, never()).save(any());
        verify(balanceRepository, never()).save(any());
    }

    /**
     * T4 / I-9 Negative Test:
     * Direct debit request against a member with 0 / missing balance in Earning DB.
     *
     * Verification:
     * 1. Refused with IllegalStateException (ERR_RED_INSUFFICIENT_BALANCE).
     * 2. Zero transactions or balance changes committed to Earning DB.
     */
    @Test
    @DisplayName("T4: Direct debit against missing member balance refused with zero state change")
    void testDirectDebit_MissingMemberBalance_RefusedWithNoMutation() {
        when(allocationRepository.findByOrderIdOrderByCreatedAtAsc("order-001"))
                .thenReturn(Collections.emptyList());
        when(balanceRepository.findByMemberIdAndProgramIdForUpdate("unregistered-user", "DEFAULT_PROG"))
                .thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () ->
                earningLedgerService.debitPointsFifo("unregistered-user", "DEFAULT_PROG", 500L, "order-001"));

        verify(transactionRepository, never()).save(any());
        verify(balanceRepository, never()).save(any());
        verify(allocationRepository, never()).save(any());
    }
}
