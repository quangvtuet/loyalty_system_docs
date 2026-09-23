package com.loyalty.capstone.domain;

/** Catalogue entry held by Redemption DB, per the I-4 responsibility of that container. */
public final class RewardItem {
    public final String rewardItemId;
    public final String name;
    public final long pointsCost;
    public final String minimumTier;

    public RewardItem(String rewardItemId, String name, long pointsCost, String minimumTier) {
        this.rewardItemId = rewardItemId;
        this.name = name;
        this.pointsCost = pointsCost;
        this.minimumTier = minimumTier;
    }
}
