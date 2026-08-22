# Name-Identity Map — Loyalty Banking Platform Capstone

> **Capstone output — outside the modeling pack.**
> Maps every Lab 1 I-4 container name to its implementing Java module, package, and primary class.
> Scope: I-11 only. Names are exact Lab 1 strings. No product names.
> **R** Dev (Lê Huy Du) · **A** SA (Vũ Trường Quang)

---

## 1. I-4 Container → Implementation

| Lab 1 I-4 Container Name | I-9 Zone | Port | Java Module (directory) | Java Package | Primary Class(es) |
|---|---|---|---|---|---|
| API Gateway | Edge & Ingestion Zone | — | *(simulated via direct HTTP routing in Spring Web layer)* | — | `PartnerEarnController`, `RedemptionController`, `ReportingController` |
| Message Broker | Edge & Ingestion Zone | 9092 | *(Kafka message bus / test double)* | — | `loyalty.earning.qp_accrued` topic, `loyalty.tiering.tier_changed` topic, `TransactionSettledConsumer` |
| Earning Engine Service | Domain Services Zone | 8081 | `earning-engine/` | `com.loyalty.earning_engine` | `EarningEngineApplication`, `EarnCalculator`, `EarningLedgerService`, `FifoDebitController`, `PartnerEarnController`, `IdempotencyService`, `TransactionSettledConsumer` |
| Tiering System Service | Domain Services Zone | 8082 | `tiering-system/` | `com.loyalty.tiering_system` | `TieringSystemApplication`, `MemberTierController`, `QpLedgerService`, `TierUpgradeService`, `GracePeriodService`, `QpAccruedConsumer` |
| Redemption Engine Service | Domain Services Zone | 8083 | `redemption-engine/` | `com.loyalty.redemption_engine` | `RedemptionEngineApplication`, `RedemptionController`, `RedemptionService`, `CatalogService`, `BalanceLockService`, `FifoDebitService`, `TieringClient` |
| Analytics & Reporting Service | Analytics Zone | 8085 | `analytics-reporting/` | `com.loyalty.analytics_reporting` | `AnalyticsReportingApplication`, `ReportingController`, `ReportingService` |
| Program Management Service | Domain Services Zone | — | *(out-of-scope administration; excluded from I-11 runtime)* | — | Not exposed or started by the I-11 capstone runtime |
| Idempotency Store | Data Services Zone | 6379 | *(Redis key-value store / in-memory double)* | `com.loyalty.earning_engine.service` | `IdempotencyService` (earning CON.1), `BalanceLockService` (redemption ALT-06) |
| Earning DB | Data Services Zone | 5432 | *(PostgreSQL — schema: earning / in-memory double)* | `com.loyalty.earning_engine.domain` | `PointTransaction`, `PointBalance`, `FifoDebitAllocation` |
| Tiering DB | Data Services Zone | 5432 | *(PostgreSQL — schema: tiering / in-memory double)* | `com.loyalty.tiering_system.domain` | `QpLedger`, `MemberTier` |
| Redemption DB | Data Services Zone | 5432 | *(PostgreSQL — schema: redemption / in-memory double)* | `com.loyalty.redemption_engine.domain` | `RedemptionOrder`, `RewardItem` |
| Data Warehouse | Analytics Zone | 5432 | *(PostgreSQL — schema: analytics / in-memory double)* | `com.loyalty.analytics_reporting.domain` | `FactPointTransaction`, `DimProgram` |

---

## 2. Documented Deployment Collapse & Infrastructure Mappings

Per capstone rules, all deployable consolidations are explicitly mapped below so no unauthorized identities exist:

| Collapsed Infrastructure | Represents I-4 Container(s) | Stand-in / Realization Strategy | I-9 Zone Mapped |
|---|---|---|---|
| Test-profile persistence doubles | `Earning DB`, `Tiering DB`, `Redemption DB`, `Data Warehouse` | In-memory/H2 test repositories stand in for the named I-7 locations with distinct schema isolation (`earning`, `tiering`, `redemption`, `analytics`). No database product is deployed by the capstone runtime. | Data Services Zone & Analytics Zone |
| Test-profile event double | `Message Broker` | In-process test event boundary stands in for named asynchronous topics (`loyalty.earning.qp_accrued`, `corebanking.transactions.settled`). No Kafka or Zookeeper product is deployed by the capstone runtime. | Edge & Ingestion Zone |
| Test-profile lock/idempotency double | `Idempotency Store` | In-memory test double stands in for the named lock/idempotency location. No Redis product is deployed by the capstone runtime. | Data Services Zone |
| Direct HTTP Endpoint Routing | `API Gateway` | API Gateway routing is simulated via direct REST requests to the four in-scope service controller ports. | Edge & Ingestion Zone |

