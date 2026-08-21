# Lab 9 — C4 After Views

**System-in-focus:** Loyalty Banking Platform | **Language:** C4 | **As-Is / To-Be / Transition:** To-Be | **Owner:** Casey Wong | **Version:** 1.0 | **Date:** 2026-08-21 | **Status:** Review

Legend: Person = actor, System = system-in-focus, System_Ext = I-3 external, Container = exact I-4 container, Component = module inside selected container. Relationship labels describe business interaction; protocol and sync/async labels appear only on Container/Component views.

## C4 Context (L1)

**R:** Sam Lee (SA) | **A:** Casey Wong (Owner) | **C:** Alex Morgan (EA), Priya Shah (BA) | **I:** Jordan Kim (Dev), Taylor Reed (Test)

```mermaid
flowchart LR
    Member[Person: Member] -->|earns, checks balance, redeems rewards| Platform[System: Loyalty Banking Platform]
    Admin[Person: Program Admin] -->|configures programs and rules| Platform
    Finance[Person: Finance] -->|reviews liability and reconciliation| Platform
    Support[Person: Support Agent] -->|requests adjustments and reversal support| Platform
    Core[External: Core Banking System] -->|publishes settled transactions and reversals| Platform
    Partner[External: Partner Systems] -->|submits earn events and receives fulfillment requests| Platform
    Platform -->|delivers customer notifications| CRM[External: CRM & Notification Gateway]
    Platform -->|provides analytical data| EDW[External: Enterprise Data Warehouse]
```

No containers, databases, buses, protocols, pods, or components appear on Context.

## C4 Container (L2)

**R:** Sam Lee (SA) | **A:** Alex Morgan (EA) | **C:** Jordan Kim (Dev), Casey Wong (Owner) | **I:** Priya Shah (BA), Taylor Reed (Test)

```mermaid
flowchart LR
    Member[Member] -->|HTTPS REST, sync| GW[API Gateway]
    Partner[Partner Systems] -->|standardized API adapter, sync| GW
    Core[Core Banking System] -->|settled transaction event, async| MB[Message Broker]
    GW -->|HTTPS REST / gRPC, sync| EE[Earning Engine Service]
    GW -->|HTTPS REST / gRPC, sync| RE[Redemption Engine Service]
    GW -->|HTTPS REST / gRPC, sync| PM[Program Management Service]
    GW -->|HTTPS REST / gRPC, sync| AR[Analytics & Reporting Service]
    EE -->|QP accrual, async| MB
    MB -->|QP accrual, async| TS[Tiering System Service]
    MB -->|CDC events, async| AR
    EE -->|idempotency check, sync| IS[Idempotency Store]
    RE -->|balance lock, sync| IS
    EE -->|ledger persistence| EDB[Earning DB]
    TS -->|tier persistence| TDB[Tiering DB]
    RE -->|order persistence| RDB[Redemption DB]
    PM -->|configuration persistence| PMDB[Program Mgmt DB]
    AR -->|analytics persistence| DW[Data Warehouse]
    RE -->|fulfillment request, sync| Partner
    AR -->|reporting data, sync| EDW[Enterprise Data Warehouse]
    RE -->|notification request, sync| CRM[CRM & Notification Gateway]
```

Every container is an exact I-4 string. No unnamed external or direct channel/database path is shown.

## Optional C4 Component (L3) — `Redemption Engine Service`

**R:** Jordan Kim (Dev) | **A:** Sam Lee (SA) | **C:** Taylor Reed (Test), Priya Shah (BA) | **I:** Casey Wong (Owner)

```mermaid
flowchart LR
    Member[Member] --> Intake[Order Intake Module]
    Intake --> Validate[Tier and Balance Validation Module]
    Validate --> FIFO[FIFO Debit Module]
    FIFO --> State[RedemptionOrder State Module]
    State --> Fulfill[Fulfillment Coordination Module]
    Validate --> IS[Idempotency Store]
    State --> DB[Redemption DB]
    Fulfill --> Partner[Partner Systems]
    State -.-> Tier[Tiering System Service]
    State -.-> CRM[CRM & Notification Gateway]
    State -. neighbor .-> Gateway[API Gateway]
```

Only `Redemption Engine Service` is exploded. Neighbor containers are black boxes; no second component view exists.
