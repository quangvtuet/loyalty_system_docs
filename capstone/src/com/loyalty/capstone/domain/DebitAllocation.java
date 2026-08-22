package com.loyalty.capstone.domain;

import java.time.Instant;

/**
 * One slice of a FIFO debit taken from a single PointTransaction batch.
 * Carries the original earn date and expiry so CON.3 can restore them unchanged.
 */
public final class DebitAllocation {
    public final String pointTransactionId;
    public final long points;
    public final Instant originalEarnDate;
    public final Instant originalExpiryDate;

    public DebitAllocation(String pointTransactionId, long points,
                           Instant originalEarnDate, Instant originalExpiryDate) {
        this.pointTransactionId = pointTransactionId;
        this.points = points;
        this.originalEarnDate = originalEarnDate;
        this.originalExpiryDate = originalExpiryDate;
    }
}