---

## 3. I-3 External System → Mock Strategy

Per capstone rules: **I-3 mocked** — no real host names, no production credentials.

| Lab 1 I-3 External System | Mock strategy | Location |
|---|---|---|
| Core Banking System | Simulated via `POST /api/v1/partners/earn` in REST tests and `TransactionSettledConsumerTest` for Kafka event consumption | `TransactionSettledConsumer`, `PartnerEarnController` |
| Partner Systems | Simulated response via `RedemptionService.fulfillOrder()` / `failAndReverseOrder()` PATCH endpoints | `RedemptionController` (`PATCH /api/v1/redemptions/orders/{orderId}/fulfill`, `.../fail`) |
| CRM & Notification Gateway | Logged only (`log.info(...)` in `RedemptionService`) — no real external HTTP call | `RedemptionService.failAndReverseOrder()` |
| Enterprise Data Warehouse | Not connected — `FactPointTransactionRepository` reads from local analytical schema | `ReportingService` |

---

## 4. I-6 Object → Implementing Entity

| Lab 1 I-6 Object | Source of Truth (I-7) | Implementing JPA Entity | States |
|---|---|---|---|
| `RedemptionOrder` | Redemption DB | `com.loyalty.redemption_engine.domain.RedemptionOrder` | `PENDING`, `IN_PROGRESS`, `FULFILLED`, `FAILED`, `REVERSED`, `CANCELLED` |

---

## 5. I-7 Data Object → Implementing Entity

| Lab 1 I-7 Data Object | Source of Truth | Implementing JPA Entity | Table Name |
|---|---|---|---|
| `PointTransaction` | Earning DB | `com.loyalty.earning_engine.domain.PointTransaction` | `point_transaction` |
| `PointBalance` | Earning DB | `com.loyalty.earning_engine.domain.PointBalance` | `point_balance` |
| `FifoDebitAllocation` | Earning DB | `com.loyalty.earning_engine.domain.FifoDebitAllocation` | `fifo_debit_allocation` |
| `MemberTier` | Tiering DB | `com.loyalty.tiering_system.domain.MemberTier` | `member_tier` |
| `RedemptionOrder` | Redemption DB | `com.loyalty.redemption_engine.domain.RedemptionOrder` | `redemption_order` |
| `FactPointTransaction` | Data Warehouse | `com.loyalty.analytics_reporting.domain.FactPointTransaction` | `fact_point_transaction` |

---

## 6. Lab 3 Module (M-series) → Implementing Class

| Lab 3 Module | Responsibility | Implementing Class |
|---|---|---|
| M1 Catalog Query | Serves reward catalogue | `com.loyalty.redemption_engine.service.CatalogService` |
| M2 Order Intake | Creates `RedemptionOrder` and transitions `PENDING` → `IN_PROGRESS` | `com.loyalty.redemption_engine.service.RedemptionService.placeOrder()` |
| M3 Eligibility Check | Tier validation via Tiering System Service (CT-12) & item cost checks | `com.loyalty.redemption_engine.client.TieringClient` + `CatalogService.validateTierEligibility()` |
| M4 Balance Lock | Distributed per-member debit lock | `com.loyalty.redemption_engine.service.BalanceLockService` |
| M5 FIFO Debit Request | Requests oldest-points-first debit from Earning Engine Service (CT-13) | `com.loyalty.redemption_engine.service.FifoDebitService.debitFifo()` → `com.loyalty.earning_engine.service.EarningLedgerService.debitPointsFifo()` |
| M6 Order State Keeper | State transitions + persistence (`PENDING` → `IN_PROGRESS` → `FULFILLED` / `FAILED` → `REVERSED` / `CANCELLED`) | `com.loyalty.redemption_engine.service.RedemptionService` |
| M7 Fulfilment Dispatch | Sends fulfillment request | `com.loyalty.redemption_engine.api.RedemptionController.fulfillOrder()` (simulated callback) |
| M8 Reversal Handler | Point restoration on failure via Earning Engine Service (CT-13) | `com.loyalty.redemption_engine.service.FifoDebitService.reverseDebit()` → `com.loyalty.earning_engine.service.EarningLedgerService.restorePoints()` |
