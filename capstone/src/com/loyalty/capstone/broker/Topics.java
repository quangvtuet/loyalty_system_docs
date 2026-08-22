package com.loyalty.capstone.broker;

/** Event names taken from the Lab 3 contract register. No topic is invented here. */
public final class Topics {
    public static final String TRANSACTION_SETTLED = "transaction.settled";
    public static final String EARNING_QP_ACCRUED = "earning.qp_accrued";
    public static final String TIERING_TIER_CHANGED = "tiering.tier_changed";
    public static final String CDC_PLATFORM_EVENTS = "cdc.platform_events";

    private Topics() { }
}
