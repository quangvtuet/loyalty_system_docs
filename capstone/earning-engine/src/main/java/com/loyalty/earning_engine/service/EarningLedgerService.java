package com.loyalty.earning_engine.service;

import com.loyalty.earning_engine.domain.PointBalance;
import com.loyalty.earning_engine.domain.FifoDebitAllocation;
import com.loyalty.earning_engine.domain.PointTransaction;
import com.loyalty.earning_engine.domain.TransactionStatus;
import com.loyalty.earning_engine.domain.TransactionType;
import com.loyalty.earning_engine.dto.FifoDebitResponse;
import com.loyalty.earning_engine.dto.FifoRestoreResponse;
import com.loyalty.earning_engine.repository.FifoDebitAllocationRepository;
import com.loyalty.earning_engine.repository.PointBalanceRepository;
import com.loyalty.earning_engine.repository.PointTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Earning Ledger Service (Sổ cái & FIFO Engine).
 *
 * Source of Truth (I-7) for PointTransaction and PointBalance.
 * Owns CT-04 (RecordEarn) and CT-13 (DebitPointsFifo, RestorePoints).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EarningLedgerService {

    private final PointTransactionRepository transactionRepository;
    private final PointBalanceRepository balanceRepository;
    private final FifoDebitAllocationRepository allocationRepository;
    
    // Constant for default program for this POC
    private static final String DEFAULT_PROGRAM = "DEFAULT_PROG";

    /**
     * Ghi sổ một giao dịch Earn (Base) vào Sổ cái.
     */
    @Transactional
    public void recordBaseEarn(String memberId, int points, String sourceTxnId) {
        PointTransaction txn = PointTransaction.builder()
                .memberId(memberId)
                .programId(DEFAULT_PROGRAM)
                .type(TransactionType.EARN)
                .amount(points)
                .remainingBalance(points)
                .status(TransactionStatus.CONFIRMED)
                .sourceTxnId(sourceTxnId)
                .expiryDate(LocalDateTime.now().plusYears(1))
                .build();
        transactionRepository.save(txn);
        updateBalance(memberId, DEFAULT_PROGRAM, points);
        log.info("[Earning Ledger] Recorded EARN entry: {} points for sourceTxnId {}", points, sourceTxnId);
    }

    /**
     * Ghi sổ một giao dịch Bonus vào Sổ cái.
     */
    @Transactional
    public void recordBonusEarn(String memberId, int points, String sourceTxnId, String campaignId) {
        PointTransaction txn = PointTransaction.builder()
                .memberId(memberId)
                .programId(DEFAULT_PROGRAM)
                .type(TransactionType.BONUS)
                .amount(points)
                .remainingBalance(points)
                .status(TransactionStatus.CONFIRMED)
                .sourceTxnId(sourceTxnId)
                .campaignId(campaignId)
                .expiryDate(LocalDateTime.now().plusYears(1))
                .build();
        transactionRepository.save(txn);
        updateBalance(memberId, DEFAULT_PROGRAM, points);
        log.info("[Earning Ledger] Recorded BONUS entry: {} points for sourceTxnId {}, campaign {}", points, sourceTxnId, campaignId);
    }
    
    /**
     * CT-13: DebitPointsFifo — Thao tác trừ điểm theo thuật toán FIFO từ các lô điểm cũ nhất.
     * I-7 Owner: Earning Engine Service / Earning DB.
     */
    @Transactional
    public FifoDebitResponse debitPointsFifo(String memberId, String programId, long pointsRequired, String orderId) {
        String prog = (programId != null && !programId.isEmpty()) ? programId : DEFAULT_PROGRAM;
        log.info("[CT-13 DebitPointsFifo] Debiting {} points for member {} (order: {})", pointsRequired, memberId, orderId);

        if (pointsRequired <= 0 || orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("ERR_RED_INVALID_DEBIT: positive points and orderId are required");
        }
        if (allocationRepository != null) {
            List<FifoDebitAllocation> existing = allocationRepository.findByOrderIdOrderByCreatedAtAsc(orderId);
            if (!existing.isEmpty()) {
                return debitResponse(orderId, memberId, existing.stream().mapToLong(a -> a.getPointsDebited()).sum(), existing.size(), existing.get(0).getId().toString());
            }
        }

        // 1. Validate balance from PointBalance (I-7 Source of Truth)
        PointBalance balance = balanceRepository.findByMemberIdAndProgramIdForUpdate(memberId, prog)
                .orElseThrow(() -> new IllegalStateException(
                        "ERR_RED_INSUFFICIENT_BALANCE: Member " + memberId + " has no balance record in Earning DB"));

        if (balance.getConfirmedBalance() < pointsRequired) {
            throw new IllegalStateException(
                    "ERR_RED_INSUFFICIENT_BALANCE: Required " + pointsRequired + " but available balance in Earning DB is " + balance.getConfirmedBalance());
        }

        // 2. Query unexpired earn batches ordered by createdAt ASC (FIFO)
        List<PointTransaction> unexpiredBatches = transactionRepository.findUnexpiredBatchesForFifoDebit(
                memberId, prog, TransactionStatus.CONFIRMED, LocalDateTime.now());

        long remainingToDebit = pointsRequired;
        int debitedBatchesCount = 0;

        for (PointTransaction batch : unexpiredBatches) {
            if (remainingToDebit <= 0) {
                break;
            }
            int batchRemaining = batch.getRemainingBalance() != null ? batch.getRemainingBalance() : 0;
            if (batchRemaining <= 0) {
                continue;
            }

            int deduct = (int) Math.min(batchRemaining, remainingToDebit);
            batch.setRemainingBalance(batchRemaining - deduct);
            transactionRepository.save(batch);
            if (allocationRepository != null) {
                allocationRepository.save(FifoDebitAllocation.builder()
                        .orderId(orderId).allocationKey(orderId + ":" + batch.getId()).memberId(memberId).programId(prog)
                        .batchId(batch.getId()).pointsDebited(deduct).restored(false).build());
            }
            remainingToDebit -= deduct;
            debitedBatchesCount++;
            log.info("[CT-13 FIFO] Deducted {} points from batch {} (original earn_date: {})",
                    deduct, batch.getId(), batch.getCreatedAt());
        }

        if (remainingToDebit > 0) {
            throw new IllegalStateException(
                    "ERR_RED_INSUFFICIENT_UNEXPIRED_POINTS: Unexpired point batches insufficient to cover " + pointsRequired);
        }

        // 3. Append REDEEM entry to PointTransaction ledger
        PointTransaction redeemTxn = PointTransaction.builder()
                .memberId(memberId)
                .programId(prog)
                .type(TransactionType.REDEEM)
                .amount((int) pointsRequired)
                .remainingBalance(0)
                .status(TransactionStatus.CONFIRMED_DEBIT)
                .sourceTxnId(orderId)
                .build();
        transactionRepository.save(redeemTxn);

        // 4. Update snapshot balance
        balance.setConfirmedBalance(balance.getConfirmedBalance() - pointsRequired);
        balanceRepository.save(balance);

        log.info("[CT-13 FIFO Debit] Successfully debited {} points from {} batches for order {}",
                pointsRequired, debitedBatchesCount, orderId);

        String allocationId = allocationRepository == null ? null : allocationRepository.findByOrderIdOrderByCreatedAtAsc(orderId).stream()
                .findFirst().map(a -> a.getId().toString()).orElse(null);
        return debitResponse(orderId, memberId, pointsRequired, debitedBatchesCount, allocationId);
    }

    /**
     * CT-13: RestorePoints — Hoàn trả điểm FIFO khi fulfillment thất bại (CON.3 / EXC-05).
     * Phục hồi remainingBalance của các lô earn gốc, giữ nguyên earn_date và expiry_date.
     * I-7 Owner: Earning Engine Service / Earning DB.
     */
    @Transactional
    public FifoRestoreResponse restorePoints(String memberId, String programId, String orderId, long pointsToRestore, String reason) {
        String prog = (programId != null && !programId.isEmpty()) ? programId : DEFAULT_PROGRAM;
        log.info("[CT-13 RestorePoints] Restoring {} points for member {} (order: {}) under CON.3 reason: {}",
                pointsToRestore, memberId, orderId, reason);

        if (pointsToRestore <= 0 || orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("ERR_RED_INVALID_RESTORE: positive points and orderId are required");
        }
        if (allocationRepository != null) {
            List<FifoDebitAllocation> allocations = allocationRepository.findByOrderIdOrderByCreatedAtAsc(orderId);
            if (allocations.isEmpty()) throw new IllegalStateException("ERR_RED_UNKNOWN_ALLOCATION: " + orderId);
            if (allocations.stream().allMatch(FifoDebitAllocation::isRestored)) {
                return FifoRestoreResponse.builder().orderId(orderId).memberId(memberId)
                        .pointsRestored(pointsToRestore).status("RESTORED").reason(reason)
                        .allocationId(allocations.get(0).getId().toString())
                        .message("Points restore already applied").build();
            }
            long allocated = allocations.stream().mapToLong(FifoDebitAllocation::getPointsDebited).sum();
            if (allocated != pointsToRestore) throw new IllegalStateException("ERR_RED_RESTORE_AMOUNT_MISMATCH: allocation is " + allocated);
            for (FifoDebitAllocation allocation : allocations) {
                PointTransaction batch = transactionRepository.findById(allocation.getBatchId())
                        .orElseThrow(() -> new IllegalStateException("ERR_RED_UNKNOWN_ALLOCATION_BATCH: " + allocation.getBatchId()));
                batch.setRemainingBalance(batch.getRemainingBalance() + allocation.getPointsDebited());
                transactionRepository.save(batch);
                allocation.setRestored(true);
                allocationRepository.save(allocation);
            }
            return completeRestore(memberId, prog, orderId, pointsToRestore, reason, allocations.get(0).getId().toString());
        }

        // Legacy fallback is retained only for unit-test collaborators without allocation persistence.
        // 1. Query unexpired earn batches to restore remaining_balance
        // (In FIFO reversal, points are restored back to the oldest unexpired batches)
        List<PointTransaction> earnBatches = transactionRepository.findUnexpiredBatchesForFifoDebit(
                memberId, prog, TransactionStatus.CONFIRMED, LocalDateTime.now());

        long remainingToRestore = pointsToRestore;

        // Restore to earn batches where remainingBalance < amount
        for (PointTransaction batch : earnBatches) {
            if (remainingToRestore <= 0) break;
            int maxCanRestore = batch.getAmount() - (batch.getRemainingBalance() != null ? batch.getRemainingBalance() : 0);
            if (maxCanRestore > 0) {
                int restoredToBatch = (int) Math.min(maxCanRestore, remainingToRestore);
                batch.setRemainingBalance(batch.getRemainingBalance() + restoredToBatch);
                transactionRepository.save(batch);
                remainingToRestore -= restoredToBatch;
                log.info("[CT-13 Restore] Restored {} points to batch {} (earn_date: {} unchanged)",
                        restoredToBatch, batch.getId(), batch.getCreatedAt());
            }
        }

        // If there's still points to restore (e.g. batches were fully depleted), restore to the newest batch
        if (remainingToRestore > 0 && !earnBatches.isEmpty()) {
            PointTransaction lastBatch = earnBatches.get(earnBatches.size() - 1);
            lastBatch.setRemainingBalance(lastBatch.getRemainingBalance() + (int) remainingToRestore);
            transactionRepository.save(lastBatch);
        }

        // 2. Append REVERSAL entry to PointTransaction ledger for audit
        PointTransaction reversalTxn = PointTransaction.builder()
                .memberId(memberId)
                .type(TransactionType.REVERSAL)
                .amount((int) pointsToRestore)
                .remainingBalance((int) pointsToRestore)
                .status(TransactionStatus.CONFIRMED)
                .sourceTxnId(orderId)
                .expiryDate(LocalDateTime.now().plusYears(1))
                .build();
        transactionRepository.save(reversalTxn);

        // 3. Update PointBalance snapshot
        PointBalance balance = balanceRepository.findByMemberIdAndProgramIdForUpdate(memberId, prog)
                .orElseGet(() -> PointBalance.builder()
                        .memberId(memberId)
                        .programId(prog)
                        .confirmedBalance(0L)
                        .pendingBalance(0L)
                        .build());
        balance.setConfirmedBalance(balance.getConfirmedBalance() + pointsToRestore);
        balanceRepository.save(balance);

        log.info("[CT-13 RestorePoints] Reversal completed under CON.3 for order {}. New balance = {}",
                orderId, balance.getConfirmedBalance());

        return FifoRestoreResponse.builder()
                .orderId(orderId)
                .memberId(memberId)
                .pointsRestored(pointsToRestore)
                .status("RESTORED")
                .reason(reason)
                .message("Points restored successfully with original FIFO earn date preserved under CON.3")
                .build();
    }

    private FifoDebitResponse debitResponse(String orderId, String memberId, long points, int count, String allocationId) {
        return FifoDebitResponse.builder().orderId(orderId).memberId(memberId).pointsDebited(points)
                .status("RESERVED").debitedBatchesCount(count).allocationId(allocationId)
                .message("FIFO debit reserved successfully in Earning DB").build();
    }

    private FifoRestoreResponse completeRestore(String memberId, String programId, String orderId, long points, String reason, String allocationId) {
        PointBalance balance = balanceRepository.findByMemberIdAndProgramIdForUpdate(memberId, programId)
                .orElseThrow(() -> new IllegalStateException("ERR_RED_INSUFFICIENT_BALANCE: balance record missing"));
        balance.setConfirmedBalance(balance.getConfirmedBalance() + points);
        balanceRepository.save(balance);
        transactionRepository.save(PointTransaction.builder().memberId(memberId).programId(programId)
                .type(TransactionType.REVERSAL).amount((int) points).remainingBalance((int) points)
                .status(TransactionStatus.CONFIRMED).sourceTxnId(orderId).build());
        return FifoRestoreResponse.builder().orderId(orderId).memberId(memberId).pointsRestored(points)
                .status("RESTORED").reason(reason).allocationId(allocationId)
                .message("Points restored successfully with original FIFO allocation").build();
    }

    /**
     * Đọc số dư hiện tại của member từ Earning DB (I-7 Source of Truth).
     */
    public Long getMemberBalance(String memberId, String programId) {
        String prog = (programId != null && !programId.isEmpty()) ? programId : DEFAULT_PROGRAM;
        return balanceRepository.findByMemberIdAndProgramId(memberId, prog)
                .map(PointBalance::getConfirmedBalance)
                .orElse(0L);
    }

    /**
     * Hủy giao dịch tích điểm khi Core Banking reverse (EXC-02, CON.1).
     */
    @Transactional
    public boolean cancelTransaction(String sourceTxnId) {
        return transactionRepository.findBySourceTxnId(sourceTxnId)
                .map(txn -> {
                    if (txn.getStatus() == TransactionStatus.CONFIRMED) {
                        txn.setStatus(TransactionStatus.CANCELLED);
                        transactionRepository.save(txn);
                        // Trừ lại số điểm đã cộng
                        balanceRepository.findByMemberIdAndProgramIdForUpdate(txn.getMemberId(), DEFAULT_PROGRAM)
                                .ifPresent(bal -> {
                                    bal.setConfirmedBalance(Math.max(0L, bal.getConfirmedBalance() - txn.getAmount()));
                                    balanceRepository.save(bal);
                                });
                        log.info("[Earning Ledger] Reversal completed for sourceTxnId: {}", sourceTxnId);
                        return true;
                    }
                    return false;
                }).orElse(false);
    }

    private void updateBalance(String memberId, String programId, int points) {
        PointBalance balance = balanceRepository.findByMemberIdAndProgramIdForUpdate(memberId, programId)
                .orElseGet(() -> PointBalance.builder()
                        .memberId(memberId)
                        .programId(programId)
                        .confirmedBalance(0L)
                        .pendingBalance(0L)
                        .build());
        
        balance.setConfirmedBalance(balance.getConfirmedBalance() + points);
        balanceRepository.save(balance);
        log.info("[Point Balance] Updated balance for member {}: new balance = {}", memberId, balance.getConfirmedBalance());
    }
}
