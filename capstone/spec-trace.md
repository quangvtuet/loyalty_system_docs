# Spec-Trace Matrix — Loyalty Banking Platform Capstone

> **Capstone output — outside the modeling pack.**
> Traces every in-scope path, I-6 transition, sequence alt, and hard rule to its OpenAPI operation and automated test.
> Scope: I-11 only. Names are exact Lab 1 strings.
> **R** Dev (Lê Huy Du) · **A** SA (Vũ Trường Quang)

---

## 1. G6 State Transitions Trace (RedemptionOrder — I-6)

| G6 ID | Transition | SUT (I-4 Container) | OpenAPI operationId | Implementing Test Class | Test Method | Notes / Validation Criteria |
|---|---|---|---|---|---|---|
| **G6-T01** | `PENDING` → `IN_PROGRESS` | Redemption Engine Service | `createRedemptionOrder` | `redemption_engine/.../service/RedemptionServiceTest` | `testPlaceOrder_Success_TransitionsToInProgress` | Validation passes; balance locked; FIFO debit reserved |
| **G6-T02** | `PENDING` → `CANCELLED` | Redemption Engine Service | `cancelRedemptionOrder` | `redemption_engine/.../service/RedemptionServiceTest` | `testCancelOrder_WhenPending_TransitionsToCancelled` | Order cancelled before debit/fulfillment dispatch |
| **G6-T03** | `IN_PROGRESS` → `FULFILLED` | Redemption Engine Service | `fulfillRedemptionOrder` | `redemption_engine/.../service/RedemptionServiceTest` | `testFulfillOrder_Success_TransitionsToFulfilled` | Partner confirms delivery; debit made permanent |
| **G6-T04** | `IN_PROGRESS` → `FAILED` | Redemption Engine Service | `failAndReverseRedemptionOrder` | `redemption_engine/.../service/RedemptionServiceTest` | `testFailAndReverseOrder_RestoredWithFifo` | Partner returns delivery failure |
| **G6-T05** | `FAILED` → `REVERSED` | Redemption Engine Service | `failAndReverseRedemptionOrder` | `redemption_engine/.../service/RedemptionServiceTest` | `testFailAndReverseOrder_RestoredWithFifo` | Auto-reversal restores debited FIFO batches with original earn date & expiry (CON.3) |

---

## 2. G6 Sequence Alternates Trace (I-11 UC-LB-01 through UC-LB-04)

| G6 ID | Use Case | Alternate Condition | Lab 3 Contract | OpenAPI operationId | SUT | Test Class | Test Method | Spec Source |
|---|---|---|---|---|---|---|---|---|
| **G6-A01** | UC-LB-01 | Sequence UC-LB-01 alt: duplicate event (CON.1 / EXC-01) | CT-04 RecordEarn | `recordEarn` (409 response) | Earning Engine Service | `earning_engine/.../api/PartnerEarnControllerTest` | `testSubmitEarn_DuplicateTransaction_Returns409` | I-11, G6, I-5 (CON.1) |
| **G6-A02** | UC-LB-02 | Sequence UC-LB-02 alt: insufficient balance (ALT-01) | CT-13 DebitPointsFifo | `debitPointsFifo` (422 response) | Redemption Engine Service & Earning Engine Service | `redemption_engine/.../service/RedemptionServiceTest` & `earning_engine/.../api/FifoDebitControllerTest` | `testPlaceOrder_InsufficientBalance_Throws`, `testDebitFifo_InsufficientBalance_Returns422` | I-11, G6, ALT-01 |
| **G6-A02** | UC-LB-02 | Sequence UC-LB-02 alt: tier-ineligible (ALT-02) | CT-12 GetMemberTier | `createRedemptionOrder` (422 response) | Redemption Engine Service | `redemption_engine/.../service/RedemptionServiceTest` & `redemption_engine/.../api/RedemptionControllerTest` | `testPlaceOrder_TierEligibilityFailed_Throws`, `testPlaceOrder_TierEligibilityFailed_Returns422` | I-11, G6, ALT-02 |
| **G6-A03** | UC-LB-02 | Sequence UC-LB-02 alt: partner fulfillment failure (CON.3) | CT-14 RequestFulfilment / CT-13 RestorePoints | `failAndReverseRedemptionOrder` | Redemption Engine Service & Earning Engine Service | `redemption_engine/.../service/RedemptionServiceTest` & `earning_engine/.../service/EarningEngineServiceTest` | `testFailAndReverseOrder_RestoredWithFifo`, `testRestorePoints_RestoresToOriginalBatches_PreservesEarnDate` | I-11, G6, I-5 (CON.3) |
| **G6-A04** | UC-LB-03 | Sequence UC-LB-03 alt: replayed QP event (CON.1) | CT-06 qp_accrued consume | *(Kafka consumer path)* | Tiering System Service | `tiering_system/.../service/TierUpgradeIdempotencyTest` | `testEvaluateAndUpgrade_ReplayedEvent_TierNotMovedTwice` | I-11, G6, I-5 (CON.1) |
| **G6-A05** | UC-LB-04 | Sequence UC-LB-04 alt: warehouse data stale > 10 min (CON.4) | CT-26 GenerateReport | `getPointLiabilityReport` (stale:true) | Analytics & Reporting Service | `analytics_reporting/.../service/ReportingServiceTest` | `testGetFinancialLiability_StaleData_FlagsAsStale` | I-11, G6, I-5 (CON.4) |

