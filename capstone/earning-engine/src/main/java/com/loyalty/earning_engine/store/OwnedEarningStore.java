package com.loyalty.earning_engine.store;

import com.loyalty.earning_engine.domain.FifoDebitAllocation;
import com.loyalty.earning_engine.domain.OwnershipViolationException;
import com.loyalty.earning_engine.domain.PointBalance;
import com.loyalty.earning_engine.domain.PointTransaction;
import com.loyalty.earning_engine.domain.TransactionStatus;
import com.loyalty.earning_engine.repository.FifoDebitAllocationRepository;
import com.loyalty.earning_engine.repository.PointBalanceRepository;
import com.loyalty.earning_engine.repository.PointTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * OwnedEarningStore — I-7 Data Store & I-9 Boundary Defense for Earning DB.
 *
 * Lab 1 I-9 Rule:
 * Member, Partner Systems, CRM & Notification Gateway, and Core Banking System
 * must NOT write directly to Earning DB.
 * Earning Engine Service is the sole authorized writer / I-7 owner.
 *
 * EXC-04 / CON.2:
 * Any write attempt from non-owners is refused with an OwnershipViolationException,
 * ensuring zero mutations occur in the underlying Earning DB repository.
 *
 * All live write paths in EarningLedgerService pass through this store using
 * callerIdentity = AUTHORIZED_OWNER ("Earning Engine Service").
 *
 * Spec-trace: I-9, EXC-04, CON.2, T4
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OwnedEarningStore {

    public static final String AUTHORIZED_OWNER = "Earning Engine Service";
    public static final String STORE_NAME = "Earning DB";

    private final PointTransactionRepository transactionRepository;
    private final PointBalanceRepository balanceRepository;
    private final FifoDebitAllocationRepository allocationRepository;

    /**
     * Enforces single I-7 ownership before any write to Earning DB.
     *
     * @param callerIdentity Name of the calling system / actor
     * @throws OwnershipViolationException if caller is not Earning Engine Service
     */
    public void assertWriter(String callerIdentity) {
        if (!AUTHORIZED_OWNER.equals(callerIdentity)) {
            log.warn("[I-9 Violation Refused] Unauthorized caller '{}' attempted direct write to '{}'",
                    callerIdentity, STORE_NAME);
            throw new OwnershipViolationException(callerIdentity, STORE_NAME);
        }
    }

    // ─── Gated Write Methods (I-9 / EXC-04 / CON.2) ──────────────────────────

    /**
     * Appends a PointTransaction to Earning DB, gated by single ownership.
     */
    public PointTransaction appendTransaction(String callerIdentity, PointTransaction transaction) {
        assertWriter(callerIdentity);
        return transactionRepository.save(transaction);
    }

    /**
     * Updates a PointBalance in Earning DB, gated by single ownership.
     */
    public PointBalance updateBalance(String callerIdentity, PointBalance balance) {
        assertWriter(callerIdentity);
        return balanceRepository.save(balance);
    }

    /**
     * Saves a FifoDebitAllocation in Earning DB, gated by single ownership.
     */
    public FifoDebitAllocation saveAllocation(String callerIdentity, FifoDebitAllocation allocation) {
        assertWriter(callerIdentity);
        return allocationRepository.save(allocation);
    }

    // ─── Query Methods ───────────────────────────────────────────────────────

    public Optional<PointBalance> findBalanceForUpdate(String memberId, String programId) {
        return balanceRepository.findByMemberIdAndProgramIdForUpdate(memberId, programId);
    }

    public Optional<PointBalance> findBalance(String memberId, String programId) {
        return balanceRepository.findByMemberIdAndProgramId(memberId, programId);
    }

    public List<PointTransaction> findUnexpiredBatchesForFifoDebit(String memberId, String programId,
                                                                   TransactionStatus status, LocalDateTime now) {
        return transactionRepository.findUnexpiredBatchesForFifoDebit(memberId, programId, status, now);
    }

    public Optional<PointTransaction> findTransactionBySourceTxnId(String sourceTxnId) {
        return transactionRepository.findBySourceTxnId(sourceTxnId);
    }

    public Optional<PointTransaction> findTransactionById(UUID id) {
        return transactionRepository.findById(id);
    }

    public List<FifoDebitAllocation> findAllocationsByOrderId(String orderId) {
        return allocationRepository.findByOrderIdOrderByCreatedAtAsc(orderId);
    }

    public long getTransactionCount() {
        return transactionRepository.count();
    }

    public List<PointTransaction> getAllTransactions() {
        return transactionRepository.findAll();
    }
}
