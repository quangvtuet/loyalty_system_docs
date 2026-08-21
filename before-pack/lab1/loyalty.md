<<<<<<< HEAD
# Lab 1 Input Index: Scopes with Concrete Values
=======
# Loyalty Banking

## Domain

Loyalty Banking

## Scope

1. Earning Engine
2. Tiering System
3. Redemption Engine
4. Program Management
5. Analytics and Reporting

---

## Lab 1 Input Index: Scopes with Concrete Values
>>>>>>> 677ef1ed5df32872dd109e318ea09a27500b12ee

**R**: BA / SA  
**A**: Owner  
**Hard limits**: Simulated scenario only. No real customer data, no production system names, no production credentials.

<<<<<<< HEAD
## I-1. Team and Topic

| Field | Your value |
|---|---|
| Group | Team 2 — Vũ Trường Quang (TN), Khuất Duy Bách, Đặng Duy Hoàng, Lê Huy Du |
=======
### I-1. Team and Topic

| Field | Your value |
|---|---|
| Group | Loyalty Banking Modeling Team |
>>>>>>> 677ef1ed5df32872dd109e318ea09a27500b12ee
| Topic / initiative name | Loyalty Banking Platform Modernization |
| System-in-focus | Loyalty Banking Platform |
| Goal | Provide a governed loyalty platform for earning points, tier progression, redemption, program configuration, and financial reporting. |
| Outcome (measurable) | Process settled earn events within 60 seconds, prevent duplicate point postings, support FIFO redemption, and provide analytics data with no more than 10 minutes staleness. |
| Product | Banking Rewards Program |
| Contract | Simulated partner earn/redeem contract and internal reporting contract |
| Baseline -> target | Baseline: fragmented loyalty processes and manual reconciliation. Target: model-driven modular platform with traceable requirements, contracts, states, and tests. |
<<<<<<< HEAD
| In scope | API Gateway, Message Broker, Earning Engine Service, Tiering System Service, Redemption Engine Service, Program Management Service, Analytics & Reporting Service, Idempotency Store, Earning DB, Tiering DB, Redemption DB, Program Mgmt DB, Data Warehouse |
| Out of scope | Production customer data, real vendor host names, production credentials, real bank infrastructure, installing additional IAM or broker products for the modeling pack. |

## I-2. Actors

| Name | ArchiMate | C4 (Person or -) | Role in the process |
|---|---|---|---|
| Member | Business Actor | Person | Earns points, checks balance, and redeems rewards |
| Program Admin | Business Actor | Person | Configures programs, campaigns, and rules |
| Finance | Business Actor | Person | Reviews liability, breakage, and reconciliation reports |
| Support Agent | Business Actor | Person | Initiates manual adjustments and reversal support |

## I-3. External Systems
=======
| In scope | Earning Engine, Tiering System, Redemption Engine, Program Management, Analytics & Reporting, API Gateway, Message Broker, Redis lock/cache, service databases, analytics warehouse. |
| Out of scope | Production customer data, real vendor host names, production credentials, real bank infrastructure, installing additional IAM/broker products for the modeling pack. |

### I-2. Actors

| Name | ArchiMate | C4 (Person or -) | Role in the process |
|---|---|---|---|
| Member / Customer | Business Actor | Person | Earns points, checks balance, redeems rewards |
| Program Admin / Marketing | Business Actor | Person | Configures programs, campaigns, and rules |
| Finance & Compliance | Business Actor | Person | Reviews liability, breakage, and reconciliation reports |
| Support Agent | Business Actor | Person | Initiates manual adjustments and reversal support |
| Core Banking System | Application Component / External Application | - | Publishes settled transaction events |
| Partner Systems | Application Component / External Application | - | Submit partner earn events and receive fulfillment requests |

### I-3. External Systems
>>>>>>> 677ef1ed5df32872dd109e318ea09a27500b12ee

| Name (simulated) | Responsibility |
|---|---|
| Core Banking System | Source of settled transaction events, authorizations, and reversals |
| Partner Systems | External merchant and fulfillment participants for earn and redeem scenarios |
| CRM & Notification Gateway | Customer profile reference and outbound notification delivery |
<<<<<<< HEAD
| Enterprise Data Warehouse | Enterprise archival and group-wide business intelligence destination |

