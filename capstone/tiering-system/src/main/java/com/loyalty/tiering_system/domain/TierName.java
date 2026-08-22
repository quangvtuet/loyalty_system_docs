package com.loyalty.tiering_system.domain;

/**
 * Tier levels — thresholds theo FR-02 và DD-02.
 * Silver: 0 QP (base), Gold: 1000 QP, Platinum: 3000 QP.
 */
public enum TierName {
    SILVER(0L, 1.0),
    GOLD(1000L, 1.5),
    PLATINUM(3000L, 2.0);

    private final long qpThreshold;
    private final double earnMultiplier;

    TierName(long qpThreshold, double earnMultiplier) {
        this.qpThreshold = qpThreshold;
        this.earnMultiplier = earnMultiplier;
    }

    public long getQpThreshold() {
        return qpThreshold;
    }

    public double getEarnMultiplier() {
        return earnMultiplier;
    }

    /**
     * Xác định tier phù hợp dựa vào tổng QP hiện tại.
     * Trả về tier cao nhất mà member đủ điều kiện.
     */
    public static TierName fromCumulativeQp(long cumulativeQp) {
        TierName result = SILVER;
        for (TierName tier : values()) {
            if (cumulativeQp >= tier.qpThreshold) {
                result = tier;
            }
        }
        return result;
    }
}
