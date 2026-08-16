# Loyalty Banking — Architecture Overview

**Domain**: Loyalty Banking  
**Version**: 1.0  
**Date**: 2026-08-14  
**Source**: [loyalty_domain.md](file:///d:/learn/loyalty/loyalty_domain.md) | [FR-01..05](file:///d:/learn/loyalty/requirements/) | [AS-01..05](file:///d:/learn/loyalty/analytics/) | [Quality-Gates-Architecture.md](file:///d:/learn/loyalty/quality-gates/Quality-Gates-Architecture.md)

---

## 1. Executive Summary

**Loyalty Banking** is an enterprise-grade financial loyalty platform providing end-to-end management of customer rewards points, tier progression, catalog redemptions, campaign promotions, and financial liability reporting. The architecture is designed as a **modular, event-driven microservices architecture** with polyglot data persistence, strict module data isolation, real-time stream ingestion, and high-throughput transactional guarantees.

### Key Architectural Characteristics
- **Throughput & Latency**: Sustained ingestion of ≥ 500 earn events/second with end-to-end processing latency ≤ 2,000 ms (p95).
- **Settlement SLA**: Real-time event consumption from core banking within ≤ 60 seconds of transaction settlement.
- **Data Integrity**: Append-only immutable Sổ cái (Earning Ledger) (`point_transaction`), mutable Point Balance snapshot (`point_balance`) for fast read access, strict FIFO point consumption, and zero duplicate credits via distributed idempotency locks.
- **Reporting Isolation**: Complete separation of transactional OLTP datastores from analytical workloads via near-real-time (< 10 min lag) Change Data Capture (CDC) into an analytical Data Warehouse.
- **Resilience & DR**: Target RTO ≤ 5 minutes, RPO ≤ 10 minutes with multi-AZ failover and automated point batch recovery.

---

## 2. System Context (C4 Level 1)

The Loyalty Banking system integrates upstream with financial settlement pipelines, downstream with fulfillment partners, and provides administrative and member interfaces.

```mermaid
C4Context
    title System Context Diagram (Level 1) - Loyalty Banking Platform

    Person(member, "Member / Customer", "Bank customer enrolled in loyalty programs")
    Person(admin, "Program Admin / Marketing", "Bank staff administering programs, campaigns, rules")
    Person(finance, "Finance & Compliance", "Finance staff monitoring point liability and breakage")
    Person(support, "Support Agent", "Customer service handling manual adjustments / reversals")

    System(loyalty_system, "Loyalty Banking Platform", "Core loyalty engine managing points, tiers, redemptions, campaigns, and analytics")

    System_Ext(core_banking, "Core Banking System", "Source of settled financial transactions, authorizations, and reversals")
    System_Ext(partner_systems, "Partner Systems", "Third-party earn/redeem merchants and catalog fulfillment providers")
    System_Ext(crm, "CRM & Notification Gateway", "Customer profile registry, email, SMS, and push notification delivery")
    System_Ext(dw_lake, "Enterprise Data Warehouse / Lake", "Enterprise business intelligence and regulatory archival")

    Rel(core_banking, loyalty_system, "Publishes settled transaction events (Kafka/Feed, ≤ 60s SLA)", "Async / TLS")
    Rel(partner_systems, loyalty_system, "Submits partner earn events & queries catalog (OAuth 2.0 REST)", "HTTPS / JSON")
    Rel(loyalty_system, partner_systems, "Dispatches redemption orders for fulfillment", "HTTPS / JSON")
    Rel(loyalty_system, crm, "Dispatches notifications (tier changes, expiry alerts, reversals)", "Async / HTTPS")
    Rel(loyalty_system, dw_lake, "Streams ledger transactions and audit events (< 10 min lag)", "CDC / Kafka")

    Rel(member, loyalty_system, "Browses catalog, submits redemptions, views balance & history", "Mobile / Web App")
    Rel(admin, loyalty_system, "Configures programs, campaigns, earn/tier/redemption rules", "Admin Portal (Web)")
    Rel(finance, loyalty_system, "Views liability reports, breakage forecasts, reconciliation", "Finance Portal (Web)")
    Rel(support, loyalty_system, "Initiates manual adjustments (dual-control approval)", "Support Portal (Web)")
```

---

## 3. Container Architecture (C4 Level 2)

The system is decomposed into 5 domain service containers, an edge API gateway, a high-throughput message broker, distributed caching layer, and dedicated operational and analytical datastores.

```mermaid
flowchart TB
    subgraph Clients["Clients & Upstream Systems"]
        MB[Member Mobile / Web]
        AP[Admin & Finance Portal]
        CB[Core Banking Transaction Feed]
        PS[External Partner Systems]
    end

    subgraph Edge["Edge & Ingestion Layer"]
        GW[API Gateway / OAuth 2.0 Auth Server]
        KAFKA[Message Broker - Apache Kafka]
    end

    subgraph CoreServices["Domain Services Layer"]
        EE[Earning Engine Service]
        TS[Tiering System Service]
        RE[Redemption Engine Service]
        PM[Program Management Service]
        AR[Analytics & Reporting Service]
    end

    subgraph DataStorage["Data Storage Layer (Database-per-Service)"]
        REDIS[(Redis Cache & Distributed Lock)]
        DB_EARN[(Earning DB - PostgreSQL)]
        DB_TIER[(Tiering DB - PostgreSQL)]
        DB_RED[(Redemption DB - PostgreSQL)]
        DB_PROG[(Program Mgmt DB - PostgreSQL)]
        DW_STORE[(Data Warehouse - Star Schema)]
    end

    subgraph External["External Integrations"]
        NOTIF[Notification Service / CRM]
        FULFILL[Partner Fulfillment Gateways]
    end

    %% Client routes
    MB -->|HTTPS / REST| GW
    AP -->|HTTPS / REST| GW
    PS -->|OAuth 2.0 / REST| GW
    CB -->|Direct Event Stream| KAFKA

    GW -->|REST / gRPC| PM
    GW -->|REST / gRPC| RE
    GW -->|REST / gRPC| TS
    GW -->|REST / gRPC| AR

    %% Kafka event flows
    KAFKA -->|settled_transactions topic| EE
    EE -->|tier_event topic| TS
    TS -->|tier_change topic| EE
    TS -->|tier_change topic| RE
    RE -->|redemption_event topic| EE
    RE -->|fulfillment_request| FULFILL

    %% Cross-service event sync to Kafka
    EE -->|cdc_stream| KAFKA
    RE -->|cdc_stream| KAFKA
    TS -->|cdc_stream| KAFKA
    PM -->|cdc_stream| KAFKA
    KAFKA -->|cdc_events topic| AR

    %% Database connections
    EE --- DB_EARN
    EE --- REDIS
    TS --- DB_TIER
    RE --- DB_RED
    RE --- REDIS
    PM --- DB_PROG
    AR --- DW_STORE

    %% Notifications
    EE -.->|expiry / earn events| NOTIF
    TS -.->|upgrade / grace alerts| NOTIF
    RE -.->|order / reversal alerts| NOTIF
```

---

## 4. Module Boundaries & Data Isolation

Following **ADR-001 (Module Boundaries & Polyglot Persistence)**, each module operates as an independent service boundary with full encapsulation of its domain entities and dedicated private datastore. **Direct cross-module database access is strictly prohibited.**

| Module | Core Responsibility | Domain Entities Owned | Data Store Type |
|---|---|---|---|
| **Earning Engine** | Ingests settled events, calculates base & bonus points, manages immutable Sổ cái (Earning Ledger) and mutable Point Balance snapshot, enforces FIFO & idempotency, schedules expiry. | `PointTransaction`, `PointBalance`, `EarnRule`, `EarnEventLog`, `ExpirySchedule` | PostgreSQL (Append-Only Ledger + Balance Table) + Redis (Idempotency Cache) |
| **Tiering System** | Accrues Qualifying Points (QP), performs instant tier upgrades, executes end-of-year batch evaluations, manages 30-day grace periods. | `MemberTier`, `QpLedger`, `TierRule`, `TierEvaluationLog`, `TierEvent` | PostgreSQL (Partitioned Ledger & Snapshot) |
| **Redemption Engine** | Manages reward catalog, validates point balances, executes atomic FIFO point debits, coordinates partner fulfillment, handles reversals. | `RewardItem`, `RedemptionOrder`, `FulfillmentRecord` | PostgreSQL (ACID Transactions) + Redis (Debit Locks) |
| **Program Management** | Program lifecycle, campaign configuration & priority, rule versioning, member enrollments, partner registry & OAuth gateway, manual adjustments. | `LoyaltyProgram`, `Campaign`, `Enrollment`, `Partner`, `ConfigVersionLog`, `ManualAdjustmentLog` | PostgreSQL (Temporal Tables & WORM Audit) |
| **Analytics & Reporting** | Aggregates program metrics, calculates financial liability & breakage, generates 7 standard reports, serves real-time dashboard & alerts. | `fact_point_transaction`, `fact_redemption_order`, `fact_enrollment`, `dim_*`, `MaterializedViews` | Dedicated Analytics Data Warehouse (Columnar / Star Schema) |

---

## 5. Component Diagrams (C4 Level 3)

The following diagrams decompose each domain service container (from §3) into its internal components. Detailed specifications per component are in the respective Detailed Design documents ([DD-01](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/design/DD-01-earning-engine.md)–[DD-05](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/design/DD-05-analytics-reporting.md)).

### 5.1 Earning Engine — Internal Components

```mermaid
flowchart TB
    subgraph EarningEngine["Earning Engine Service"]
        direction TB
        INGESTION["Ingestion Layer\n• Kafka Consumer (settled_txn)\n• Partner Earn REST Controller\n• Schema Validator"]
        DEDUP["Idempotency Guard\n• SHA-256 Key Generator\n• Redis NX Cache (TTL: 24h)"]
        CALC["Calculation Engine\n• Tier Multiplier Resolver\n• Base Earn Evaluator (FLOOR)\n• Campaign Bonus Evaluator"]
        LEDGER["Ledger & Expiry Service\n• Append-Only PointTransaction Writer\n• PointBalance Snapshot Updater\n• FIFO Expiry Scheduler"]
        EGRESS["Event Publisher\n• Kafka Producer (qp_accrued)\n• CDC Stream Emitter"]
    end

    INGESTION --> DEDUP
    DEDUP --> CALC
    CALC --> LEDGER
    LEDGER --> EGRESS
```

### 5.2 Tiering System — Internal Components

```mermaid
flowchart TB
    subgraph TieringSystem["Tiering System Service"]
        direction TB
        QP_INGEST["QP Accrual Ingestion\n• Kafka Consumer (qp_accrued)\n• QP Ledger Writer"]
        RT_ENGINE["Real-Time Upgrade Engine\n• Threshold Checker (≤ 500ms)\n• Tier Rules Cache"]
        BATCH["Batch Evaluation Engine\n• End-of-Period Job (31 Dec)\n• 1M Members in ≤ 4 Hours\n• Idempotent Re-run"]
        GRACE["Grace Period Manager\n• 30-Day State Machine\n• Grace Rescue Evaluator"]
        DISPATCH["Tier Event Dispatcher\n• Kafka Producer (tier_changed)\n• Notification Trigger"]
    end

    QP_INGEST --> RT_ENGINE
    QP_INGEST --> BATCH
    RT_ENGINE --> DISPATCH
    BATCH --> GRACE
    GRACE --> DISPATCH
```

### 5.3 Redemption Engine — Internal Components

```mermaid
flowchart TB
    subgraph RedemptionEngine["Redemption Engine Service"]
        direction TB
        API_LAYER["API Layer\n• Catalog REST Controller\n• Order REST Controller"]
        VALIDATION["Validation & Locking\n• Tier Access Validator\n• Redis Distributed Lock\n  (lock:member:bal:UUID)"]
        FIFO["FIFO Debit Engine\n• Earning Ledger Client (gRPC)\n• FIFO Batch Allocator\n• Oldest earn_date first"]
        ORDER_SM["Order State Machine\n• PENDING → IN_PROGRESS\n• → FULFILLED / FAILED / REVERSED"]
        FULFILL["Fulfillment Dispatcher\n• Partner API Client\n• Auto-Reversal Handler"]
    end

    API_LAYER --> VALIDATION
    VALIDATION --> FIFO
    FIFO --> ORDER_SM
    ORDER_SM --> FULFILL
    FULFILL -->|"On Failure"| FIFO
```

### 5.4 Program Management — Internal Components

```mermaid
flowchart TB
    subgraph ProgramMgmt["Program Management Service"]
        direction TB
        ADMIN_API["Admin REST API\n• Program CRUD\n• Campaign CRUD\n• Enrollment Management"]
        PARTNER_GW["Partner OAuth 2.0 Gateway\n• Token Issuer (JWT, TTL: 3600s)\n• Redis Rate Limiter (1,000 RPM)"]
        RULE_ENGINE["Versioned Rule Registry\n• Prospective Version Control\n• valid_from / valid_to Management\n• Config Audit Logger"]
        CAMPAIGN_ENGINE["Campaign Priority Engine\n• argmin(priority) Resolver\n• Stacking Configuration"]
        ADJUST_WF["Manual Adjustment Workflow\n• Dual-Control State Machine\n• Threshold Gate (5,000 pts)\n• WORM Audit Logger"]
        CONFIG_PUB["Config Event Publisher\n• Kafka (rule_updated)\n• Kafka (campaign_activated)"]
    end

    ADMIN_API --> RULE_ENGINE
    ADMIN_API --> CAMPAIGN_ENGINE
    ADMIN_API --> ADJUST_WF
    PARTNER_GW --> ADMIN_API
    RULE_ENGINE --> CONFIG_PUB
    CAMPAIGN_ENGINE --> CONFIG_PUB
```

### 5.5 Analytics & Reporting — Internal Components

```mermaid
flowchart TB
    subgraph AnalyticsService["Analytics & Reporting Service"]
        direction TB
        CDC_INGEST["CDC Ingestion Pipeline\n• Debezium WAL Reader\n• Kafka Connect Workers\n• Star Schema Transformer"]
        DW_STORE["Data Warehouse Store\n• Fact Tables (point_txn, redemption)\n• Dimension Tables (member, program, ...)\n• Materialized Views (5-min refresh)"]
        REPORT_ENGINE["Report Generation Engine\n• 7 Standard Reports\n• CSV / XLSX / PDF Export\n• Async Queue for >1M rows"]
        ALERT_MONITOR["Real-Time Alert Monitor\n• 5-Minute Polling Cycle\n• Liability Ceiling Alerts\n• Latency Breach Alerts"]
        RBAC_GUARD["RBAC Program Scoping\n• Row-Level Security\n• Program-Level Filters"]
        DELIVERY["Delivery Channels\n• Dashboard WebSocket\n• Email / SFTP Scheduler\n• PagerDuty Integration"]
    end

    CDC_INGEST --> DW_STORE
    DW_STORE --> REPORT_ENGINE
    DW_STORE --> ALERT_MONITOR
    REPORT_ENGINE --> RBAC_GUARD
    RBAC_GUARD --> DELIVERY
    ALERT_MONITOR --> DELIVERY
```

---

## 6. Communication Patterns & API Standards

### 6.1 Synchronous REST / gRPC Interfaces
- **API Standard**: REST over HTTPS with OpenAPI 3.0 specification; inter-service synchronous reads use gRPC over HTTP/2 for sub-50ms latency (NFR-02-003).
- **URI Versioning**: Standardized URI path prefix `/api/v1/...` with forward-compatible semantic schemas.
- **Security**: OAuth 2.0 Bearer JWT tokens for internal/external users; OAuth 2.0 Client Credentials flow for third-party partners.
- **Error Standard**: Standardized RFC 7807 Problem Details payload:
```json
{
  "type": "https://api.loyaltybanking.com/errors/insufficient-balance",
  "title": "Insufficient Points Balance",
  "status": 422,
  "detail": "Requested 300 points exceeds available confirmed balance of 200 points.",
  "instance": "/api/v1/redemptions/orders",
  "code": "ERR_RED_INSUFFICIENT_BALANCE",
  "correlation_id": "req-9b81f26a-4d7a-42c2-8361-ec853c074df3",
  "timestamp": "2026-08-14T14:30:00Z"
}
```

### 6.2 Asynchronous Event Streams (Kafka Topics)
- **Message Protocol**: Apache Kafka with JSON / Apache Avro schema registry.
- **Standard Envelope**: All events contain metadata headers (`event_id`, `event_type`, `timestamp`, `correlation_id`, `source_module`, `partition_key`).

| Topic Name | Producer | Consumer(s) | Key Schema / Payload |
|---|---|---|---|
| `corebanking.transactions.settled` | Core Banking | Earning Engine | `source_txn_id`, `member_id`, `amount`, `channel`, `settled_ts` |
| `loyalty.earning.qp_accrued` | Earning Engine | Tiering System | `event_id`, `member_id`, `program_id`, `qp_amount`, `source_txn_id` |
| `loyalty.tiering.tier_changed` | Tiering System | Earning Engine, Redemption Engine, CRM | `member_id`, `old_tier`, `new_tier`, `multiplier`, `effective_ts` |
| `loyalty.redemption.debit_requested` | Redemption Engine | Earning Engine | `order_id`, `member_id`, `points_required`, `fifo_batches` |
| `loyalty.redemption.fulfilled` | Redemption Engine | Earning Engine, Analytics, CRM | `order_id`, `member_id`, `status` (`FULFILLED` / `FAILED`), `points` |
| `loyalty.cdc.platform_events` | All Services | Analytics (DW Ingest) | Debezium CDC change streams from all operational databases |

---

## 7. Technology Stack & Infrastructure

| Component Layer | Technology Choice | Architectural Justification |
|---|---|---|
| **Service Runtimes** | Go / Java Spring Boot / Node.js LTS | High-concurrency I/O, low-latency transaction processing, strong typing. |
| **Edge API Gateway** | Kong / Envoy Gateway | High-performance routing, OAuth 2.0 token introspection, rate limiting (1,000 req/min). |
| **Event Broker** | Apache Kafka | Ordered partitioned event delivery, high throughput (≥ 500 events/sec), replayability. |
| **Operational DBs** | PostgreSQL 16+ | Strong ACID compliance, table partitioning, JSONB for flexible rule schemas, row-level locking. |
| **In-Memory Cache / Lock** | Redis Cluster 7.x | Distributed locking (`Redlock`), sub-millisecond idempotency deduplication cache. |
| **Data Warehouse** | ClickHouse / PostgreSQL DW Replica | Fast columnar scans for large aggregation reports (> 1M rows), star schema support. |
| **Change Data Capture** | Debezium / Kafka Connect | Low-overhead streaming from PostgreSQL WAL into Kafka for analytics pipeline (< 10 min lag). |
| **Observability** | OpenTelemetry, Prometheus, Grafana, Jaeger | Distributed tracing with correlation IDs, p95 latency monitoring, operational alerting. |

---

## 8. Deployment Topology & Resilience (Multi-AZ)

```mermaid
flowchart TD
    subgraph MultiAZ["Multi-AZ High Availability Deployment (AWS / Azure / GCP)"]
        subgraph AZ_A["Availability Zone A (Primary)"]
            GW_A[API Gateway Node 1]
            APP_A[Service Pods - Replica 1]
            DB_A_PRI[(PostgreSQL Primary)]
            KAFKA_A[Kafka Broker 1]
            REDIS_A[Redis Master]
        end

        subgraph AZ_B["Availability Zone B (Standby / Active Replica)"]
            GW_B[API Gateway Node 2]
            APP_B[Service Pods - Replica 2]
            DB_B_STBY[(PostgreSQL Sync Standby)]
            KAFKA_B[Kafka Broker 2]
            REDIS_B[Redis Replica]
        end

        subgraph Analytics_Zone["Analytics Read Replica & DW Zone"]
            DW_NODE[(Analytics Data Warehouse)]
            ETL_WORKER[Kafka Connect CDC Workers]
        end
    end

    GW_A <--> APP_A
    GW_B <--> APP_B
    APP_A --> DB_A_PRI
    APP_B --> DB_A_PRI
    DB_A_PRI -.->|Sync Streaming Replication| DB_B_STBY
    DB_A_PRI -.->|WAL / CDC Stream| ETL_WORKER
    ETL_WORKER --> DW_NODE
    KAFKA_A <--> KAFKA_B
    REDIS_A -.->|Replication| REDIS_B
```

### High Availability & DR Guarantees
- **Failover**: Automated primary database promotion via Patroni / cloud managed RDS within **< 60 seconds**.
- **Recovery Time Objective (RTO)**: **≤ 5 minutes** for full transactional recovery.
- **Recovery Point Objective (RPO)**: **≤ 10 minutes** for data warehouse analytics; **0 data loss (RPO = 0)** for confirmed Sổ cái (Earning Ledger) entries.
- **Backup & Archival**: Continuous WAL archiving + automated snapshot every 6 hours, tested quarterly.
