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
| G6-T01 | `PENDING -> IN_PROGRESS` | Redemption Engine Service | `G6-T01` | Yes (service-level reservation & type) |
| G6-T02 | `PENDING -> CANCELLED` | Redemption Engine Service | `G6-T02` | Yes (service-level cancel & type) |
| G6-T03 | `IN_PROGRESS -> FULFILLED` | Redemption Engine Service | `G6-T03` | Yes (service-level fulfillment & type) |
| G6-T04 | `IN_PROGRESS -> FAILED` | Redemption Engine Service | `G6-T04` | Yes (service-level failure & type) |
| G6-T05 | `FAILED -> REVERSED` | Redemption Engine Service | `G6-T05` | Yes (service-level CON.3 restoration & type) |
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
| EXC-05 (CON.3) | Partner Systems fail after the debit | `IN_PROGRESS -> FAILED -> REVERSED`; original batches restored with original earn date and expiry; member notified | Redemption Engine Service, restoration by Earning Engine Service | `G6-A03`, `G6-T05` asserts the batch is whole, **no new batch was created**, earn date and expiry are unchanged, and a reversal notice was sent |
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
| I-5 Anti-tamper — Client attempts forged tier on partner earn | `recordEarn` with forged tier metadata | Ignored by server; authoritative tier projection used | `NEG-I5-02` |
| CON.1 — the same source transaction is posted twice | Second `recordEarn` with the same source transaction | Duplicate result returned, one ledger row only | `G6-A01` |
| I-6 — a transition outside the six states | `markFulfilled` from `PENDING`, `markReversed` from `PENDING`, `markInProgress` from `CANCELLED` | `IllegalStateTransition` thrown | `NEG-I6-01` |

API Gateway has no route that writes a store, so the forbidden path is not reachable over HTTP at all; the tests exercise it at the module boundary where it would otherwise be possible.

---

## 5. Lab 3 contract register — in scope and N/A

| Contract row | Status | Where |
|---|---|---|
| CT-01, CT-02 `transaction.settled` | In scope | `CoreBankingSystemMock` publishes; `EarningEngineService` consumes |
| CT-03, CT-04 `SubmitPartnerEarn` | In scope | `POST /partner-earn` |
| CT-05, CT-06 `earning.qp_accrued` | In scope | Drives UC-LB-03 |
| CT-07 `tiering.tier_changed` published | In scope | `TieringSystemService` publishes on upgrade |
| CT-08 `tiering.tier_changed` consumed by Earning Engine Service | In scope | `EarningEngineService.onTierChanged` keeps the local tier projection used for the earn multiplier |
| CT-09 `tiering.tier_changed` consumed by Redemption Engine Service | **N/A** | Redemption reads the tier synchronously on CT-12 at validation time |
| CT-10, CT-11 `SubmitRedemption` | In scope | `POST /redemptions` |
| CT-12 `GetMemberTier` | In scope, internal | `TierAndBalanceValidationModule` to `TieringSystemService` |
| CT-13 `DebitPointsFifo`, `RestorePoints` | In scope, internal | `FifoDebitModule` to `EarningEngineService` |
| CT-14 `RequestFulfilment` | In scope, internal | `FulfillmentCoordinationModule` to `PartnerSystemsMock` |
| CT-15 `ReturnFulfilmentOutcome` | In scope | Modelled as the synchronous return in the Lab 10 UC-LB-02 sequence, not a second endpoint |
| CT-22 member notification | In scope | `CrmNotificationGatewayMock` on reversal and on stale report |
| CT-23, CT-24 `cdc.platform_events` | In scope | `EarningEngineService` publishes; `AnalyticsReportingService` builds `FactPointTransaction` |
| CT-25, CT-26 `RequestReport` | In scope | `GET /reports/point-liability` |
| CT-16, CT-17 `UpdateProgramConfiguration` | **N/A** | No I-11 use case; configuration is seeded, not exposed |
| CT-18, CT-19, CT-20 `config.rule_updated` | **N/A** | No I-11 use case |
| CT-21 `earning.point_expired` | **N/A** | Expiry is not an I-11 use case |
| CT-27 `PublishPeriodFigures` | **N/A** | Stub exists; no I-11 use case drives it |

N/A rows are not implemented and are not callable. No N/A row was turned into an extra use case.

---

## 6. Every route, handler, and package is on the trace

| Package | Purpose | On the trace via |
|---|---|---|
| `com.loyalty.capstone` | `Platform` composition root, `Main` | All rows |
| `com.loyalty.capstone.gateway` | `ApiGateway` routes, `Json` | The three OpenAPI operations |
| `com.loyalty.capstone.service` | Earning, Tiering, Program Management, Analytics | UC-LB-01, UC-LB-03, UC-LB-04, CT-08 |
| `com.loyalty.capstone.service.redemption` | The five Lab 9 modules | UC-LB-02 |
| `com.loyalty.capstone.domain` | I-7 types and the I-6 typed object | G6-T01…T05, NEG-I6-01 |
| `com.loyalty.capstone.store` | I-4 data containers and the ownership guard | NEG-I5-01, NEG-I9-01, NEG-I5-02 |
| `com.loyalty.capstone.broker` | In-process bus and topic names | CT-01…CT-24 rows |
| `com.loyalty.capstone.external` | I-3 stubs and fakes | All rows that touch an external |

There is no route, handler, or package that does not appear above.

---

## 7. Compliance and after-pack fidelity

| Design principle | Implementation fidelity | Evidence |
|---|---|---|
| `EarningEngineService` does not access `TieringDb` | Earning Engine Service never touches Tiering DB. It consumes asynchronous `tiering.tier_changed` events (CT-08) and maintains an internal projection for earn multipliers | `EarningEngineService.onTierChanged`, `projectedTier` |
| Modeling packs are unchanged | Modeling packs (Labs 1–10) are preserved as pristine sources of truth. The runtime realizes the published architecture without reopening prior labs | `before-pack/`, `lab-*.md` unchanged |
| Automated HTTP tests proving OpenAPI status and body | Six HTTP tests execute against the running gateway on ephemeral ports | `HttpContractTests` (§2.1) |
| Automated OpenAPI drift guard | Four drift tests parse `openapi.yaml` and verify route, status, and enum alignment with runtime | `OpenApiDriftTests` (§2.2) |
| Service-level lifecycle execution | G6-T01 through G6-T05 execute through `RedemptionEngineService` service methods | `CapstoneTests` (`G6-T01`..`G6-T05`) |

Test count: 28 automated tests in `CapstoneTests` + `HttpContractTests` + `OpenApiDriftTests`. All pass with 100% success.

