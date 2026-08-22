package com.loyalty.capstone.store;

import com.loyalty.capstone.domain.RedemptionOrder;
import com.loyalty.capstone.domain.RewardItem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** I-4 container 'Redemption DB'. I-7 owner of RedemptionOrder; also holds the reward catalogue. */
public final class RedemptionDb extends OwnedStore {

    private final Map<String, RedemptionOrder> orders = new LinkedHashMap<>();
    private final Map<String, RewardItem> catalogue = new LinkedHashMap<>();

    public RedemptionDb() { super("Redemption DB", "Redemption Engine Service"); }

    public void saveOrder(String writerContainerName, RedemptionOrder order) {
        assertWriter(writerContainerName);
        orders.put(order.orderId, order);
    }

    public RedemptionOrder order(String orderId) { return orders.get(orderId); }

    public List<RedemptionOrder> allOrders() { return new ArrayList<>(orders.values()); }

    public void saveRewardItem(String writerContainerName, RewardItem item) {
        assertWriter(writerContainerName);
        catalogue.put(item.rewardItemId, item);
    }

    public RewardItem rewardItem(String rewardItemId) { return catalogue.get(rewardItemId); }

    public List<RewardItem> catalogue() { return new ArrayList<>(catalogue.values()); }
}
