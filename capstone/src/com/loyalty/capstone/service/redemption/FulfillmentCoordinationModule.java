package com.loyalty.capstone.service.redemption;

import com.loyalty.capstone.domain.RedemptionOrder;
import com.loyalty.capstone.external.CrmNotificationGateway;
import com.loyalty.capstone.external.PartnerSystems;

/**
 * Lab 9 Component 'Fulfillment Coordination Module'.
 * On failure it runs the CON.3 compensating action: restore the original batches, then REVERSED, then notify.
 */
public final class FulfillmentCoordinationModule {

    private final PartnerSystems partnerSystems;
    private final CrmNotificationGateway crmNotificationGateway;
    private final FifoDebitModule fifoDebitModule;
    private final RedemptionOrderStateModule stateModule;

    public FulfillmentCoordinationModule(PartnerSystems partnerSystems,
                                         CrmNotificationGateway crmNotificationGateway,
                                         FifoDebitModule fifoDebitModule,
                                         RedemptionOrderStateModule stateModule) {
        this.partnerSystems = partnerSystems;
        this.crmNotificationGateway = crmNotificationGateway;
        this.fifoDebitModule = fifoDebitModule;
        this.stateModule = stateModule;
    }

    public void dispatch(RedemptionOrder order) {
        boolean delivered = partnerSystems.requestFulfillment(order.orderId, order.rewardItemId);
        if (delivered) {
            stateModule.toFulfilled(order);
            return;
        }
        stateModule.toFailed(order, "partner fulfillment failed");
        fifoDebitModule.restore(order.allocations());
        stateModule.toReversed(order);
        crmNotificationGateway.notifyMemberOfReversal(order.memberId, order.orderId);
    }
}
