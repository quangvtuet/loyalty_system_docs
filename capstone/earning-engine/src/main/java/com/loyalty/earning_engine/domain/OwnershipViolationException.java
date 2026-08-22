package com.loyalty.earning_engine.domain;

/**
 * EXC-04 / I-9 Security & Data Ownership Violation Exception.
 *
 * Thrown when an unauthorized or non-owning actor (e.g. Partner Systems,
 * Core Banking System, CRM & Notification Gateway, Member) attempts a direct write
 * to Earning DB (PointTransaction, PointBalance), bypassing Earning Engine Service.
 *
 * Spec-trace: I-9, EXC-04, CON.2, T4
 */
public class OwnershipViolationException extends SecurityException {

    private final String unauthorizedCaller;
    private final String targetStore;

    public OwnershipViolationException(String unauthorizedCaller, String targetStore) {
        super(String.format("EXC-04: Non-owner '%s' is forbidden from writing directly to '%s'. All mutations must be routed through the owning service aggregate (Earning Engine Service).",
                unauthorizedCaller, targetStore));
        this.unauthorizedCaller = unauthorizedCaller;
        this.targetStore = targetStore;
    }

    public OwnershipViolationException(String message) {
        super(message);
        this.unauthorizedCaller = "UNKNOWN";
        this.targetStore = "Earning DB";
    }

    public String getUnauthorizedCaller() {
        return unauthorizedCaller;
    }

    public String getTargetStore() {
        return targetStore;
    }
}
