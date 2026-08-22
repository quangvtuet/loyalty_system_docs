package com.loyalty.capstone.domain;

/** I-7 object owned by Tiering DB. */
public final class MemberTier {
    public final String memberId;
    private String tier = "SILVER";
    private long qualifyingPoints;

    public MemberTier(String memberId) { this.memberId = memberId; }

    public String tier() { return tier; }

    public long qualifyingPoints() { return qualifyingPoints; }

    public void accrue(long points) { qualifyingPoints += points; }

    public void moveTo(String newTier) { this.tier = newTier; }
}
