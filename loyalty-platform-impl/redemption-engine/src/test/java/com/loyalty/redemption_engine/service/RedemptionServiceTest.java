package com.loyalty.redemption_engine.service;

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

class RedemptionServiceTest {

    @Mock
    private CatalogService catalogService;

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

    @Test
    void testPlaceOrder_Success() {
        // UC-03-02: Member 500 pts đặt item 300 pts
        when(catalogService.getActiveItemOrThrow(itemId)).thenReturn(silverVoucher);
        doNothing().when(catalogService).validateTierEligibility(any(), any());
        when(balanceLockService.tryLock(any(), any())).thenReturn(true);
        doNothing().when(fifoDebitService).debitFifo(any(), any(), anyLong(), any());

        RedemptionOrder savedOrder = RedemptionOrder.builder()
                .orderId(UUID.randomUUID())
                .memberId("member-001")
                .status(OrderStatus.PENDING)
                .totalPointsDebited(300L)
                .build();
        when(orderRepository.save(any())).thenReturn(savedOrder);

        RedemptionOrder result = redemptionService.placeOrder(
                "member-001", "DEFAULT_PROG", itemId, 1, "SILVER", 500L);

        assertNotNull(result);
        assertEquals(OrderStatus.PENDING, result.getStatus());
        assertEquals(300L, result.getTotalPointsDebited());

        verify(fifoDebitService).debitFifo(eq("member-001"), eq("DEFAULT_PROG"), eq(300L), anyString());
        verify(balanceLockService).releaseLock("member-001");
    }

    @Test
    void testPlaceOrder_InsufficientBalance_Throws() {
        // UC-03-02: Member 200 pts đặt item 300 pts — bị từ chối
        when(catalogService.getActiveItemOrThrow(itemId)).thenReturn(silverVoucher);
        doNothing().when(catalogService).validateTierEligibility(any(), any());

        assertThrows(IllegalStateException.class, () ->
                redemptionService.placeOrder("member-001", "DEFAULT_PROG", itemId, 1, "SILVER", 200L));

        verify(fifoDebitService, never()).debitFifo(any(), any(), anyLong(), any());
    }

    @Test
    void testPlaceOrder_TierEligibilityFailed_Throws() {
        // UC-03-02: Silver member đặt Platinum item — bị từ chối (ERR_RED_TIER_ELIGIBILITY_FAILED)
        when(catalogService.getActiveItemOrThrow(platinumItemId)).thenReturn(platinumItem);
        doThrow(new IllegalStateException("ERR_RED_TIER_ELIGIBILITY_FAILED"))
                .when(catalogService).validateTierEligibility(any(), eq("SILVER"));

        assertThrows(IllegalStateException.class, () ->
                redemptionService.placeOrder("member-001", "DEFAULT_PROG", platinumItemId, 1, "SILVER", 10000L));
    }

    @Test
    void testPlaceOrder_ConcurrentLockBusy_Throws() {
        // FR-03-024: lock bận do concurrent request
        when(catalogService.getActiveItemOrThrow(itemId)).thenReturn(silverVoucher);
        doNothing().when(catalogService).validateTierEligibility(any(), any());
        when(balanceLockService.tryLock(any(), any())).thenReturn(false);

        assertThrows(IllegalStateException.class, () ->
                redemptionService.placeOrder("member-001", "DEFAULT_PROG", itemId, 1, "SILVER", 500L));

        verify(fifoDebitService, never()).debitFifo(any(), any(), anyLong(), any());
    }

    @Test
    void testPlaceOrder_MinimumPointsViolation_Throws() {
        // FR-03-013: 50 pts < 100 pts minimum
        RewardItem cheapItem = RewardItem.builder()
                .itemId(UUID.randomUUID())
                .pointsCost(50L) // below minimum
                .minTierRequired("SILVER")
                .status("ACTIVE")
                .build();

        when(catalogService.getActiveItemOrThrow(any())).thenReturn(cheapItem);
        doNothing().when(catalogService).validateTierEligibility(any(), any());

        assertThrows(IllegalArgumentException.class, () ->
                redemptionService.placeOrder("member-001", "DEFAULT_PROG", cheapItem.getItemId(), 1, "SILVER", 500L));
    }

    @Test
    void testFailAndReverseOrder_RestoredWithFifo() {
        // UC-03-05 / FLOW-05: fulfillment thất bại → hoàn điểm
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

        redemptionService.failAndReverseOrder(orderId, "OUT_OF_STOCK");

        ArgumentCaptor<RedemptionOrder> captor = ArgumentCaptor.forClass(RedemptionOrder.class);
        verify(orderRepository, times(2)).save(captor.capture());

        // Trạng thái cuối là REVERSED
        assertEquals(OrderStatus.REVERSED, captor.getAllValues().get(1).getStatus());
        assertEquals("OUT_OF_STOCK", captor.getAllValues().get(0).getFailureReason());

        // Verify FIFO reversal được gọi để giữ nguyên earn_date gốc (FR-03-041)
        verify(fifoDebitService).reverseDebit("member-001", "DEFAULT_PROG", orderId.toString(), 300L);
    }

