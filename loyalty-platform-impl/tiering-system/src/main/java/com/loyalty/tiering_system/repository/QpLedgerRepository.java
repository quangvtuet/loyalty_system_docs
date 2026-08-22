package com.loyalty.tiering_system.repository;

import com.loyalty.tiering_system.domain.QpLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.UUID;

@Repository
public interface QpLedgerRepository extends JpaRepository<QpLedger, UUID> {

    /**
     * Tính tổng QP tích lũy của member trong một period.
     * Dùng cho: real-time upgrade check & batch evaluation.
     */
    @Query("SELECT COALESCE(SUM(q.qpAmount), 0) FROM QpLedger q " +
           "WHERE q.memberId = :memberId AND q.programId = :programId " +
           "AND q.tierPeriodStart = :periodStart AND q.tierPeriodEnd = :periodEnd")
    Long sumQpByMemberAndPeriod(
            @Param("memberId") String memberId,
            @Param("programId") String programId,
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd
    );

    /**
     * CON.1 — Check whether a QP accrual event has already been recorded.
     * Used by QpLedgerService to prevent duplicate ledger rows for replayed Kafka events (EXC-03).
     * The natural idempotency key is (memberId, programId, sourceEventId).
     */
    boolean existsByMemberIdAndProgramIdAndSourceEventId(
            String memberId,
            String programId,
            String sourceEventId
    );
}

