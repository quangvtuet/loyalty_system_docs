# Loyalty Banking — Model-Driven Design Index

**Domain**: Loyalty Banking  
**Version**: 1.0  
**Date**: 2026-08-16  
**Purpose**: Master catalog mapping every formal model artifact in the documentation suite to its location, type, and traceability references.

---

## 1. Overview

This index is the **single entry point** for navigating all model-driven artifacts in the Loyalty Banking platform documentation. Every design decision, behavioral specification, and structural model can be located from this page.

### Model Categories

| Category | Description | Count |
|---|---|---|
| **Structural Models** | Class diagrams, ER diagrams, component diagrams, deployment topology | 10 |
| **Behavioral Models** | Sequence diagrams, state machines, activity flows | 22 |
| **Data Models** | Physical DDL schemas, dimensional star schema, partitioning | 6 |
| **Decision Records** | Architecture Decision Records (ADRs) | 3 |
| **Contract Models** | Domain event catalog, API standards | 2 |
| **Quality Models** | Quality gate criteria, traceability matrix | 3 |

---

## 2. Structural Models

### 2.1 System-Level Architecture (C4)

| Model | Type | Document | Section | Scope |
|---|---|---|---|---|
| System Context Diagram (C4 L1) | C4 Context | [Architecture-Overview.md](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/architecture/Architecture-Overview.md) | §2 | All 5 modules + 4 external systems |
| Container Architecture (C4 L2) | C4 Container | [Architecture-Overview.md](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/architecture/Architecture-Overview.md) | §3 | Edge, Domain Services, Data Storage, External |
| Deployment Topology (Multi-AZ) | Infrastructure | [Architecture-Overview.md](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/architecture/Architecture-Overview.md) | §7 | Primary/Standby AZ, Analytics Zone |

### 2.2 Component Architecture (per Module)

| Model | Type | Document | Section | Entities Covered |
|---|---|---|---|---|
| Earning Engine Components | Flowchart | [DD-01-earning-engine.md](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/design/DD-01-earning-engine.md) | §2 | Ingestion, Dedup, Calculation, Ledger, Egress |
| Tiering System Components | Flowchart | [DD-02-tiering-system.md](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/design/DD-02-tiering-system.md) | §2 | QP Ingestion, Upgrade Engine, Batch, Dispatch |
| Redemption Engine Components | Flowchart | [DD-03-redemption-engine.md](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/design/DD-03-redemption-engine.md) | §2 | API, Validation, FIFO Debit, Fulfillment |
| Program Management Components | Flowchart | [DD-04-program-management.md](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/design/DD-04-program-management.md) | §2 | Admin API, Core Engines, Audit, Event Pub |
| Analytics & Reporting Components | Flowchart | [DD-05-analytics-reporting.md](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/design/DD-05-analytics-reporting.md) | §2 | CDC Ingestion, DW, Reporting, Delivery |

### 2.3 Domain Model

| Model | Type | Document | Section | Entities Covered |
|---|---|---|---|---|
| Domain Class Model (UML) | Class Diagram | [loyalty_domain.md](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/loyalty_domain.md) | §Domain Class Model | All 15 domain entities across 4 bounded contexts |
| Aggregate Boundary Rules | Table | [loyalty_domain.md](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/loyalty_domain.md) | §Aggregate Boundary Rules | 5 bounded contexts, aggregate roots, databases |

---

## 3. Data Models

### 3.1 OLTP Physical Schemas

| Model | Type | Document | Section | Tables |
|---|---|---|---|---|
| Earning Engine Schema | DDL (PostgreSQL) | [Data-Architecture-and-Schema.md](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/architecture/Data-Architecture-and-Schema.md) | §3.1 | `point_balance`, `point_transaction`, `earn_rule` |
| Tiering System Schema | DDL (PostgreSQL) | [Data-Architecture-and-Schema.md](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/architecture/Data-Architecture-and-Schema.md) | §3.2 | `qp_ledger`, `member_tier`, `tier_rule`, `tier_evaluation_log` |
| Redemption Engine Schema | DDL (PostgreSQL) | [Data-Architecture-and-Schema.md](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/architecture/Data-Architecture-and-Schema.md) | §3.3 | `reward_item`, `redemption_order`, `fulfillment_record` |
| Program Management Schema | DDL (PostgreSQL) | [Data-Architecture-and-Schema.md](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/architecture/Data-Architecture-and-Schema.md) | §3.4 | `loyalty_program`, `campaign`, `partner`, `enrollment`, `config_version_log`, `manual_adjustment_log` |

### 3.2 Analytical Schemas

