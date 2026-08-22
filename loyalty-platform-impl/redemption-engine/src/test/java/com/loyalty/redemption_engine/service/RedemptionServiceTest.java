package com.loyalty.redemption_engine.service;

import com.loyalty.redemption_engine.client.TieringClient;
import com.loyalty.redemption_engine.domain.FulfillmentType;
import com.loyalty.redemption_engine.domain.OrderStatus;
import com.loyalty.redemption_engine.domain.RedemptionOrder;
import com.loyalty.redemption_engine.domain.RewardItem;
import com.loyalty.redemption_engine.repository.RedemptionOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RedemptionService — UC-LB-02 Redeem reward with FIFO.
 *
 * Spec-trace:
 * - G6-T01: PENDING → IN_PROGRESS (debit reserved via CT-13)
 * - G6-T02: PENDING → CANCELLED (cancellation while pending)
 * - G6-T03: IN_PROGRESS → FULFILLED (partner confirmed delivery)
 * - G6-T04: IN_PROGRESS → FAILED (partner delivery failure)
 * - G6-T05: FAILED → REVERSED (CON.3 auto-reversal via CT-13)
 * - G6-A02: ALT-01 insufficient balance / ALT-02 tier ineligible (CT-12)
 * - G6-A03: ALT-05 partner failure
 */
class RedemptionServiceTest {

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

    private RewardItem silverVoucher;
    private RewardItem platinumItem;
    private UUID itemId;
    private UUID platinumItemId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        itemId = UUID.randomUUID();
        platinumItemId = UUID.randomUUID();

        silverVoucher = RewardItem.builder()
                .itemId(itemId)
                .programId("DEFAULT_PROG")
                .name("Coffee Voucher")
                .category("VOUCHER")
                .pointsCost(300L)
                .currencyValue(BigDecimal.valueOf(3.00))
                .fulfillmentType(FulfillmentType.DIGITAL)
                .minTierRequired("SILVER")
                .status("ACTIVE")
                .build();

