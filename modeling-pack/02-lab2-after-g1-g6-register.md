# Lab 2 After Pass: Requirements, Analysis, Quality Gates

**R**: BA for requirements, EA for trace to Motivation  
**A**: Owner  
**Source scope**: Lab 1 index in `01-lab1-input-index.md`.

## Requirements List

| Requirement ID | Requirement | Traces to |
|---|---|---|
| REQ-LB-01 | Loyalty Banking Platform shall consume settled transaction events and process earn points within the 60-second settlement SLA. | Goal, Outcome, I-5 step 1, CON.1 |
| REQ-LB-02 | Earning Engine Service shall prevent duplicate point postings for the same source transaction before writing to the earning ledger. | CON.1, `PointTransaction` state, UC-LB-01 |
| REQ-LB-03 | Service-owned databases shall only be modified through their owning service APIs, events, or internal persistence logic. | CON.2, I-7, I-9 forbidden path |
| REQ-LB-04 | Redemption Engine Service shall validate member tier, available balance, and minimum redemption rules before reserving FIFO point batches. | I-5 step 6, `RedemptionOrder` PENDING -> IN_PROGRESS |
| REQ-LB-05 | Fulfillment failure shall trigger automatic reversal and restore points with their original FIFO earn date and expiry schedule. | CON.3, `RedemptionOrder` FAILED -> REVERSED |
| REQ-LB-06 | Tiering System Service shall update member tier when QP thresholds are reached and publish tier change events to downstream services. | I-5 step 4, UC-LB-03 |
| REQ-LB-07 | Analytics & Reporting Service shall compute liability and reporting KPIs from warehouse data, not from transactional service databases. | Outcome, CON.2, I-7 |
| REQ-LB-08 | C4 Container names, ArchiMate Application Component names, UML lifeline names, and test SUT names shall use the Lab 1 name-identity index. | Guide name identity, G3, G6 |

Existing detailed requirement evidence remains in:

- `requirements/FR-01-earning-engine.md`
- `requirements/FR-02-tiering-system.md`
- `requirements/FR-03-redemption-engine.md`
- `requirements/FR-04-program-management.md`
- `requirements/FR-05-analytics-reporting.md`

## Analysis

### As-Is

The baseline problem is a fragmented loyalty process where point earning, tiering, redemption, campaign configuration, and reporting can be specified or implemented independently. This creates risks of duplicate point postings, inconsistent source-of-truth ownership, direct database coupling, and missing compensation paths for failed redemption fulfillment.

### To-Be

The target is a model-driven modular platform:

- ArchiMate aligns business goals, constraints, business process, application cooperation, and deployment locations.
- C4 defines one system boundary and one container view using the same container names as the Lab 1 index.
- UML defines behavior and tests for named use cases only.
- `G1` to `G6` gates are used for trainee pack review.

### Capabilities Implied by the Goal

| Capability | Evidence |
|---|---|
| Real-time earning and idempotent ledger posting | `FR-01`, `DD-01`, `ADR-002` |
| Automated tier lifecycle | `FR-02`, `DD-02`, `entity-lifecycle-models.md` |
| FIFO redemption and failure compensation | `FR-03`, `DD-03`, `CON.3` |
| Governed program and campaign configuration | `FR-04`, `DD-04` |
| Isolated analytics and liability reporting | `FR-05`, `AS-05`, `ADR-003` |

### Exception Paths Named

| Exception ID | Trigger | Related constraint | Modeled response |
|---|---|---|---|
| EX-LB-01 | Duplicate earn event received | CON.1 | Earning Engine Service rejects or returns original result without a second ledger entry |
| EX-LB-02 | External/channel attempts direct database write | CON.2 | Path is forbidden; request must route through owning service/API/event |
| EX-LB-03 | Partner fulfillment fails after debit reservation | CON.3 | Redemption Engine Service marks order failed and executes auto-reversal |
| EX-LB-04 | Tier-restricted reward requested by ineligible member | CON.2 | Redemption Engine Service rejects before FIFO debit |

## Gate Register: G1 to G6

| Gate | Pass rule for Loyalty Banking | Evidence artifact | Pass? |
|---|---|---|---|
| G1 Strategy signed | Goal, outcome, and constraints listed | `01-lab1-input-index.md` I-1 and I-10; `architecture/archimate/motivation-layer.md`; `architecture/archimate/strategy-layer.md` | Pass |
| G2 Process + states | Happy path and named states match the state view | `01-lab1-input-index.md` I-5 and I-6; `design/entity-lifecycle-models.md`; `design/DD-03-redemption-engine.md` | Pass |
| G3 C4 Context + Container | No unnamed externals; sync/async labeled; names match Input index | `architecture/Architecture-Overview.md` sections 2 and 3; `01-lab1-input-index.md` I-2 to I-4 and I-8 | Pass |
| G4 Contracts | Contract or equivalent register exists for every Container relationship | `03-lab3-build-design-test-spec.md` Contract Register; `architecture/domain-event-catalog.md` | Pass with register |
| G5 Critical exception path | Critical failure path has compensating action | `03-lab3-build-design-test-spec.md` Exception Spec; `design/DD-03-redemption-engine.md` fulfillment failure sequence | Pass |
| G6 Test coverage | All state transitions and sequence alternatives are mapped to planned tests; participants are C4 names | `03-lab3-build-design-test-spec.md` Test Spec; implementation tests under `loyalty-platform-impl/*/src/test` | Pass with planned coverage |

## Trace Table

| Requirement ID | Process step | CON.* | Named object/state |
|---|---|---|---|
| REQ-LB-01 | I-5 step 1 -> step 2 | CON.1 | `PointTransaction` created |
| REQ-LB-02 | I-5 step 2 | CON.1 | `PointTransaction` duplicate avoided |
| REQ-LB-03 | I-5 all service-owned writes | CON.2 | Source-of-truth objects in I-7 |
| REQ-LB-04 | I-5 step 6 | CON.2 | `RedemptionOrder`: PENDING -> IN_PROGRESS |
| REQ-LB-05 | I-5 step 7 | CON.3 | `RedemptionOrder`: FAILED -> REVERSED |
| REQ-LB-06 | I-5 step 4 | CON.2 | `MemberTier` update |
| REQ-LB-07 | I-5 step 8 | CON.2 | `FactPointTransaction` in Data Warehouse |
| REQ-LB-08 | All diagrams and tests | CON.2 | UML lifeline and SUT names match I-4 |

## Before and After File Policy

- Before requirements remain in the existing FR documents and branch history.
- This file is the after-pass Lab 2 gate register and trace supplement.
- No extra trainee gate set is introduced here; `G1` to `G6` are authoritative for the modeling pack.