## I-4. Internal Containers
=======
| Enterprise Data Warehouse / Lake | Enterprise archival and group-wide business intelligence destination |

### I-4. Internal Containers
>>>>>>> 677ef1ed5df32872dd109e318ea09a27500b12ee

Same strings are used on ArchiMate Application Cooperation and C4 Container views.

| Name | Responsibility |
|---|---|
<<<<<<< HEAD
| API Gateway | Routes member, admin, finance, support, and partner calls; owns authentication and rate-limit enforcement at the edge |
| Message Broker | Carries settled transaction, tier, redemption, configuration, and CDC event streams |
=======
| API Gateway / OAuth 2.0 Auth Server | Routes member, admin, finance, support, and partner calls; owns authentication and rate-limit enforcement at the edge |
| Message Broker - Apache Kafka | Carries settled transaction, tier, redemption, configuration, and CDC event streams |
>>>>>>> 677ef1ed5df32872dd109e318ea09a27500b12ee
| Earning Engine Service | Calculates points, enforces idempotency, writes the earning ledger, and publishes QP accrual events |
| Tiering System Service | Accrues qualifying points, evaluates tier upgrades/downgrades, and publishes tier change events |
| Redemption Engine Service | Manages reward catalog, validates redemption, performs FIFO debit, and handles fulfillment reversal |
| Program Management Service | Configures loyalty programs, campaigns, rules, partner settings, and manual adjustments |
| Analytics & Reporting Service | Ingests CDC data, computes KPIs, and exposes reports and dashboards |
<<<<<<< HEAD
| Idempotency Store | Provides idempotency cache, distributed debit locks, and rate-limit support |
| Earning DB | Stores point transactions and point balances |
| Tiering DB | Stores QP ledger and member tier state |
| Redemption DB | Stores reward items and redemption orders |
| Program Mgmt DB | Stores loyalty programs, campaigns, and configuration audit logs |
| Data Warehouse | Stores analytical facts, dimensions, and KPI materializations |

## I-5. Business Process (Happy Path)

Named business object that moves: `PointTransaction`.

1. Core Banking System publishes a settled transaction event for a Member.
2. Earning Engine Service validates the event, checks idempotency against Idempotency Store, calculates base and campaign points, and writes a `PointTransaction` to Earning DB.
3. Earning Engine Service publishes QP accrual to Message Broker.
4. Tiering System Service consumes QP accrual from Message Broker and updates `MemberTier` in Tiering DB if a threshold is crossed.
5. Member redeems a reward through API Gateway.
6. Redemption Engine Service validates tier and balance, locks the member balance via Idempotency Store, and performs FIFO debit in Redemption DB.
7. Partner Systems fulfill the reward.
8. Analytics & Reporting Service consumes CDC events from Message Broker and updates liability and engagement reporting in Data Warehouse.
=======
| Redis Cache & Distributed Lock | Provides idempotency cache, distributed debit locks, and rate-limit support |
| Earning DB - PostgreSQL | Stores point transactions and point balances |
| Tiering DB - PostgreSQL | Stores QP ledger and member tier state |
| Redemption DB - PostgreSQL | Stores reward items and redemption orders |
| Program Mgmt DB - PostgreSQL | Stores loyalty programs, campaigns, and configuration audit logs |
| Data Warehouse - Star Schema | Stores analytical facts, dimensions, and KPI materializations |

### I-5. Business Process (Happy Path)

Named business object that moves: `PointTransaction`.

1. Core Banking System publishes a settled transaction event for a member.
2. Earning Engine Service validates the event, checks idempotency, calculates base and campaign points, and writes a `PointTransaction`.
3. Earning Engine Service publishes QP accrual to Message Broker - Apache Kafka.
4. Tiering System Service consumes QP accrual and updates `MemberTier` if a threshold is crossed.
5. Member / Customer redeems a reward through API Gateway / OAuth 2.0 Auth Server.
6. Redemption Engine Service validates tier and balance, locks the member balance, and performs FIFO debit.
7. Partner Systems fulfill the reward.
8. Analytics & Reporting Service consumes CDC events and updates liability and engagement reporting.
>>>>>>> 677ef1ed5df32872dd109e318ea09a27500b12ee

