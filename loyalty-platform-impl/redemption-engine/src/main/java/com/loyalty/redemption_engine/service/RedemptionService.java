package com.loyalty.redemption_engine.service;

import com.loyalty.redemption_engine.client.TieringClient;
import com.loyalty.redemption_engine.domain.OrderStatus;
import com.loyalty.redemption_engine.domain.RedemptionOrder;
import com.loyalty.redemption_engine.domain.RewardItem;
import com.loyalty.redemption_engine.repository.RedemptionOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Redemption Service — điều phối luồng chính của redemption (UC-LB-02).
 *
 * Realizes Lab 3 Section 3 & Lab 10 Sequence and State Machine:
 * 1. Acquire distributed lock (BalanceLockService / M4)
 * 2. Validate tier eligibility via Tiering System Service (CT-12 / M3)
 * 3. Validate minimum 100 points (FR-03-013)
 * 4. Request FIFO debit from Earning Engine Service (CT-13 / M5)
 * 5. Move RedemptionOrder PENDING → IN_PROGRESS (G6-T01)
 * 6. Release lock (M4)
 * 7. Dispatch fulfillment (simulated M7)
 * 8. If delivery confirmed: IN_PROGRESS → FULFILLED (G6-T03)
 * 9. If delivery fails: IN_PROGRESS → FAILED → REVERSED (G6-T04, G6-T05, EXC-05, CON.3)
 * 10. If cancelled when PENDING: PENDING → CANCELLED (G6-T02)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedemptionService {

    private static final int MINIMUM_POINTS = 100;

    private final CatalogService catalogService;
    private final TieringClient tieringClient;
    private final BalanceLockService balanceLockService;
    private final FifoDebitService fifoDebitService;
    private final RedemptionOrderRepository orderRepository;

    @Transactional
    public RedemptionOrder placeOrder(String memberId, String programId, UUID rewardItemId, int quantity) {
        String prog = (programId != null && !programId.isEmpty()) ? programId : "DEFAULT_PROG";

        // 1. Fetch item from catalog
        RewardItem item = catalogService.getActiveItemOrThrow(rewardItemId);
        long totalPoints = item.getPointsCost() * quantity;

        // 2. Validate minimum redemption points (FR-03-013)
        if (totalPoints < MINIMUM_POINTS) {
            throw new IllegalArgumentException(
                    "ERR_RED_MIN_POINTS: Minimum redemption is " + MINIMUM_POINTS + " points");
        }

        // 3. Query current MemberTier from Tiering System Service (CT-12) — do NOT trust client
        String memberTier = tieringClient.getMemberTier(memberId, prog);
        catalogService.validateTierEligibility(item, memberTier);

        String orderId = UUID.randomUUID().toString();

        // 4. Acquire distributed lock (FR-03-024 / M4)
        if (!balanceLockService.tryLock(memberId, orderId)) {
            throw new IllegalStateException(
                    "ERR_RED_CONCURRENT_REQUEST: Balance lock busy, retry shortly");
        }

        try {
            // 5. Delegate FIFO debit to Earning Engine Service (CT-13 / M5)
            // (If balance is insufficient in Earning DB, Earning Engine throws ERR_RED_INSUFFICIENT_BALANCE)
            fifoDebitService.debitFifo(memberId, prog, totalPoints, orderId);

            // 6. Create Order and move PENDING → IN_PROGRESS (G6-T01 debit reserved)
            RedemptionOrder order = RedemptionOrder.builder()
                    .memberId(memberId)
                    .programId(prog)
                    .rewardItemId(rewardItemId)
                    .quantity(quantity)
                    .totalPointsDebited(totalPoints)
                    .memberTierAtOrder(memberTier)
                    .status(OrderStatus.IN_PROGRESS) // Moved to IN_PROGRESS upon debit reservation
                    .build();

            order = orderRepository.save(order);
            log.info("[Redemption] Order created and moved to IN_PROGRESS: {} | member: {} | points: {}",
                    order.getOrderId(), memberId, totalPoints);
            return order;

        } finally {
            balanceLockService.releaseLock(memberId);
        }
    }

    /**
     * Callback khi fulfillment thành công: IN_PROGRESS → FULFILLED (G6-T03).
     */
    @Transactional
    public RedemptionOrder fulfillOrder(UUID orderId) {
        RedemptionOrder order = getOrderOrThrow(orderId);
        if (order.getStatus() != OrderStatus.IN_PROGRESS) {
            throw new IllegalStateException("Order can only be fulfilled when IN_PROGRESS");
        }
        order.setStatus(OrderStatus.FULFILLED);
        order = orderRepository.save(order);
        log.info("[Redemption] Order FULFILLED: {}", orderId);
        return order;
    }

    /**
     * Callback khi fulfillment thất bại — tự động hoàn điểm (G6-T04, G6-T05, EXC-05, CON.3).
     * Order moves IN_PROGRESS → FAILED → REVERSED.
     * Earning Engine Service performs the point restoration.
     */
    @Transactional
    public RedemptionOrder failAndReverseOrder(UUID orderId, String reason) {
        RedemptionOrder order = getOrderOrThrow(orderId);
        
        // Move to FAILED
        order.setStatus(OrderStatus.FAILED);
        order.setFailureReason(reason);
        orderRepository.save(order);
        log.info("[Redemption] Order FAILED: {} | reason: {}", orderId, reason);

        // Delegate restoration to Earning Engine Service (CT-13 RestorePoints)
        try {
            fifoDebitService.reverseDebit(order.getMemberId(), order.getProgramId(),
                    orderId.toString(), order.getTotalPointsDebited());
            order.setStatus(OrderStatus.REVERSED);
            order = orderRepository.save(order);
            log.info("[Redemption] Order REVERSED: {} under CON.3", orderId);
        } catch (RuntimeException ex) {
            order.setFailureReason(reason + ": RESTORE_FAILED");
            orderRepository.save(order);
            throw new IllegalStateException("ERR_RED_RESTORE_FAILED: order remains FAILED", ex);
        }
        return order;
    }

    /**
     * Member hủy đơn — chỉ được khi PENDING (G6-T02).
     */
    @Transactional
    public RedemptionOrder cancelOrder(UUID orderId, String memberId) {
        RedemptionOrder order = getOrderOrThrow(orderId);
        if (!order.getMemberId().equals(memberId)) {
            throw new IllegalArgumentException("Order does not belong to this member");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException(
                    "ERR_RED_CANCEL_NOT_ALLOWED: Order can only be cancelled when PENDING");
        }
        order.setStatus(OrderStatus.CANCELLED);
        order = orderRepository.save(order);
        log.info("[Redemption] Order CANCELLED: {} by member {}", orderId, memberId);
        return order;
    }

    public List<RedemptionOrder> getHistory(String memberId, String programId) {
        return orderRepository.findByMemberIdAndProgramIdOrderByCreatedAtDesc(memberId, programId);
    }

    public RedemptionOrder getOrderOrThrow(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
    }
}
