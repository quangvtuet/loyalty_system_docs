# Loyalty Banking — Model-Driven Design Index

> **Official Modeling Pack Reference & Master Catalog**  
> The authoritative 10-lab modeling pack and runnable capstone reside at repository root and in `capstone/`.  
> Below is the master index connecting the official pack artifacts and the reference material with clean workspace paths.

**Domain**: Loyalty Banking Platform  
**Version**: 2.0 (Post-MDD Standardization)  
**Status**: All Quality Gates G1–G6 Closed · Capstone Verified Green (27 Passed, 0 Failed)

---

## 0. Official 10-Lab Modeling Pack Deliverables

| Lab | Deliverable Artifact | Focus & Key Standards | Primary Roles |
|---|---|---|---|
| **Lab 1** | [`loyalty.md`](loyalty.md) | Scope Index I-1..I-11, name-identity index, CON.1–CON.4 | R: BA/SA · A: Owner |
| **Lab 2** | [`lab2-requirements.md`](lab2-requirements.md) | Requirements list (REQ-LB-01..63), Analysis, Gate Register G1–G6, Trace Table | R: BA/EA · A: Owner |
| **Lab 3** | [`lab3-spec.md`](lab3-spec.md) | Build list (13 containers), 5 Component modules, Sequence, Contracts (CT-01..27), Exceptions, Tests | R: Dev/Test · A: SA |
| **Lab 4** | [`lab4-cleanup.md`](lab4-cleanup.md) | Full MDD Standardization Report, Zero-fork check, Language check, 23-defect catalog, Before vs After | R: SA · A: EA |
| **Lab 5** | [`lab-05-uml-before.md`](lab-05-uml-before.md) (archive: [`before-pack/lab5/`](before-pack/lab5/)) | Low-level design (UML) before pack | R: Dev/Test · A: SA/BA |
| **Lab 6** | [`lab-06-ecosystem-before.md`](lab-06-ecosystem-before.md) (archive: [`before-pack/lab6/`](before-pack/lab6/)) | Integration ecosystem before sketch | R: SA · A: SA |
| **Lab 7** | [`lab7-adoption.md`](lab7-adoption.md) | Guide adoption record, 4-person group roster, adopted RACI matrix, G1–G6 Gate Register | R: EA · A: Owner |
| **Lab 8** | [`lab8-archimate-views.md`](lab8-archimate-views.md) | 4 ArchiMate views: Motivation (G1), Business Process (G2), App Cooperation, Technology | R: EA/BA/SA/Ops · A: Owner/EA/SA |
| **Lab 9** | [`lab-09-c4-after.md`](lab-09-c4-after.md) | C4 Context L1 (G3), C4 Container L2 (G3), C4 Component L3 (`Redemption Engine Service`) | R: SA/Dev · A: Owner/EA/SA |
| **Lab 10** | [`lab-10-uml-after.md`](lab-10-uml-after.md) | UML Sequences (UC-LB-01..04), UML Activity, State Machine (`RedemptionOrder`), G6 Note | R: Dev/Test · A: SA/BA |
| **Capstone** | [`capstone/`](capstone/) | Runnable I-11 slice, OpenAPI 3.0 contract, spec-trace, SA sign-off, 27 automated tests | R: Dev · A: SA · C: Test |

---

## 1. Structural Models (C4 & Component Level)

| Model | Type | Document | Section | Scope |
|---|---|---|---|---|
| System Context (L1) | C4 Context | [`lab-09-c4-after.md`](lab-09-c4-after.md) | Level 1 | 4 Actors + System-in-focus + 4 Externals (G3) |
| Container Architecture (L2) | C4 Container | [`lab-09-c4-after.md`](lab-09-c4-after.md) | Level 2 | 13 I-4 Containers + 4 Externals, Sync/Async labeled (G3) |
| Redemption Engine Component (L3) | C4 Component | [`lab-09-c4-after.md`](lab-09-c4-after.md) | Level 3 | 5 Canonical Modules + Black-Box Neighbors |
| Multi-AZ Deployment Topology | ArchiMate Tech / Infra | [`lab8-archimate-views.md`](lab8-archimate-views.md) | View 4 | 4 Zones from I-9 + Forbidden Path Guard |
| Earning Engine Flow | Flowchart | [`design/DD-01-earning-engine.md`](design/DD-01-earning-engine.md) | §2 | Ingestion, Dedup, Calculation, Ledger, Egress |
| Tiering System Flow | Flowchart | [`design/DD-02-tiering-system.md`](design/DD-02-tiering-system.md) | §2 | QP Ingestion, Upgrade Engine, Batch, Dispatch |
| Redemption Engine Flow | Flowchart | [`design/DD-03-redemption-engine.md`](design/DD-03-redemption-engine.md) | §2 | API, Validation, FIFO Debit, Fulfillment |
| Program Management Flow | Flowchart | [`design/DD-04-program-management.md`](design/DD-04-program-management.md) | §2 | Admin API, Core Engines, Audit, Event Pub |
| Analytics & Reporting Flow | Flowchart | [`design/DD-05-analytics-reporting.md`](design/DD-05-analytics-reporting.md) | §2 | CDC Ingestion, DW, Reporting, Delivery |

