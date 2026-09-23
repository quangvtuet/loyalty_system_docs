package com.loyalty.capstone.store;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * I-4 container 'Idempotency Store'. Two responsibilities, exactly as I-4 words them:
 * duplicate-check keys (CON.1) and the per-member debit lock.
 * It is shared infrastructure rather than an I-7 source of truth, so it has no single owner.
 */
public final class IdempotencyStore {

    private final Map<String, String> seenKeys = new HashMap<>();
    private final Set<String> heldLocks = new HashSet<>();

    public String storeContainerName() { return "Idempotency Store"; }

    /** Returns null when the key is new and has been claimed; returns the earlier result when seen. */
    public String claimOrGetExisting(String idempotencyKey, String resultReference) {
        String existing = seenKeys.get(idempotencyKey);
        if (existing != null) return existing;
        seenKeys.put(idempotencyKey, resultReference);
        return null;
    }

    /** Replaces the placeholder claim with the real result reference once it exists. */
    public void remember(String idempotencyKey, String resultReference) {
        seenKeys.put(idempotencyKey, resultReference);
    }

    public boolean hasSeen(String idempotencyKey) { return seenKeys.containsKey(idempotencyKey); }

    public boolean acquireBalanceLock(String memberId) { return heldLocks.add(memberId); }

    public void releaseBalanceLock(String memberId) { heldLocks.remove(memberId); }

    public boolean isLocked(String memberId) { return heldLocks.contains(memberId); }
}
