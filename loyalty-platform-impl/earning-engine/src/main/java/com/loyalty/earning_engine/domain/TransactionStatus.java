package com.loyalty.earning_engine.domain;

public enum TransactionStatus {
    PENDING,
    CONFIRMED,
    CONFIRMED_DEBIT,
    REVERSED,
    FAILED,
    CANCELLED
}
