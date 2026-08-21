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

**R**: BA / SA  
**A**: Owner  
**Hard limits**: Simulated scenario only. No real customer data, no production system names, no production credentials.

### I-1. Team and Topic

| Field | Your value |
|---|---|
| Group | Loyalty Banking Modeling Team |
| Topic / initiative name | Loyalty Banking Platform Modernization |
| System-in-focus | Loyalty Banking Platform |
| Goal | Provide a governed loyalty platform for earning points, tier progression, redemption, program configuration, and financial reporting. |
| Outcome (measurable) | Process settled earn events within 60 seconds, prevent duplicate point postings, support FIFO redemption, and provide analytics data with no more than 10 minutes staleness. |
| Product | Banking Rewards Program |
| Contract | Simulated partner earn/redeem contract and internal reporting contract |
| Baseline -> target | Baseline: fragmented loyalty processes and manual reconciliation. Target: model-driven modular platform with traceable requirements, contracts, states, and tests. |
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

| Name (simulated) | Responsibility |
|---|---|
| Core Banking System | Source of settled transaction events, authorizations, and reversals |
| Partner Systems | External merchant and fulfillment participants for earn and redeem scenarios |
| CRM & Notification Gateway | Customer profile reference and outbound notification delivery |
| Enterprise Data Warehouse / Lake | Enterprise archival and group-wide business intelligence destination |

### I-4. Internal Containers

Same strings are used on ArchiMate Application Cooperation and C4 Container views.

| Name | Responsibility |
|---|---|
| API Gateway / OAuth 2.0 Auth Server | Routes member, admin, finance, support, and partner calls; owns authentication and rate-limit enforcement at the edge |
| Message Broker - Apache Kafka | Carries settled transaction, tier, redemption, configuration, and CDC event streams |
| Earning Engine Service | Calculates points, enforces idempotency, writes the earning ledger, and publishes QP accrual events |
| Tiering System Service | Accrues qualifying points, evaluates tier upgrades/downgrades, and publishes tier change events |
| Redemption Engine Service | Manages reward catalog, validates redemption, performs FIFO debit, and handles fulfillment reversal |
| Program Management Service | Configures loyalty programs, campaigns, rules, partner settings, and manual adjustments |
| Analytics & Reporting Service | Ingests CDC data, computes KPIs, and exposes reports and dashboards |
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

**Principle / hard rules**:

- Channel or external systems must not write directly to any core ledger database.
- The same source transaction must not create duplicate point postings.
- Reporting queries must not degrade transactional workload.
- Point reversal must preserve original FIFO earn date and expiry information.

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

| ID | Constraint | Effect on the process |
|---|---|---|
| CON.1 | No duplicate point posting for the same source transaction | Earning Engine Service must perform idempotency check before ledger write |
| CON.2 | No direct database writes from channels, partners, or other services into service-owned databases | All changes must pass through owning service APIs/events |
| CON.3 | Fulfillment failure must compensate by restoring points with original FIFO earn date and expiry | Redemption Engine Service must execute auto-reversal and notify the member |

### I-11. Named Use Cases for UML

| Use case | Happy path | At least one exception (`alt`) |
|---|---|---|
| UC-LB-01 Process settled earn event | Core Banking System publishes settled event; Earning Engine Service writes confirmed point transaction; QP event is published | Duplicate event detected under CON.1 |
| UC-LB-02 Redeem reward with FIFO | Member submits order; Redemption Engine Service locks balance, validates tier, allocates FIFO batches, dispatches fulfillment | Insufficient balance, tier-ineligible reward, or partner fulfillment failure under CON.3 |
| UC-LB-03 Apply tier upgrade | Tiering System Service consumes QP accrual and upgrades tier when threshold is reached | Event replay is ignored by idempotent event handling |
| UC-LB-04 Generate point liability report | Analytics & Reporting Service reads warehouse facts and computes liability report | Warehouse data is stale beyond CON-derived reporting SLA |

**One container** for optional C4 Component: `Redemption Engine Service`.
