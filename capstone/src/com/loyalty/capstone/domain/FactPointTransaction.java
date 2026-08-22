package com.loyalty.capstone.domain;

import java.time.Instant;

/** I-7 object owned by Data Warehouse. */
public final class FactPointTransaction {
    public final String pointTransactionId;
    public final String memberId;
    public final long outstandingPoints;
    public final Instant ingestedAt;

    public FactPointTransaction(String pointTransactionId, String memberId,
                                long outstandingPoints, Instant ingestedAt) {
        this.pointTransactionId = pointTransactionId;
        this.memberId = memberId;
        this.outstandingPoints = outstandingPoints;
        this.ingestedAt = ingestedAt;
    }
}
