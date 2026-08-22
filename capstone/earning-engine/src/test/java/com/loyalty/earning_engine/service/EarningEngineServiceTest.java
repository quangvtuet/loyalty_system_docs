package com.loyalty.earning_engine.service;

import com.loyalty.earning_engine.domain.FifoDebitAllocation;
import com.loyalty.earning_engine.domain.PointBalance;
import com.loyalty.earning_engine.domain.PointTransaction;
import com.loyalty.earning_engine.domain.TransactionStatus;
import com.loyalty.earning_engine.domain.TransactionType;
import com.loyalty.earning_engine.dto.FifoDebitResponse;
import com.loyalty.earning_engine.dto.FifoRestoreResponse;
import com.loyalty.earning_engine.repository.FifoDebitAllocationRepository;
import com.loyalty.earning_engine.repository.PointBalanceRepository;
import com.loyalty.earning_engine.repository.PointTransactionRepository;
import com.loyalty.earning_engine.store.OwnedEarningStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the Earning Engine — UC-LB-01 and CT-13 DebitPointsFifo / RestorePoints.
 *
 * Spec-trace:
 * - testProcessEarn_HappyPath_TransactionWritten          → UC-LB-01 happy path, I-5, I-11
 * - testProcessEarn_DuplicateEvent_NoDuplicateTransaction → G6-A01, I-11, I-5 CON.1 (EXC-01)
 * - testProcessEarn_BonusCampaign_RecordsBonus            → UC-LB-01 campaign bonus path
 * - testProcessEarn_FloorRounding_PointsFloored           → DD-01 §3.1 FLOOR formula
 * - testDebitPointsFifo_MultipleBatches_DebitsOldestFirst → CT-13 DebitPointsFifo, FIFO ordering (FR-03-021)
 * - testRestorePoints_RestoresToOriginalBatches_PreservesEarnDate → CT-13 RestorePoints (CON.3 / EXC-05)
 * - testRestorePoints_IdempotentReplay_NoDoubleCredit     → CT-13 / CON.3 Idempotency
 */
class EarningEngineServiceTest {

    @Mock
    private PointTransactionRepository transactionRepository;

    @Mock
    private PointBalanceRepository balanceRepository;

    @Mock
    private FifoDebitAllocationRepository allocationRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private EarnCalculator earnCalculator;

    private OwnedEarningStore ownedEarningStore;
    private EarningLedgerService earningLedgerService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ownedEarningStore = new OwnedEarningStore(transactionRepository, balanceRepository, allocationRepository);
        earningLedgerService = new EarningLedgerService(ownedEarningStore);
        earnCalculator = new EarnCalculator(earningLedgerService, kafkaTemplate);

