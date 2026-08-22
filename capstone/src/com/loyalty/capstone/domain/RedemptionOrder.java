package com.loyalty.capstone.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The I-6 named object as a type. Every I-6 transition is an operation on this type;
 * states are not loose strings and a transition outside I-6 throws.
 */
public final class RedemptionOrder {

    public final String orderId;
    public final String memberId;
    public final String rewardItemId;
    public final long pointsCost;
    public final String memberTierAtOrder;

    private OrderState state = OrderState.PENDING;
    private String reason;
    private final List<DebitAllocation> allocations = new ArrayList<>();

    public RedemptionOrder(String orderId, String memberId, String rewardItemId,
                           long pointsCost, String memberTierAtOrder) {
        this.orderId = orderId;
        this.memberId = memberId;
        this.rewardItemId = rewardItemId;
        this.pointsCost = pointsCost;
        this.memberTierAtOrder = memberTierAtOrder;
    }

    public OrderState state() { return state; }

    public String reason() { return reason; }

    public List<DebitAllocation> allocations() { return Collections.unmodifiableList(allocations); }

    /** PENDING -> IN_PROGRESS : validation passes and the FIFO debit is reserved. */
    public void markInProgress(List<DebitAllocation> reserved) {
        require(OrderState.PENDING, OrderState.IN_PROGRESS);
        allocations.clear();
        allocations.addAll(reserved);
        state = OrderState.IN_PROGRESS;
    }

    /** PENDING -> CANCELLED : validation fails or the member cancels. */
    public void cancel(String why) {
        require(OrderState.PENDING, OrderState.CANCELLED);
        this.reason = why;
        state = OrderState.CANCELLED;
    }

    /** IN_PROGRESS -> FULFILLED : Partner Systems confirms delivery. */
    public void markFulfilled() {
        require(OrderState.IN_PROGRESS, OrderState.FULFILLED);
        state = OrderState.FULFILLED;
    }

    /** IN_PROGRESS -> FAILED : Partner Systems fulfillment fails. */
    public void markFailed(String why) {
        require(OrderState.IN_PROGRESS, OrderState.FAILED);
        this.reason = why;
        state = OrderState.FAILED;
    }

    /** FAILED -> REVERSED : auto-reversal has restored the points. */
    public void markReversed() {
        require(OrderState.FAILED, OrderState.REVERSED);
        state = OrderState.REVERSED;
    }

    private void require(OrderState expected, OrderState next) {
        if (state != expected) {
            throw new IllegalStateTransition(state, next);
        }
    }
}
