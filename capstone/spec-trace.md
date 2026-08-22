# Spec-trace — capstone runtime

Every in-scope path ties to an OpenAPI operation and a test id. Every Lab 3 / G6 row that the four I-11 use cases do not need is listed N/A rather than silently dropped.

Test ids are the coverage ids printed by the suite in `test/com/loyalty/capstone/` — `CapstoneTests` (entry point), `HttpContractTests`, `OpenApiDriftTests`. The G6 ids are the Lab 10 rows.

---

## 1. I-11 use cases

| Use case | Path | OpenAPI operation | Code entry point | Test id |
|---|---|---|---|---|
| UC-LB-01 Process settled earn event | Happy path | `SubmitPartnerEarn` (`POST /partner-earn`) | `EarningEngineService.recordEarn` | `UC-LB-01` |
| UC-LB-01 | `alt` duplicate event under CON.1 | `SubmitPartnerEarn` → 409 | `EarningEngineService.recordEarn` duplicate branch | `G6-A01` |
| UC-LB-02 Redeem reward with FIFO | Happy path | `SubmitRedemption` (`POST /redemptions`) → 201 | `RedemptionEngineService.submitRedemption` | `UC-LB-02` |
| UC-LB-02 | `alt` insufficient balance | `SubmitRedemption` → 422 | `TierAndBalanceValidationModule.validate` | `G6-A02` |
| UC-LB-02 | `alt` tier-ineligible reward | `SubmitRedemption` → 422 | `TierAndBalanceValidationModule.validate` | `G6-A02` |
| UC-LB-02 | `alt` partner fulfillment failure under CON.3 | `SubmitRedemption` → 201 with `state=REVERSED` | `FulfillmentCoordinationModule.dispatch` | `G6-A03` |
| UC-LB-03 Apply tier upgrade | Happy path | none — async, driven by `earning.qp_accrued` (CT-05, CT-06) | `TieringSystemService.onQualifyingPointsAccrued` | `UC-LB-03` |
| UC-LB-03 | `alt` replayed event ignored | none — async | `TieringSystemService` replay guard | `G6-A04` |
| UC-LB-04 Generate point liability report | Happy path | `RequestReport` (`GET /reports/point-liability`) → 200, `stale=false` | `AnalyticsReportingService.pointLiabilityReport` | `UC-LB-04` |
| UC-LB-04 | `alt` warehouse stale beyond ten minutes under CON.4 | `RequestReport` → 200, `stale=true` | `AnalyticsReportingService.pointLiabilityReport` | `G6-A05` |

UC-LB-03 has no OpenAPI operation because no Lab 3 contract row exposes it through API Gateway. It is reached only over the asynchronous CT-05 / CT-06 pair, and it is proven by running tests rather than by an endpoint. Adding a route for it would put an operation in the public contract that the register does not have.

---

## 2. G6 coverage from Lab 10

| Lab 10 G6 id | What it covers | SUT (I-4 name) | Test id | Executes |
|---|---|---|---|---|
| G6-T01 | `PENDING -> IN_PROGRESS` | Redemption Engine Service | `G6-T01` | Yes |
| G6-T02 | `PENDING -> CANCELLED` | Redemption Engine Service | `G6-T02` | Yes |
| G6-T03 | `IN_PROGRESS -> FULFILLED` | Redemption Engine Service | `G6-T03` | Yes |
| G6-T04 | `IN_PROGRESS -> FAILED` | Redemption Engine Service | `G6-T04` | Yes |
| G6-T05 | `FAILED -> REVERSED` | Redemption Engine Service | `G6-T05` | Yes |
| G6-A01 | UC-LB-01 duplicate under CON.1 | Earning Engine Service | `G6-A01` | Yes |
| G6-A02 | UC-LB-02 insufficient balance or tier-ineligible | Redemption Engine Service | `G6-A02` | Yes |
| G6-A03 | UC-LB-02 partner failure and CON.3 compensation | Redemption Engine Service | `G6-A03` | Yes |
| G6-A04 | UC-LB-03 replayed event | Tiering System Service | `G6-A04` | Yes |
| G6-A05 | UC-LB-04 stale beyond ten minutes under CON.4 | Analytics & Reporting Service | `G6-A05` | Yes |

Ten of ten Lab 10 G6 rows execute. None is a checklist entry.

