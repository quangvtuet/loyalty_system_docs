package com.loyalty.capstone.external;

import com.loyalty.capstone.broker.MessageBroker;
import com.loyalty.capstone.broker.Topics;

import java.util.LinkedHashMap;
import java.util.Map;

/** Stub for I-3 'Core Banking System'. No real host, no credential. */
public final class CoreBankingSystemMock implements CoreBankingSystem {

    private final MessageBroker messageBroker;

    public CoreBankingSystemMock(MessageBroker messageBroker) { this.messageBroker = messageBroker; }

    @Override
    public void publishSettledTransaction(String sourceTransactionId, String memberId,
                                          String programId, double amount) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("sourceTransactionId", sourceTransactionId);
        event.put("memberId", memberId);
        event.put("programId", programId);
        event.put("amount", amount);
        messageBroker.publish(Topics.TRANSACTION_SETTLED, event);
    }
}
