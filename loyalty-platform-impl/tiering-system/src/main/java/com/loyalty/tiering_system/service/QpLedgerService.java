package com.loyalty.tiering_system.service;

import com.loyalty.tiering_system.domain.QpLedger;
import com.loyalty.tiering_system.repository.QpLedgerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * QP Ledger Service — ghi nhận (append-only) mỗi QP accrual event.
 * FR-02-004: Record every QP accrual in qp_ledger.
 *
 * CON.1 / EXC-03: If (memberId, programId, sourceEventId) already exists in the ledger,
 * the write is skipped and the current cumulative total is returned unchanged.
 * This prevents duplicate QP rows from Kafka message re-delivery.
 * Spec-trace: G6-A04 — tested by TierUpgradeIdempotencyTest.testEvaluateAndUpgrade_ReplayedEvent_TierNotMovedTwice
 *             and QpLedgerServiceTest.testRecordQpAndGetCumulative_DuplicateEvent_SkipsWrite.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QpLedgerService {

    private final QpLedgerRepository qpLedgerRepository;

    /**
     * Ghi một dòng QP accrual vào sổ cái và trả về tổng QP tích lũy trong period.
     *
     * CON.1 idempotency: if sourceEventId already recorded for this member+program,
     * skip the write and return the current cumulative (no duplicate row).
     */
    @Transactional
    public long recordQpAndGetCumulative(String memberId, String programId, String sourceEventId, long qpAmount) {
        LocalDate periodStart = LocalDate.of(LocalDate.now().getYear(), 1, 1);
        LocalDate periodEnd = LocalDate.of(LocalDate.now().getYear(), 12, 31);

        // CON.1 — idempotency check at ledger level (EXC-03)
        boolean alreadyRecorded = qpLedgerRepository.existsByMemberIdAndProgramIdAndSourceEventId(
                memberId, programId, sourceEventId);
        if (alreadyRecorded) {
            log.warn("[CON.1 / EXC-03] Duplicate QP event skipped for member {} (sourceEventId: {}). " +
                     "No new ledger row written.", memberId, sourceEventId);
            // Return current cumulative — no change
            return qpLedgerRepository.sumQpByMemberAndPeriod(memberId, programId, periodStart, periodEnd);
        }

        QpLedger ledger = QpLedger.builder()
                .memberId(memberId)
                .programId(programId)
                .sourceEventId(sourceEventId)
                .qpAmount(qpAmount)
                .tierPeriodStart(periodStart)
                .tierPeriodEnd(periodEnd)
                .build();

        qpLedgerRepository.save(ledger);
        log.info("[QP Ledger] Recorded {} QP for member {} (sourceEventId: {})", qpAmount, memberId, sourceEventId);

        long cumulative = qpLedgerRepository.sumQpByMemberAndPeriod(memberId, programId, periodStart, periodEnd);
        log.info("[QP Ledger] Cumulative QP for member {} in period {}: {} QP", memberId, periodStart.getYear(), cumulative);

        return cumulative;
    }
}
