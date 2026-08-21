# Lab 6 — Ecosystem Sketch (Before)

**Status:** Done | **Phase:** Messy current style | **R:** SA | **A:** EA | **C:** Sec, Ops

The sketch uses only I-4 containers and I-3 external systems. Product names, if later added, remain labels inside these containers.

```mermaid
flowchart LR
    Member[Member] -->|HTTPS REST, sync| Gateway[API Gateway]
    ProgramAdmin[Program Admin] -->|HTTPS REST, sync| Gateway
    Finance[Finance] -->|HTTPS REST, sync| Gateway
    Support[Support Agent] -->|HTTPS REST, sync| Gateway
    Core[Core Banking System] -->|settled transaction, async| Broker[Message Broker]
    Partner[Partner Systems] -->|standardized API adapter, sync| Gateway
    Gateway -->|HTTPS REST / gRPC, sync| Earn[Earning Engine Service]
    Gateway -->|HTTPS REST / gRPC, sync| Redeem[Redemption Engine Service]
    Gateway -->|HTTPS REST / gRPC, sync| Program[Program Management Service]
    Earn -->|QP accrual event, async| Broker
    Broker -->|QP accrual event, async| Tier[Tiering System Service]
    Broker -->|CDC events, async| Analytics[Analytics & Reporting Service]
    Earn -->|idempotency check, sync| Idem[Idempotency Store]
    Redeem -->|balance lock, sync| Idem
    Earn --> EarnDb[(Earning DB)]
    Tier --> TierDb[(Tiering DB)]
    Redeem --> RedDb[(Redemption DB)]
    Program --> ProgDb[(Program Mgmt DB)]
    Analytics --> Warehouse[(Data Warehouse)]
    Redeem -->|fulfillment request, sync| Partner
    Analytics -->|reporting interaction, sync| Finance
    Gateway -->|notification request, sync| CRM[CRM & Notification Gateway]
    Core -. forbidden direct write .-> EarnDb
    Partner -. forbidden direct write .-> RedDb
```

## Negative Evidence

This is a simulation-only ecosystem model. No Docker, cluster, IAM realm, broker administration, runtime, deployment, or executable test was performed. `API Gateway` already owns authentication, so no separate IAM system is added. No new system is introduced for an adapter; it is represented by the I-8 legacy/adapter mechanism through `API Gateway`.
