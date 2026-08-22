# Spec-Trace — Loyalty Banking Platform Capstone

> **Capstone output — outside the modeling pack.**
> Traces every in-scope Lab 10 path → Lab 3 contract register → OpenAPI operation → automated test.
> Scope: I-11 only. Covers happy paths and all named `alt` branches from `lab10-uml-after.md` G6 coverage note.
> **R** Dev (Lê Huy Du) · **A** SA (Vũ Trường Quang)

---

## 1. Column Guide

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

## 2. In-Scope Trace Matrix (G6 Paths)

| Path ID | Use Case | Lab 10 Path | Lab 3 Contract | OpenAPI operationId | SUT | Test Class | Test Method | Coverage Tag |
|---|---|---|---|---|---|---|---|---|
| **G6-T01** | UC-LB-02 | State: PENDING → IN_PROGRESS (debit reserved) | CT-11 / CT-13 | `createRedemptionOrder`, `debitPointsFifo` | Redemption Engine Service & Earning Engine Service | `redemption_engine/.../RedemptionServiceTest` + CT-13 HTTP boundary test | `testPlaceOrder_Success` (orchestration) | I-11, G6, T-01 |
| **G6-T02** | UC-LB-02 | State: PENDING → CANCELLED | CT-11 CancelRedemptionOrder | `cancelRedemptionOrder` | Redemption Engine Service | `redemption_engine/.../RedemptionServiceTest` | `testCancelOrder_WhenPending_Success` | I-11, G6, T-02 |
| **G6-T03** | UC-LB-02 | State: IN_PROGRESS → FULFILLED | CT-11 / CT-14 | `fulfillRedemptionOrder` | Redemption Engine Service | `redemption_engine/.../RedemptionServiceTest` | `testFulfillOrder_Success` | I-11, G6, T-03 |
| **G6-T04** | UC-LB-02 | State: IN_PROGRESS → FAILED | CT-14 / partner failure | `failAndReverseRedemptionOrder` | Redemption Engine Service | `redemption_engine/.../RedemptionServiceTest` | `testFailAndReverseOrder_RestoredWithFifo` | I-11, G6, T-04 |
| **G6-T05** | UC-LB-02 | State: FAILED → REVERSED (CON.3) | CT-13 RestorePoints | `restorePoints`, `failAndReverseRedemptionOrder` | Earning Engine Service | persisted allocation integration test + RedemptionServiceTest | persisted allocation restore (required runtime evidence) | I-11, G6, I-5 (CON.3), T-05 |
| **G6-A01** | UC-LB-01 | Sequence UC-LB-01 alt: duplicate event (CON.1) | CT-04 RecordEarn | `recordEarn` (409 response) | Earning Engine Service | `earning_engine/.../EarningEngineServiceTest` | `testProcessEarn_DuplicateEvent_NoDuplicateTransaction` | I-11, G6, I-5 (CON.1) |
| **G6-A02** | UC-LB-02 | Sequence UC-LB-02 alt: insufficient balance (ALT-01) | CT-13 DebitPointsFifo | `debitPointsFifo` (422 response) | Redemption Engine Service & Earning Engine Service | `redemption_engine/.../RedemptionServiceTest` | `testPlaceOrder_InsufficientBalance_Throws` | I-11, G6, ALT-01 |
| **G6-A02** | UC-LB-02 | Sequence UC-LB-02 alt: tier-ineligible (ALT-02) | CT-12 GetMemberTier | `createRedemptionOrder` (422 response) | Redemption Engine Service | `redemption_engine/.../RedemptionServiceTest` | `testPlaceOrder_TierEligibilityFailed_Throws` | I-11, G6, ALT-02 |
| **G6-A03** | UC-LB-02 | Sequence UC-LB-02 alt: partner fulfillment failure (CON.3) | CT-14 RequestFulfilment / CT-13 RestorePoints | `failAndReverseRedemptionOrder` | Redemption Engine Service & Earning Engine Service | `redemption_engine/.../RedemptionServiceTest` | `testFailAndReverseOrder_RestoredWithFifo` | I-11, G6, I-5 (CON.3) |
| **G6-A04** | UC-LB-03 | Sequence UC-LB-03 alt: replayed QP event (CON.1) | CT-06 qp_accrued consume | *(Kafka consumer path — no REST op)* | Tiering System Service | `tiering_system/.../TierUpgradeIdempotencyTest` | `testEvaluateAndUpgrade_ReplayedEvent_TierNotMovedTwice` | I-11, G6, I-5 (CON.1) |
| **G6-A05** | UC-LB-04 | Sequence UC-LB-04 alt: warehouse data stale > 10 min (CON.4) | CT-26 GenerateReport | `getPointLiabilityReport` (stale:true) | Analytics & Reporting Service | `analytics_reporting/.../ReportingServiceTest` | `testGetFinancialLiability_StaleData_FlagsAsStale` | I-11, G6, I-5 (CON.4) |