### 2.1 HTTP contract tests

Added after the capstone review, which noted that module-level tests did not prove the documented HTTP behaviour. These start the gateway on an ephemeral port and make real requests. SUT is the I-4 container `API Gateway`.

| Test id | Operation | Asserts |
|---|---|---|
| `HTTP-01` | `SubmitPartnerEarn` | 201 with `duplicate=false`, then 409 with `constraint=CON.1`; still one ledger entry |
| `HTTP-02` | `SubmitRedemption` | 201 with `state=FULFILLED` on the happy path |
| `HTTP-03` | `SubmitRedemption` | 422 with `state=CANCELLED` for insufficient balance and for a tier-ineligible reward |
| `HTTP-04` | `SubmitRedemption` | 201 with `state=REVERSED` and `constraint=CON.3`; points restored |
| `HTTP-05` | `RequestReport` | 200 `stale=false`, then 200 `stale=true` with `constraint=CON.4` after the clock advances |
| `HTTP-06` | all three | 405 on a wrong method, 400 on a missing field, 404 on an unknown reward item |

### 2.2 G4 drift guard

Added after the review, which noted the drift check was manual. These read `openapi.yaml` and compare it with the runtime.

| Test id | Asserts |
|---|---|
| `G4-D01` | The documented path set equals the served route set — no extra and no missing operation |
| `G4-D02` | Every status the runtime returns is documented for that path |
| `G4-D03` | Every documented status is one the runtime can actually produce |
| `G4-D04` | The documented order-state enum is exactly the six I-6 states |

---

## 3. G5 — each named `alt` runs the Lab 3 exception spec

A named error status alone does not pass G5, so each row below names the compensating action and asserts it.

| Lab 3 exception | Trigger | Compensating action | Who performs it | Asserted by |
|---|---|---|---|---|
| EXC-01 (CON.1) | Source transaction already rewarded | No second `PointTransaction`; the original result is returned | Earning Engine Service | `G6-A01` asserts exactly one ledger row and one credit |
| EXC-03 (CON.1) | Same qualifying accrual delivered twice | Replay ignored; `MemberTier` not moved again | Tiering System Service | `G6-A04` asserts qualifying points and tier unchanged |
| EXC-04 (CON.2) | A non-owner attempts a write | Write refused | The owning container | `NEG-I5-01`, `NEG-I9-01` |
| EXC-05 (CON.3) | Partner Systems fail after the debit | `IN_PROGRESS -> FAILED -> REVERSED`; original batches restored with original earn date and expiry; member notified | Redemption Engine Service, restoration by Earning Engine Service | `G6-A03` asserts the batch is whole, **no new batch was created**, earn date and expiry are unchanged, and a reversal notice was sent |
| EXC-07 (CON.4) | Warehouse more than ten minutes behind | Report marked stale; Finance alerted | Analytics & Reporting Service | `G6-A05` asserts `stale=true` and one alert |

---

## 4. I-5 hard rules and the I-9 forbidden path

The brief requires a test that **attempts** the violation and a runtime that rejects it.

| Rule | Attempt made by the test | Runtime response | Test id |
|---|---|---|---|
| I-9 forbidden path — Partner Systems writes Earning DB | `earningDb.appendTransaction("Partner Systems", forged)` | `OwnershipViolation` thrown; no ledger row created | `NEG-I9-01` |
| I-9 forbidden path — Core Banking System writes Earning DB | `earningDb.appendTransaction("Core Banking System", forged)` | `OwnershipViolation` thrown | `NEG-I9-01` |
| CON.2 — Redemption Engine Service writes Tiering DB | `tieringDb.tierForWrite("Redemption Engine Service", …)` | `OwnershipViolation` thrown | `NEG-I5-01` |
| CON.2 — API Gateway writes Redemption DB | `redemptionDb.saveOrder("API Gateway", …)` | `OwnershipViolation` thrown | `NEG-I5-01` |
| CON.2 — Earning Engine Service writes Data Warehouse | `dataWarehouse.upsertFact("Earning Engine Service", …)` | `OwnershipViolation` thrown | `NEG-I5-01` |
| CON.1 — the same source transaction is posted twice | Second `recordEarn` with the same source transaction | Duplicate result returned, one ledger row only | `G6-A01` |
| I-6 — a transition outside the six states | `markFulfilled` from `PENDING`, `markReversed` from `PENDING`, `markInProgress` from `CANCELLED` | `IllegalStateTransition` thrown | `NEG-I6-01` |

