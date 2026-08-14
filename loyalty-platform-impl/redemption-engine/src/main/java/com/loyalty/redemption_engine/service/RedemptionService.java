package com.loyalty.redemption_engine.service;

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
 * Redemption Service — điều phối luồng chính của redemption.
 *
 * FLOW-04 (DD-03 Section 4.1):
 * 1. Acquire distributed lock (BalanceLockService)
 * 2. Validate tier eligibility (CatalogService)
 * 3. Validate & debit points FIFO (FifoDebitService)
 * 4. Create RedemptionOrder
 * 5. Release lock
 * 6. Dispatch fulfillment (simulated)
 *
 * FR-03-012: balance validation
 * FR-03-013: minimum 100 points
 * NFR-03-003: atomic debit — rollback on failure
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedemptionService {

    private static final int MINIMUM_POINTS = 100;

    private final CatalogService catalogService;
    private final BalanceLockService balanceLockService;
    private final FifoDebitService fifoDebitService;
    private final RedemptionOrderRepository orderRepository;

    @Transactional
    public RedemptionOrder placeOrder(String memberId, String programId, UUID rewardItemId,
                                      int quantity, String memberTier, long availableBalance) {
        // 1. Validate item & tier eligibility
        RewardItem item = catalogService.getActiveItemOrThrow(rewardItemId);
        catalogService.validateTierEligibility(item, memberTier);

        long totalPoints = item.getPointsCost() * quantity;

        // 2. Validate minimum redemption (FR-03-013)
        if (totalPoints < MINIMUM_POINTS) {
            throw new IllegalArgumentException(
                    "ERR_RED_MIN_POINTS: Minimum redemption is " + MINIMUM_POINTS + " points");
        }

        // 3. Validate balance (FR-03-012)
        if (availableBalance < totalPoints) {
            throw new IllegalStateException(
                    "ERR_RED_INSUFFICIENT_BALANCE: Required " + totalPoints + " but available " + availableBalance);
        }

        String orderId = UUID.randomUUID().toString();

        // 4. Acquire distributed lock (FR-03-024)
        if (!balanceLockService.tryLock(memberId, orderId)) {
            throw new IllegalStateException(
                    "ERR_RED_CONCURRENT_REQUEST: Balance lock busy, retry shortly");
        }

        try {
            // 5. FIFO Debit (FR-03-020, FR-03-021)
            fifoDebitService.debitFifo(memberId, programId, totalPoints, orderId);

            // 6. Create Order
            RedemptionOrder order = RedemptionOrder.builder()
                    .memberId(memberId)
                    .programId(programId)
                    .rewardItemId(rewardItemId)
                    .quantity(quantity)
                    .totalPointsDebited(totalPoints)
                    .memberTierAtOrder(memberTier)
                    .status(OrderStatus.PENDING)
                    .build();

            order = orderRepository.save(order);
            log.info("[Redemption] Order created: {} | member: {} | points: {}", order.getOrderId(), memberId, totalPoints);
            return order;

        } finally {
            balanceLockService.releaseLock(memberId);
        }
    }

    /**
     * Callback khi fulfillment thành công (FR-03-032).
     */
    @Transactional
    public void fulfillOrder(UUID orderId) {
        RedemptionOrder order = getOrderOrThrow(orderId);
        order.setStatus(OrderStatus.FULFILLED);
        orderRepository.save(order);
        fifoDebitService.confirmDebit(order.getMemberId(), order.getProgramId(), orderId.toString());
        log.info("[Redemption] Order FULFILLED: {}", orderId);
    }

    /**
     * Callback khi fulfillment thất bại — tự động hoàn điểm (FR-03-040, FR-03-041).
     */
    @Transactional
    public void failAndReverseOrder(UUID orderId, String reason) {
        RedemptionOrder order = getOrderOrThrow(orderId);
        order.setStatus(OrderStatus.FAILED);
        order.setFailureReason(reason);
        orderRepository.save(order);

        // Hoàn điểm, giữ nguyên earn_date gốc (FR-03-041)
        fifoDebitService.reverseDebit(order.getMemberId(), order.getProgramId(),
                orderId.toString(), order.getTotalPointsDebited());

        order.setStatus(OrderStatus.REVERSED);
        orderRepository.save(order);
        log.info("[Redemption] Order REVERSED: {} | reason: {}", orderId, reason);
    }

    /**
     * Member hủy đơn — chỉ được khi PENDING (FR-03-043).
     */
    @Transactional
    public void cancelOrder(UUID orderId, String memberId) {
        RedemptionOrder order = getOrderOrThrow(orderId);
        if (!order.getMemberId().equals(memberId)) {
            throw new IllegalArgumentException("Order does not belong to this member");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException(
                    "ERR_RED_CANCEL_NOT_ALLOWED: Order can only be cancelled when PENDING");
        }
        fifoDebitService.reverseDebit(memberId, order.getProgramId(),
                orderId.toString(), order.getTotalPointsDebited());
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        log.info("[Redemption] Order CANCELLED: {} by member {}", orderId, memberId);
    }

    public List<RedemptionOrder> getHistory(String memberId, String programId) {
        return orderRepository.findByMemberIdAndProgramIdOrderByCreatedAtDesc(memberId, programId);
    }

    private RedemptionOrder getOrderOrThrow(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
    }
}
