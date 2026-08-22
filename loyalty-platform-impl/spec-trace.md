# Spec-Trace — Loyalty Banking Platform Capstone

> **Capstone output — outside the modeling pack.**
> Traces every in-scope Lab 10 path → Lab 3 contract register → OpenAPI operation → automated test.
> Scope: I-11 only. Covers happy paths and all named `alt` branches from lab10-uml-after.md G6 coverage note.
> **R** Dev (Lê Huy Du) · **A** SA (Vũ Trường Quang)

## Column Guide

| Column | Source |
|---|---|
| **Path ID** | Lab 10 G6 coverage ID (G6-T01…T05 = I-6 transitions; G6-A01…A05 = sequence alts) |
| **Use Case** | I-11 use case |
| **Lab 10 Sequence / State** | Artifact in `lab-10-uml-after.md` |
| **Lab 3 Contract** | Contract ID from `lab3-spec.md §4` |
| **OpenAPI operationId** | Operation in `openapi.yaml` |
| **SUT** | System Under Test — exact I-4 Lab 1 container name |
| **Test Class** | Java test class (relative to `src/test/java/`) |
| **Test Method** | JUnit 5 test method name |
| **Coverage Tag** | Which capstone coverage axis this satisfies |

---

## Trace Table

| Path ID | Use Case | Lab 10 Path | Lab 3 Contract | OpenAPI operationId | SUT | Test Class | Test Method | Coverage Tag |
|---|---|---|---|---|---|---|---|---|
| **G6-T01** | UC-LB-02 | State: PENDING → IN_PROGRESS | CT-11 CreateRedemptionOrder | `createRedemptionOrder` | Redemption Engine Service | `redemption_engine/.../RedemptionServiceTest` | `testPlaceOrder_Success` | I-11, G6 |
| **G6-T02** | UC-LB-02 | State: PENDING → CANCELLED | CT-11 CreateRedemptionOrder | `createRedemptionOrder` / `cancelRedemptionOrder` | Redemption Engine Service | `redemption_engine/.../RedemptionServiceTest` | `testCancelOrder_WhenPending_Success` | I-11, G6 |
| **G6-T03** | UC-LB-02 | State: IN_PROGRESS → FULFILLED | CT-11 / fulfillOrder | `fulfillRedemptionOrder` | Redemption Engine Service | `redemption_engine/.../RedemptionServiceTest` | `testFulfillOrder_Success` | I-11, G6 |
| **G6-T04** | UC-LB-02 | State: IN_PROGRESS → FAILED | CT-14 / partner failure | `failAndReverseRedemptionOrder` | Redemption Engine Service | `redemption_engine/.../RedemptionServiceTest` | `testFailAndReverseOrder_MovesToFailed` | I-11, G6 |
| **G6-T05** | UC-LB-02 | State: FAILED → REVERSED (CON.3) | CT-13 RestorePoints | `failAndReverseRedemptionOrder` | Redemption Engine Service | `redemption_engine/.../RedemptionServiceTest` | `testFailAndReverseOrder_RestoredWithFifo` | I-11, G6, I-5 (CON.3) |
| **G6-A01** | UC-LB-01 | Sequence UC-LB-01 alt: duplicate event (CON.1) | CT-04 RecordEarn | `recordEarn` (409 response) | Earning Engine Service | `earning_engine/.../EarningEngineServiceTest` | `testProcessEarn_DuplicateEvent_NoDuplicateTransaction` | I-11, G6, I-5 (CON.1) |
| **G6-A02** | UC-LB-02 | Sequence UC-LB-02 alt: insufficient balance or tier-ineligible | CT-11 CreateRedemptionOrder | `createRedemptionOrder` (422 response) | Redemption Engine Service | `redemption_engine/.../RedemptionServiceTest` | `testPlaceOrder_InsufficientBalance_Throws` | I-11, G6 |
| **G6-A02** | UC-LB-02 | Sequence UC-LB-02 alt: tier-ineligible | CT-11 CreateRedemptionOrder | `createRedemptionOrder` (422 response) | Redemption Engine Service | `redemption_engine/.../RedemptionServiceTest` | `testPlaceOrder_TierEligibilityFailed_Throws` | I-11, G6 |
| **G6-A03** | UC-LB-02 | Sequence UC-LB-02 alt: partner fulfillment failure (CON.3) | CT-14 RequestFulfilment / CT-13 RestorePoints | `failAndReverseRedemptionOrder` | Redemption Engine Service | `redemption_engine/.../RedemptionServiceTest` | `testFailAndReverseOrder_RestoredWithFifo` | I-11, G6, I-5 (CON.3) |
| **G6-A04** | UC-LB-03 | Sequence UC-LB-03 alt: replayed QP event (CON.1) | CT-06 qp_accrued consume | *(Kafka consumer path — no REST op)* | Tiering System Service | `tiering_system/.../TierUpgradeIdempotencyTest` | `testEvaluateAndUpgrade_ReplayedEvent_TierNotMovedTwice` | I-11, G6, I-5 (CON.1) |
| **G6-A05** | UC-LB-04 | Sequence UC-LB-04 alt: warehouse data stale > 10 min (CON.4) | CT-26 GenerateReport | `getPointLiabilityReport` (stale:true) | Analytics & Reporting Service | `analytics_reporting/.../ReportingServiceTest` | `testGetFinancialLiability_StaleData_FlagsAsStale` | I-11, G6, I-5 (CON.4) |

