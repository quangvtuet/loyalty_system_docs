package com.loyalty.capstone.service.redemption;

/** Result of the Lab 9 'Tier and Balance Validation Module'. */
public final class ValidationOutcome {
    public final boolean accepted;
    public final String rejectionReason;

    private ValidationOutcome(boolean accepted, String rejectionReason) {
        this.accepted = accepted;
        this.rejectionReason = rejectionReason;
    }

    public static ValidationOutcome accepted() { return new ValidationOutcome(true, null); }

    public static ValidationOutcome rejected(String reason) { return new ValidationOutcome(false, reason); }
}
