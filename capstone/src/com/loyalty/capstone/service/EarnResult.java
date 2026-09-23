package com.loyalty.capstone.service;

/** Outcome of one earn attempt. duplicate=true means CON.1 stopped a second posting. */
public final class EarnResult {
    public final String pointTransactionId;
    public final long pointsAwarded;
    public final boolean duplicate;

    public EarnResult(String pointTransactionId, long pointsAwarded, boolean duplicate) {
        this.pointTransactionId = pointTransactionId;
        this.pointsAwarded = pointsAwarded;
        this.duplicate = duplicate;
    }
}