---

## Additional Coverage (I-5 Business Process, I-9 Deployment)

| Axis | Path Description | Test Class | Test Method | Coverage Tag |
|---|---|---|---|---|
| **I-5 CON.1** | Happy path: new earn event → PointTransaction written; no second write on replay | `earning_engine/.../EarningEngineServiceTest` | `testProcessEarn_HappyPath_TransactionWritten` | I-5, I-11 |
| **I-5 CON.3** | Fulfillment failure → auto-reversal restores points with original FIFO earn date | `redemption_engine/.../RedemptionServiceTest` | `testFailAndReverseOrder_RestoredWithFifo` | I-5 |
| **I-5 CON.4** | Stale warehouse → report flagged, not presented as current | `analytics_reporting/.../ReportingServiceTest` | `testGetFinancialLiability_StaleData_FlagsAsStale` | I-5 |
| **I-9 isolation** | Analytics & Reporting Service never reads from transactional DB | `analytics_reporting/.../ReportingServiceTest` | `testGetFinancialLiability_OnlyReadsDataWarehouse` | I-9 |
| **UC-LB-03 happy path** | QP accrual → MemberTier upgraded when threshold crossed | `tiering_system/.../TierUpgradeIdempotencyTest` | `testEvaluateAndUpgrade_HappyPath_TierUpgraded` | I-11 |
| **UC-LB-04 happy path** | Finance reads fresh report → returns liability with stale:false | `analytics_reporting/.../ReportingServiceTest` | `testGetFinancialLiability_Fresh_ReturnsReport` | I-11 |
| **I-11 UC-LB-02 ALT-06** | Balance lock busy → concurrent order refused | `redemption_engine/.../RedemptionServiceTest` | `testPlaceOrder_ConcurrentLockBusy_Throws` | I-11 |
| **I-11 UC-LB-02 ALT-03** | Below 100-point minimum → order cancelled | `redemption_engine/.../RedemptionServiceTest` | `testPlaceOrder_MinimumPointsViolation_Throws` | I-11 |
| **I-11 UC-LB-02 cancel** | Member cancels fulfilled order → refused (ALT-04 boundary) | `redemption_engine/.../RedemptionServiceTest` | `testCancelOrder_WhenFulfilled_Throws` | I-11 |

---

## G4 Satisfied — Contract register to OpenAPI reconciliation

| Lab 3 CT | Lab 3 Contract Name | OpenAPI operationId | Match? |
|---|---|---|---|
| CT-04 | RecordEarn | `recordEarn` | ✅ |
| CT-11 | CreateRedemptionOrder, QueryCatalog | `createRedemptionOrder`, `addCatalogItem` | ✅ |
| CT-12 | GetMemberTier | `getMemberTier` | ✅ |
| CT-26 | GenerateReport, ReadDashboard | `getPointLiabilityReport` | ✅ (ReadDashboard is the same endpoint with stale flag) |
| CT-13 | DebitPointsFifo, RestorePoints | *(internal service call; exercised via `failAndReverseRedemptionOrder`)* | ✅ |
| CT-14 | RequestFulfilment | *(simulated via `fulfillRedemptionOrder` / `failAndReverseRedemptionOrder`)* | ✅ |

> G4 pass rule: every relationship drawn on Lab 9 Container view has a matching row in `lab3-spec.md §4` with producer, consumer, sync/async, and operation name. All six in-scope CT rows above are documented and realised.

---

## G5 Satisfied — Critical exception path

`EXC-05` / `CON.3` / `ALT-05`: Fulfillment failure path is implemented in `RedemptionService.failAndReverseOrder()`.
Order moves `IN_PROGRESS → FAILED → REVERSED`. Points restored via `FifoDebitService.reverseDebit()` keeping original earn date and expiry.
Tested by: `testFailAndReverseOrder_RestoredWithFifo` (Path ID G6-T05 / G6-A03).

---

## G6 Coverage Summary

