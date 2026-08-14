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

        when(qpLedgerRepository.save(any())).thenReturn(new QpLedger());
        when(qpLedgerRepository.sumQpByMemberAndPeriod(any(), any(), eq(periodStart), eq(periodEnd)))
                .thenReturn(500L);

        long result = qpLedgerService.recordQpAndGetCumulative("new-member", "DEFAULT_PROG", "event-new", 500L);

        assertEquals(500L, result);
    }
}