API Gateway has no route that writes a store, so the forbidden path is not reachable over HTTP at all; the tests exercise it at the module boundary where it would otherwise be possible.

---

## 5. Lab 3 contract register — how every in-scope row is represented

Every in-scope contract row has a written contract. Which **kind** of contract depends on the I-8 pattern of the row, so the register is split by pattern rather than forced into a single document.

- **Public HTTP** rows are published in `openapi.yaml`. That document is G4 for this sitting and is guarded against drift by `G4-D01`…`G4-D04`.
- **Non-public** rows are asynchronous events on Message Broker or in-process calls between containers. They are not HTTP routes, so they carry no OpenAPI path. Their contract is defined in §5.2 below: producer, consumer, mechanism, and payload.
- **N/A** rows are outside the I-11 slice, are not implemented, and are not callable.

Putting a non-public row into `openapi.yaml` would publish an operation that the Lab 3 register does not expose through API Gateway, which is the "OpenAPI lists extra operations" failure. The SA agreement recorded in `SIGN-OFF.md` §3 covers this split.

### 5.1 Public HTTP contract rows — in `openapi.yaml`

| Contract row | OpenAPI operation | Runtime |
|---|---|---|
| CT-03, CT-04 `SubmitPartnerEarn` | `SubmitPartnerEarn` — `POST /partner-earn` | `ApiGateway.handlePartnerEarn` |
| CT-10, CT-11 `SubmitRedemption` | `SubmitRedemption` — `POST /redemptions` | `ApiGateway.handleRedemptions` |
| CT-25, CT-26 `RequestReport` | `RequestReport` — `GET /reports/point-liability` | `ApiGateway.handlePointLiability` |

Three register rows, three OpenAPI operations, three served routes. `G4-D01` asserts the sets are equal.

### 5.2 Non-public in-scope contract rows — event and in-process contracts

These are contracts, written here because they have no HTTP surface. Each names its mechanism and its payload.

**Asynchronous event contracts** — mechanism: I-8 Async, published on Message Broker.

| Contract row | Topic | Producer | Consumer | Payload fields |
|---|---|---|---|---|
| CT-01, CT-02 | `transaction.settled` | Core Banking System | Earning Engine Service | `sourceTransactionId`, `memberId`, `programId`, `amount` |
| CT-05, CT-06 | `earning.qp_accrued` | Earning Engine Service | Tiering System Service | `eventId`, `memberId`, `qualifyingPoints` |
| CT-07 | `tiering.tier_changed` | Tiering System Service | Message Broker | `memberId`, `fromTier`, `toTier` |
| CT-08 | `tiering.tier_changed` | Message Broker | Earning Engine Service | `memberId`, `fromTier`, `toTier` |
| CT-23, CT-24 | `cdc.platform_events` | Earning Engine Service | Analytics & Reporting Service | `pointTransactionId`, `memberId`, `outstandingPoints` |
| CT-22 | member and Finance notification | Redemption Engine Service, Analytics & Reporting Service | CRM & Notification Gateway | reversal: `memberId`, `orderId`; stale report: `dataAgeSeconds` |

`eventId` on `earning.qp_accrued` is the idempotency key that makes the UC-LB-03 replay `alt` possible; `G6-A04` publishes a duplicate `eventId` and asserts the tier does not move.

**In-process call contracts** — mechanism: I-8 Sync, container to container inside the collapse.

| Contract row | Operation | Caller | Callee | Signature |
|---|---|---|---|---|
| CT-12 | `GetMemberTier` | Redemption Engine Service | Tiering System Service | `String currentTier(String memberId)` |
| CT-13 | `DebitPointsFifo` | Redemption Engine Service | Earning Engine Service | `List<DebitAllocation> debitFifo(String memberId, long points)` |
| CT-13 | `RestorePoints` | Redemption Engine Service | Earning Engine Service | `void restore(List<DebitAllocation> allocations)` |
| CT-14 | `RequestFulfilment` | Redemption Engine Service | Partner Systems | `boolean requestFulfillment(String orderId, String rewardItemId)` |
| CT-15 | `ReturnFulfilmentOutcome` | Partner Systems | Redemption Engine Service | The boolean return of CT-14, exactly as the Lab 10 UC-LB-02 sequence draws it — a return message, not a second inbound call |

