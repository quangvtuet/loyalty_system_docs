package com.loyalty.redemption_engine.service;

import com.loyalty.redemption_engine.client.TieringClient;
import com.loyalty.redemption_engine.domain.FulfillmentType;
import com.loyalty.redemption_engine.domain.RewardItem;
import com.loyalty.redemption_engine.repository.RedemptionOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * I-5 Hard Rule & Invariant Test: Client Balance / Tier Tamper Resistance.
 *
 * Rule: Order Intake must never trust client-supplied balance or tier numbers.
 * The system MUST query Tiering System Service (CT-12) for MemberTier and
 * delegate debit/balance check to Earning Engine Service (CT-13).
 *
 * Spec-trace: I-5, CON.2, T3
 */
class I5BalanceTamperTest {

    @Mock
    private CatalogService catalogService;

    @Mock
    private TieringClient tieringClient;

    @Mock
    private BalanceLockService balanceLockService;

    @Mock
    private FifoDebitService fifoDebitService;

    @Mock
    private RedemptionOrderRepository orderRepository;

    @InjectMocks
    private RedemptionService redemptionService;

    private UUID itemId;
    private RewardItem goldVoucher;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        itemId = UUID.randomUUID();
        goldVoucher = RewardItem.builder()
                .itemId(itemId)
                .programId("DEFAULT_PROG")
                .name("Gold Lounge Pass")
                .pointsCost(1000L)
                .currencyValue(BigDecimal.valueOf(10.00))
                .fulfillmentType(FulfillmentType.DIGITAL)
                .minTierRequired("GOLD")
                .status("ACTIVE")
                .build();
    }

    /**
     * T3 Hard rule test:
     * A malicious client attempts to claim they are GOLD with 1,000,000 balance.
     * The system queries CT-12 (which returns SILVER) → order is refused with ERR_RED_TIER_ELIGIBILITY_FAILED.
     */
    @Test
    void testClientForgedTier_RejectedByServerQueryingCT12() {
        when(catalogService.getActiveItemOrThrow(itemId)).thenReturn(goldVoucher);
        // CT-12 returns the real tier from Tiering DB: SILVER
        when(tieringClient.getMemberTier("attacker-001", "DEFAULT_PROG")).thenReturn("SILVER");
        doThrow(new IllegalStateException("ERR_RED_TIER_ELIGIBILITY_FAILED: SILVER < GOLD"))
                .when(catalogService).validateTierEligibility(goldVoucher, "SILVER");

        assertThrows(IllegalStateException.class, () ->
                redemptionService.placeOrder("attacker-001", "DEFAULT_PROG", itemId, 1));

        // Assert: CT-12 was consulted
        verify(tieringClient).getMemberTier("attacker-001", "DEFAULT_PROG");
        // Assert: No debit requested
        verify(fifoDebitService, never()).debitFifo(any(), any(), anyLong(), any());
    }

    /**
     * T3 Hard rule test:
     * A malicious client attempts to place an order while lacking balance in Earning DB.
     * CT-13 delegates to Earning Engine Service which detects insufficient balance and throws → order rejected.
     */
    @Test
    void testClientForgedBalance_RejectedByServerQueryingCT13() {
        when(catalogService.getActiveItemOrThrow(itemId)).thenReturn(goldVoucher);
        when(tieringClient.getMemberTier("attacker-001", "DEFAULT_PROG")).thenReturn("GOLD");
        doNothing().when(catalogService).validateTierEligibility(goldVoucher, "GOLD");
        when(balanceLockService.tryLock(any(), any())).thenReturn(true);

        // Earning DB has only 50 points, item costs 1000
        doThrow(new IllegalStateException("ERR_RED_INSUFFICIENT_BALANCE: Required 1000 but available in Earning DB is 50"))
                .when(fifoDebitService).debitFifo(eq("attacker-001"), eq("DEFAULT_PROG"), eq(1000L), anyString());

        assertThrows(IllegalStateException.class, () ->
                redemptionService.placeOrder("attacker-001", "DEFAULT_PROG", itemId, 1));

        verify(fifoDebitService).debitFifo(eq("attacker-001"), eq("DEFAULT_PROG"), eq(1000L), anyString());
        verify(balanceLockService).releaseLock("attacker-001");
    }
}