---

## 3. Additional Invariants & Hard Rules Coverage (I-5, I-9, CON.1..4)

| Axis | Path Description | Test Class | Test Method | Coverage Tag |
|---|---|---|---|---|
| **I-5 CON.1** | Happy path: new earn event → PointTransaction written; no second write on replay | `earning_engine/.../EarningEngineServiceTest` | `testProcessEarn_HappyPath_TransactionWritten` | I-5, I-11 |
| **I-5 CON.1 (reversal)** | Core Banking reversal cancels PointTransaction and corrects PointBalance | `earning_engine/.../PartnerEarnControllerTest` | `testCancelEarn_HappyPath_Returns200` | I-5, EXC-02 |
| **I-5 FIFO Allocation** | Points debited in FIFO order from oldest unexpired batches | Earning Engine persisted allocation integration test | debit allocates and persists source batch IDs | I-5, CT-13 |
| **I-5 CON.3** | Fulfillment failure → auto-reversal restores points with original FIFO earn date | Earning Engine persisted allocation integration test | restore rehydrates depleted source batches; duplicate restore is idempotent | I-5, CON.3, G5 |
| **I-5 Tamper Resistance** | Client cannot forge balance or tier; server reads I-7 owners | `redemption_engine/.../I5BalanceTamperTest` | `testClientForgedTier_RejectedByServerQueryingCT12`, `testClientForgedBalance_RejectedByServerQueryingCT13` | I-5, T3 |
| **I-9 Zone Security** | Forbidden direct database write to Earning DB from outside service aggregate rejected | Application boundary security test | unauthorized write attempt is rejected without state change | I-9, CON.2, T4 |
| **I-5 CON.4** | Stale warehouse → report flagged, not presented as current | `analytics_reporting/.../ReportingServiceTest` | `testGetFinancialLiability_StaleData_FlagsAsStale` | I-5, CON.4 |
| **I-9 DW Isolation** | Analytics & Reporting Service only reads Data Warehouse | `analytics_reporting/.../ReportingServiceTest` | `testGetFinancialLiability_OnlyReadsDataWarehouse` | I-9, EXC-08 |

---

## 4. G4 Satisfied — Contract Register to OpenAPI Reconciliation

| Lab 3 CT | Lab 3 Contract Name | Producer → Consumer | OpenAPI operationId | Match? |
|---|---|---|---|:---:|
| **CT-04** | RecordEarn | API Gateway → Earning Engine Service | `recordEarn`, `cancelEarnTransaction` | ✅ |
| **CT-11** | CreateRedemptionOrder | API Gateway → Redemption Engine Service | `createRedemptionOrder` | ✅ |
| **CT-12** | GetMemberTier | Redemption Engine Service → Tiering System Service | `getMemberTier` | ✅ |
| **CT-13** | DebitPointsFifo, RestorePoints | Redemption Engine Service → Earning Engine Service | `debitPointsFifo`, `restorePoints`, `getMemberBalance` | ✅ |
| **CT-14** | RequestFulfilment | Redemption Engine Service → Partner Systems | `fulfillRedemptionOrder`, `failAndReverseRedemptionOrder` (PATCH) | ✅ |
| **CT-26** | GenerateReport, ReadDashboard | API Gateway → Analytics & Reporting Service | `getPointLiabilityReport` | ✅ |

