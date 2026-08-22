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

    /**
     * Every transition this order actually made, in order.
     * A test asserting one I-6 row can therefore prove that row rather than the end state.
     */
    private final List<String> transitions = new ArrayList<>();

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

    public List<String> transitions() { return Collections.unmodifiableList(transitions); }

    /** True when this order really made that I-6 transition, whatever state it ended in. */
    public boolean hasTransition(OrderState from, OrderState to) {
        return transitions.contains(label(from, to));
    }

    private static String label(OrderState from, OrderState to) { return from + " -> " + to; }

    /** PENDING -> IN_PROGRESS : validation passes and the FIFO debit is reserved. */
    public void markInProgress(List<DebitAllocation> reserved) {
        allocations.clear();
        allocations.addAll(reserved);
        transitionTo(OrderState.PENDING, OrderState.IN_PROGRESS);
    }

    /** PENDING -> CANCELLED : validation fails or the member cancels. */
    public void cancel(String why) {
        this.reason = why;
        transitionTo(OrderState.PENDING, OrderState.CANCELLED);
    }

    /** IN_PROGRESS -> FULFILLED : Partner Systems confirms delivery. */
    public void markFulfilled() {
        transitionTo(OrderState.IN_PROGRESS, OrderState.FULFILLED);
    }

    /** IN_PROGRESS -> FAILED : Partner Systems fulfillment fails. */
    public void markFailed(String why) {
        this.reason = why;
        transitionTo(OrderState.IN_PROGRESS, OrderState.FAILED);
    }

    /** FAILED -> REVERSED : auto-reversal has restored the points. */
    public void markReversed() {
        transitionTo(OrderState.FAILED, OrderState.REVERSED);
    }

    private void transitionTo(OrderState expected, OrderState next) {
        if (state != expected) {
            throw new IllegalStateTransition(state, next);
        }
        transitions.add(label(expected, next));
        state = next;
    }
}
