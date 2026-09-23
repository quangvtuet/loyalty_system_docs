package com.loyalty.capstone.external;

/** I-3 external 'Partner Systems'. Mocked fulfillment neighbour of Redemption Engine Service. */
public interface PartnerSystems {
    boolean requestFulfillment(String orderId, String rewardItemId);
}