| Model | Type | Document | Section | Tables |
|---|---|---|---|---|
| Data Warehouse Star Schema | ER Diagram | [Data-Architecture-and-Schema.md](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/architecture/Data-Architecture-and-Schema.md) | §4 | `fact_point_transaction`, `fact_redemption_order`, `dim_*` |
| Storage Partitioning & Retention | Table | [Data-Architecture-and-Schema.md](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/architecture/Data-Architecture-and-Schema.md) | §5 | Hot/Warm/Cold tiers |

### 3.3 Entity-Relationship Diagram

| Model | Type | Document | Section |
|---|---|---|---|
| OLTP Core ER Diagram | ER Diagram (Mermaid) | [Data-Architecture-and-Schema.md](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/architecture/Data-Architecture-and-Schema.md) | §2 |

---

## 4. Behavioral Models

### 4.1 Entity Lifecycle State Machines

| Model | Entity | Document | Section | Status Values |
|---|---|---|---|---|
| PointTransaction Lifecycle | `PointTransaction` | [entity-lifecycle-models.md](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/design/entity-lifecycle-models.md) | §2 | PENDING, CONFIRMED, CANCELLED, PENDING_DEBIT, CONFIRMED_DEBIT, EXPIRED |
| RedemptionOrder Lifecycle | `RedemptionOrder` | [entity-lifecycle-models.md](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/design/entity-lifecycle-models.md) | §3 | PENDING, IN_PROGRESS, FULFILLED, FAILED, CANCELLED, REVERSED |
| MemberTier Lifecycle | `MemberTier` | [entity-lifecycle-models.md](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/design/entity-lifecycle-models.md) | §4 | ACTIVE, IN_GRACE_PERIOD, DOWNGRADED |
| LoyaltyProgram Lifecycle | `LoyaltyProgram` | [entity-lifecycle-models.md](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/design/entity-lifecycle-models.md) | §5 | DRAFT, ACTIVE, SUSPENDED, DEACTIVATED |
| Campaign Lifecycle | `Campaign` | [entity-lifecycle-models.md](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/design/entity-lifecycle-models.md) | §6 | DRAFT, ACTIVE, PAUSED, COMPLETED, DEACTIVATED |
| Enrollment Lifecycle | `Enrollment` | [entity-lifecycle-models.md](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/design/entity-lifecycle-models.md) | §7 | PENDING, ACTIVE, SUSPENDED, CANCELLED |
| FulfillmentRecord Lifecycle | `FulfillmentRecord` | [entity-lifecycle-models.md](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/design/entity-lifecycle-models.md) | §8 | PENDING, IN_PROGRESS, FULFILLED, FAILED |
| Cross-Entity Interaction Map | All entities | [entity-lifecycle-models.md](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/design/entity-lifecycle-models.md) | §9 | — |

### 4.2 State Machines (Embedded in Design Docs)

| Model | Document | Section | Notes |
|---|---|---|---|
| Grace Period State Machine | [DD-02-tiering-system.md](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/design/DD-02-tiering-system.md) | §4 | Tier evaluation + grace rescue flow |
| RedemptionOrder State Machine | [DD-03-redemption-engine.md](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/design/DD-03-redemption-engine.md) | §4 | Order lifecycle with concurrent protection |
| LoyaltyProgram State Machine | [DD-04-program-management.md](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/design/DD-04-program-management.md) | §4.1 | Program activation pre-conditions |
| Campaign State Machine | [DD-04-program-management.md](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/design/DD-04-program-management.md) | §4.2 | Priority conflict + auto-completion |
| Enrollment State Machine | [DD-04-program-management.md](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/design/DD-04-program-management.md) | §4.3 | Multi-program enrollment support |
| Manual Adjustment Workflow | [Security-and-Integration-Architecture.md](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/architecture/Security-and-Integration-Architecture.md) | §4 | Dual-control approval state machine |

### 4.3 Sequence Diagrams

