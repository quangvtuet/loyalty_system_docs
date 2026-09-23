package com.loyalty.capstone.domain;

/** Raised when a caller attempts a RedemptionOrder transition that I-6 does not allow. */
public class IllegalStateTransition extends RuntimeException {
    public IllegalStateTransition(OrderState from, OrderState to) {
        super("RedemptionOrder transition " + from + " -> " + to + " is not in I-6");
    }
}
