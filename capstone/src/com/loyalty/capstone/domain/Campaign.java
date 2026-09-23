package com.loyalty.capstone.domain;

/** I-7 object owned by Program Mgmt DB. Lower priority number wins. */
public final class Campaign {
    public final String campaignId;
    public final String programId;
    public final double multiplier;
    public final int priority;
    public final boolean active;

    public Campaign(String campaignId, String programId, double multiplier, int priority, boolean active) {
        this.campaignId = campaignId;
        this.programId = programId;
        this.multiplier = multiplier;
        this.priority = priority;
        this.active = active;
    }
}