| Model | Flow ID | Document | Section | Modules Crossed |
|---|---|---|---|---|
| Standard Earn + Bonus | FLOW-01 | [DD-01](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/design/DD-01-earning-engine.md) | §4.1 | Core Banking → Earning → Ledger |
| Deduplication of Duplicate Event | UC-01-05 | [DD-01](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/design/DD-01-earning-engine.md) | §4.2 | Core Banking → Earning → Redis |
| Automated Point Expiry Sweep | FLOW-12 | [DD-01](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/design/DD-01-earning-engine.md) | §4.3 | Cron → Ledger → Notification → DW |
| Real-Time Tier Upgrade | FLOW-02 | [DD-02](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/design/DD-02-tiering-system.md) | §5.1 | Earning → Tiering → Kafka → Notification |
| Tier Downgrade + Grace Rescue | FLOW-03 | [DD-02](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/design/DD-02-tiering-system.md) | §5.2 | Batch → Tiering → Earning → Notification |
| Redemption with FIFO | FLOW-04 | [DD-03](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/design/DD-03-redemption-engine.md) | §5.1 | Member → Redemption → Ledger → Partner |
| Fulfillment Failure Reversal | FLOW-05 | [DD-03](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/design/DD-03-redemption-engine.md) | §5.2 | Partner → Redemption → Ledger → Notification |
| Tier-Restricted Redemption | FLOW-09 | [DD-03](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/design/DD-03-redemption-engine.md) | §5.3 | Member → Redemption → Tiering |
| Prospective Rule Change | FLOW-07 | [DD-04](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/design/DD-04-program-management.md) | §5.1 | Admin → Program Mgmt → Audit → Kafka |
| Partner Earn via OAuth | FLOW-08 | [DD-04](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/design/DD-04-program-management.md) | §5.2 | Partner → Gateway → Earning → Ledger |
| Manual Adjustment Approval | FLOW-11 | [DD-04](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/design/DD-04-program-management.md) | §5.3 | Operator → Approval → Ledger → Audit |
| CDC to Data Warehouse | FLOW-10 | [DD-05](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/design/DD-05-analytics-reporting.md) | §4.1 | Earning DB → Debezium → Kafka → DW |
| Scheduled Report Delivery | UC-05-09 | [DD-05](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/design/DD-05-analytics-reporting.md) | §4.2 | Scheduler → Engine → RBAC → DW → Email |
| Liability Alert Dashboard | UC-05-08 | [DD-05](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/design/DD-05-analytics-reporting.md) | §4.3 | Poller → DW → Dashboard → PagerDuty |
| Partner OAuth 2.0 Flow | — | [Security-Architecture.md](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/architecture/Security-and-Integration-Architecture.md) | §2 | Partner → Gateway → Rate Limiter → Earning |

### 4.4 Mathematical Specifications

| Model | Document | Section | Formulas |
|---|---|---|---|
| Base Points Calculation | [DD-01](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/design/DD-01-earning-engine.md) | §3.1 | `FLOOR(amount × rate × multiplier)` |
| Bonus Campaign Evaluation | [DD-01](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/design/DD-01-earning-engine.md) | §3.2 | Priority selection; stacking summation |
| Campaign Priority Resolution | [DD-04](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/design/DD-04-program-management.md) | §3.2 | `argmin(priority)` |
| Point Liability & Aging | [DD-05](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/design/DD-05-analytics-reporting.md) | §3.1 | `SUM(unspent) × cost_per_point` |
| Program Health KPIs | [DD-05](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/design/DD-05-analytics-reporting.md) | §3.2 | Redemption Rate, Breakage Rate, Active Rate, Earn Lift |

---

## 5. Contract Models

### 5.1 Domain Event Catalog

| Model | Document | Events Documented |
|---|---|---|
| Full Domain Event Catalog | [domain-event-catalog.md](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/architecture/domain-event-catalog.md) | 11 events (EVT-001..011) with payload schemas, ordering, idempotency |
| Event Flow Topology | [domain-event-catalog.md](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/architecture/domain-event-catalog.md) §3 | Mermaid flowchart of all event flows |
| Kafka Topic Summary | [Architecture-Overview.md](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/architecture/Architecture-Overview.md) | §5.2 — 6 topics summarized |

### 5.2 API & Integration Standards

| Model | Document | Section | Scope |
|---|---|---|---|
| REST API Standard (RFC 7807) | [Architecture-Overview.md](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/architecture/Architecture-Overview.md) | §5.1 | Error payloads, versioning, security |
| RBAC Matrix | [Security-Architecture.md](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/architecture/Security-and-Integration-Architecture.md) | §3 | 6 roles × 9 resource endpoints |
| STRIDE Threat Model | [Security-Architecture.md](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/architecture/Security-and-Integration-Architecture.md) | §6 | 6 threat categories with mitigations |

---

## 6. Decision Records