| G6 ID | Covered? | Test method |
|---|---|---|
| G6-T01 PENDING → IN_PROGRESS | ✅ | `testPlaceOrder_Success` (service); `testPlaceOrder_HappyPath_Returns201` (HTTP layer) |
| G6-T02 PENDING → CANCELLED | ✅ | `testCancelOrder_WhenPending_Success` |
| G6-T03 IN_PROGRESS → FULFILLED | ✅ | `testFulfillOrder_Success` |
| G6-T04 IN_PROGRESS → FAILED | ✅ | `testFailAndReverseOrder_MovesToFailed` |
| G6-T05 FAILED → REVERSED | ✅ | `testFailAndReverseOrder_RestoredWithFifo` |
| G6-A01 UC-LB-01 duplicate event | ✅ | `testProcessEarn_DuplicateEvent_NoDuplicateTransaction` (service); `testSubmitEarn_DuplicateTransaction_Returns409` (HTTP 409) |
| G6-A02 UC-LB-02 insufficient balance/tier | ✅ | `testPlaceOrder_InsufficientBalance_Throws` (service); `testPlaceOrder_InsufficientBalance_Returns422` (HTTP 422); `testPlaceOrder_TierEligibilityFailed_Returns422` (HTTP 422) |
| G6-A03 UC-LB-02 partner failure | ✅ | `testFailAndReverseOrder_RestoredWithFifo` (service); `testFailOrder_CON3_CompensatingAction_Returns200` (HTTP 200 + G5) |
| G6-A04 UC-LB-03 replayed event | ✅ | `testEvaluateAndUpgrade_ReplayedEvent_TierNotMovedTwice` (tier); `testRecordQpAndGetCumulative_DuplicateEvent_SkipsWrite` (QP ledger) |
| G6-A05 UC-LB-04 stale warehouse | ✅ | `testGetFinancialLiability_StaleData_FlagsAsStale` |

---

## HTTP-layer controller tests — closing T3 / T5 gaps

The following `@WebMvcTest` tests assert HTTP status codes and response bodies against the OpenAPI contract.
These are in addition to the service-layer unit tests in the G6 table above.

| Test class | Test method | HTTP Status | OpenAPI operationId | Capstone axis |
|---|---|---|---|---|
| `earning_engine/.../PartnerEarnControllerTest` | `testSubmitEarn_HappyPath_Returns202` | **202** | `recordEarn` | I-11, T5 |
| `earning_engine/.../PartnerEarnControllerTest` | `testSubmitEarn_DuplicateTransaction_Returns409` | **409** | `recordEarn` (CON.1) | G6-A01, T3, T5 |
| `earning_engine/.../PartnerEarnControllerTest` | `testSubmitEarn_MissingTransactionId_Returns400` | **400** | `recordEarn` | T5 |
| `redemption_engine/.../RedemptionControllerTest` | `testPlaceOrder_HappyPath_Returns201` | **201** | `createRedemptionOrder` | I-11, T5 |
| `redemption_engine/.../RedemptionControllerTest` | `testPlaceOrder_InsufficientBalance_Returns422` | **422** | `createRedemptionOrder` (ALT-01) | G6-A02, T3, T5 |
| `redemption_engine/.../RedemptionControllerTest` | `testPlaceOrder_TierEligibilityFailed_Returns422` | **422** | `createRedemptionOrder` (ALT-02) | G6-A02, T3 |
| `redemption_engine/.../RedemptionControllerTest` | `testPlaceOrder_MinimumPoints_Returns400` | **400** | `createRedemptionOrder` (ALT-03) | T5 |
| `redemption_engine/.../RedemptionControllerTest` | `testFailOrder_CON3_CompensatingAction_Returns200` | **200** | `failAndReverseRedemptionOrder` (CON.3) | G6-A03, G5, T5 |

---

## Out-of-scope paths — N/A declaration

The following endpoints are present in the runtime for **data setup / seed only**.
They are **not** I-11 use cases and are **not** reachable from the API Gateway in the I-11 scope.
They are listed here as explicitly N/A per capstone requirement (D5, C4).

| Service | Endpoint | Reason N/A |
|---|---|---|
| Program Management Service | `GET /api/v1/programs`, `POST /api/v1/programs`, `GET /api/v1/campaigns`, `POST /api/v1/campaigns` | Lab 3 CT-01/CT-02 rows are out of I-11 scope. Program CRUD and campaign CRUD support data seeding only. Not callable from API Gateway path in I-11 demo. |
| Redemption Engine | `GET /api/v1/catalog`, `POST /api/v1/catalog/items` | Catalog seeding endpoints. Not an I-11 use case. CT-11 covers item retrieval during order placement (internal call), not a separate catalog browser use case. |

> These paths are not new I-4 containers and do not represent new use cases. They are helper endpoints in existing I-4 containers and are documented here to satisfy the "not silently omitted" rule from capstone.md.

---

## SA Sign-Off

See [`SIGN-OFF.md`](./SIGN-OFF.md) for SA (**A**) acceptance of this runtime.

**SA:** Vũ Trường Quang  
**Date accepted:** 2026-08-22

