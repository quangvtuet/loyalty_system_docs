package com.loyalty.capstone.external;

/** I-3 external 'Partner Systems'. Mocked fulfilment neighbour of Redemption Engine Service. */
public interface PartnerSystems {
    boolean requestFulfilment(String orderId, String rewardItemId);
}
