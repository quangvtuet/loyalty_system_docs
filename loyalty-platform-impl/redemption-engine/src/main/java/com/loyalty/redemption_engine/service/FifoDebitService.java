package com.loyalty.redemption_engine.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * FIFO Debit Service — thực thi thuật toán tiêu điểm FIFO.
 *
 * Trong production, service này sẽ gọi trực tiếp tới earning-engine DB
 * (hoặc qua shared DB schema) để SELECT point_transaction FOR UPDATE theo earn_date ASC.
 * Trong POC này, chúng ta simulate logic để đảm bảo unit test kiểm soát được.
 *
 * FR-03-020: Debit points immediately on approval (PENDING_DEBIT)
 * FR-03-021: FIFO ordering — oldest earn_date first
 * FR-03-022: Exclude expired points
 * FR-03-041: Reversal restores original FIFO position (earn_date + expiry_date preserved)
 */
@Service
@Slf4j
public class FifoDebitService {

    /**
     * Allocate FIFO debit: đánh dấu điểm là PENDING_DEBIT theo thứ tự earn_date ASC.
     * Trong production: SELECT ... FOR UPDATE → loop batches → UPDATE remaining_balance.
     */
    public void debitFifo(String memberId, String programId, long pointsRequired, String orderId) {
        log.info("[FIFO Debit] Allocating {} pts for member {} (order: {}) — FIFO from oldest earn_date",
                pointsRequired, memberId, orderId);
        // POC: logic thực tế sẽ query point_transaction table theo earn_date ASC
        // và cập nhật remaining_balance từng batch đến khi đủ pointsRequired
    }

    /**
     * Confirm debit sau khi fulfillment thành công: PENDING_DEBIT → CONFIRMED_DEBIT.
     * FR-03-032.
     */
    public void confirmDebit(String memberId, String programId, String orderId) {
        log.info("[FIFO Debit] Confirming debit for order {} (member: {})", orderId, memberId);
        // POC: UPDATE point_transaction SET status='CONFIRMED_DEBIT' WHERE source_txn_id = orderId
    }

    /**
     * Reversal: hoàn trả remaining_balance về giá trị cũ — giữ nguyên earn_date và expiry_date.
     * Append một dòng REVERSAL vào ledger để audit.
     * FR-03-041.
     */
    public void reverseDebit(String memberId, String programId, String orderId, long pointsToRestore) {
        log.info("[FIFO Reversal] Restoring {} pts for member {} (order: {}) — FIFO position preserved",
                pointsToRestore, memberId, orderId);
        // POC: UPDATE point_transaction SET remaining_balance += debit_amount WHERE source_txn_id = orderId
        // + INSERT point_transaction (type='REVERSAL', amount=pointsToRestore)
    }
}