`RequestFulfilment` and `ReturnFulfilmentOutcome` keep the spelling `lab3-spec.md` defined for them; see the spelling policy in `name-identity-map.md` §10.

### 5.3 N/A rows — outside the I-11 slice

| Contract row | Why it is out |
|---|---|
| CT-09 `tiering.tier_changed` consumed by Redemption Engine Service | Redemption reads the tier synchronously on CT-12 at validation time, so the event consumer is not needed |
| CT-16, CT-17 `UpdateProgramConfiguration` | No I-11 use case; configuration is seeded, not exposed |
| CT-18, CT-19, CT-20 `config.rule_updated` | No I-11 use case |
| CT-21 `earning.point_expired` | Point expiry is not an I-11 use case |
| CT-27 `PublishPeriodFigures` | The I-3 stub exists; no I-11 use case drives it |

N/A rows are not implemented and are not callable. No N/A row was turned into an extra use case, and none was silently dropped.

---

## 6. Every route, handler, and package is on the trace

| Package | Purpose | On the trace via |
|---|---|---|
| `com.loyalty.capstone` | `Platform` composition root, `Main` | All rows |
| `com.loyalty.capstone.gateway` | `ApiGateway` routes, `Json` | The three OpenAPI operations |
| `com.loyalty.capstone.service` | Earning, Tiering, Program Management, Analytics | UC-LB-01, UC-LB-03, UC-LB-04, CT-08 |
| `com.loyalty.capstone.service.redemption` | The five Lab 9 modules | UC-LB-02 |
| `com.loyalty.capstone.domain` | I-7 types and the I-6 typed object | G6-T01…T05, NEG-I6-01 |
| `com.loyalty.capstone.store` | I-4 data containers and the ownership guard | NEG-I5-01, NEG-I9-01 |
| `com.loyalty.capstone.broker` | In-process bus and topic names | CT-01…CT-24 rows |
| `com.loyalty.capstone.external` | I-3 stubs and fakes | All rows that touch an external |

There is no route, handler, or package that does not appear above.

---

## 7. Changes made after the capstone review

| Review point | Change | Evidence |
|---|---|---|
| `EarningEngineService` read `TieringDb` directly, a coupling the after pack did not draw | The service no longer touches Tiering DB. It consumes `tiering.tier_changed` and keeps a local tier projection, which is contract row CT-08 and the I-8 async pattern | `EarningEngineService.onTierChanged`, `projectedTier` |
| That edge was missing from the drawn after pack | The SA added it: `Message Broker -> Earning Engine Service "tier changed"` on Lab 8 view 3, and `Broker --> Earning : tier change event / async` on the Lab 9 Container. Code and pack now agree | `../lab8-archimate-views.md`, `../lab-09-c4-after.md` |
| No automated HTTP tests proving OpenAPI status and body | Six HTTP tests added against the running gateway | §2.1 |
| No automated OpenAPI drift guard | Four drift tests added that parse `openapi.yaml` and compare it to the runtime | §2.2 |

Test count moved from 17 to 27. No I-11 use case, name, operation, or state was added.

### Second review round

| Review point | Change |
|---|---|
| C8 — no SA sign-off artifact | `SIGN-OFF.md` added: evidence assembled, five decisions listed for SA confirmation, decision block left unsigned |
| D2/D4 — async and internal rows had no contract representation | §5 rewritten. Public HTTP rows in `openapi.yaml`; non-public rows carry event and in-process contracts in §5.2 with producer, consumer, mechanism and payload; N/A rows separated into §5.3 |
| C1 — `fulfilment` and `fulfillment` mixed | Normalised to Lab 1's `fulfillment` across `capstone/` and `lab3-spec.md`. The two source-defined identifiers `RequestFulfilment` and `ReturnFulfilmentOutcome` are frozen and documented in `name-identity-map.md` §10 |
| Lab 7 gate register stale | `../lab7-adoption.md` §5 updated: G1–G6 now carry real Lab 8–10 evidence and read Pass; §0.1 corrected to say the archive holds Labs 1, 2, 3, 5, 6 |
| Tracked build output | `capstone/out/` untracked and deleted; `.gitignore` already covered it |
