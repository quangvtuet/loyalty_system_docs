package com.loyalty.capstone.store;

import com.loyalty.capstone.domain.MemberTier;

import java.util.LinkedHashMap;
import java.util.Map;

/** I-4 container 'Tiering DB'. I-7 owner of MemberTier. */
public final class TieringDb extends OwnedStore {

    private final Map<String, MemberTier> tiers = new LinkedHashMap<>();

    public TieringDb() { super("Tiering DB", "Tiering System Service"); }

    public MemberTier tierForWrite(String writerContainerName, String memberId) {
        assertWriter(writerContainerName);
        return tiers.computeIfAbsent(memberId, MemberTier::new);
    }

    public String readTier(String memberId) {
        MemberTier tier = tiers.get(memberId);
        return tier == null ? "SILVER" : tier.tier();
    }

    public long readQualifyingPoints(String memberId) {
        MemberTier tier = tiers.get(memberId);
        return tier == null ? 0L : tier.qualifyingPoints();
    }
}
