package com.loyalty.earning_engine.repository;

import com.loyalty.earning_engine.domain.PointTransaction;
import com.loyalty.earning_engine.domain.TransactionStatus;
import com.loyalty.earning_engine.domain.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PointTransactionRepository extends JpaRepository<PointTransaction, UUID> {

    Optional<PointTransaction> findByIdempotencyKey(String idempotencyKey);

    Optional<PointTransaction> findBySourceTxnId(String sourceTxnId);

    List<PointTransaction> findBySourceTxnIdAndType(String sourceTxnId, TransactionType type);

    @Query("SELECT pt FROM PointTransaction pt WHERE pt.memberId = :memberId " +
           "AND (pt.programId = :programId OR pt.programId IS NULL) " +
           "AND pt.remainingBalance > 0 " +
           "AND pt.status = :status " +
           "AND (pt.expiryDate IS NULL OR pt.expiryDate > :now) " +
           "ORDER BY pt.createdAt ASC, pt.id ASC")
    List<PointTransaction> findUnexpiredBatchesForFifoDebit(
            @Param("memberId") String memberId,
            @Param("programId") String programId,
            @Param("status") TransactionStatus status,
            @Param("now") LocalDateTime now
    );

    /**
     * Query unexpired earn batches with remaining points in FIFO order (oldest createdAt first).
     * Used by EarningLedgerService for CT-13 DebitPointsFifo.
     */
    @Query("SELECT pt FROM PointTransaction pt WHERE pt.memberId = :memberId " +
           "AND pt.remainingBalance > 0 " +
           "AND pt.status = :status " +
           "AND (pt.expiryDate IS NULL OR pt.expiryDate > :now) " +
           "ORDER BY pt.createdAt ASC, pt.id ASC")
    List<PointTransaction> findUnexpiredBatchesForFifoDebit(
            @Param("memberId") String memberId,
            @Param("status") TransactionStatus status,
            @Param("now") LocalDateTime now
    );

    @Query("SELECT pt FROM PointTransaction pt WHERE pt.memberId = :memberId " +
           "AND pt.remainingBalance > 0 AND pt.status = :status " +
           "AND (pt.expiryDate IS NULL OR pt.expiryDate > :now) " +
           "ORDER BY pt.createdAt ASC, pt.id ASC")
    List<PointTransaction> findUnexpiredBatchesForFifoDebit(
            @Param("memberId") String memberId,
            @Param("status") TransactionStatus status,
            @Param("now") LocalDateTime now
    );
}
