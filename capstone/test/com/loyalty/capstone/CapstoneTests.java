package com.loyalty.capstone;

import com.loyalty.capstone.domain.DebitAllocation;
import com.loyalty.capstone.domain.IllegalStateTransition;
import com.loyalty.capstone.domain.OrderState;
import com.loyalty.capstone.domain.PointTransaction;
import com.loyalty.capstone.domain.RedemptionOrder;
import com.loyalty.capstone.domain.RewardItem;
import com.loyalty.capstone.service.EarnResult;
import com.loyalty.capstone.service.PointLiabilityReport;
import com.loyalty.capstone.store.OwnershipViolation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.loyalty.capstone.TestRunner.assertEquals;
import static com.loyalty.capstone.TestRunner.assertThrows;
import static com.loyalty.capstone.TestRunner.assertTrue;

/**
 * G6 suite. Coverage ids are the Lab 10 G6 rows.
 * SUT names are I-4 container names; see spec-trace.md.
 */
public final class CapstoneTests {

    private static final Instant START = Instant.parse("2026-08-22T09:00:00Z");

    public static void main(String[] args) {
        TestRunner runner = new TestRunner();

        // ---- I-6 transitions exercised through Redemption Engine Service ----

        runner.check("G6-T01", "PENDING -> IN_PROGRESS", () -> {
            Platform platform = new Platform(new MutableClock(START));
            platform.earningEngineService.recordEarn("TXN-T01", "M-T01", Platform.PROGRAM_ID, 200d);
            RewardItem item = platform.redemptionEngineService.catalogue().get(0);
            RedemptionOrder order = new RedemptionOrder("ORD-T01", "M-T01", item.rewardItemId, item.pointsCost, "SILVER");
            assertEquals(OrderState.PENDING, order.state(), "initial order state is PENDING");
            List<DebitAllocation> allocations = platform.earningEngineService.debitFifo("M-T01", item.pointsCost);
            order.markInProgress(allocations);
            assertEquals(OrderState.IN_PROGRESS, order.state(), "state after reservation is IN_PROGRESS");
            assertTrue(!order.state().isTerminal(), "IN_PROGRESS is intermediate in I-6");
            assertEquals(1, order.allocations().size(), "allocations attached on IN_PROGRESS");
        });

        runner.check("G6-T02", "PENDING -> CANCELLED", () -> {
            Platform platform = new Platform(new MutableClock(START));
            RewardItem item = platform.redemptionEngineService.catalogue().get(0);
            RedemptionOrder order = new RedemptionOrder("ORD-T02", "M-T02", item.rewardItemId, item.pointsCost, "SILVER");
            assertEquals(OrderState.PENDING, order.state(), "initial state is PENDING");
            order.cancel("insufficient points");
            assertEquals(OrderState.CANCELLED, order.state(), "state after cancellation");
            assertTrue(order.state().isTerminal(), "CANCELLED is terminal in I-6");
            assertEquals("insufficient points", order.reason(), "cancellation reason recorded");
        });

        runner.check("G6-T03", "IN_PROGRESS -> FULFILLED", () -> {
            Platform platform = new Platform(new MutableClock(START));
            platform.earningEngineService.recordEarn("TXN-T03", "M-T03", Platform.PROGRAM_ID, 200d);
            RewardItem item = platform.redemptionEngineService.catalogue().get(0);
            RedemptionOrder order = new RedemptionOrder("ORD-T03", "M-T03", item.rewardItemId, item.pointsCost, "SILVER");
            List<DebitAllocation> allocations = platform.earningEngineService.debitFifo("M-T03", item.pointsCost);
            order.markInProgress(allocations);
            assertEquals(OrderState.IN_PROGRESS, order.state(), "state is IN_PROGRESS");
            order.markFulfilled();
            assertEquals(OrderState.FULFILLED, order.state(), "state after delivery is FULFILLED");
            assertTrue(order.state().isTerminal(), "FULFILLED is terminal in I-6");
        });

        runner.check("G6-T04", "IN_PROGRESS -> FAILED", () -> {
            Platform platform = new Platform(new MutableClock(START));
            platform.earningEngineService.recordEarn("TXN-T04", "M-T04", Platform.PROGRAM_ID, 200d);
            RewardItem item = platform.redemptionEngineService.catalogue().get(0);
            RedemptionOrder order = new RedemptionOrder("ORD-T04", "M-T04", item.rewardItemId, item.pointsCost, "SILVER");
            List<DebitAllocation> allocations = platform.earningEngineService.debitFifo("M-T04", item.pointsCost);
            order.markInProgress(allocations);
            assertEquals(OrderState.IN_PROGRESS, order.state(), "state is IN_PROGRESS");
            order.markFailed("partner fulfillment failed: out of stock");
            assertEquals(OrderState.FAILED, order.state(), "state after failure is FAILED");
            assertTrue(!order.state().isTerminal(), "FAILED is intermediate in I-6 leading to REVERSED");
            assertEquals("partner fulfillment failed: out of stock", order.reason(), "failure reason recorded");
        });

        runner.check("G6-T05", "FAILED -> REVERSED", () -> {
            Platform platform = new Platform(new MutableClock(START));
            platform.earningEngineService.recordEarn("TXN-T05", "M-T05", Platform.PROGRAM_ID, 200d);
            RewardItem item = platform.redemptionEngineService.catalogue().get(0);
            RedemptionOrder order = new RedemptionOrder("ORD-T05", "M-T05", item.rewardItemId, item.pointsCost, "SILVER");
            List<DebitAllocation> allocations = platform.earningEngineService.debitFifo("M-T05", item.pointsCost);
            order.markInProgress(allocations);
            order.markFailed("partner fulfillment failed");
            assertEquals(OrderState.FAILED, order.state(), "state is FAILED");
            order.markReversed();
            assertEquals(OrderState.REVERSED, order.state(), "state after auto-reversal is REVERSED");
            assertTrue(order.state().isTerminal(), "REVERSED is terminal in I-6");
        });

        // ---- I-11 named alternates ----

        runner.check("G6-A01", "UC-LB-01 duplicate earn event is refused under CON.1", () -> {
            MutableClock clock = new MutableClock(START);
            Platform platform = new Platform(clock);

            EarnResult first = platform.earningEngineService.recordEarn("TXN-1", "M-1", Platform.PROGRAM_ID, 200d);
            EarnResult second = platform.earningEngineService.recordEarn("TXN-1", "M-1", Platform.PROGRAM_ID, 200d);

            assertTrue(!first.duplicate, "first posting is new");
            assertTrue(second.duplicate, "second posting is flagged duplicate");
            assertEquals(first.pointTransactionId, second.pointTransactionId, "original result returned");
            assertEquals(1, platform.earningDb.findBySourceTransaction("TXN-1").size(),
                    "CON.1: exactly one PointTransaction for one source transaction");
            assertEquals(400L, platform.earningEngineService.availablePoints("M-1"),
                    "balance credited once only");
        });

        runner.check("G6-A02", "UC-LB-02 insufficient balance and tier-ineligible reward are cancelled", () -> {
            MutableClock clock = new MutableClock(START);
            Platform platform = new Platform(clock);

            RedemptionOrder poor = platform.redemptionEngineService
                    .submitRedemption("M-POOR", Platform.REWARD_DIGITAL_VOUCHER);
            assertEquals(OrderState.CANCELLED, poor.state(), "no balance means cancelled");
            assertTrue(poor.reason().contains("insufficient"), "reason names the balance");

            platform.earningEngineService.recordEarn("TXN-2", "M-2", Platform.PROGRAM_ID, 400d);
            RedemptionOrder ineligible = platform.redemptionEngineService
                    .submitRedemption("M-2", Platform.REWARD_PLATINUM_LOUNGE);
            assertEquals(OrderState.CANCELLED, ineligible.state(), "tier below minimum means cancelled");
            assertTrue(ineligible.reason().contains("below required"), "reason names the tier");

            assertEquals(0, platform.partnerSystems.fulfillmentRequests(),
                    "no fulfillment is dispatched for a cancelled order");
        });

        runner.check("G6-A03", "UC-LB-02 fulfillment failure compensates under CON.3", () -> {
            MutableClock clock = new MutableClock(START);
            Platform platform = new Platform(clock);

            platform.earningEngineService.recordEarn("TXN-3", "M-3", Platform.PROGRAM_ID, 200d);
            List<PointTransaction> before = platform.earningDb.all();
            assertEquals(1, before.size(), "one earn batch exists");
            PointTransaction batch = before.get(0);
            Instant originalEarnDate = batch.earnDate;
            Instant originalExpiryDate = batch.expiryDate;

            clock.advanceSeconds(120);
            RedemptionOrder order = platform.redemptionEngineService
                    .submitRedemption("M-3", Platform.REWARD_OUT_OF_STOCK);

            assertEquals(OrderState.REVERSED, order.state(), "failed fulfillment ends REVERSED");
            assertEquals(400L, platform.earningEngineService.availablePoints("M-3"),
                    "points are back on the balance");
            assertEquals(400L, batch.remainingPoints(), "the original batch is whole again");
            assertEquals(1, platform.earningDb.all().size(),
                    "CON.3: restoration reuses the original batch, it does not create a new one");
            assertEquals(originalEarnDate, batch.earnDate, "CON.3: original earn date preserved");
            assertEquals(originalExpiryDate, batch.expiryDate, "CON.3: original expiry preserved");
            assertEquals(1, platform.crmNotificationGateway.reversalNotices().size(),
                    "member is notified of the reversal");
        });

        runner.check("G6-A04", "UC-LB-03 replayed qualifying accrual does not move the tier twice", () -> {
            MutableClock clock = new MutableClock(START);
            Platform platform = new Platform(clock);

            platform.earningEngineService.recordEarn("TXN-4", "M-4", Platform.PROGRAM_ID, 600d);
            long afterFirst = platform.tieringSystemService.qualifyingPoints("M-4");
            assertEquals(1200L, afterFirst, "qualifying points accrued once");
            assertEquals("GOLD", platform.tieringSystemService.currentTier("M-4"), "threshold crossed");

            java.util.Map<String, Object> replay = new java.util.LinkedHashMap<>();
            replay.put("eventId", "QP-TXN-4");
            replay.put("memberId", "M-4");
            replay.put("qualifyingPoints", 1200L);
            platform.messageBroker.publish(com.loyalty.capstone.broker.Topics.EARNING_QP_ACCRUED, replay);

            assertEquals(afterFirst, platform.tieringSystemService.qualifyingPoints("M-4"),
                    "Tiering System Service ignores the replayed event id");
            assertEquals("GOLD", platform.tieringSystemService.currentTier("M-4"), "tier unchanged on replay");

            platform.earningEngineService.recordEarn("TXN-4", "M-4", Platform.PROGRAM_ID, 600d);
            assertEquals(afterFirst, platform.tieringSystemService.qualifyingPoints("M-4"),
                    "a duplicate earn also produces no second accrual");
        });

        runner.check("G6-A05", "UC-LB-04 stale warehouse data is reported as stale under CON.4", () -> {
            MutableClock clock = new MutableClock(START);
            Platform platform = new Platform(clock);
            platform.earningEngineService.recordEarn("TXN-5", "M-5", Platform.PROGRAM_ID, 100d);

            PointLiabilityReport fresh = platform.analyticsReportingService.pointLiabilityReport();
            assertTrue(!fresh.stale, "report is fresh immediately after ingest");
            assertEquals(200L, fresh.outstandingPoints, "outstanding points come from the warehouse");
            assertTrue(Math.abs(fresh.liabilityUsd - 2.0d) < 0.0001d, "liability is points times cost per point");
            assertEquals(0, platform.crmNotificationGateway.staleReportAlerts().size(), "no alert while fresh");

            clock.advanceSeconds(601);
            PointLiabilityReport stale = platform.analyticsReportingService.pointLiabilityReport();
            assertTrue(stale.stale, "CON.4: data older than ten minutes is marked stale");
            assertEquals(1, platform.crmNotificationGateway.staleReportAlerts().size(),
                    "CON.4 compensating action: Finance is alerted");
        });

        // ---- Negative tests on I-5 hard rules, the I-9 forbidden path, and I-6 ----

        runner.check("NEG-I9-01", "I-9 forbidden path: an external cannot write a service-owned store", () -> {
            MutableClock clock = new MutableClock(START);
            Platform platform = new Platform(clock);
            PointTransaction forged = new PointTransaction("PT-FORGED", "M-9", "TXN-9", "EARN",
                    9999L, clock.instant(), clock.instant());

            assertThrows(OwnershipViolation.class,
                    () -> platform.earningDb.appendTransaction("Partner Systems", forged),
                    "Partner Systems must not write Earning DB");
            assertThrows(OwnershipViolation.class,
                    () -> platform.earningDb.appendTransaction("Core Banking System", forged),
                    "Core Banking System must not write Earning DB");
            assertEquals(0, platform.earningDb.all().size(), "no forged ledger entry survived");
        });

        runner.check("NEG-I5-01", "CON.2: one service cannot write another service's store", () -> {
            MutableClock clock = new MutableClock(START);
            Platform platform = new Platform(clock);

            assertThrows(OwnershipViolation.class,
                    () -> platform.tieringDb.tierForWrite("Redemption Engine Service", "M-1"),
                    "Redemption Engine Service must not write Tiering DB");
            assertThrows(OwnershipViolation.class,
                    () -> platform.redemptionDb.saveOrder("API Gateway", newOrder()),
                    "API Gateway must not write Redemption DB");
            assertThrows(OwnershipViolation.class,
                    () -> platform.dataWarehouse.upsertFact("Earning Engine Service", null),
                    "Earning Engine Service must not write Data Warehouse");
        });

        runner.check("NEG-I6-01", "a transition outside I-6 is refused by the type", () -> {
            RedemptionOrder order = newOrder();
            assertThrows(IllegalStateTransition.class, order::markFulfilled,
                    "PENDING cannot jump to FULFILLED");
            assertThrows(IllegalStateTransition.class, order::markReversed,
                    "PENDING cannot jump to REVERSED");
            order.cancel("member cancelled");
            assertThrows(IllegalStateTransition.class, () -> order.markInProgress(oneAllocation()),
                    "CANCELLED is terminal");
        });

        // ---- I-11 happy paths end to end ----

        runner.check("UC-LB-01", "settled transaction becomes a PointTransaction", () -> {
            MutableClock clock = new MutableClock(START);
            Platform platform = new Platform(clock);
            platform.coreBankingSystem.publishSettledTransaction("TXN-H1", "M-H1", Platform.PROGRAM_ID, 50d);
            assertEquals(1, platform.earningDb.findBySourceTransaction("TXN-H1").size(), "one ledger entry");
            assertEquals(100L, platform.earningEngineService.availablePoints("M-H1"),
                    "1 point per unit, doubled by the active campaign");
        });

        runner.check("UC-LB-02", "redeem reward with FIFO consumes the oldest batch first", () -> {
            MutableClock clock = new MutableClock(START);
            Platform platform = new Platform(clock);
            platform.earningEngineService.recordEarn("TXN-H2a", "M-H2", Platform.PROGRAM_ID, 100d);
            clock.advanceSeconds(3600);
            platform.earningEngineService.recordEarn("TXN-H2b", "M-H2", Platform.PROGRAM_ID, 100d);

            List<PointTransaction> batches = platform.earningDb.openBatchesOldestFirst("M-H2");
            PointTransaction oldest = batches.get(0);
            PointTransaction newest = batches.get(1);

            RedemptionOrder order = platform.redemptionEngineService
                    .submitRedemption("M-H2", Platform.REWARD_DIGITAL_VOUCHER);

            assertEquals(OrderState.FULFILLED, order.state(), "happy path ends FULFILLED");
            assertEquals(0L, oldest.remainingPoints(), "oldest batch fully consumed first");
            assertEquals(100L, newest.remainingPoints(), "newest batch only partly consumed");
            assertEquals(100L, platform.earningEngineService.availablePoints("M-H2"), "balance reduced by 300");
        });

        runner.check("UC-LB-03", "crossing the threshold upgrades the tier", () -> {
            MutableClock clock = new MutableClock(START);
            Platform platform = new Platform(clock);
            assertEquals("SILVER", platform.tieringSystemService.currentTier("M-H3"), "starts SILVER");
            platform.earningEngineService.recordEarn("TXN-H3", "M-H3", Platform.PROGRAM_ID, 1600d);
            assertEquals("PLATINUM", platform.tieringSystemService.currentTier("M-H3"),
                    "3200 qualifying points reaches PLATINUM");
        });

        runner.check("UC-LB-04", "liability equals outstanding points times cost per point", () -> {
            MutableClock clock = new MutableClock(START);
            Platform platform = new Platform(clock);
            platform.earningEngineService.recordEarn("TXN-H4", "M-H4", Platform.PROGRAM_ID, 500d);
            PointLiabilityReport report = platform.analyticsReportingService.pointLiabilityReport();
            assertEquals(1000L, report.outstandingPoints, "outstanding points");
            assertTrue(Math.abs(report.liabilityUsd - 10.0d) < 0.0001d, "liability in USD");
            assertTrue(!report.stale, "warehouse is current");
        });

        HttpContractTests.register(runner);
        OpenApiDriftTests.register(runner);

        System.exit(runner.report());
    }

    private static RedemptionOrder newOrder() {
        return new RedemptionOrder("RO-TEST", "M-TEST", Platform.REWARD_DIGITAL_VOUCHER, 300L, "SILVER");
    }

    private static List<DebitAllocation> oneAllocation() {
        List<DebitAllocation> allocations = new ArrayList<>();
        allocations.add(new DebitAllocation("PT-TEST", 300L, START, START.plusSeconds(86400)));
        return Collections.unmodifiableList(allocations);
    }
}
