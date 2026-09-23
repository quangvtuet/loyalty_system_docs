package com.loyalty.capstone.service;

import com.loyalty.capstone.broker.MessageBroker;
import com.loyalty.capstone.broker.Topics;
import com.loyalty.capstone.domain.LoyaltyProgram;
import com.loyalty.capstone.domain.MemberTier;
import com.loyalty.capstone.store.TieringDb;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * I-4 container 'Tiering System Service'. Sole writer of Tiering DB.
 * UC-LB-03: a replayed qualifying-accrual event is ignored rather than applied twice.
 */
public final class TieringSystemService {

    public static final String CONTAINER = "Tiering System Service";

    private final TieringDb tieringDb;
    private final ProgramManagementService programManagementService;
    private final MessageBroker messageBroker;
    private final String programId;
    private final Set<String> handledEventIds = new HashSet<>();

    public TieringSystemService(TieringDb tieringDb, ProgramManagementService programManagementService,
                                MessageBroker messageBroker, String programId) {
        this.tieringDb = tieringDb;
        this.programManagementService = programManagementService;
        this.messageBroker = messageBroker;
        this.programId = programId;
        this.messageBroker.subscribe(Topics.EARNING_QP_ACCRUED, this::onQualifyingPointsAccrued);
    }

    private void onQualifyingPointsAccrued(Map<String, Object> event) {
        String eventId = String.valueOf(event.get("eventId"));
        if (!handledEventIds.add(eventId)) {
            return;
        }
        String memberId = String.valueOf(event.get("memberId"));
        long qualifyingPoints = ((Number) event.get("qualifyingPoints")).longValue();

        MemberTier memberTier = tieringDb.tierForWrite(CONTAINER, memberId);
        String before = memberTier.tier();
        memberTier.accrue(qualifyingPoints);

        LoyaltyProgram program = programManagementService.program(programId);
        String after = program.tierForQualifyingPoints(memberTier.qualifyingPoints());
        if (!after.equals(before)) {
            memberTier.moveTo(after);
            publishTierChanged(memberId, before, after);
        }
    }

    private void publishTierChanged(String memberId, String fromTier, String toTier) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("memberId", memberId);
        event.put("fromTier", fromTier);
        event.put("toTier", toTier);
        messageBroker.publish(Topics.TIERING_TIER_CHANGED, event);
    }

    /** Serves Redemption Engine Service on the Lab 9 relationship 'tier status'. */
    public String currentTier(String memberId) { return tieringDb.readTier(memberId); }

    public long qualifyingPoints(String memberId) { return tieringDb.readQualifyingPoints(memberId); }
}
