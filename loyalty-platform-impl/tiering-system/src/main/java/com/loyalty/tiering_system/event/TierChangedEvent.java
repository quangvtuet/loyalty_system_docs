package com.loyalty.tiering_system.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event publish ra khi tier thay đổi — topic: loyalty.tiering.tier_changed
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TierChangedEvent {
    private String memberId;
    private String programId;
    private String previousTier;
    private String newTier;
    private double newEarnMultiplier;
    private long cumulativeQp;
    private String reason; // "UPGRADE", "GRACE_RESCUED", "DOWNGRADED"
}
