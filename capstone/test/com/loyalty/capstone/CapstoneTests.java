package com.loyalty.capstone;

import com.loyalty.capstone.domain.DebitAllocation;
import com.loyalty.capstone.domain.IllegalStateTransition;
import com.loyalty.capstone.domain.OrderState;
import com.loyalty.capstone.domain.PointTransaction;
import com.loyalty.capstone.domain.RedemptionOrder;
import com.loyalty.capstone.gateway.ApiGateway;
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

        // ---- I-6 transitions of RedemptionOrder. SUT: Redemption Engine Service ----

        runner.check("G6-T01", "PENDING -> IN_PROGRESS via Redemption Engine Service", () -> {
            MutableClock clock = new MutableClock(START);
            Platform platform = new Platform(clock);
            platform.earningEngineService.recordEarn("TXN-T1", "M-T1", Platform.PROGRAM_ID, 200d); // credits 400 pts
            
            RedemptionOrder order = platform.redemptionEngineService
                    .createAndReserveOrder("M-T1", Platform.REWARD_DIGITAL_VOUCHER);
            assertEquals(OrderState.IN_PROGRESS, order.state(), "state after FIFO reservation by service");
            assertEquals(1, order.allocations().size(), "reserved FIFO allocations recorded");
            assertEquals(100L, platform.earningEngineService.availablePoints("M-T1"), "300 pts reserved/debited from balance");
        });

        runner.check("G6-T02", "PENDING -> CANCELLED via Redemption Engine Service", () -> {
            MutableClock clock = new MutableClock(START);
            Platform platform = new Platform(clock);
            // Case A: service cancel on insufficient balance
            RedemptionOrder order = platform.redemptionEngineService
                    .createAndReserveOrder("M-POOR-T2", Platform.REWARD_DIGITAL_VOUCHER);
            assertEquals(OrderState.CANCELLED, order.state(), "state after cancellation on insufficient balance");
            assertTrue(order.state().isTerminal(), "CANCELLED is terminal in I-6");
            
            // Case B: direct type cancellation
            RedemptionOrder manualOrder = newOrder();
            manualOrder.cancel("member cancelled");
            assertEquals(OrderState.CANCELLED, manualOrder.state(), "manual cancel state");
        });

        runner.check("G6-T03", "IN_PROGRESS -> FULFILLED via Redemption Engine Service", () -> {
            MutableClock clock = new MutableClock(START);
            Platform platform = new Platform(clock);
            platform.earningEngineService.recordEarn("TXN-T3", "M-T3", Platform.PROGRAM_ID, 200d);
            
            RedemptionOrder order = platform.redemptionEngineService
                    .createAndReserveOrder("M-T3", Platform.REWARD_DIGITAL_VOUCHER);
            assertEquals(OrderState.IN_PROGRESS, order.state(), "in progress before delivery");
            
            platform.redemptionEngineService.fulfillOrder(order);
            assertEquals(OrderState.FULFILLED, order.state(), "state after delivery confirmed by partner");
            assertTrue(order.state().isTerminal(), "FULFILLED is terminal in I-6");
        });

        runner.check("G6-T04", "IN_PROGRESS -> FAILED via Redemption Engine Service", () -> {
            MutableClock clock = new MutableClock(START);
            Platform platform = new Platform(clock);
            platform.earningEngineService.recordEarn("TXN-T4", "M-T4", Platform.PROGRAM_ID, 200d);
            
            RedemptionOrder order = platform.redemptionEngineService
                    .createAndReserveOrder("M-T4", Platform.REWARD_DIGITAL_VOUCHER);
            assertEquals(OrderState.IN_PROGRESS, order.state(), "in progress before delivery");
            
            platform.redemptionEngineService.failOrder(order, "partner delivery failed");
            assertEquals(OrderState.FAILED, order.state(), "state after delivery failure");
        });

        runner.check("G6-T05", "FAILED -> REVERSED via Redemption Engine Service (CON.3)", () -> {
            MutableClock clock = new MutableClock(START);
            Platform platform = new Platform(clock);
            platform.earningEngineService.recordEarn("TXN-T5", "M-T5", Platform.PROGRAM_ID, 200d);
            
            RedemptionOrder order = platform.redemptionEngineService
                    .createAndReserveOrder("M-T5", Platform.REWARD_DIGITAL_VOUCHER);
            assertEquals(100L, platform.earningEngineService.availablePoints("M-T5"), "300 debited");
            
            platform.redemptionEngineService.failOrder(order, "partner delivery failed");
            assertEquals(OrderState.FAILED, order.state(), "order marked FAILED");
            
            platform.redemptionEngineService.reverseOrder(order);
            assertEquals(OrderState.REVERSED, order.state(), "order marked REVERSED under CON.3");
            assertEquals(400L, platform.earningEngineService.availablePoints("M-T5"), "points restored to original batch");
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

        runner.check("NEG-I5-02", "I-5 anti-tamper: earn payload sending forged tier field is rejected; server uses authoritative tier", () -> {
            MutableClock clock = new MutableClock(START);
            Platform platform = new Platform(clock);
            ApiGateway gateway = new ApiGateway(platform);
            try {
                gateway.start(0);
                String url = "http://localhost:" + gateway.port() + "/partner-earn";
                // Malicious client sends a forged tier: PLATINUM (2.0x) along with 100 spend amount
                String forgedPayload = "{\"sourceTransactionId\":\"TXN-FORGE-01\",\"memberId\":\"M-FORGE\",\"amount\":100,\"tier\":\"PLATINUM\"}";
                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder(java.net.URI.create(url))
                        .header("Content-Type", "application/json")
                        .POST(java.net.http.HttpRequest.BodyPublishers.ofString(forgedPayload))
                        .build();
                java.net.http.HttpResponse<String> response = java.net.http.HttpClient.newHttpClient()
                        .send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

                assertEquals(201, response.statusCode(), "earn transaction created");
                // Authoritative SILVER (1.0x) * 2.0x campaign * 100 amount = 200 points. If forged PLATINUM (2.0x) was trusted, it would be 400 points.
                assertTrue(response.body().contains("\"pointsAwarded\":200"),
                        "awarded points strictly use authoritative SILVER (1.0x = 200 pts), NOT forged PLATINUM (400 pts)");
                assertEquals(200L, platform.earningEngineService.availablePoints("M-FORGE"),
                        "ledger balance reflects authoritative tier calculation only");
                assertEquals("SILVER", platform.earningEngineService.projectedTier("M-FORGE"),
                        "member tier projection remains authoritative SILVER");
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                gateway.stop();
            }
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