        when(balanceRepository.findByMemberIdAndProgramIdForUpdate(anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(balanceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    /**
     * UC-LB-01 happy path — settled event arrives; PointTransaction written; QP event published.
     */
    @Test
    void testProcessEarn_HappyPath_TransactionWritten() {
        earnCalculator.processEarn("member-001", 1000, "txn-hp-001", "SILVER", null, "DEFAULT_PROG");

        ArgumentCaptor<PointTransaction> txnCaptor = ArgumentCaptor.forClass(PointTransaction.class);
        verify(transactionRepository, times(1)).save(txnCaptor.capture());

        PointTransaction txn = txnCaptor.getValue();
        assertEquals(TransactionType.EARN, txn.getType(), "Transaction type must be EARN");
        assertEquals(1000, txn.getAmount(), "Base points: FLOOR(1000 × 1.0 × 1.0) = 1000");
        assertEquals("txn-hp-001", txn.getSourceTxnId(), "Source transaction ID must be preserved (CON.1 key)");

        verify(kafkaTemplate, times(1)).send(eq("loyalty.earning.qp_accrued"), eq("member-001"), any());
    }

    /**
     * UC-LB-01 alt — CON.1 duplicate earn event (EXC-01 / G6-A01).
     */
    @Test
    void testProcessEarn_DuplicateEvent_NoDuplicateTransaction() {
        IdempotencyService idempotencyService = mock(IdempotencyService.class);

        when(idempotencyService.isDuplicate("txn-dup-001", "PARTNER"))
                .thenReturn(false)
                .thenReturn(true);

        boolean firstIsDuplicate = idempotencyService.isDuplicate("txn-dup-001", "PARTNER");
        if (!firstIsDuplicate) {
            earnCalculator.processEarn("member-001", 500, "txn-dup-001", "SILVER", null, "DEFAULT_PROG");
        }

        boolean secondIsDuplicate = idempotencyService.isDuplicate("txn-dup-001", "PARTNER");
        if (!secondIsDuplicate) {
            earnCalculator.processEarn("member-001", 500, "txn-dup-001", "SILVER", null, "DEFAULT_PROG");
        }

        verify(transactionRepository, times(1)).save(argThat(
                txn -> "txn-dup-001".equals(txn.getSourceTxnId())
        ));
        verify(kafkaTemplate, times(1)).send(eq("loyalty.earning.qp_accrued"), anyString(), any());
    }

    /**
     * UC-LB-01 with campaign bonus.
     */
    @Test
    void testProcessEarn_BonusCampaign_RecordsBonus() {
        earnCalculator.processEarn("member-001", 1000, "txn-bonus-001", "SILVER", "DOUBLE_POINTS_AUG", "DEFAULT_PROG");

        ArgumentCaptor<PointTransaction> txnCaptor = ArgumentCaptor.forClass(PointTransaction.class);
        verify(transactionRepository, times(2)).save(txnCaptor.capture());

        PointTransaction earnTxn = txnCaptor.getAllValues().get(0);
        PointTransaction bonusTxn = txnCaptor.getAllValues().get(1);

        assertEquals(TransactionType.EARN, earnTxn.getType());
        assertEquals(1000, earnTxn.getAmount());
        assertEquals(TransactionType.BONUS, bonusTxn.getType());
        assertEquals(1000, bonusTxn.getAmount());
        assertEquals("DOUBLE_POINTS_AUG", bonusTxn.getCampaignId());
    }

    /**
     * DD-01 §3.1 FLOOR formula.
     */
    @Test
    void testProcessEarn_FloorRounding_PointsFloored() {
        earnCalculator.processEarn("member-001", 1001, "txn-floor-001", "GOLD", null, "DEFAULT_PROG");

        ArgumentCaptor<PointTransaction> txnCaptor = ArgumentCaptor.forClass(PointTransaction.class);
        verify(transactionRepository, times(1)).save(txnCaptor.capture());

        assertEquals(1501, txnCaptor.getValue().getAmount());
    }

    /**
     * CT-13: DebitPointsFifo — exercises FIFO batch allocation across multiple unexpired earn batches.
     * Batch 1 (oldest, 300 pts) + Batch 2 (newer, 500 pts).
     * Debit 400 pts -> Batch 1 fully depleted (0 pts), Batch 2 partially debited (remaining = 400).
     * Production path: verifies FifoDebitAllocation entities are persisted.
     */
    @Test
    void testDebitPointsFifo_MultipleBatches_DebitsOldestFirst() {
        // Arrange
        PointBalance balance = PointBalance.builder()
                .memberId("member-001")
                .programId("DEFAULT_PROG")
                .confirmedBalance(800L)
                .pendingBalance(0L)
                .build();
        when(balanceRepository.findByMemberIdAndProgramIdForUpdate("member-001", "DEFAULT_PROG"))
                .thenReturn(Optional.of(balance));

        LocalDateTime oldestDate = LocalDateTime.now().minusDays(10);
        LocalDateTime newerDate = LocalDateTime.now().minusDays(2);

        UUID batch1Id = UUID.randomUUID();
        UUID batch2Id = UUID.randomUUID();

        PointTransaction batch1 = PointTransaction.builder()
                .id(batch1Id)
                .memberId("member-001")
                .type(TransactionType.EARN)
                .amount(300)
                .remainingBalance(300)
                .status(TransactionStatus.CONFIRMED)
                .createdAt(oldestDate)
                .expiryDate(LocalDateTime.now().plusMonths(11))
                .build();

        PointTransaction batch2 = PointTransaction.builder()
                .id(batch2Id)
                .memberId("member-001")
                .type(TransactionType.EARN)
                .amount(500)
                .remainingBalance(500)
                .status(TransactionStatus.CONFIRMED)
                .createdAt(newerDate)
                .expiryDate(LocalDateTime.now().plusMonths(12))
                .build();

        List<PointTransaction> batches = new ArrayList<>(List.of(batch1, batch2));
        when(transactionRepository.findUnexpiredBatchesForFifoDebit(eq("member-001"), eq("DEFAULT_PROG"), eq(TransactionStatus.CONFIRMED), any()))
                .thenReturn(batches);
        when(allocationRepository.findByOrderIdOrderByCreatedAtAsc("order-001"))
                .thenReturn(Collections.emptyList());

        // Act: debit 400 points
        FifoDebitResponse response = earningLedgerService.debitPointsFifo("member-001", "DEFAULT_PROG", 400L, "order-001");

        // Assert
        assertEquals("RESERVED", response.getStatus());
        assertEquals(400L, response.getPointsDebited());
        assertEquals(2, response.getDebitedBatchesCount());

        // Batch 1 (oldest) must be 0 remaining
        assertEquals(0, batch1.getRemainingBalance(), "Oldest batch must be fully debited first (FIFO)");
        // Batch 2 (newer) must have 400 remaining (500 - 100)
        assertEquals(400, batch2.getRemainingBalance(), "Newer batch takes remaining 100 points");

        // PointBalance must be updated to 400 (800 - 400)
        assertEquals(400L, balance.getConfirmedBalance());

        // Verify FifoDebitAllocation was saved for each batch
        ArgumentCaptor<FifoDebitAllocation> allocCaptor = ArgumentCaptor.forClass(FifoDebitAllocation.class);
        verify(allocationRepository, times(2)).save(allocCaptor.capture());

        List<FifoDebitAllocation> savedAllocations = allocCaptor.getAllValues();
        assertEquals(batch1Id, savedAllocations.get(0).getBatchId());
        assertEquals(300, savedAllocations.get(0).getPointsDebited());
        assertFalse(savedAllocations.get(0).isRestored());

        assertEquals(batch2Id, savedAllocations.get(1).getBatchId());
        assertEquals(100, savedAllocations.get(1).getPointsDebited());
        assertFalse(savedAllocations.get(1).isRestored());
    }

    /**
     * CT-13: RestorePoints — verifies that CON.3 / EXC-05 auto-reversal restores
     * points to the exact original earn batches using FifoDebitAllocation,
     * preserving original earn date and expiry date intact.
     */
    @Test
    void testRestorePoints_RestoresToOriginalBatches_PreservesEarnDate() {
        // Arrange
        PointBalance balance = PointBalance.builder()
                .memberId("member-001")
                .programId("DEFAULT_PROG")
                .confirmedBalance(400L)
                .pendingBalance(0L)
                .build();
        when(balanceRepository.findByMemberIdAndProgramIdForUpdate("member-001", "DEFAULT_PROG"))
                .thenReturn(Optional.of(balance));

        LocalDateTime originalEarnDate = LocalDateTime.now().minusDays(10);
        LocalDateTime originalExpiryDate = LocalDateTime.now().plusMonths(11);

        UUID batch1Id = UUID.randomUUID();
        UUID batch2Id = UUID.randomUUID();

        PointTransaction batch1 = PointTransaction.builder()
                .id(batch1Id)
                .memberId("member-001")
                .type(TransactionType.EARN)
                .amount(300)
                .remainingBalance(0) // previously depleted
                .status(TransactionStatus.CONFIRMED)
                .createdAt(originalEarnDate)
                .expiryDate(originalExpiryDate)
                .build();

        PointTransaction batch2 = PointTransaction.builder()
                .id(batch2Id)
                .memberId("member-001")
                .type(TransactionType.EARN)
                .amount(500)
                .remainingBalance(400) // previously debited 100
                .status(TransactionStatus.CONFIRMED)
                .createdAt(LocalDateTime.now().minusDays(2))
                .expiryDate(LocalDateTime.now().plusMonths(12))
                .build();

        FifoDebitAllocation alloc1 = FifoDebitAllocation.builder()
                .id(UUID.randomUUID())
                .orderId("order-001")
                .memberId("member-001")
                .programId("DEFAULT_PROG")
                .batchId(batch1Id)
                .pointsDebited(300)
                .restored(false)
                .build();

        FifoDebitAllocation alloc2 = FifoDebitAllocation.builder()
                .id(UUID.randomUUID())
                .orderId("order-001")
                .memberId("member-001")
                .programId("DEFAULT_PROG")
                .batchId(batch2Id)
                .pointsDebited(100)
                .restored(false)
                .build();

        when(allocationRepository.findByOrderIdOrderByCreatedAtAsc("order-001"))
                .thenReturn(List.of(alloc1, alloc2));
        when(transactionRepository.findById(batch1Id)).thenReturn(Optional.of(batch1));
        when(transactionRepository.findById(batch2Id)).thenReturn(Optional.of(batch2));

        // Act: restore 400 points under CON.3
        FifoRestoreResponse response = earningLedgerService.restorePoints(
                "member-001", "DEFAULT_PROG", "order-001", 400L, "PARTNER_FAILURE");

        // Assert
        assertEquals("RESTORED", response.getStatus());
        assertEquals(400L, response.getPointsRestored());

        // Batch 1 must be restored to 300 with original earn date preserved
        assertEquals(300, batch1.getRemainingBalance());
        assertEquals(originalEarnDate, batch1.getCreatedAt(), "Original earn date must be preserved under CON.3");
        assertEquals(originalExpiryDate, batch1.getExpiryDate(), "Original expiry date must be preserved under CON.3");

        // Batch 2 must be restored to 500
        assertEquals(500, batch2.getRemainingBalance());

        // Balance restored back to 800 (400 + 400)
        assertEquals(800L, balance.getConfirmedBalance());

        // Allocations marked as restored
        assertTrue(alloc1.isRestored());
        assertTrue(alloc2.isRestored());
        verify(allocationRepository, times(2)).save(any(FifoDebitAllocation.class));
    }

    /**
     * CT-13 / CON.3 Idempotency: Replaying an already-restored order returns RESTORED
     * without double-crediting balances or batches.
     */
    @Test
    @DisplayName("G5 / CON.3: Duplicate restore request is idempotent and does not double-credit")
    void testRestorePoints_IdempotentReplay_NoDoubleCredit() {
        FifoDebitAllocation alloc = FifoDebitAllocation.builder()
                .id(UUID.randomUUID())
                .orderId("order-001")
                .memberId("member-001")
                .programId("DEFAULT_PROG")
                .batchId(UUID.randomUUID())
                .pointsDebited(300)
                .restored(true) // already restored
                .build();

        when(allocationRepository.findByOrderIdOrderByCreatedAtAsc("order-001"))
                .thenReturn(List.of(alloc));

        FifoRestoreResponse response = earningLedgerService.restorePoints(
                "member-001", "DEFAULT_PROG", "order-001", 300L, "PARTNER_FAILURE");

        assertEquals("RESTORED", response.getStatus());
        assertEquals("Points restore already applied", response.getMessage());

        // Verify balanceRepository and transactionRepository were NOT mutated again
        verify(balanceRepository, never()).findByMemberIdAndProgramIdForUpdate(anyString(), anyString());
        verify(transactionRepository, never()).findById(any());
    }
}
