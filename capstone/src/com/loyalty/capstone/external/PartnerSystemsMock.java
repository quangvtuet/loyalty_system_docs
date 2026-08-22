package com.loyalty.capstone.external;

import java.util.HashSet;
import java.util.Set;

/** Fake for I-3 'Partner Systems'. Delivery outcome is controlled by the test or the demo. */
public final class PartnerSystemsMock implements PartnerSystems {

    private final Set<String> rewardItemsThatFail = new HashSet<>();
    private int fulfilmentRequests;

    public void makeRewardItemFail(String rewardItemId) { rewardItemsThatFail.add(rewardItemId); }

    public int fulfilmentRequests() { return fulfilmentRequests; }

    @Override
    public boolean requestFulfilment(String orderId, String rewardItemId) {
        fulfilmentRequests++;
        return !rewardItemsThatFail.contains(rewardItemId);
    }
}
