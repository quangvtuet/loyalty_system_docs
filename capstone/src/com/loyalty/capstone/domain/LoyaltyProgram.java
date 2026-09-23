package com.loyalty.capstone.domain;

/** I-7 object owned by Program Mgmt DB. Holds the rule values the other services read. */
public final class LoyaltyProgram {
    public final String programId;
    public final double earnRatePointsPerCurrencyUnit;
    public final long goldThresholdQualifyingPoints;
    public final long platinumThresholdQualifyingPoints;
    public final long minimumRedemptionPoints;
    public final double costPerPointUsd;
    public final int pointLifetimeDays;

    public LoyaltyProgram(String programId, double earnRatePointsPerCurrencyUnit,
                          long goldThresholdQualifyingPoints, long platinumThresholdQualifyingPoints,
                          long minimumRedemptionPoints, double costPerPointUsd, int pointLifetimeDays) {
        this.programId = programId;
        this.earnRatePointsPerCurrencyUnit = earnRatePointsPerCurrencyUnit;
        this.goldThresholdQualifyingPoints = goldThresholdQualifyingPoints;
        this.platinumThresholdQualifyingPoints = platinumThresholdQualifyingPoints;
        this.minimumRedemptionPoints = minimumRedemptionPoints;
        this.costPerPointUsd = costPerPointUsd;
        this.pointLifetimeDays = pointLifetimeDays;
    }

    public double tierMultiplier(String tier) {
        if ("PLATINUM".equals(tier)) return 2.0d;
        if ("GOLD".equals(tier)) return 1.5d;
        return 1.0d;
    }

    public String tierForQualifyingPoints(long qualifyingPoints) {
        if (qualifyingPoints >= platinumThresholdQualifyingPoints) return "PLATINUM";
        if (qualifyingPoints >= goldThresholdQualifyingPoints) return "GOLD";
        return "SILVER";
    }
}