| ADR | Title | Document | Status | Key Decision |
|---|---|---|---|---|
| ADR-001 | Module Boundaries & Data Isolation | [ADR-001](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/architecture/adrs/ADR-001-module-boundaries-and-data-isolation.md) | Accepted | Database-per-service; no cross-DB access |
| ADR-002 | Event-Driven Earn Ingestion & Idempotency | [ADR-002](file:///Users/dusainbolt/Documents/vcb/loyalty_system_docs/architecture/adrs/ADR-002-event-driven-earn-ingestion-and-idempotency.md) | Accepted | Kafka + Redis SHA-256 idempotency |
| ADR-003 | Data Warehouse & Analytics Isolation | [ADR-003](file:///Users/dusainbolt/Documents/vc## 7. Quality, Governance & Traceability Models

| Model | Document | Criteria / Scope Count |
|---|---|---|
| Architectural Governance Framework | [Architectural-Governance-Framework.md](file:///Users/hoangdd/Desktop/personal/workspace/AI-study-2/architecture/Architectural-Governance-Framework.md) | 5-Tier Hierarchy, Focus Matrix (7×6), 5 Quality Gates, 9-Role RACI |
| Architecture Quality Gates (ARCH-GATE-01..04) | [Quality-Gates-Architecture.md](file:///Users/hoangdd/Desktop/personal/workspace/AI-study-2/quality-gates/Quality-Gates-Architecture.md) | 64 criteria |
| Design Quality Gates (DESIGN-GATE-01..04) | [Quality-Gates-Design.md](file:///Users/hoangdd/Desktop/personal/workspace/AI-study-2/quality-gates/Quality-Gates-Design.md) | 103 criteria |
| Traceability Matrix | [traceability.md](file:///Users/hoangdd/Desktop/personal/workspace/AI-study-2/traceability.md) | 5 modules × 8 columns + 12 cross-module flows |

---

## 8. Requirements & Analytics Specifications

### 8.1 Functional Requirements

| Module | Document | FR Count |
|---|---|---|
| Earning Engine | [FR-01-earning-engine.md](file:///Users/hoangdd/Desktop/personal/workspace/AI-study-2/requirements/FR-01-earning-engine.md) | 22 FRs + 4 NFRs |
| Tiering System | [FR-02-tiering-system.md](file:///Users/hoangdd/Desktop/personal/workspace/AI-study-2/requirements/FR-02-tiering-system.md) | 21 FRs + 3 NFRs |
| Redemption Engine | [FR-03-redemption-engine.md](file:///Users/hoangdd/Desktop/personal/workspace/AI-study-2/requirements/FR-03-redemption-engine.md) | 22 FRs + 4 NFRs |
| Program Management | [FR-04-program-management.md](file:///Users/hoangdd/Desktop/personal/workspace/AI-study-2/requirements/FR-04-program-management.md) | 22 FRs + 4 NFRs |
| Analytics & Reporting | [FR-05-analytics-reporting.md](file:///Users/hoangdd/Desktop/personal/workspace/AI-study-2/requirements/FR-05-analytics-reporting.md) | 26 FRs + 5 NFRs |

### 8.2 Analytics Specifications

| Module | Document | Metrics | Reports |
|---|---|---|---|
| Earning Engine | [AS-01-earning-engine.md](file:///Users/hoangdd/Desktop/personal/workspace/AI-study-2/analytics/AS-01-earning-engine.md) | 9 | 4 |
| Tiering System | [AS-02-tiering-system.md](file:///Users/hoangdd/Desktop/personal/workspace/AI-study-2/analytics/AS-02-tiering-system.md) | 10 | 5 |
| Redemption Engine | [AS-03-redemption-engine.md](file:///Users/hoangdd/Desktop/personal/workspace/AI-study-2/analytics/AS-03-redemption-engine.md) | 10 | 5 |
| Program Management | [AS-04-program-management.md](file:///Users/hoangdd/Desktop/personal/workspace/AI-study-2/analytics/AS-04-program-management.md) | 11 | 5 |
| Analytics & Reporting | [AS-05-analytics-reporting.md](file:///Users/hoangdd/Desktop/personal/workspace/AI-study-2/analytics/AS-05-analytics-reporting.md) | 9 | 5 |

---

## 9. Document Inventory Summary

| Category | Documents | Models | New in Governance Suite |
|---|---|---|---|
| Domain Model | 1 | 2 (class diagram + aggregate table) | ✅ UML Class Diagram |
| Requirements | 5 | 5 (FR specs with use case tables) | — |
| Analytics | 5 | 5 (metric + report specs) | — |
| Architecture & Governance | 4 docs + 3 ADRs | 14 (C4, Hierarchy, Focus Matrix, RACI, ER, deployment, security, ADRs) | ✅ Governance Framework, Event Catalog |
| Design | 5 DD docs + 1 lifecycle doc | 22 (components, sequences, state machines, formulas) | ✅ Entity Lifecycle Models |
| Quality Gates | 2 | 2 (167 total criteria + 5-stage lifecycle) | ✅ 5-Stage Gate Lifecycle |
| Traceability | 1 | 1 (full cross-reference matrix) | — |
| MDD Index | 1 | — | ✅ Master Catalog |
| **Total** | **28 documents** | **51 formal models** | **5 core governance & model artifacts** |

*Generated from: Loyalty Banking documentation suite (v1.2) · Architectural Governance & Model-Driven Design*