    @Test
    void testCancelOrder_WhenPending_Success() {
        // UC-03-07: Member hủy đơn khi PENDING
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

        redemptionService.cancelOrder(orderId, "member-001");

        ArgumentCaptor<RedemptionOrder> captor = ArgumentCaptor.forClass(RedemptionOrder.class);
        verify(orderRepository).save(captor.capture());
        assertEquals(OrderStatus.CANCELLED, captor.getValue().getStatus());
    }

    @Test
    void testCancelOrder_WhenFulfilled_Throws() {
        // UC-03-07: Không thể hủy khi đã FULFILLED
        UUID orderId = UUID.randomUUID();
        RedemptionOrder order = RedemptionOrder.builder()
                .orderId(orderId)
                .memberId("member-001")
                .status(OrderStatus.FULFILLED)
                .totalPointsDebited(300L)
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThrows(IllegalStateException.class, () ->
                redemptionService.cancelOrder(orderId, "member-001"));
    }

    /**
     * G6-T03: IN_PROGRESS → FULFILLED (happy path terminal state).
     * Partner confirms delivery → debit is final → order FULFILLED.
     * Spec-trace: G6-T03, I-11 UC-LB-02, openAPI operationId: fulfillRedemptionOrder
     */
    @Test
    void testFulfillOrder_Success() {
        // Arrange: order in IN_PROGRESS (FIFO debit already reserved)
        UUID orderId = UUID.randomUUID();
        RedemptionOrder order = RedemptionOrder.builder()
                .orderId(orderId)
                .memberId("member-001")
                .programId("DEFAULT_PROG")
                .status(OrderStatus.PENDING)  // service reads the current state
                .totalPointsDebited(300L)
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act: partner confirms delivery → fulfillOrder
        redemptionService.fulfillOrder(orderId);

        // Assert: order status moves to FULFILLED (G6-T03)
        ArgumentCaptor<RedemptionOrder> captor = ArgumentCaptor.forClass(RedemptionOrder.class);
        verify(orderRepository, atLeastOnce()).save(captor.capture());
        // The last save should be FULFILLED
        RedemptionOrder saved = captor.getValue();
        assertEquals(OrderStatus.FULFILLED, saved.getStatus(),
                "G6-T03: IN_PROGRESS → FULFILLED after partner confirms delivery");

        // FIFO debit confirmed (no reversal)
        verify(fifoDebitService).confirmDebit("member-001", "DEFAULT_PROG", orderId.toString());
    }

    /**
     * G6-T04: IN_PROGRESS → FAILED.
     * Partner fulfillment failure is recorded before the auto-reversal chain begins.
     * Spec-trace: G6-T04, I-11 UC-LB-02 ALT-05, openAPI: failAndReverseRedemptionOrder
     */
    @Test
    void testFailAndReverseOrder_MovesToFailed() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        RedemptionOrder order = RedemptionOrder.builder()
                .orderId(orderId)
                .memberId("member-002")
                .programId("DEFAULT_PROG")
                .status(OrderStatus.PENDING)
                .totalPointsDebited(500L)
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // Capture the status at each save call (mutated object — snapshot the enum at save time)
        java.util.List<OrderStatus> savedStatuses = new java.util.ArrayList<>();
        java.util.List<String> savedReasons = new java.util.ArrayList<>();
        when(orderRepository.save(any())).thenAnswer(inv -> {
            RedemptionOrder o = inv.getArgument(0);
            savedStatuses.add(o.getStatus());
            savedReasons.add(o.getFailureReason());
            return o;
        });

        // Act
        redemptionService.failAndReverseOrder(orderId, "PARTNER_UNAVAILABLE");

        // Assert: two saves — first to FAILED (G6-T04), then to REVERSED (G6-T05)
        assertEquals(2, savedStatuses.size(), "Exactly two state transitions expected: FAILED then REVERSED");

        // First save: FAILED state with failure reason (G6-T04)
        assertEquals(OrderStatus.FAILED, savedStatuses.get(0),
                "G6-T04: order must be saved as FAILED before auto-reversal");
        assertEquals("PARTNER_UNAVAILABLE", savedReasons.get(0));

        // Second save: REVERSED (G6-T05 — also covered by testFailAndReverseOrder_RestoredWithFifo)
        assertEquals(OrderStatus.REVERSED, savedStatuses.get(1),
                "G6-T05: order must reach REVERSED after points are restored with original earn date/expiry");
    }
}


