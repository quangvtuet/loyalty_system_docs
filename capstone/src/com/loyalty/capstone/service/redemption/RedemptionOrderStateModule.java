package com.loyalty.capstone.service.redemption;

import com.loyalty.capstone.domain.DebitAllocation;
import com.loyalty.capstone.domain.RedemptionOrder;
import com.loyalty.capstone.store.RedemptionDb;

import java.util.List;

/**
 * Lab 9 Component 'RedemptionOrder State Module'.
 * Every I-6 transition goes through here and is persisted by the owning container.
 */
public final class RedemptionOrderStateModule {

    private final RedemptionDb redemptionDb;
    private final String containerName;

    public RedemptionOrderStateModule(RedemptionDb redemptionDb, String containerName) {
        this.redemptionDb = redemptionDb;
        this.containerName = containerName;
    }

    public void createPending(RedemptionOrder order) {
        redemptionDb.saveOrder(containerName, order);
    }

    public void toInProgress(RedemptionOrder order, List<DebitAllocation> allocations) {
        order.markInProgress(allocations);
        redemptionDb.saveOrder(containerName, order);
    }

    public void toCancelled(RedemptionOrder order, String reason) {
        order.cancel(reason);
        redemptionDb.saveOrder(containerName, order);
    }

    public void toFulfilled(RedemptionOrder order) {
        order.markFulfilled();
        redemptionDb.saveOrder(containerName, order);
    }

    public void toFailed(RedemptionOrder order, String reason) {
        order.markFailed(reason);
        redemptionDb.saveOrder(containerName, order);
    }

    public void toReversed(RedemptionOrder order) {
        order.markReversed();
        redemptionDb.saveOrder(containerName, order);
    }
}
