# Lab 10 — UML Low-Level Design for Named Use Cases

Title:      Loyalty Banking Platform — UML Named Use Cases and State Audit
Viewpoint:  UML Sequence / Activity / State
Layer(s):   Base — delivery behavior
As-Is | To-Be | Transition:  To-Be
Owner:      Role Dev         Name Lê Huy Du
RACI:       R Dev   A SA   C BA/PO Test   I EA DA Sec Ops Owner
Version:    v1.0  Date 2026-08-21  Status Review
Legend:     actor lifeline, exact Lab 9 Container participant, module inside `Redemption Engine Service`, `alt` exception branch, state transition
RACI legend: R = draws · A = approves · C = consulted · I = informed
Scope:      in-scope the four exact I-11 named use cases, one `RedemptionOrder` state machine, participant-to-SUT mapping, and planned G6 coverage / out-of-scope source code, runtime tests, unnamed lifelines, extra use cases, and components outside the selected container

The RACI follows the Lab 7 adopted table. UML Sequence is drawn by Dev Lê Huy Du and approved by SA Vũ Trường Quang. UML Activity / State is drawn by Test Lê Huy Du and approved by BA/PO Đặng Duy Hoàng. R and A are different people on each artifact even though Dev and Test are the same roster member.

## Participant-to-SUT Map

| Lifeline or actor | Exact mapping and SUT |
|---|---|
| Member | I-2 actor; request enters `API Gateway` |
| Program Admin | I-2 actor; configuration request enters `API Gateway` |
| Finance | I-2 actor; reporting request enters `API Gateway` |
| Core Banking System | I-3 external; event is handled by `Earning Engine Service` through `Message Broker` |
| Partner Systems | I-3 external; fulfillment neighbor of `Redemption Engine Service` |
| CRM & Notification Gateway | I-3 external; notification neighbor |
| API Gateway | Exact I-4 participant and request entry SUT |
| Message Broker | Exact I-4 participant and event transport SUT |
| Earning Engine Service | Exact I-4 SUT for UC-LB-01 |
| Tiering System Service | Exact I-4 SUT for UC-LB-03 |
| Redemption Engine Service | Exact I-4 SUT for UC-LB-02 and selected Component container |
| Analytics & Reporting Service | Exact I-4 SUT for UC-LB-04 |
| Idempotency Store | Exact I-4 neighbor used for duplicate check and balance lock |
| Earning DB | Exact I-4 persistence participant |
| Tiering DB | Exact I-4 persistence participant |
| Redemption DB | Exact I-4 persistence participant |
| Data Warehouse | Exact I-4 persistence participant |

## UML Sequence — UC-LB-01 Process settled earn event

**Artifact RACI:** R Dev — Lê Huy Du | A SA — Vũ Trường Quang | C BA/PO — Đặng Duy Hoàng, Test — Lê Huy Du | I EA/DA/Sec — Khuất Duy Bách, Ops — Đặng Duy Hoàng, Owner — Facilitator

```plantuml
@startuml
actor "Core Banking System" as Core
participant "Message Broker" as Broker
participant "Earning Engine Service" as Earning
participant "Idempotency Store" as Idempotency
database "Earning DB" as EarningDB
Core -> Broker : settled transaction event
Broker -> Earning : deliver settled event
Earning -> Idempotency : check source transaction
alt duplicate event (CON.1)
  Idempotency --> Earning : duplicate key found
  Earning --> Broker : retain original result; no second posting
else new event
  Earning -> EarningDB : write PointTransaction
  Earning -> Broker : publish QP accrual event
end
@enduml
```

## UML Sequence — UC-LB-02 Redeem reward with FIFO

**Artifact RACI:** same UML Sequence row: R Dev — Lê Huy Du | A SA — Vũ Trường Quang | C BA/PO — Đặng Duy Hoàng, Test — Lê Huy Du | I EA/DA/Sec — Khuất Duy Bách, Ops — Đặng Duy Hoàng, Owner — Facilitator

```plantuml
@startuml
actor Member
participant "API Gateway" as Gateway
participant "Redemption Engine Service" as Redemption
participant "Idempotency Store" as Idempotency
database "Redemption DB" as RedemptionDB
participant "Partner Systems" as Partners
Member -> Gateway : submit redemption order
Gateway -> Redemption : route redemption request
Redemption -> Idempotency : lock member balance
Redemption -> RedemptionDB : create PENDING; reserve FIFO debit
Redemption -> Partners : dispatch fulfillment request
alt insufficient balance or tier-ineligible
  Redemption -> RedemptionDB : PENDING -> CANCELLED
else partner fulfillment failure (CON.3)
  Partners --> Redemption : fulfillment failure
  Redemption -> RedemptionDB : IN_PROGRESS -> FAILED
  Redemption -> Idempotency : restore original FIFO earn date and expiry
  Redemption -> RedemptionDB : FAILED -> REVERSED
else fulfillment succeeds
  Partners --> Redemption : delivery confirmation
  Redemption -> RedemptionDB : IN_PROGRESS -> FULFILLED
end
@enduml
```

## UML Sequence — UC-LB-03 Apply tier upgrade

**Artifact RACI:** same UML Sequence row: R Dev — Lê Huy Du | A SA — Vũ Trường Quang | C BA/PO — Đặng Duy Hoàng, Test — Lê Huy Du | I EA/DA/Sec — Khuất Duy Bách, Ops — Đặng Duy Hoàng, Owner — Facilitator

