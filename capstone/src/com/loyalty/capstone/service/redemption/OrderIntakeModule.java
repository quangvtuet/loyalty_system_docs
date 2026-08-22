package com.loyalty.capstone.service.redemption;

import com.loyalty.capstone.domain.RedemptionOrder;

import java.util.concurrent.atomic.AtomicLong;

/** Lab 9 Component 'Order Intake Module' inside Redemption Engine Service. */
public final class OrderIntakeModule {

    private final AtomicLong sequence = new AtomicLong();

    public RedemptionOrder accept(String memberId, String rewardItemId, long pointsCost, String memberTier) {
        String orderId = "RO-" + sequence.incrementAndGet();
        return new RedemptionOrder(orderId, memberId, rewardItemId, pointsCost, memberTier);
    }
}