        platinumItem = RewardItem.builder()
                .itemId(platinumItemId)
                .programId("DEFAULT_PROG")
                .name("Platinum Lounge Pass")
                .pointsCost(5000L)
                .minTierRequired("PLATINUM")
                .status("ACTIVE")
                .build();
    }

    /**
     * G6-T01: Happy path order intake — validation passes, CT-12 tier queried,
     * CT-13 FIFO debit reserved in Earning Engine, order moves PENDING → IN_PROGRESS.
     */
    @Test
    void testPlaceOrder_Success() {
        when(catalogService.getActiveItemOrThrow(itemId)).thenReturn(silverVoucher);
        when(tieringClient.getMemberTier("member-001", "DEFAULT_PROG")).thenReturn("SILVER");
        doNothing().when(catalogService).validateTierEligibility(any(), eq("SILVER"));
        when(balanceLockService.tryLock(any(), any())).thenReturn(true);
        doNothing().when(fifoDebitService).debitFifo(any(), any(), anyLong(), any());

        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RedemptionOrder result = redemptionService.placeOrder(
                "member-001", "DEFAULT_PROG", itemId, 1);

        assertNotNull(result);
        assertEquals(OrderStatus.IN_PROGRESS, result.getStatus(),
                "G6-T01: Order must move to IN_PROGRESS upon FIFO debit reservation");
        assertEquals(300L, result.getTotalPointsDebited());

        // Verify CT-12 tier check was performed
        verify(tieringClient).getMemberTier("member-001", "DEFAULT_PROG");
        // Verify CT-13 FIFO debit was requested from Earning Engine Service
        verify(fifoDebitService).debitFifo(eq("member-001"), eq("DEFAULT_PROG"), eq(300L), anyString());
        // Verify lock was released
        verify(balanceLockService).releaseLock("member-001");
    }

    /**
     * G6-A02 / ALT-01: Insufficient balance in Earning DB.
     * CT-13 FIFO debit throws ERR_RED_INSUFFICIENT_BALANCE → order creation fails.
     */
    @Test
    void testPlaceOrder_InsufficientBalance_Throws() {
        when(catalogService.getActiveItemOrThrow(itemId)).thenReturn(silverVoucher);
        when(tieringClient.getMemberTier("member-001", "DEFAULT_PROG")).thenReturn("SILVER");
        doNothing().when(catalogService).validateTierEligibility(any(), eq("SILVER"));
        when(balanceLockService.tryLock(any(), any())).thenReturn(true);

        doThrow(new IllegalStateException("ERR_RED_INSUFFICIENT_BALANCE: Required 300 but available 200"))
                .when(fifoDebitService).debitFifo(any(), any(), anyLong(), any());

        assertThrows(IllegalStateException.class, () ->
                redemptionService.placeOrder("member-001", "DEFAULT_PROG", itemId, 1));

        verify(balanceLockService).releaseLock("member-001");
        verify(orderRepository, never()).save(any());
    }

    /**
     * G6-A02 / ALT-02: MemberTier queried via CT-12 is below minimum tier requirement.
     */
    @Test
    void testPlaceOrder_TierEligibilityFailed_Throws() {
        when(catalogService.getActiveItemOrThrow(platinumItemId)).thenReturn(platinumItem);
        when(tieringClient.getMemberTier("member-001", "DEFAULT_PROG")).thenReturn("SILVER");
        doThrow(new IllegalStateException("ERR_RED_TIER_ELIGIBILITY_FAILED: SILVER is below PLATINUM"))
                .when(catalogService).validateTierEligibility(any(), eq("SILVER"));

        assertThrows(IllegalStateException.class, () ->
                redemptionService.placeOrder("member-001", "DEFAULT_PROG", platinumItemId, 1));

        verify(fifoDebitService, never()).debitFifo(any(), any(), anyLong(), any());
    }

    /**
     * ALT-06: Distributed balance lock busy.
     */
    @Test
    void testPlaceOrder_ConcurrentLockBusy_Throws() {
        when(catalogService.getActiveItemOrThrow(itemId)).thenReturn(silverVoucher);
        when(tieringClient.getMemberTier("member-001", "DEFAULT_PROG")).thenReturn("SILVER");
        doNothing().when(catalogService).validateTierEligibility(any(), eq("SILVER"));
        when(balanceLockService.tryLock(any(), any())).thenReturn(false);

        assertThrows(IllegalStateException.class, () ->
                redemptionService.placeOrder("member-001", "DEFAULT_PROG", itemId, 1));

        verify(fifoDebitService, never()).debitFifo(any(), any(), anyLong(), any());
    }

    /**
     * ALT-03: Below 100 points minimum.
     */
    @Test
    void testPlaceOrder_MinimumPointsViolation_Throws() {
        RewardItem cheapItem = RewardItem.builder()
                .itemId(UUID.randomUUID())
                .pointsCost(50L) // below 100 minimum
                .minTierRequired("SILVER")
                .status("ACTIVE")
                .build();

        when(catalogService.getActiveItemOrThrow(any())).thenReturn(cheapItem);

        assertThrows(IllegalArgumentException.class, () ->
                redemptionService.placeOrder("member-001", "DEFAULT_PROG", cheapItem.getItemId(), 1));

        verify(tieringClient, never()).getMemberTier(any(), any());
        verify(fifoDebitService, never()).debitFifo(any(), any(), anyLong(), any());
    }

    /**
     * G6-T03: IN_PROGRESS → FULFILLED.
     */
    @Test
    void testFulfillOrder_Success() {
        UUID orderId = UUID.randomUUID();
        RedemptionOrder order = RedemptionOrder.builder()
                .orderId(orderId)
                .memberId("member-001")
                .programId("DEFAULT_PROG")
                .status(OrderStatus.IN_PROGRESS)
                .totalPointsDebited(300L)
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RedemptionOrder fulfilled = redemptionService.fulfillOrder(orderId);

        assertEquals(OrderStatus.FULFILLED, fulfilled.getStatus(),
                "G6-T03: Order moves IN_PROGRESS → FULFILLED");
        verify(orderRepository).save(order);
    }

    /**
     * G6-T04 / G6-T05 / G6-A03 / EXC-05: Partner fulfillment failure triggers auto-reversal.
     * Order moves IN_PROGRESS → FAILED → REVERSED.
     * Earning Engine Service is called via CT-13 to restore points with original earn date/expiry.
     */
    @Test
    void testFailAndReverseOrder_RestoredWithFifo() {
        UUID orderId = UUID.randomUUID();
        RedemptionOrder order = RedemptionOrder.builder()
                .orderId(orderId)
                .memberId("member-001")
                .programId("DEFAULT_PROG")
                .status(OrderStatus.IN_PROGRESS)
                .totalPointsDebited(300L)
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RedemptionOrder reversed = redemptionService.failAndReverseOrder(orderId, "OUT_OF_STOCK");

        assertEquals(OrderStatus.REVERSED, reversed.getStatus(),
                "G6-T05: Order moves to terminal state REVERSED");
        assertEquals("OUT_OF_STOCK", reversed.getFailureReason());

        // Verify CT-13 RestorePoints was delegated to Earning Engine Service (EXC-05)
        verify(fifoDebitService).reverseDebit("member-001", "DEFAULT_PROG", orderId.toString(), 300L);
    }

    /**
     * G6-T02: PENDING → CANCELLED when member cancels while pending.
     */
    @Test
    void testCancelOrder_WhenPending_Success() {
        UUID orderId = UUID.randomUUID();
        RedemptionOrder order = RedemptionOrder.builder()
                .orderId(orderId)
                .memberId("member-001")
                .programId("DEFAULT_PROG")
                .status(OrderStatus.PENDING)
                .totalPointsDebited(300L)
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RedemptionOrder cancelled = redemptionService.cancelOrder(orderId, "member-001");

        assertEquals(OrderStatus.CANCELLED, cancelled.getStatus(),
                "G6-T02: Order moves PENDING → CANCELLED");
    }

    /**
     * Cancel refused if already IN_PROGRESS or FULFILLED.
     */
    @Test
    void testCancelOrder_WhenInProgress_Throws() {
        UUID orderId = UUID.randomUUID();
        RedemptionOrder order = RedemptionOrder.builder()
                .orderId(orderId)
                .memberId("member-001")
                .status(OrderStatus.IN_PROGRESS)
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThrows(IllegalStateException.class, () ->
                redemptionService.cancelOrder(orderId, "member-001"));
    }
}
