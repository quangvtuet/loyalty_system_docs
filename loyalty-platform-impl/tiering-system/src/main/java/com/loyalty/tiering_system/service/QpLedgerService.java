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
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QpLedgerService {

    private final QpLedgerRepository qpLedgerRepository;

    /**
     * Ghi một dòng QP accrual vào sổ cái và trả về tổng QP tích lũy trong period.
     */
    @Transactional
    public long recordQpAndGetCumulative(String memberId, String programId, String sourceEventId, long qpAmount) {
        LocalDate periodStart = LocalDate.of(LocalDate.now().getYear(), 1, 1);
        LocalDate periodEnd = LocalDate.of(LocalDate.now().getYear(), 12, 31);

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
