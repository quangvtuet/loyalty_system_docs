package com.loyalty.earning_engine.security;

import com.loyalty.earning_engine.domain.OwnershipViolationException;
import com.loyalty.earning_engine.domain.PointBalance;
import com.loyalty.earning_engine.domain.PointTransaction;
import com.loyalty.earning_engine.domain.TransactionStatus;
import com.loyalty.earning_engine.domain.TransactionType;
import com.loyalty.earning_engine.repository.FifoDebitAllocationRepository;
import com.loyalty.earning_engine.repository.PointBalanceRepository;
import com.loyalty.earning_engine.repository.PointTransactionRepository;
import com.loyalty.earning_engine.service.EarningLedgerService;
import com.loyalty.earning_engine.store.OwnedEarningStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
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

    private OwnedEarningStore ownedEarningStore;
    private EarningLedgerService earningLedgerService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ownedEarningStore = new OwnedEarningStore(transactionRepository, balanceRepository, allocationRepository);
        earningLedgerService = new EarningLedgerService(ownedEarningStore);
    }

    /**
     * NEG-I9-01: Direct write attempt by Partner Systems to Earning DB is refused under EXC-04.
     */
    @Test
    @DisplayName("NEG-I9-01: Direct write attempt by Partner Systems to Earning DB throws OwnershipViolationException and preserves ledger")
    void testPartnerSystems_DirectWriteToEarningDb_RefusedWithUnchangedLedger() {
        PointTransaction forgedTxn = PointTransaction.builder()
                .memberId("member-001")
                .programId("DEFAULT_PROG")
                .sourceTxnId("forged-partner-001")
                .amount(50000)
                .remainingBalance(50000)
                .type(TransactionType.EARN)
                .status(TransactionStatus.CONFIRMED)
                .createdAt(LocalDateTime.now())
                .build();

        // Attempt direct write from Partner Systems
        assertThrows(OwnershipViolationException.class, () ->
                ownedEarningStore.appendTransaction("Partner Systems", forgedTxn)
        );

        // Assert: 0 writes occurred to Earning DB
        verify(transactionRepository, never()).save(any(PointTransaction.class));
        verify(balanceRepository, never()).save(any(PointBalance.class));
    }

    /**
     * NEG-I9-02: Direct write attempt by Core Banking System to Earning DB is refused under EXC-04.
     */
    @Test
    @DisplayName("NEG-I9-02: Direct write attempt by Core Banking System to Earning DB throws OwnershipViolationException and preserves ledger")
    void testCoreBankingSystem_DirectWriteToEarningDb_RefusedWithUnchangedLedger() {
        PointTransaction forgedTxn = PointTransaction.builder()
                .memberId("member-001")
                .programId("DEFAULT_PROG")
                .sourceTxnId("forged-core-001")
                .amount(100000)
                .remainingBalance(100000)
                .type(TransactionType.EARN)
                .status(TransactionStatus.CONFIRMED)
                .createdAt(LocalDateTime.now())
                .build();

        // Attempt direct write from Core Banking System
        assertThrows(OwnershipViolationException.class, () ->
                ownedEarningStore.appendTransaction("Core Banking System", forgedTxn)
        );

        verify(transactionRepository, never()).save(any(PointTransaction.class));
        verify(balanceRepository, never()).save(any(PointBalance.class));
    }

    /**
     * NEG-I9-03: Direct write attempt by CRM & Notification Gateway to Earning DB is refused under EXC-04.
     */
    @Test
    @DisplayName("NEG-I9-03: Direct write attempt by CRM & Notification Gateway to Earning DB throws OwnershipViolationException and preserves ledger")
    void testCrmNotificationGateway_DirectWriteToEarningDb_RefusedWithUnchangedLedger() {
        PointBalance forgedBalance = PointBalance.builder()
                .memberId("member-001")
                .programId("DEFAULT_PROG")
                .confirmedBalance(999999L)
                .pendingBalance(0L)
                .build();

        // Attempt direct balance update from CRM & Notification Gateway
        assertThrows(OwnershipViolationException.class, () ->
                ownedEarningStore.updateBalance("CRM & Notification Gateway", forgedBalance)
        );

        verify(balanceRepository, never()).save(any(PointBalance.class));
        verify(transactionRepository, never()).save(any(PointTransaction.class));
    }

    /**
     * NEG-I9-04: Direct write attempt by Member to Earning DB is refused under EXC-04.
     */
    @Test
    @DisplayName("NEG-I9-04: Direct write attempt by Member to Earning DB throws OwnershipViolationException and preserves ledger")
    void testMember_DirectWriteToEarningDb_RefusedWithUnchangedLedger() {
        PointTransaction forgedTxn = PointTransaction.builder()
                .memberId("attacker-001")
                .programId("DEFAULT_PROG")
                .sourceTxnId("forged-member-001")
                .amount(1000000)
                .remainingBalance(1000000)
                .type(TransactionType.BONUS)
                .status(TransactionStatus.CONFIRMED)
                .createdAt(LocalDateTime.now())
                .build();

        assertThrows(OwnershipViolationException.class, () ->
                ownedEarningStore.appendTransaction("Member", forgedTxn)
        );

        verify(transactionRepository, never()).save(any(PointTransaction.class));
    }

    /**
     * POS-I9-01: Authorized owner (Earning Engine Service) can write to Earning DB.
     */
    @Test
    @DisplayName("POS-I9-01: Authorized writer Earning Engine Service successfully appends to Earning DB")
    void testEarningEngineService_AuthorizedOwnerWrite_AppendedSuccessfully() {
        PointTransaction validTxn = PointTransaction.builder()
                .memberId("member-001")
                .programId("DEFAULT_PROG")
                .sourceTxnId("valid-txn-001")
                .amount(500)
                .remainingBalance(500)
                .type(TransactionType.EARN)
                .status(TransactionStatus.CONFIRMED)
                .createdAt(LocalDateTime.now())
                .build();

        when(transactionRepository.save(validTxn)).thenReturn(validTxn);

        PointTransaction saved = ownedEarningStore.appendTransaction("Earning Engine Service", validTxn);

        assertNotNull(saved);
        verify(transactionRepository, times(1)).save(validTxn);
    }

    /**
     * T4 / I-9 Hard Rule Negative Test:
     * Non-owning caller attempts direct FIFO debit mutation with invalid parameters.
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