**Principle / hard rules**:

- Channel or external systems must not write directly to any core ledger database.
- The same source transaction must not create duplicate point postings.
- Reporting queries must not degrade transactional workload.
- Point reversal must preserve original FIFO earn date and expiry information.

<<<<<<< HEAD
## I-6. Named Object States

**Object:** `RedemptionOrder`

States and transitions:

| State | Trigger | Next | Terminal? |
|---|---|---|---|
| PENDING | Balance, tier, and minimum-points validation pass; FIFO debit is reserved | IN_PROGRESS | No |
| PENDING | Validation fails or member cancels before processing | CANCELLED | Yes |
| IN_PROGRESS | Partner confirms reward delivery | FULFILLED | Yes |
| IN_PROGRESS | Partner fulfillment fails | FAILED | No |
| FAILED | Auto-reversal restores points | REVERSED | Yes |

Terminal states: `CANCELLED`, `FULFILLED`, `REVERSED`.

## I-7. Source of Truth

| Data object | Meaning | Source of truth (one container or external) |
|---|---|---|
| `PointTransaction` | Immutable point ledger movement | Earning DB |
| `PointBalance` | Current member point balance snapshot | Earning DB |
| `MemberTier` | Current and prior member tier state | Tiering DB |
| `RedemptionOrder` | Reward order lifecycle and fulfillment status | Redemption DB |
| `LoyaltyProgram` | Program configuration and lifecycle | Program Mgmt DB |
| `Campaign` | Promotional configuration and priority | Program Mgmt DB |
| `FactPointTransaction` | Analytical point transaction fact | Data Warehouse |

## I-8. Integration

| Pattern | Mechanism | Example on your landscape |
|---|---|---|
| Sync | HTTPS REST / gRPC through API Gateway | Member submits redemption order to Redemption Engine Service |
| Async | Event stream through Message Broker | Earning Engine Service publishes QP accrual to Tiering System Service |
| Legacy / adapter (if any) | Standardized API adapter through API Gateway | Partner Systems submit partner earn events through API Gateway |

## I-9. Deployment

| Location | What runs there |
|---|---|
| Edge & Ingestion Zone | API Gateway, Message Broker |
| Domain Services Zone | Earning Engine Service, Tiering System Service, Redemption Engine Service, Program Management Service |
| Analytics Zone | Analytics & Reporting Service, Data Warehouse |
| Data Services Zone | Earning DB, Tiering DB, Redemption DB, Program Mgmt DB, Idempotency Store |

Forbidden path: Member, Partner Systems, CRM & Notification Gateway, and Core Banking System must not write directly to Earning DB or any other service-owned database.

## I-10. Constraints
=======
### I-6. Named Object States

**Object:** `RedemptionOrder`

| From | To | Trigger |
|---|---|---|
| Start | PENDING | Member submits redemption request |
| PENDING | IN_PROGRESS | Balance, tier, and minimum-points validation pass; FIFO debit is reserved |
| PENDING | CANCELLED | Validation fails or member cancels before processing |
| IN_PROGRESS | FULFILLED | Partner confirms reward delivery |
| IN_PROGRESS | FAILED | Partner fulfillment fails |
| FAILED | REVERSED | Auto-reversal restores points |

Terminal states: `CANCELLED`, `FULFILLED`, `REVERSED`.

### I-7. Source of Truth

| Data object | Meaning | Source of truth (one container or external) |
|---|---|---|
| `PointTransaction` | Immutable point ledger movement | Earning DB - PostgreSQL |
| `PointBalance` | Current member point balance snapshot | Earning DB - PostgreSQL |
| `MemberTier` | Current and prior member tier state | Tiering DB - PostgreSQL |
| `RedemptionOrder` | Reward order lifecycle and fulfillment status | Redemption DB - PostgreSQL |
| `LoyaltyProgram` | Program configuration and lifecycle | Program Mgmt DB - PostgreSQL |
| `Campaign` | Promotional configuration and priority | Program Mgmt DB - PostgreSQL |
| `FactPointTransaction` | Analytical point transaction fact | Data Warehouse - Star Schema |

