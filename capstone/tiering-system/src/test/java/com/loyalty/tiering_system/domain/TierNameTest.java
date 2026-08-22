package com.loyalty.tiering_system.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TierNameTest {

    @Test
    void testFromCumulativeQp_Below1000_ReturnsSilver() {
        assertEquals(TierName.SILVER, TierName.fromCumulativeQp(0));
        assertEquals(TierName.SILVER, TierName.fromCumulativeQp(500));
        assertEquals(TierName.SILVER, TierName.fromCumulativeQp(999));
    }

    @Test
    void testFromCumulativeQp_ExactGoldThreshold_ReturnsGold() {
        assertEquals(TierName.GOLD, TierName.fromCumulativeQp(1000));
    }

    @Test
    void testFromCumulativeQp_Between1000And3000_ReturnsGold() {
        assertEquals(TierName.GOLD, TierName.fromCumulativeQp(1500));
        assertEquals(TierName.GOLD, TierName.fromCumulativeQp(2999));
    }

    @Test
    void testFromCumulativeQp_ExactPlatinumThreshold_ReturnsPlatinum() {
        assertEquals(TierName.PLATINUM, TierName.fromCumulativeQp(3000));
    }

    @Test
    void testFromCumulativeQp_Above3000_ReturnsPlatinum() {
        assertEquals(TierName.PLATINUM, TierName.fromCumulativeQp(5000));
    }

    @Test
    void testTierMultipliers() {
        assertEquals(1.0, TierName.SILVER.getEarnMultiplier());
        assertEquals(1.5, TierName.GOLD.getEarnMultiplier());
        assertEquals(2.0, TierName.PLATINUM.getEarnMultiplier());
    }
}
