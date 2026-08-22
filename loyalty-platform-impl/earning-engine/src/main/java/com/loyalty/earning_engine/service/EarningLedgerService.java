package com.loyalty.earning_engine.service;

import com.loyalty.earning_engine.domain.PointBalance;
import com.loyalty.earning_engine.domain.PointTransaction;
import com.loyalty.earning_engine.domain.TransactionStatus;
import com.loyalty.earning_engine.domain.TransactionType;
import com.loyalty.earning_engine.repository.PointBalanceRepository;
import com.loyalty.earning_engine.repository.PointTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Earning Ledger Service (Sổ cái).
 * Chuyên trách việc ghi nhận (append-only) các giao dịch điểm vào Sổ cái.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EarningLedgerService {

    private final PointTransactionRepository transactionRepository;
    private final PointBalanceRepository balanceRepository;
    
    // Constant for default program for this POC
    private static final String DEFAULT_PROGRAM = "DEFAULT_PROG";

    /**
     * Ghi sổ một giao dịch Earn (Base) vào Sổ cái.
     */
    @Transactional
    public void recordBaseEarn(String memberId, int points, String sourceTxnId) {
        PointTransaction txn = PointTransaction.builder()
                .memberId(memberId)
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
}