```plantuml
@startuml
participant "Earning Engine Service" as Earning
participant "Message Broker" as Broker
participant "Tiering System Service" as Tiering
database "Tiering DB" as TieringDB
Earning -> Broker : publish QP accrual event
Broker -> Tiering : deliver QP accrual event
alt replayed event
  Tiering --> Broker : ignore replay through idempotent handling
else threshold crossed
  Tiering -> TieringDB : update MemberTier
  Tiering -> Broker : publish tier change event
end
@enduml
```

## UML Sequence — UC-LB-04 Generate point liability report

**Artifact RACI:** same UML Sequence row: R Dev — Lê Huy Du | A SA — Vũ Trường Quang | C BA/PO — Đặng Duy Hoàng, Test — Lê Huy Du | I EA/DA/Sec — Khuất Duy Bách, Ops — Đặng Duy Hoàng, Owner — Facilitator

```plantuml
@startuml
actor Finance
participant "API Gateway" as Gateway
participant "Analytics & Reporting Service" as Analytics
database "Data Warehouse" as Warehouse
Finance -> Gateway : request point liability report
Gateway -> Analytics : route report request
Analytics -> Warehouse : read analytical facts
alt warehouse data stale beyond 10 minutes (CON.4)
  Analytics --> Finance : mark report stale and do not claim fresh result
else current facts
  Analytics --> Finance : return point liability report
end
@enduml
```

## UML Activity — I-5 Happy Path

**Artifact RACI:** R Test — Lê Huy Du | A BA/PO — Đặng Duy Hoàng | C SA — Vũ Trường Quang, Sec — Khuất Duy Bách | I EA/DA/Dev/Ops — Khuất Duy Bách / Lê Huy Du / Đặng Duy Hoàng, Owner — Facilitator

The activity follows I-5 exactly and uses business activities rather than C4 containers as activity boxes.

```plantuml
@startuml
start
:1. Capture settled transaction for Member;
:2. Validate event and check duplicate;
if (CON.1 duplicate?) then (yes)
  :Reject duplicate point posting;
  stop
else (no)
  :Calculate points and write PointTransaction;
endif
:3. Publish QP accrual;
:4. Update MemberTier if threshold crossed;
:5. Member requests reward;
:6. Validate tier and balance; reserve FIFO debit;
if (CON.2 forbidden direct write?) then (yes)
  :Reject forbidden database path;
  stop
else (no)
  :7. Partner Systems fulfill reward;
endif
:8. Compute liability and engagement reporting;
if (CON.4 data older than 10 minutes?) then (yes)
  :Mark report stale;
else (no)
  :Publish report;
endif
stop
@enduml
```

## UML State Machine — `RedemptionOrder`

**Artifact RACI:** same UML Activity / State row: R Test — Lê Huy Du | A BA/PO — Đặng Duy Hoàng | C SA — Vũ Trường Quang, Sec — Khuất Duy Bách | I EA/DA/Dev/Ops — Khuất Duy Bách / Lê Huy Du / Đặng Duy Hoàng, Owner — Facilitator

Exactly one object is modeled. The states are exactly the six I-6 values; terminal states are `CANCELLED`, `FULFILLED`, and `REVERSED`.

```plantuml
@startuml
[*] --> PENDING : create RedemptionOrder
PENDING --> IN_PROGRESS : validation passes; FIFO debit reserved
PENDING --> CANCELLED : validation fails or member cancels
IN_PROGRESS --> FULFILLED : Partner Systems confirms delivery
IN_PROGRESS --> FAILED : Partner Systems fulfillment fails
FAILED --> REVERSED : auto-reversal restores points
@enduml
```

## G6 Coverage Note

| Coverage ID | Planned test mapping | Evidence source |
|---|---|---|
| G6-T01 | `PENDING -> IN_PROGRESS` | I-6 / State Machine |
| G6-T02 | `PENDING -> CANCELLED` | I-6 / State Machine |
| G6-T03 | `IN_PROGRESS -> FULFILLED` | I-6 / State Machine |
| G6-T04 | `IN_PROGRESS -> FAILED` | I-6 / State Machine |
| G6-T05 | `FAILED -> REVERSED` | I-6 / State Machine |
| G6-A01 | UC-LB-01 duplicate event under CON.1 | Sequence UC-LB-01 |
| G6-A02 | UC-LB-02 insufficient balance or tier-ineligible | Sequence UC-LB-02 |
| G6-A03 | UC-LB-02 partner fulfillment failure and CON.3 compensation | Sequence UC-LB-02 |
| G6-A04 | UC-LB-03 replayed event | Sequence UC-LB-03 |
| G6-A05 | UC-LB-04 warehouse data stale beyond ten minutes under CON.4 | Sequence UC-LB-04 |

No tests were executed. G6 evidence is a planned modeling checklist only.

## Comparison with Lab 5 Before Pack

The same four exact I-11 use cases are retained. Lab 5 was the messy current-style source; this after pack standardizes it against Lab 8 Application Cooperation and Lab 9 C4 Container. Ambiguous or noncanonical lifelines are removed, all container participants now resolve to exact I-4 strings, modules appear only inside the selected `Redemption Engine Service`, and the Guide header, legend, and artifact-level RACI are added. The Lab 5 archive remains unchanged under `modeling-pack/archive-before/`.

## Lab 10 Done-when Check

| Requirement | Status |
|---|---|
| One audited sequence for every I-11 use case | Yes — UC-LB-01 through UC-LB-04 |
| Participants match Lab 9 Container names | Yes — participant-to-SUT map |
| One state machine for one I-6 object | Yes — `RedemptionOrder` only |
| All I-6 transitions covered | Yes — G6-T01 through G6-T05 |
| Every sequence `alt` covered | Yes — G6-A01 through G6-A05 |
| Header, legend, and RACI present | Yes — Guide template filled |
| Before archive unchanged | Yes — archive remains frozen |
