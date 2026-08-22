package com.loyalty.capstone.domain;

/** The six I-6 states of the named object RedemptionOrder. No seventh state exists. */
public enum OrderState {
    PENDING(false),
    IN_PROGRESS(false),
    FULFILLED(true),
    FAILED(false),
    CANCELLED(true),
    REVERSED(true);

    private final boolean terminal;

    OrderState(boolean terminal) { this.terminal = terminal; }

    public boolean isTerminal() { return terminal; }
}