### I-8. Integration

| Pattern | Mechanism | Example on your landscape |
|---|---|---|
| Sync | HTTPS REST or gRPC through API Gateway / OAuth 2.0 Auth Server | Member submits redemption order to Redemption Engine Service |
| Async | Kafka event stream through Message Broker - Apache Kafka | Earning Engine Service publishes QP accrual to Tiering System Service |
| Legacy / adapter (if any) | Simulated edge adapter through API Gateway / OAuth 2.0 Auth Server | Partner Systems submit partner earn events through standardized API |

### I-9. Deployment

| Location | What runs there |
|---|---|
| Edge & Ingestion Zone | API Gateway / OAuth 2.0 Auth Server, Message Broker - Apache Kafka |
| Domain Services Zone | Earning Engine Service, Tiering System Service, Redemption Engine Service, Program Management Service |
| Analytics Zone | Analytics & Reporting Service, Data Warehouse - Star Schema |
| Data Services Zone | PostgreSQL service databases and Redis Cache & Distributed Lock |

Forbidden path: Member / Customer, Partner Systems, CRM & Notification Gateway, and Core Banking System must not write directly to Earning DB - PostgreSQL or any other service-owned database.

### I-10. Constraints
>>>>>>> 677ef1ed5df32872dd109e318ea09a27500b12ee

| ID | Constraint | Effect on the process |
|---|---|---|
| CON.1 | No duplicate point posting for the same source transaction | Earning Engine Service must perform idempotency check before ledger write |
<<<<<<< HEAD
| CON.2 | No direct database writes from channels, partners, or other services into service-owned databases | All changes must pass through owning service APIs or event handlers |
| CON.3 | Fulfillment failure must compensate by restoring points with original FIFO earn date and expiry | Redemption Engine Service must execute auto-reversal and notify the member |
| CON.4 | Analytics reporting data must not exceed 10 minutes staleness | Analytics & Reporting Service must refresh KPI and fact tables within 10 minutes of CDC event capture |

## I-11. Named Use Cases for UML
=======
| CON.2 | No direct database writes from channels, partners, or other services into service-owned databases | All changes must pass through owning service APIs/events |
| CON.3 | Fulfillment failure must compensate by restoring points with original FIFO earn date and expiry | Redemption Engine Service must execute auto-reversal and notify the member |

### I-11. Named Use Cases for UML
>>>>>>> 677ef1ed5df32872dd109e318ea09a27500b12ee

| Use case | Happy path | At least one exception (`alt`) |
|---|---|---|
| UC-LB-01 Process settled earn event | Core Banking System publishes settled event; Earning Engine Service writes confirmed point transaction; QP event is published | Duplicate event detected under CON.1 |
| UC-LB-02 Redeem reward with FIFO | Member submits order; Redemption Engine Service locks balance, validates tier, allocates FIFO batches, dispatches fulfillment | Insufficient balance, tier-ineligible reward, or partner fulfillment failure under CON.3 |
| UC-LB-03 Apply tier upgrade | Tiering System Service consumes QP accrual and upgrades tier when threshold is reached | Event replay is ignored by idempotent event handling |
<<<<<<< HEAD
| UC-LB-04 Generate point liability report | Analytics & Reporting Service reads warehouse facts and computes liability report | Warehouse data is stale beyond 10-minute SLA under CON.4 |

**One container** for optional C4 Component: `Redemption Engine Service`.
=======
| UC-LB-04 Generate point liability report | Analytics & Reporting Service reads warehouse facts and computes liability report | Warehouse data is stale beyond CON-derived reporting SLA |

**One container** for optional C4 Component: `Redemption Engine Service`.

---

## Lab 2 Output — Requirements, Analysis, Quality Gates

**R**: BA (requirements) · EA (trace to Motivation) · **A**: Owner  
**Rule**: After pass. G1–G6 from Guide as written. No extra gates.

### Gate Register (After Pass)