---

## 5. HTTP-Layer Controller Tests (T3 / T5 Verification)

| Test Class | Test Method | HTTP Verb & Status | OpenAPI operationId | Capstone Axis |
|---|---|---|---|---|
| `earning_engine/.../PartnerEarnControllerTest` | `testSubmitEarn_HappyPath_Returns202` | **POST 202** | `recordEarn` | I-11, T5 |
| `earning_engine/.../PartnerEarnControllerTest` | `testSubmitEarn_DuplicateTransaction_Returns409` | **POST 409** | `recordEarn` (CON.1) | G6-A01, T3, T5 |
| `earning_engine/.../PartnerEarnControllerTest` | `testSubmitEarn_MissingTransactionId_Returns400` | **POST 400** | `recordEarn` | T5 |
| `earning_engine/.../PartnerEarnControllerTest` | `testCancelEarn_HappyPath_Returns200` | **PATCH 200** | `cancelEarnTransaction` (EXC-02, CON.1) | I-11, T5 |
| `earning_engine/.../FifoDebitControllerTest` | `testDebitFifo_Success_Returns200` | **POST 200** | `debitPointsFifo` (CT-13) | I-11, T5 |
| `earning_engine/.../FifoDebitControllerTest` | `testDebitFifo_InsufficientBalance_Returns422` | **POST 422** | `debitPointsFifo` (CT-13) | G6-A02, T3, T5 |
| `earning_engine/.../FifoDebitControllerTest` | `testRestoreFifo_Success_Returns200` | **POST 200** | `restorePoints` (CT-13) | G6-T05, G5, T5 |
| `tiering_system/.../MemberTierControllerTest` | `testGetMemberTier_ExistingMember_Returns200` | **GET 200** | `getMemberTier` (CT-12) | I-11, T5 |
| `redemption_engine/.../RedemptionControllerTest` | `testPlaceOrder_HappyPath_Returns201` | **POST 201** | `createRedemptionOrder` | G6-T01, I-11, T5 |
| `redemption_engine/.../RedemptionControllerTest` | `testPlaceOrder_InsufficientBalance_Returns422` | **POST 422** | `createRedemptionOrder` (ALT-01) | G6-A02, T3, T5 |
| `redemption_engine/.../RedemptionControllerTest` | `testPlaceOrder_TierEligibilityFailed_Returns422` | **POST 422** | `createRedemptionOrder` (ALT-02) | G6-A02, T3 |
| `redemption_engine/.../RedemptionControllerTest` | `testPlaceOrder_MinimumPoints_Returns400` | **POST 400** | `createRedemptionOrder` (ALT-03) | T5 |
| `redemption_engine/.../RedemptionControllerTest` | `testFulfillOrder_HappyPath_Returns200` | **PATCH 200** | `fulfillRedemptionOrder` | G6-T03, T5 |
| `redemption_engine/.../RedemptionControllerTest` | `testFailOrder_CON3_CompensatingAction_Returns200` | **PATCH 200** | `failAndReverseRedemptionOrder` | G6-T04/T05, G5, T5 |
| `redemption_engine/.../RedemptionControllerTest` | `testCancelOrder_WhenPending_Returns200` | **PATCH 200** | `cancelRedemptionOrder` | G6-T02, T5 |

---

## 6. Out-of-Scope Paths — Not Exposed

Program configuration, campaign configuration, catalog administration, and catalog query routes are not part of the I-11 capstone runtime. Their controllers and OpenAPI operations are absent from the capstone profile, so they are not callable paths and are not included in G4-G6 evidence.