---

## 3. Additional Invariants & Hard Rules Coverage (I-5, I-9, CON.1..4)

| Axis | Path Description | Test Class | Test Method | Coverage Tag |
|---|---|---|---|---|
| **I-11 Async Consume** | Core Banking settled event consumed via Kafka broker | `earning_engine/.../kafka/TransactionSettledConsumerTest` | `testConsume_HappyPath_ProcessesEarn`, `testConsume_DuplicateEvent_Skipped` | I-11, G3 |
| **I-5 CON.1** | Happy path: new earn event → PointTransaction written; no second write on replay | `earning_engine/.../service/EarningEngineServiceTest` | `testProcessEarn_HappyPath_TransactionWritten` | I-5, I-11 |
| **I-5 CON.1 (reversal)** | Core Banking reversal cancels PointTransaction and corrects PointBalance | `earning_engine/.../api/PartnerEarnControllerTest` | `testCancelEarn_HappyPath_Returns200` | I-5, EXC-02 |
| **I-5 FIFO Allocation** | Points debited in FIFO order from oldest unexpired batches; FifoDebitAllocation persisted | `earning_engine/.../service/EarningEngineServiceTest` | `testDebitPointsFifo_MultipleBatches_DebitsOldestFirst` | I-5, CT-13 |
| **I-5 CON.3** | Fulfillment failure → auto-reversal restores points with original FIFO earn date & allocations | `earning_engine/.../service/EarningEngineServiceTest` | `testRestorePoints_RestoresToOriginalBatches_PreservesEarnDate`, `testRestorePoints_IdempotentReplay_NoDoubleCredit` | I-5, CON.3, G5 |
| **I-5 Tamper Resistance (Redeem)** | Client cannot forge balance or tier; server reads I-7 owners (CT-12, CT-13) | `redemption_engine/.../service/I5BalanceTamperTest` | `testClientForgedTier_RejectedByServerQueryingCT12`, `testClientForgedBalance_RejectedByServerQueryingCT13` | I-5, CON.2, T3 |
| **NEG-I5-02 Tamper Resistance (Earn)** | Client cannot forge tier in earn event; server queries CT-12 (Tiering DB) and awards 1.0x points | `earning_engine/.../service/I5EarnTierTamperTest` | `testClientForgedTier_OverriddenByCT12_AwardsBaseMultiplierOnly`, `testLegitimatePlatinumMember_ConfirmedByCT12_ReceivesDoublePoints` | I-5, CON.2, CT-12, T3 |
| **NEG-I9-01..04 Zone Security (Earning DB)** | Non-owners (Partner Systems, Core Banking, CRM, Member) forbidden from direct write to Earning DB under EXC-04 | `earning_engine/.../security/I9SecurityIsolationTest` | `testPartnerSystems_DirectWriteToEarningDb_RefusedWithUnchangedLedger`, `testCoreBankingSystem_DirectWriteToEarningDb_RefusedWithUnchangedLedger`, `testCrmNotificationGateway_DirectWriteToEarningDb_RefusedWithUnchangedLedger`, `testMember_DirectWriteToEarningDb_RefusedWithUnchangedLedger`, `testEarningEngineService_AuthorizedOwnerWrite_AppendedSuccessfully` | I-9, EXC-04, CON.2, T4 |
| **I-5 CON.4** | Stale warehouse → report flagged, not presented as current | `analytics_reporting/.../service/ReportingServiceTest` | `testGetFinancialLiability_StaleData_FlagsAsStale` | I-5, CON.4 |
| **I-9 DW Isolation** | Analytics & Reporting Service only reads Data Warehouse | `analytics_reporting/.../service/ReportingServiceTest` | `testGetFinancialLiability_OnlyReadsDataWarehouse` | I-9, EXC-08 |

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
| `earning_engine/.../api/PartnerEarnControllerTest` | `testSubmitEarn_HappyPath_Returns202` | **POST 202** | `recordEarn` | I-11, T5 |
| `earning_engine/.../api/PartnerEarnControllerTest` | `testSubmitEarn_DuplicateTransaction_Returns409` | **POST 409** | `recordEarn` (CON.1) | G6-A01, T3, T5 |
| `earning_engine/.../api/PartnerEarnControllerTest` | `testSubmitEarn_MissingTransactionId_Returns400` | **POST 400** | `recordEarn` | T5 |
| `earning_engine/.../api/PartnerEarnControllerTest` | `testCancelEarn_HappyPath_Returns200` | **PATCH 200** | `cancelEarnTransaction` (EXC-02, CON.1) | I-11, T5 |
| `earning_engine/.../api/FifoDebitControllerTest` | `testDebitFifo_Success_Returns200` | **POST 200** | `debitPointsFifo` (CT-13) | I-11, T5 |
| `earning_engine/.../api/FifoDebitControllerTest` | `testDebitFifo_InsufficientBalance_Returns422` | **POST 422** | `debitPointsFifo` (CT-13) | G6-A02, T3, T5 |
| `earning_engine/.../api/FifoDebitControllerTest` | `testRestoreFifo_Success_Returns200` | **POST 200** | `restorePoints` (CT-13) | G6-T05, G5, T5 |
| `tiering_system/.../api/MemberTierControllerTest` | `testGetMemberTier_ExistingMember_Returns200` | **GET 200** | `getMemberTier` (CT-12) | I-11, T5 |
| `redemption_engine/.../api/RedemptionControllerTest` | `testPlaceOrder_HappyPath_Returns201` | **POST 201** | `createRedemptionOrder` | G6-T01, I-11, T5 |
| `redemption_engine/.../api/RedemptionControllerTest` | `testPlaceOrder_InsufficientBalance_Returns422` | **POST 422** | `createRedemptionOrder` (ALT-01) | G6-A02, T3, T5 |
| `redemption_engine/.../api/RedemptionControllerTest` | `testPlaceOrder_TierEligibilityFailed_Returns422` | **POST 422** | `createRedemptionOrder` (ALT-02) | G6-A02, T3 |
| `redemption_engine/.../api/RedemptionControllerTest` | `testPlaceOrder_MinimumPoints_Returns400` | **POST 400** | `createRedemptionOrder` (ALT-03) | T5 |
| `redemption_engine/.../api/RedemptionControllerTest` | `testFulfillOrder_HappyPath_Returns200` | **PATCH 200** | `fulfillRedemptionOrder` | G6-T03, T5 |
| `redemption_engine/.../api/RedemptionControllerTest` | `testFailOrder_CON3_CompensatingAction_Returns200` | **PATCH 200** | `failAndReverseRedemptionOrder` | G6-T04/T05, G5, T5 |
| `redemption_engine/.../api/RedemptionControllerTest` | `testCancelOrder_WhenPending_Returns200` | **PATCH 200** | `cancelRedemptionOrder` | G6-T02, T5 |
