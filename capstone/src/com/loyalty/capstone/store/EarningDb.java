package com.loyalty.capstone.store;

import com.loyalty.capstone.domain.PointBalance;
import com.loyalty.capstone.domain.PointTransaction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** I-4 container 'Earning DB'. I-7 owner of PointTransaction and PointBalance. */
public final class EarningDb extends OwnedStore {

    private final Map<String, PointTransaction> transactions = new LinkedHashMap<>();
    private final Map<String, PointBalance> balances = new LinkedHashMap<>();

    public EarningDb() { super("Earning DB", "Earning Engine Service"); }

    public void appendTransaction(String writerContainerName, PointTransaction transaction) {
        assertWriter(writerContainerName);
        transactions.put(transaction.pointTransactionId, transaction);
    }

    public PointTransaction transaction(String pointTransactionId) {
        return transactions.get(pointTransactionId);
    }

    public List<PointTransaction> all() { return new ArrayList<>(transactions.values()); }

    public List<PointTransaction> findBySourceTransaction(String sourceTransactionId) {
        List<PointTransaction> found = new ArrayList<>();
        for (PointTransaction t : transactions.values()) {
            if (t.sourceTransactionId.equals(sourceTransactionId)) found.add(t);
        }
        return found;
    }

    /** Oldest earn date first — the FIFO order the outcome and CON.3 depend on. */
    public List<PointTransaction> openBatchesOldestFirst(String memberId) {
        List<PointTransaction> open = new ArrayList<>();
        for (PointTransaction t : transactions.values()) {
            if (t.memberId.equals(memberId) && t.remainingPoints() > 0) open.add(t);
        }
        open.sort(Comparator.comparing(t -> t.earnDate));
        return open;
    }

    public PointBalance balance(String writerContainerName, String memberId) {
        assertWriter(writerContainerName);
        return balances.computeIfAbsent(memberId, PointBalance::new);
    }

    public long readConfirmedPoints(String memberId) {
        PointBalance balance = balances.get(memberId);
        return balance == null ? 0L : balance.confirmedPoints();
    }
}
