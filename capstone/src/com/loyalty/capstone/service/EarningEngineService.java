package com.loyalty.capstone.service;

import com.loyalty.capstone.broker.MessageBroker;
import com.loyalty.capstone.broker.Topics;
import com.loyalty.capstone.domain.Campaign;
import com.loyalty.capstone.domain.DebitAllocation;
import com.loyalty.capstone.domain.LoyaltyProgram;
import com.loyalty.capstone.domain.PointTransaction;
import com.loyalty.capstone.store.EarningDb;
import com.loyalty.capstone.store.IdempotencyStore;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * I-4 container 'Earning Engine Service'.
 * Sole writer of Earning DB and the enforcement point for CON.1.
 * Serves the FIFO debit and the CON.3 restoration to Redemption Engine Service.
 *
 * It never reads Tiering DB. The earn multiplier comes from a local projection fed by the
 * asynchronous 'tiering.tier_changed' event, which is contract row CT-08 and the I-8 async pattern.
 */
public final class EarningEngineService {

    public static final String CONTAINER = "Earning Engine Service";

    private static final String DEFAULT_TIER = "SILVER";

    private final EarningDb earningDb;
    private final IdempotencyStore idempotencyStore;
    private final ProgramManagementService programManagementService;
    private final MessageBroker messageBroker;
    private final Clock clock;
    private final AtomicLong sequence = new AtomicLong();

    /** Local read model of MemberTier. Tiering DB stays owned by Tiering System Service. */
    private final Map<String, String> tierProjection = new HashMap<>();

    public EarningEngineService(EarningDb earningDb, IdempotencyStore idempotencyStore,
                                ProgramManagementService programManagementService,
                                MessageBroker messageBroker, Clock clock) {
        this.earningDb = earningDb;
        this.idempotencyStore = idempotencyStore;
        this.programManagementService = programManagementService;
        this.messageBroker = messageBroker;
        this.clock = clock;
        this.messageBroker.subscribe(Topics.TRANSACTION_SETTLED, this::onSettledTransaction);
        this.messageBroker.subscribe(Topics.TIERING_TIER_CHANGED, this::onTierChanged);
    }

    /** CT-08: consume 'tiering.tier_changed' and update the local projection. */
    private void onTierChanged(Map<String, Object> event) {
        tierProjection.put(String.valueOf(event.get("memberId")), String.valueOf(event.get("toTier")));
    }

    /** The tier this service believes the member holds, from the projection only. */
    public String projectedTier(String memberId) {
        return tierProjection.getOrDefault(memberId, DEFAULT_TIER);
    }

    private void onSettledTransaction(Map<String, Object> event) {
        recordEarn(String.valueOf(event.get("sourceTransactionId")),
                String.valueOf(event.get("memberId")),
                String.valueOf(event.get("programId")),
                ((Number) event.get("amount")).doubleValue());
    }

    /**
     * UC-LB-01. The duplicate check happens before any write, so a repeated source transaction
     * returns the original result and never creates a second PointTransaction.
     */
    public EarnResult recordEarn(String sourceTransactionId, String memberId, String programId, double amount) {
        String idempotencyKey = sourceTransactionId + "|" + programId;
        String existing = idempotencyStore.claimOrGetExisting(idempotencyKey, "pending");
        if (existing != null) {
            List<PointTransaction> already = earningDb.findBySourceTransaction(sourceTransactionId);
            long awarded = already.isEmpty() ? 0L : already.get(0).points;
            String id = already.isEmpty() ? existing : already.get(0).pointTransactionId;
            return new EarnResult(id, awarded, true);
        }

        LoyaltyProgram program = programManagementService.program(programId);
        double multiplier = program.tierMultiplier(projectedTier(memberId));
        Campaign campaign = programManagementService.winningCampaign(programId);
        double campaignMultiplier = campaign == null ? 1.0d : campaign.multiplier;

        long points = (long) Math.floor(amount * program.earnRatePointsPerCurrencyUnit
                * multiplier * campaignMultiplier);

        Instant now = clock.instant();
        String pointTransactionId = "PT-" + sequence.incrementAndGet();
        PointTransaction transaction = new PointTransaction(pointTransactionId, memberId, sourceTransactionId,
                "EARN", points, now, now.plus(program.pointLifetimeDays, ChronoUnit.DAYS));

        earningDb.appendTransaction(CONTAINER, transaction);
        earningDb.balance(CONTAINER, memberId).credit(points);
        idempotencyStore.remember(idempotencyKey, pointTransactionId);

        publishQualifyingPoints(sourceTransactionId, memberId, points);
        publishChangeEvent(transaction);

        return new EarnResult(pointTransactionId, points, false);
    }

    private void publishQualifyingPoints(String sourceTransactionId, String memberId, long points) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("eventId", "QP-" + sourceTransactionId);
        event.put("memberId", memberId);
        event.put("qualifyingPoints", points);
        messageBroker.publish(Topics.EARNING_QP_ACCRUED, event);
    }

    private void publishChangeEvent(PointTransaction transaction) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("pointTransactionId", transaction.pointTransactionId);
        event.put("memberId", transaction.memberId);
        event.put("outstandingPoints", transaction.remainingPoints());
        messageBroker.publish(Topics.CDC_PLATFORM_EVENTS, event);
    }

    public long availablePoints(String memberId) {
        return earningDb.readConfirmedPoints(memberId);
    }

    /** Serves Redemption Engine Service: oldest batches first. */
    public List<DebitAllocation> debitFifo(String memberId, long pointsRequested) {
        List<DebitAllocation> allocations = new ArrayList<>();
        long remaining = pointsRequested;
        for (PointTransaction batch : earningDb.openBatchesOldestFirst(memberId)) {
            if (remaining <= 0) break;
            long take = Math.min(remaining, batch.remainingPoints());
            batch.consume(take);
            allocations.add(new DebitAllocation(batch.pointTransactionId, take, batch.earnDate, batch.expiryDate));
            remaining -= take;
        }
        if (remaining > 0) {
            restore(allocations);
            throw new IllegalStateException("insufficient points for FIFO debit");
        }
        earningDb.balance(CONTAINER, memberId).debit(pointsRequested);
        for (DebitAllocation allocation : allocations) {
            publishChangeEvent(earningDb.transaction(allocation.pointTransactionId));
        }
        return allocations;
    }

    /**
     * CON.3 compensating action. Points go back onto the same batches, so the original
     * earn date and expiry are preserved instead of a new batch being created.
     */
    public void restore(List<DebitAllocation> allocations) {
        long total = 0L;
        String memberId = null;
        for (DebitAllocation allocation : allocations) {
            PointTransaction batch = earningDb.transaction(allocation.pointTransactionId);
            batch.restore(allocation.points);
            total += allocation.points;
            memberId = batch.memberId;
            publishChangeEvent(batch);
        }
        if (memberId != null && total > 0) {
            earningDb.balance(CONTAINER, memberId).credit(total);
        }
    }
}
