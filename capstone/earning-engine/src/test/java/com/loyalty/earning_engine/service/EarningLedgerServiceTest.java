package com.loyalty.earning_engine.service;

import com.loyalty.earning_engine.domain.PointBalance;
import com.loyalty.earning_engine.domain.PointTransaction;
import com.loyalty.earning_engine.store.OwnedEarningStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class EarningLedgerServiceTest {

    @Mock
    private OwnedEarningStore earningStore;

    @InjectMocks
    private EarningLedgerService earningLedgerService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testRecordBaseEarn_NewBalance() {
        String memberId = "user-123";
        String programId = "DEFAULT_PROG";
        
        // Mock that balance does not exist yet
        when(earningStore.findBalanceForUpdate(memberId, programId))
                .thenReturn(Optional.empty());

        earningLedgerService.recordBaseEarn(memberId, 100, "txn-001");

        // Verify transaction is saved through OwnedEarningStore with AUTHORIZED_OWNER
        ArgumentCaptor<PointTransaction> txnCaptor = ArgumentCaptor.forClass(PointTransaction.class);
        verify(earningStore, times(1)).appendTransaction(eq(OwnedEarningStore.AUTHORIZED_OWNER), txnCaptor.capture());
        assertEquals(100, txnCaptor.getValue().getAmount());
        assertEquals("EARN", txnCaptor.getValue().getType().name());

        // Verify balance is saved through OwnedEarningStore with AUTHORIZED_OWNER
        ArgumentCaptor<PointBalance> balanceCaptor = ArgumentCaptor.forClass(PointBalance.class);
        verify(earningStore, times(1)).updateBalance(eq(OwnedEarningStore.AUTHORIZED_OWNER), balanceCaptor.capture());
        
        PointBalance savedBalance = balanceCaptor.getValue();
        assertEquals(memberId, savedBalance.getMemberId());
        assertEquals(programId, savedBalance.getProgramId());
        assertEquals(100L, savedBalance.getConfirmedBalance());
    }

    @Test
    void testRecordBonusEarn_ExistingBalance() {
        String memberId = "user-456";
        String programId = "DEFAULT_PROG";
        
        // Mock that balance already exists with 50 points
        PointBalance existingBalance = PointBalance.builder()
                .memberId(memberId)
                .programId(programId)
                .confirmedBalance(50L)
                .pendingBalance(0L)
                .build();
                
        when(earningStore.findBalanceForUpdate(memberId, programId))
                .thenReturn(Optional.of(existingBalance));

        earningLedgerService.recordBonusEarn(memberId, 200, "txn-002", "SUMMER_BONUS");

        // Verify transaction is saved through OwnedEarningStore with AUTHORIZED_OWNER
        ArgumentCaptor<PointTransaction> txnCaptor = ArgumentCaptor.forClass(PointTransaction.class);
        verify(earningStore, times(1)).appendTransaction(eq(OwnedEarningStore.AUTHORIZED_OWNER), txnCaptor.capture());
        assertEquals(200, txnCaptor.getValue().getAmount());
        assertEquals("BONUS", txnCaptor.getValue().getType().name());
        assertEquals("SUMMER_BONUS", txnCaptor.getValue().getCampaignId());

        // Verify balance is saved through OwnedEarningStore with AUTHORIZED_OWNER
        ArgumentCaptor<PointBalance> balanceCaptor = ArgumentCaptor.forClass(PointBalance.class);
        verify(earningStore, times(1)).updateBalance(eq(OwnedEarningStore.AUTHORIZED_OWNER), balanceCaptor.capture());
        
        PointBalance savedBalance = balanceCaptor.getValue();
        assertEquals(memberId, savedBalance.getMemberId());
        assertEquals(250L, savedBalance.getConfirmedBalance()); // 50 + 200
    }
}
