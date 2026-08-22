# Name-Identity Map — Loyalty Banking Platform Capstone

> **Capstone output — outside the modeling pack.**
> Maps every Lab 1 I-4 container name to its implementing Java module, package, and primary class.
> Scope: I-11 only. Names are exact Lab 1 strings. No product names.
> **R** Dev (Lê Huy Du) · **A** SA (Vũ Trường Quang)

## I-4 Container → Implementation

| Lab 1 I-4 Container Name | I-9 Zone | Port | Java Module (directory) | Java Package | Primary Class(es) |
|---|---|---|---|---|---|
| API Gateway | Edge & Ingestion Zone | — | *(simulated via direct HTTP calls in tests; not a separate Java module in this implementation)* | — | — |
| Message Broker | Edge & Ingestion Zone | 9092 | *(Kafka — provided by `docker-compose.yml`)* | — | `loyalty.earning.qp_accrued` topic, `loyalty.tiering.tier_changed` topic |
| Earning Engine Service | Domain Services Zone | 8081 | `earning-engine/` | `com.loyalty.earning_engine` | `EarningEngineApplication`, `EarnCalculator`, `EarningLedgerService`, `IdempotencyService` |
| Tiering System Service | Domain Services Zone | 8082 | `tiering-system/` | `com.loyalty.tiering_system` | `TieringSystemApplication`, `MemberTierController`, `QpLedgerService`, `TierUpgradeService`, `GracePeriodService` |
| Redemption Engine Service | Domain Services Zone | 8083 | `redemption-engine/` | `com.loyalty.redemption_engine` | `RedemptionEngineApplication`, `RedemptionService`, `CatalogService`, `BalanceLockService`, `FifoDebitService` |
| Program Management Service | Domain Services Zone | 8084 | `program-management/` | `com.loyalty.program_management` | `ProgramManagementApplication`, `ProgramService`, `CampaignService`, `AuditLogService` |
| Analytics & Reporting Service | Analytics Zone | 8085 | `analytics-reporting/` | `com.loyalty.analytics_reporting` | `AnalyticsReportingApplication`, `ReportingService` |
| Idempotency Store | Data Services Zone | 6379 | *(Redis — provided by `docker-compose.yml`)* | `com.loyalty.earning_engine.service` | `IdempotencyService` (earning), `BalanceLockService` (redemption) |
| Earning DB | Data Services Zone | 5432 | *(PostgreSQL — provided by `docker-compose.yml`)* | `com.loyalty.earning_engine.domain` | `PointTransaction`, `PointBalance` |
| Tiering DB | Data Services Zone | 5432 | *(PostgreSQL — shared instance, separate schema prefix)* | `com.loyalty.tiering_system.domain` | `QpLedger`, `MemberTier` |
| Redemption DB | Data Services Zone | 5432 | *(PostgreSQL — shared instance)* | `com.loyalty.redemption_engine.domain` | `RedemptionOrder`, `RewardItem` |
| Program Mgmt DB | Data Services Zone | 5432 | *(PostgreSQL — shared instance)* | `com.loyalty.program_management.domain` | `LoyaltyProgram`, `Campaign`, `ConfigVersionLog` |
| Data Warehouse | Analytics Zone | 5432 | *(PostgreSQL — shared instance; star schema)* | `com.loyalty.analytics_reporting.domain` | `FactPointTransaction` |

## I-3 External System → Mock Strategy

Per capstone rules: **I-3 mocked** — no real host names, no production credentials.

| Lab 1 I-3 External System | Mock strategy | Location |
|---|---|---|
| Core Banking System | Simulated via `POST /api/v1/partners/earn` in tests and `e2e_test.sh` | `TransactionSettledConsumer` (Kafka consumer path) |
| Partner Systems | Simulated response via `RedemptionService.fulfillOrder()` / `failAndReverseOrder()` API calls | `RedemptionController` — PATCH endpoints |
| CRM & Notification Gateway | Logged only (`log.info(...)` in `RedemptionService`) — no real HTTP call | `RedemptionService.failAndReverseOrder()` |
| Enterprise Data Warehouse | Not connected — `FactPointTransactionRepository` reads from the local analytical schema | `ReportingService` |

## I-6 Object → Implementing Entity

| Lab 1 I-6 Object | Source of Truth (I-7) | Implementing JPA Entity | States |
|---|---|---|---|
| `RedemptionOrder` | Redemption DB | `com.loyalty.redemption_engine.domain.RedemptionOrder` | `PENDING`, `IN_PROGRESS`, `FULFILLED`, `FAILED`, `REVERSED`, `CANCELLED` |

## I-7 Data Object → Implementing Entity

| Lab 1 I-7 Data Object | Source of Truth | Implementing JPA Entity | Table Name |
|---|---|---|---|
| `PointTransaction` | Earning DB | `com.loyalty.earning_engine.domain.PointTransaction` | `point_transaction` |
| `PointBalance` | Earning DB | `com.loyalty.earning_engine.domain.PointBalance` | `point_balance` |
| `MemberTier` | Tiering DB | `com.loyalty.tiering_system.domain.MemberTier` | `member_tier` |
| `RedemptionOrder` | Redemption DB | `com.loyalty.redemption_engine.domain.RedemptionOrder` | `redemption_order` |
| `LoyaltyProgram` | Program Mgmt DB | `com.loyalty.program_management.domain.LoyaltyProgram` | `loyalty_program` |
| `Campaign` | Program Mgmt DB | `com.loyalty.program_management.domain.Campaign` | `campaign` |
| `FactPointTransaction` | Data Warehouse | `com.loyalty.analytics_reporting.domain.FactPointTransaction` | `fact_point_transaction` |

## Lab 3 Module (M-series) → Implementing Class

| Lab 3 Module | Responsibility | Implementing Class |
|---|---|---|
| M1 Catalog Query | Serves reward catalogue | `com.loyalty.redemption_engine.service.CatalogService` |
| M2 Order Intake | Creates `RedemptionOrder` in `PENDING` | `com.loyalty.redemption_engine.service.RedemptionService.placeOrder()` |
| M3 Eligibility Check | Tier and balance validation | `com.loyalty.redemption_engine.service.CatalogService.validateTierEligibility()` + inline balance check in `RedemptionService` |
| M4 Balance Lock | Distributed per-member debit lock | `com.loyalty.redemption_engine.service.BalanceLockService` |
| M5 FIFO Debit Request | Oldest-points-first debit | `com.loyalty.redemption_engine.service.FifoDebitService.debitFifo()` |
| M6 Order State Keeper | State transitions + persistence | `com.loyalty.redemption_engine.service.RedemptionService` (all state transitions) |
| M7 Fulfilment Dispatch | Sends fulfillment request | `com.loyalty.redemption_engine.api.RedemptionController.fulfillOrder()` (simulated callback) |
| M8 Reversal Handler | Point restoration on failure | `com.loyalty.redemption_engine.service.RedemptionService.failAndReverseOrder()` + `FifoDebitService.reverseDebit()` |
