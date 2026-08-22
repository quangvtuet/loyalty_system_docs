package com.loyalty.capstone.external;

/** I-3 external 'Core Banking System'. Mocked: it only publishes simulated settled events. */
public interface CoreBankingSystem {
    void publishSettledTransaction(String sourceTransactionId, String memberId, String programId, double amount);
}
