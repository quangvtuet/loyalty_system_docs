package com.loyalty.capstone;

import java.net.http.HttpResponse;

import static com.loyalty.capstone.TestRunner.assertEquals;
import static com.loyalty.capstone.TestRunner.assertTrue;

/**
 * HTTP tests against the running API Gateway.
 * They prove the status codes and bodies documented in openapi.yaml are what the runtime returns.
 * SUT for every row here is the I-4 container 'API Gateway'.
 */
public final class HttpContractTests {

    private HttpContractTests() { }

    static void register(TestRunner runner) {

        runner.check("HTTP-01", "POST /partner-earn returns 201 then 409 on the duplicate", () -> {
            HttpTestClient.withRuntime((base, clock, platform) -> {
                HttpResponse<String> first = HttpTestClient.post(base + "/partner-earn",
                        "{\"sourceTransactionId\":\"TXN-H01\",\"memberId\":\"M-H01\",\"amount\":200}");
                assertEquals(201, first.statusCode(), "first submission is created");
                assertTrue(first.body().contains("\"duplicate\":false"), "first is not a duplicate");
                assertTrue(first.body().contains("\"pointsAwarded\":400"), "400 points awarded");

                HttpResponse<String> repeat = HttpTestClient.post(base + "/partner-earn",
                        "{\"sourceTransactionId\":\"TXN-H01\",\"memberId\":\"M-H01\",\"amount\":200}");
                assertEquals(409, repeat.statusCode(), "CON.1 duplicate is refused with 409");
                assertTrue(repeat.body().contains("\"duplicate\":true"), "body flags the duplicate");
                assertTrue(repeat.body().contains("\"constraint\":\"CON.1\""), "body names CON.1");
                assertEquals(1, platform.earningDb.findBySourceTransaction("TXN-H01").size(),
                        "still exactly one ledger entry");
            });
        });

        runner.check("HTTP-02", "POST /redemptions returns 201 FULFILLED on the happy path", () -> {
            HttpTestClient.withRuntime((base, clock, platform) -> {
                HttpTestClient.post(base + "/partner-earn",
                        "{\"sourceTransactionId\":\"TXN-H02\",\"memberId\":\"M-H02\",\"amount\":200}");
                HttpResponse<String> response = HttpTestClient.post(base + "/redemptions",
                        "{\"memberId\":\"M-H02\",\"rewardItemId\":\"" + Platform.REWARD_DIGITAL_VOUCHER + "\"}");
                assertEquals(201, response.statusCode(), "accepted order is created");
                assertTrue(response.body().contains("\"state\":\"FULFILLED\""), "order ends FULFILLED");
            });
        });

        runner.check("HTTP-03", "POST /redemptions returns 422 CANCELLED when the alt fires", () -> {
            HttpTestClient.withRuntime((base, clock, platform) -> {
                HttpResponse<String> poor = HttpTestClient.post(base + "/redemptions",
                        "{\"memberId\":\"M-H03\",\"rewardItemId\":\"" + Platform.REWARD_DIGITAL_VOUCHER + "\"}");
                assertEquals(422, poor.statusCode(), "insufficient balance is 422");
                assertTrue(poor.body().contains("\"state\":\"CANCELLED\""), "order is CANCELLED");

                HttpTestClient.post(base + "/partner-earn",
                        "{\"sourceTransactionId\":\"TXN-H03\",\"memberId\":\"M-H03\",\"amount\":400}");
                HttpResponse<String> ineligible = HttpTestClient.post(base + "/redemptions",
                        "{\"memberId\":\"M-H03\",\"rewardItemId\":\"" + Platform.REWARD_PLATINUM_LOUNGE + "\"}");
                assertEquals(422, ineligible.statusCode(), "tier-ineligible reward is 422");
                assertTrue(ineligible.body().contains("\"state\":\"CANCELLED\""), "order is CANCELLED");
            });
        });

        runner.check("HTTP-04", "POST /redemptions returns 201 REVERSED with CON.3 after partner failure", () -> {
            HttpTestClient.withRuntime((base, clock, platform) -> {
                HttpTestClient.post(base + "/partner-earn",
                        "{\"sourceTransactionId\":\"TXN-H04\",\"memberId\":\"M-H04\",\"amount\":200}");
                HttpResponse<String> response = HttpTestClient.post(base + "/redemptions",
                        "{\"memberId\":\"M-H04\",\"rewardItemId\":\"" + Platform.REWARD_OUT_OF_STOCK + "\"}");
                assertEquals(201, response.statusCode(), "the order was accepted before the failure");
                assertTrue(response.body().contains("\"state\":\"REVERSED\""), "order ends REVERSED");
                assertTrue(response.body().contains("\"constraint\":\"CON.3\""), "body names CON.3");
                assertEquals(400L, platform.earningEngineService.availablePoints("M-H04"),
                        "points restored by the compensating action");
            });
        });

        runner.check("HTTP-05", "GET /reports/point-liability returns 200 fresh then 200 stale under CON.4", () -> {
            HttpTestClient.withRuntime((base, clock, platform) -> {
                HttpTestClient.post(base + "/partner-earn",
                        "{\"sourceTransactionId\":\"TXN-H05\",\"memberId\":\"M-H05\",\"amount\":100}");

                HttpResponse<String> fresh = HttpTestClient.get(base + "/reports/point-liability");
                assertEquals(200, fresh.statusCode(), "report is served");
                assertTrue(fresh.body().contains("\"stale\":false"), "fresh data is not marked stale");
                assertTrue(fresh.body().contains("\"outstandingPoints\":200"), "outstanding points reported");

                clock.advanceSeconds(601);
                HttpResponse<String> stale = HttpTestClient.get(base + "/reports/point-liability");
                assertEquals(200, stale.statusCode(), "a stale report is still served, not an error");
                assertTrue(stale.body().contains("\"stale\":true"), "CON.4 marks the report stale");
                assertTrue(stale.body().contains("\"constraint\":\"CON.4\""), "body names CON.4");
            });
        });

        runner.check("HTTP-06", "wrong method and missing fields match the documented problem responses", () -> {
            HttpTestClient.withRuntime((base, clock, platform) -> {
                assertEquals(405, HttpTestClient.get(base + "/partner-earn").statusCode(), "GET on an earn route is 405");
                assertEquals(400, HttpTestClient.post(base + "/redemptions", "{\"memberId\":\"M-H06\"}").statusCode(),
                        "missing rewardItemId is 400");
                assertEquals(404, HttpTestClient.post(base + "/redemptions",
                                "{\"memberId\":\"M-H06\",\"rewardItemId\":\"RI-DOES-NOT-EXIST\"}").statusCode(),
                        "unknown reward item is 404");
            });
        });
    }

}
