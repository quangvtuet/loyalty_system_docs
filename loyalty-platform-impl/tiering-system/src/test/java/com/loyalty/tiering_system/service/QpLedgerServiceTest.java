package com.loyalty.tiering_system.service;

import com.loyalty.tiering_system.domain.QpLedger;
import com.loyalty.tiering_system.repository.QpLedgerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for QpLedgerService — including CON.1 ledger-level idempotency.
 *
 * Spec-trace:
 * - testRecordQpAndGetCumulative_SavesLedgerAndReturnsCumulative → UC-LB-03 happy path, I-11
 * - testRecordQpAndGetCumulative_NewMember_ReturnsSingleQpAmount → I-11 boundary
 * - testRecordQpAndGetCumulative_DuplicateEvent_SkipsWrite       → CON.1 / EXC-03 / G6-A04 (UPGRADED from tier-only to ledger-level)
 */
class QpLedgerServiceTest {

    @Mock
    private QpLedgerRepository qpLedgerRepository;

    @InjectMocks
    private QpLedgerService qpLedgerService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testRecordQpAndGetCumulative_SavesLedgerAndReturnsCumulative() {
        LocalDate periodStart = LocalDate.of(LocalDate.now().getYear(), 1, 1);
        LocalDate periodEnd = LocalDate.of(LocalDate.now().getYear(), 12, 31);

        when(qpLedgerRepository.existsByMemberIdAndProgramIdAndSourceEventId(
                "member-001", "DEFAULT_PROG", "event-001")).thenReturn(false);
        when(qpLedgerRepository.save(any())).thenReturn(new QpLedger());
        when(qpLedgerRepository.sumQpByMemberAndPeriod("member-001", "DEFAULT_PROG", periodStart, periodEnd))
                .thenReturn(1100L);

        long result = qpLedgerService.recordQpAndGetCumulative("member-001", "DEFAULT_PROG", "event-001", 200L);

        // Verify ledger entry saved
        ArgumentCaptor<QpLedger> captor = ArgumentCaptor.forClass(QpLedger.class);
        verify(qpLedgerRepository).save(captor.capture());
        assertEquals("member-001", captor.getValue().getMemberId());
        assertEquals("DEFAULT_PROG", captor.getValue().getProgramId());
        assertEquals("event-001", captor.getValue().getSourceEventId());
        assertEquals(200L, captor.getValue().getQpAmount());
        assertEquals(periodStart, captor.getValue().getTierPeriodStart());
        assertEquals(periodEnd, captor.getValue().getTierPeriodEnd());

        // Verify cumulative returned correctly
        assertEquals(1100L, result);
    }

    @Test
    void testRecordQpAndGetCumulative_NewMember_ReturnsSingleQpAmount() {
        LocalDate periodStart = LocalDate.of(LocalDate.now().getYear(), 1, 1);
        LocalDate periodEnd = LocalDate.of(LocalDate.now().getYear(), 12, 31);

        when(qpLedgerRepository.existsByMemberIdAndProgramIdAndSourceEventId(
                "new-member", "DEFAULT_PROG", "event-new")).thenReturn(false);
        when(qpLedgerRepository.save(any())).thenReturn(new QpLedger());
        when(qpLedgerRepository.sumQpByMemberAndPeriod(any(), any(), eq(periodStart), eq(periodEnd)))
                .thenReturn(500L);

        long result = qpLedgerService.recordQpAndGetCumulative("new-member", "DEFAULT_PROG", "event-new", 500L);

        assertEquals(500L, result);
    }

    /**
     * CON.1 / EXC-03 / G6-A04 — Duplicate QP event at the LEDGER level.
     *
     * When the same (memberId, programId, sourceEventId) arrives again (Kafka re-delivery),
     * QpLedgerService must NOT write a second ledger row.
     * The existing cumulative total is returned unchanged.
     *
     * This closes the earlier partial gap where CON.1 was only enforced at the tier-ordinal level
     * in TierUpgradeService but not at the QP ledger level, meaning a replay could create a
     * ghost QP row that inflated the cumulative total.
     *
     * Spec-trace: CON.1, EXC-03, G6-A04, I-5
     */
    @Test
    void testRecordQpAndGetCumulative_DuplicateEvent_SkipsWrite() {
        LocalDate periodStart = LocalDate.of(LocalDate.now().getYear(), 1, 1);
        LocalDate periodEnd = LocalDate.of(LocalDate.now().getYear(), 12, 31);

        // Arrange: sourceEventId "event-dup-001" already exists in the ledger
        when(qpLedgerRepository.existsByMemberIdAndProgramIdAndSourceEventId(
                "member-001", "DEFAULT_PROG", "event-dup-001")).thenReturn(true);
        // Current cumulative before replay: 1000 QP
        when(qpLedgerRepository.sumQpByMemberAndPeriod(
                "member-001", "DEFAULT_PROG", periodStart, periodEnd)).thenReturn(1000L);

        // Act: same event delivered again
        long result = qpLedgerService.recordQpAndGetCumulative(
                "member-001", "DEFAULT_PROG", "event-dup-001", 200L);

        // Assert: no new ledger row written (CON.1 / EXC-03)
        verify(qpLedgerRepository, never()).save(any());

        // Assert: current cumulative returned unchanged (1000, not 1200)
        assertEquals(1000L, result,
                "CON.1 / EXC-03: replayed QP event must not inflate cumulative total");
    }
}
