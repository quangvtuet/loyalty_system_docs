package com.loyalty.capstone.domain;

import java.time.Instant;

/** I-7 object owned by Earning DB. Append-only: only remainingPoints moves. */
public final class PointTransaction {
    public final String pointTransactionId;
    public final String memberId;
    public final String sourceTransactionId;
    public final String kind;
    public final long points;
    public final Instant earnDate;
    public final Instant expiryDate;

    private long remainingPoints;

    public PointTransaction(String pointTransactionId, String memberId, String sourceTransactionId,
                            String kind, long points, Instant earnDate, Instant expiryDate) {
        this.pointTransactionId = pointTransactionId;
        this.memberId = memberId;
        this.sourceTransactionId = sourceTransactionId;
        this.kind = kind;
        this.points = points;
        this.earnDate = earnDate;
        this.expiryDate = expiryDate;
        this.remainingPoints = points;
    }

    public long remainingPoints() { return remainingPoints; }

    public void consume(long amount) {
        if (amount > remainingPoints) {
            throw new IllegalArgumentException("cannot consume more than remaining");
        }
        remainingPoints -= amount;
    }

    /** CON.3 restoration: put the points back on this same batch, keeping earnDate and expiryDate. */
    public void restore(long amount) { remainingPoints += amount; }
}
