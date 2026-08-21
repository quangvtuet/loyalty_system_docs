# Lab 5 — UML Before Pack

**Status:** Done | **Phase:** Messy current style | **R:** Dev / Test | **A:** SA / BA

No Guide header, RACI template, or implementation evidence is applied.

## UC-LB-01 Process settled earn event

```plantuml
@startuml
actor "Core Banking System" as Core
participant "API Gateway" as Gateway
participant "Earning Engine Service" as Earning
participant "Idempotency Store" as Store
database "Earning DB" as EarningDb
Core -> Gateway: publish settled event
Gateway -> Earning: forward event
Earning -> Store: check source transaction
alt duplicate event (CON.1)
  Store --> Earning: duplicate
  Earning --> Core: no second posting
else new event
  Earning -> EarningDb: write PointTransaction
  Earning -> Gateway: publish QP accrual
end
@enduml
```

## UC-LB-02 Redeem reward with FIFO

```plantuml
@startuml
actor Member
participant "API Gateway" as Gateway
participant "Redemption Engine Service" as Redemption
participant "Idempotency Store" as Store
database "Redemption DB" as DB
participant "Partner Systems" as Partner
Member -> Gateway: submit order
Gateway -> Redemption: validate request
Redemption -> Store: lock balance
Redemption -> DB: reserve FIFO debit; PENDING
Redemption -> Partner: request fulfillment
alt insufficient balance or tier-ineligible
  Redemption -> DB: cancel order
else partner failure (CON.3)
  Partner --> Redemption: failure
  Redemption -> DB: FAILED then REVERSED
else fulfilled
  Partner --> Redemption: confirmation
  Redemption -> DB: FULFILLED
end
@enduml
```

## UC-LB-03 Apply tier upgrade

```plantuml
@startuml
participant "Earning Engine Service" as Earning
participant "Message Broker" as Broker
participant "Tiering System Service" as Tiering
database "Tiering DB" as DB
Earning -> Broker: QP accrual
Broker -> Tiering: consume event
alt replayed event
  Tiering --> Broker: ignore replay
else threshold crossed
  Tiering -> DB: update MemberTier
end
@enduml
```

## UC-LB-04 Generate point liability report

```plantuml
@startuml
actor Finance
participant "API Gateway" as Gateway
participant "Analytics & Reporting Service" as Analytics
database "Data Warehouse" as Warehouse
Finance -> Gateway: request liability report
Gateway -> Analytics: request report
Analytics -> Warehouse: read analytical facts
alt facts older than 10 minutes (CON.4)
  Analytics --> Finance: mark report stale
else current facts
  Analytics --> Finance: return liability report
end
@enduml
```

## Activity: I-5 Happy Path

```mermaid
flowchart TD
    A[Core Banking System publishes settled event] --> B[Earning Engine Service validates and checks Idempotency Store]
    B --> C{CON.1 duplicate?}
    C -- yes --> D[Do not post duplicate]
    C -- no --> E[Write PointTransaction to Earning DB]
    E --> F[Publish QP accrual to Message Broker]
    F --> G[Tiering System Service updates MemberTier in Tiering DB]
    G --> H[Member submits redemption through API Gateway]
    H --> I{CON.2 direct write?}
    I -- yes --> J[Reject forbidden path]
    I -- no --> K[Redemption Engine Service validates and reserves FIFO debit]
    K --> L[Partner Systems fulfill reward]
    L --> M[Analytics & Reporting Service updates Data Warehouse]
    M --> N{CON.4 within ten minutes?}
    N -- no --> O[Mark reporting stale]
    N -- yes --> P[Publish liability and engagement report]
```

## State Machine: `RedemptionOrder`

```mermaid
stateDiagram-v2
    [*] --> PENDING: create order
    PENDING --> IN_PROGRESS: validation passes and FIFO debit reserved
    PENDING --> CANCELLED: validation fails or member cancels
    IN_PROGRESS --> FULFILLED: partner confirms delivery
    IN_PROGRESS --> FAILED: partner fulfillment fails
    FAILED --> REVERSED: auto-reversal restores points
```

## G6 Planning Checklist

| Planned check | Coverage |
|---|---|
| T-01 | PENDING -> IN_PROGRESS |
| T-02 | PENDING -> CANCELLED |
| T-03 | IN_PROGRESS -> FULFILLED |
| T-04 | IN_PROGRESS -> FAILED |
| T-05 | FAILED -> REVERSED |
| ALT-01 | UC-LB-01 duplicate event |
| ALT-02 | UC-LB-02 insufficient balance or tier-ineligible |
| ALT-03 | UC-LB-02 partner failure and compensation |
| ALT-04 | UC-LB-03 replayed event |
| ALT-05 | UC-LB-04 stale warehouse data |

These are planned modeling checks only; no tests were executed.