---

## 2. Behavioral Models (Sequences & State Machines)

| Model | Flow / Use Case | Document | Scope |
|---|---|---|---|
| UC-LB-01 Process settled earn event | Sequence | [`lab-10-uml-after.md`](lab-10-uml-after.md) §UC-LB-01 | Core Banking → Broker → Earning → Idempotency → Ledger (CON.1) |
| UC-LB-02 Redeem reward with FIFO | Sequence | [`lab-10-uml-after.md`](lab-10-uml-after.md) §UC-LB-02 | Member → Gateway → 5 Modules → Partner (CON.3 Reversal) |
| UC-LB-03 Apply tier upgrade | Sequence | [`lab-10-uml-after.md`](lab-10-uml-after.md) §UC-LB-03 | Earning → Broker → Tiering → Tiering DB (Idempotent Replay) |
| UC-LB-04 Generate liability report | Sequence | [`lab-10-uml-after.md`](lab-10-uml-after.md) §UC-LB-04 | Finance → Gateway → Analytics → DW (CON.4 Staleness Guard) |
| I-5 Happy Path Activity | Activity | [`lab-10-uml-after.md`](lab-10-uml-after.md) §Activity | Steps 1 to 8 with CON.1, CON.2, CON.4 decision branches |
| `RedemptionOrder` Lifecycle | State Machine | [`lab-10-uml-after.md`](lab-10-uml-after.md) §State | 6 States (`PENDING`, `IN_PROGRESS`, `FULFILLED`, `FAILED`, `CANCELLED`, `REVERSED`) |
| Domain Entity Lifecycles | State Machines | [`design/entity-lifecycle-models.md`](design/entity-lifecycle-models.md) | PointTransaction, MemberTier, LoyaltyProgram, Campaign, Enrollment |

---

## 3. Data & Storage Models

| Model | Type | Document | Scope |
|---|---|---|---|
| OLTP Physical DDL Schemas | SQL DDL | [`architecture/Data-Architecture-and-Schema.md`](architecture/Data-Architecture-and-Schema.md) | Earning DB, Tiering DB, Redemption DB, Program Mgmt DB |
| Star Schema Dimensional Model | ER Diagram | [`architecture/Data-Architecture-and-Schema.md`](architecture/Data-Architecture-and-Schema.md) | `fact_point_transaction`, `fact_redemption_order`, Dimensions |
| Core ER Diagram | Mermaid ER | [`architecture/Data-Architecture-and-Schema.md`](architecture/Data-Architecture-and-Schema.md) | Relational entity mappings across 5 bounded contexts |

---

## 4. Contract, Governance & Decision Records

| Record | Standard | Document | Description |
|---|---|---|---|
| Public HTTP API Contract | OpenAPI 3.0 | [`capstone/openapi.yaml`](capstone/openapi.yaml) | 3 public endpoints: `POST /partner-earn`, `POST /redemptions`, `GET /reports/point-liability` |
| Inter-Container Contract Register | G4 Table | [`lab3-spec.md`](lab3-spec.md) §4 | 27 contract rows across Sync, Async, Adapter patterns |
| Domain Event Catalog | Async Event Schemas | [`architecture/domain-event-catalog.md`](architecture/domain-event-catalog.md) | 11 event schemas, partition keys, idempotency contracts |
| ADR-001 | Decision Record | [`architecture/adrs/ADR-001-module-boundaries-and-data-isolation.md`](architecture/adrs/ADR-001-module-boundaries-and-data-isolation.md) | Database-per-service isolation; single owner per store |
| ADR-002 | Decision Record | [`architecture/adrs/ADR-002-event-driven-earn-ingestion-and-idempotency.md`](architecture/adrs/ADR-002-event-driven-earn-ingestion-and-idempotency.md) | Kafka stream ingestion + SHA-256 idempotency cache |
| ADR-003 | Decision Record | [`architecture/adrs/ADR-003-data-warehouse-and-analytics-isolation.md`](architecture/adrs/ADR-003-data-warehouse-and-analytics-isolation.md) | CDC-driven star schema Data Warehouse to isolate reporting |
| SA Sign-Off Record | Acceptance Form | [`capstone/SIGN-OFF.md`](capstone/SIGN-OFF.md) | Formally signed SA acceptance of runnable I-11 slice |

