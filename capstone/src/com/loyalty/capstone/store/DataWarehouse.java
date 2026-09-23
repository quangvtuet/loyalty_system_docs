package com.loyalty.capstone.store;

import com.loyalty.capstone.domain.FactPointTransaction;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** I-4 container 'Data Warehouse'. I-7 owner of FactPointTransaction. */
public final class DataWarehouse extends OwnedStore {

    private final Map<String, FactPointTransaction> facts = new LinkedHashMap<>();
    private Instant lastIngestedAt;

    public DataWarehouse() { super("Data Warehouse", "Analytics & Reporting Service"); }

    public void upsertFact(String writerContainerName, FactPointTransaction fact) {
        assertWriter(writerContainerName);
        facts.put(fact.pointTransactionId, fact);
        lastIngestedAt = fact.ingestedAt;
    }

    public List<FactPointTransaction> facts() { return new ArrayList<>(facts.values()); }

    public Instant lastIngestedAt() { return lastIngestedAt; }
}