| Gate | Pass rule (Loyalty Banking wording) | Evidence artifact | Pass? |
|---|---|---|---|
| **G1** Strategy signed | Goal "governed loyalty platform for earn/tier/redeem/reporting" and measurable outcome (60 s earn SLA, zero duplicates, FIFO redemption, ≤10 min analytics lag) and CON.1–CON.3 are listed in Lab 1 I-1 and I-10; Motivation/Strategy views exist | [`loyalty.md` I-1, I-10](file:///Users/hoangdd/Desktop/personal/workspace/AI-study-2/loyalty.md) · [`archimate/motivation-layer.md`](file:///Users/hoangdd/Desktop/personal/workspace/AI-study-2/architecture/archimate/motivation-layer.md) · [`archimate/strategy-layer.md`](file:///Users/hoangdd/Desktop/personal/workspace/AI-study-2/architecture/archimate/strategy-layer.md) | ✅ |
| **G2** Process + states | Named states `PENDING`, `IN_PROGRESS`, `FULFILLED`, `FAILED`, `CANCELLED`, `REVERSED` for `RedemptionOrder` match state view and appear on Business Process branches | [`loyalty.md` I-6](file:///Users/hoangdd/Desktop/personal/workspace/AI-study-2/loyalty.md) · [`design/entity-lifecycle-models.md` §3](file:///Users/hoangdd/Desktop/personal/workspace/AI-study-2/design/entity-lifecycle-models.md) · [`archimate/business-layer.md`](file:///Users/hoangdd/Desktop/personal/workspace/AI-study-2/architecture/archimate/business-layer.md) | ✅ |
| **G3** C4 Context + Container | No unnamed externals (I-3 names 4 externals); sync/async edges labeled (I-8); all container names = I-4 strings | [`architecture/C4/context.png`](file:///Users/hoangdd/Desktop/personal/workspace/AI-study-2/architecture/C4/context.png) · [`architecture/C4/container.png`](file:///Users/hoangdd/Desktop/personal/workspace/AI-study-2/architecture/C4/container.png) · [`loyalty.md` I-3, I-4, I-8](file:///Users/hoangdd/Desktop/personal/workspace/AI-study-2/loyalty.md) | ✅ |
| **G4** Contracts | One contract row per C4 Container relationship in Lab 3 Contract Register | [Lab 3 Contract Register G4 — this file §Lab 3](#lab-3-output) | ✅ |
| **G5** Critical exception path | CON.3 fulfillment failure compensating action (auto-reversal restoring FIFO earn date and expiry) is modeled in Redemption Engine sequence and exception spec | [`design/DD-03-redemption-engine.md` §5.2 FLOW-05](file:///Users/hoangdd/Desktop/personal/workspace/AI-study-2/design/DD-03-redemption-engine.md) · [Lab 3 Exception Spec G5 — this file §Lab 3](#lab-3-output) | ✅ |
| **G6** Test coverage | All `RedemptionOrder` state transitions and sequence `alt` branches in I-11 use cases are mapped to planned test rows with C4 SUT names | [`design/entity-lifecycle-models.md` Transition Rules](file:///Users/hoangdd/Desktop/personal/workspace/AI-study-2/design/entity-lifecycle-models.md) · [Lab 3 Test Spec G6 — this file §Lab 3](#lab-3-output) | ✅ |

### Trace Table — Requirement ID → Process Step → CON.* → Named Object/State

| Requirement ID | Process step (I-5) | CON.* triggered | Named object → state |
|---|---|---|---|
| FR-01-010 UC-LB-01 Receive earn event | Step 2 — Earning Engine validates event and checks idempotency | CON.1 — no duplicate posting for same source transaction | `PointTransaction` → `PENDING` |
| FR-01-031 Confirm on settlement | Step 2 — Settlement confirmation received from Core Banking | CON.1 — idempotency check passed before ledger write | `PointTransaction` → `CONFIRMED` |
| FR-01-032 Cancel on reversal | Step 2 — Source transaction reversed before settlement | CON.1 — no posting if source reversed | `PointTransaction` → `CANCELLED` |
| FR-01-051 Point expiry sweep | Step 2 (background) — Cron job sweeps expired balances | CON.1 — no re-post of expired batches | `PointTransaction` → `EXPIRED` |
| FR-02-010 UC-LB-03 QP accrual and tier upgrade | Step 3–4 — Earning Engine publishes QP; Tiering System evaluates threshold | CON.2 — Tiering Service must consume event, not read Earning DB directly | `MemberTier` → `ACTIVE` (upgraded) |
| FR-02-030 Grace period evaluation | Step 4 — Batch downgrades member below threshold after grace window | CON.2 — Tiering Service owns tier state; no cross-DB write | `MemberTier` → `IN_GRACE_PERIOD` → `DOWNGRADED` |
| FR-03-010 UC-LB-02 Submit redemption order | Step 5–6 — Member submits order; Redemption Engine validates tier and balance | CON.2 — all validation through Redemption Engine API | `RedemptionOrder` → `PENDING` |
| FR-03-020 FIFO debit reserved | Step 6 — Redemption Engine locks balance and allocates FIFO batches | CON.3 — FIFO earn date preserved for reversal | `RedemptionOrder` → `IN_PROGRESS` |
| FR-03-030 Partner confirms delivery | Step 7 — Partner Systems confirm fulfillment | CON.3 — fulfillment success closes debit | `RedemptionOrder` → `FULFILLED` |
| FR-03-040 Partner failure → auto-reversal | Step 7 (alt) — Partner fulfillment fails; auto-reversal triggered | CON.3 — compensating action restores points with original earn date and expiry | `RedemptionOrder` → `FAILED` → `REVERSED` |
| FR-04-010 Rule change (prospective) | Step (background) — Program Admin configures rule with effective date | CON.2 — Program Management Service is the only writer to Program Mgmt DB | `LoyaltyProgram` → `ACTIVE` |
| FR-05-010 UC-LB-04 Liability report | Step 8 — Analytics Service reads warehouse facts | CON.2 — Analytics queries DW, not OLTP databases | `FactPointTransaction` (DW) — read-only |

---

## Lab 3 Output — Architecture, Design, and Test Artifacts {#lab-3-output}

**R**: Dev (build list, Component, contracts) · Test (test spec) · **A**: SA  
**Rule**: One selected container = `Redemption Engine Service` (I-11). Same names as Lab 1.

### Build List

Every I-4 container with owner, build order, and environment from I-9.

| # | Container name (I-4) | Owner (role) | Build order | Environment (I-9 zone) |
|---|---|---|---|---|
| 1 | Message Broker - Apache Kafka | Ops | 1 | Edge & Ingestion Zone |
| 2 | Redis Cache & Distributed Lock | Ops | 2 | Data Services Zone |
| 3 | API Gateway / OAuth 2.0 Auth Server | SA | 3 | Edge & Ingestion Zone |
| 4 | Earning DB - PostgreSQL | DA | 4 | Data Services Zone |
| 5 | Tiering DB - PostgreSQL | DA | 5 | Data Services Zone |
| 6 | Redemption DB - PostgreSQL | DA | 6 | Data Services Zone |
| 7 | Program Mgmt DB - PostgreSQL | DA | 7 | Data Services Zone |
| 8 | Data Warehouse - Star Schema | DA | 8 | Analytics Zone |
| 9 | Earning Engine Service | Dev | 9 | Domain Services Zone |
| 10 | Tiering System Service | Dev | 10 | Domain Services Zone |
| 11 | Redemption Engine Service | Dev | 11 | Domain Services Zone |
| 12 | Program Management Service | Dev | 12 | Domain Services Zone |
| 13 | Analytics & Reporting Service | Dev | 13 | Analytics Zone |

### Contract Register (G4)

One row per C4 Container relationship. Producer, consumer, sync or async, operation or event name.

| # | Producer (C4 name) | Consumer (C4 name) | Sync / Async | Operation or Event name |
|---|---|---|---|---|
| C-01 | Core Banking System *(external)* | Message Broker - Apache Kafka | Async | `EVT-001` · Topic: `corebanking.transactions.settled` |
| C-02 | Partner Systems *(external)* | API Gateway / OAuth 2.0 Auth Server | Sync | `POST /v1/earning/partner-events` (OAuth 2.0 Client Credentials) |
| C-03 | API Gateway / OAuth 2.0 Auth Server | Earning Engine Service | Sync | `POST /v1/earning/events` (REST) |
| C-04 | Earning Engine Service | Redis Cache & Distributed Lock | Sync | `SET NX EX` idempotency key on `transaction_ref_id` |
| C-05 | Earning Engine Service | Earning DB - PostgreSQL | Sync | `INSERT point_transaction`; `UPDATE point_balance` (JDBC) |
| C-06 | Earning Engine Service | Message Broker - Apache Kafka | Async | `EVT-002` · Topic: `loyalty.earning.qp_accrued` |
| C-07 | Message Broker - Apache Kafka | Tiering System Service | Async | `EVT-002` · Topic: `loyalty.earning.qp_accrued` (Kafka Consume) |
| C-08 | Tiering System Service | Tiering DB - PostgreSQL | Sync | `UPDATE qp_ledger`; `UPDATE member_tier` (JDBC) |
| C-09 | Tiering System Service | Message Broker - Apache Kafka | Async | `EVT-003` · Topic: `loyalty.tiering.tier_changed` |
| C-10 | Member / Customer *(actor)* | API Gateway / OAuth 2.0 Auth Server | Sync | `POST /v1/redemption/orders` (JWT Bearer) |
| C-11 | API Gateway / OAuth 2.0 Auth Server | Redemption Engine Service | Sync | `POST /v1/redemption/orders` (REST) |
| C-12 | Redemption Engine Service | Earning Engine Service | Sync | `GET /v1/earning/balance/{memberId}` (REST) |
| C-13 | Redemption Engine Service | Tiering System Service | Sync | `GET /v1/tiering/member/{memberId}/tier` (REST) |
| C-14 | Redemption Engine Service | Redis Cache & Distributed Lock | Sync | Redlock acquire on `member:{memberId}:debit` |
| C-15 | Redemption Engine Service | Redemption DB - PostgreSQL | Sync | `INSERT redemption_order`; `UPDATE point_transaction` FIFO (JDBC) |
| C-16 | Redemption Engine Service | Partner Systems *(external)* | Sync | `POST /fulfillment/rewards` (REST partner contract) |
| C-17 | Redemption Engine Service | Message Broker - Apache Kafka | Async | `EVT-005/006/007` · Topics: `loyalty.redemption.*` |
| C-18 | Message Broker - Apache Kafka | Analytics & Reporting Service | Async | `EVT-010` · Topic: `loyalty.cdc.platform_events` (CDC stream) |
| C-19 | Earning DB - PostgreSQL | Message Broker - Apache Kafka | Async | CDC via Debezium WAL → `loyalty.cdc.platform_events` |
| C-20 | Analytics & Reporting Service | Data Warehouse - Star Schema | Sync | `INSERT/UPSERT fact_point_transaction`, `fact_redemption_order` |
| C-21 | Program Admin *(actor)* | API Gateway / OAuth 2.0 Auth Server | Sync | `POST /v1/programs` · `PUT /v1/campaigns/{id}` (REST Admin) |
| C-22 | API Gateway / OAuth 2.0 Auth Server | Program Management Service | Sync | `POST/PUT /v1/programs`, `/v1/campaigns` (REST) |
| C-23 | Program Management Service | Program Mgmt DB - PostgreSQL | Sync | `INSERT/UPDATE loyalty_program`; `campaign`; `config_version_log` (JDBC) |

### Exception Spec (G5)

Critical failure path from CON.*. Trigger, compensating action, who performs it.

| CON.* | Critical failure trigger | Compensating action | Who performs it |
|---|---|---|---|
| **CON.1** | Duplicate earn event arrives: same `transaction_ref_id` from Core Banking or Partner Systems | Earning Engine Service checks Redis idempotency key. If key exists → reject event with `DUPLICATE_EVENT` error; no ledger write; no QP publish. Redis key retains original TTL. | Earning Engine Service (idempotency guard) |
| **CON.2** | External system attempts direct write to a service-owned database (bypassing service API) | Network policy (Kubernetes NetworkPolicy) blocks the connection. Alert raised to Ops. Offending request logged in audit trail. | Ops (network enforcement) · API Gateway / OAuth 2.0 Auth Server (edge block) |
| **CON.3** | Partner fulfillment fails or times out after `RedemptionOrder` is `IN_PROGRESS` | (1) Redemption Engine Service sets `RedemptionOrder` → `FAILED`. (2) Auto-reversal job restores each `PointTransaction` from `PENDING_DEBIT` → `CONFIRMED`, preserving original `earn_date` and `expiry_date`. (3) CRM & Notification Gateway notifies member of failure and restoration. (4) `RedemptionOrder` transitions to `REVERSED`. | Redemption Engine Service (auto-reversal) · CRM & Notification Gateway (member alert) |

### Test Spec (G6)

One row per state transition and per sequence `alt`. SUT must be a C4 / I-4 container name.

#### State Transition Tests (`RedemptionOrder` — I-6)

| Test ID | SUT (C4 name) | Trigger | Pre-condition | Expected result |
|---|---|---|---|---|
| TS-001 | Redemption Engine Service | Member submits valid redemption request | Balance ≥ min_points; member tier eligible | `RedemptionOrder` → `IN_PROGRESS`; FIFO batches reserved; Redlock acquired |
| TS-002 | Redemption Engine Service | Member submits request; insufficient balance | `point_balance.available < reward.min_points` | `RedemptionOrder` → `CANCELLED`; no debit; error `INSUFFICIENT_BALANCE` |
| TS-003 | Redemption Engine Service | Member submits request; tier ineligible | Member tier below reward minimum tier | `RedemptionOrder` → `CANCELLED`; no debit; error `TIER_INELIGIBLE` |
| TS-004 | Redemption Engine Service | Partner confirms reward delivery | `RedemptionOrder` is `IN_PROGRESS` | `RedemptionOrder` → `FULFILLED`; `PointTransaction` → `CONFIRMED_DEBIT` |
| TS-005 | Redemption Engine Service | Partner fulfillment API fails (timeout or 5xx) | `RedemptionOrder` is `IN_PROGRESS` | `RedemptionOrder` → `FAILED` |
| TS-006 | Redemption Engine Service | Auto-reversal triggered after FAILED | `RedemptionOrder` is `FAILED` | `PointTransaction` `PENDING_DEBIT` → `CONFIRMED` (earn_date and expiry preserved); `RedemptionOrder` → `REVERSED`; member notified |
| TS-007 | Earning Engine Service | Settlement confirmation received | `PointTransaction` is `PENDING` | `PointTransaction` → `CONFIRMED`; `point_balance` updated |
| TS-008 | Earning Engine Service | Source transaction reversed | `PointTransaction` is `PENDING` | `PointTransaction` → `CANCELLED`; no balance effect |
| TS-009 | Earning Engine Service | Expiry sweep cron fires | `PointTransaction` is `CONFIRMED`; earn_date + 12 months past | `PointTransaction` → `EXPIRED`; balance decremented |

#### Sequence Alt Tests (I-11 Use Cases)

| Test ID | SUT (C4 name) | Use case | `alt` branch trigger | Expected result |
|---|---|---|---|---|
| SA-001 | Earning Engine Service | UC-LB-01 Process earn event | Duplicate `transaction_ref_id` received (CON.1) | Reject with `DUPLICATE_EVENT`; no ledger write; Redis key unchanged |
| SA-002 | Redemption Engine Service | UC-LB-02 Redeem reward FIFO | `point_balance.available < reward.min_points` | Order `CANCELLED`; no FIFO debit; `INSUFFICIENT_BALANCE` returned |
| SA-003 | Redemption Engine Service | UC-LB-02 Redeem reward FIFO | Member tier below reward minimum tier | Order `CANCELLED`; no debit; `TIER_INELIGIBLE` returned |
| SA-004 | Redemption Engine Service | UC-LB-02 Redeem reward FIFO | Partner fulfillment times out or returns 5xx (CON.3) | Order `FAILED` → `REVERSED`; points restored with original earn_date; member notified |
| SA-005 | Tiering System Service | UC-LB-03 Apply tier upgrade | Duplicate QP accrual event replayed (idempotency) | Event ignored; `qp_ledger` unchanged; no duplicate tier evaluation |
| SA-006 | Analytics & Reporting Service | UC-LB-04 Generate point liability report | DW data staleness exceeds 10-minute SLA | Staleness alert raised; report delivered with staleness warning label |
>>>>>>> 677ef1ed5df32872dd109e318ea09a27500b12ee
