## Lab 3 Build, Design, Contract, Exception, and Test Specification

**R**: Dev for build list, Component, contracts; Test for test spec  
**A**: SA  
**Selected I-11 container for optional C4 Component**: `Redemption Engine Service`

### Build List

| Build order | I-4 container | Owner | Environment from I-9 | Existing evidence |
|---:|---|---|---|---|
| 1 | Message Broker - Apache Kafka | Dev / Ops | Edge & Ingestion Zone | `docker-compose.yml` |
| 2 | Redis Cache & Distributed Lock | Dev / Ops | Data Services Zone | `docker-compose.yml` |
| 3 | Earning DB - PostgreSQL | Dev / DA | Data Services Zone | `architecture/Data-Architecture-and-Schema.md` |
| 4 | Tiering DB - PostgreSQL | Dev / DA | Data Services Zone | `architecture/Data-Architecture-and-Schema.md` |
| 5 | Redemption DB - PostgreSQL | Dev / DA | Data Services Zone | `architecture/Data-Architecture-and-Schema.md` |
| 6 | Program Mgmt DB - PostgreSQL | Dev / DA | Data Services Zone | `architecture/Data-Architecture-and-Schema.md` |
| 7 | Data Warehouse - Star Schema | Dev / DA | Analytics Zone | `architecture/Data-Architecture-and-Schema.md` |
| 8 | Earning Engine Service | Dev | Domain Services Zone | `earning-engine/` |
| 9 | Tiering System Service | Dev | Domain Services Zone | `tiering-system/` |
| 10 | Redemption Engine Service | Dev | Domain Services Zone | `redemption-engine/` |
| 11 | Program Management Service | Dev | Domain Services Zone | `program-management/` |
| 12 | Analytics & Reporting Service | Dev | Analytics Zone | `analytics-reporting/` |
| 13 | API Gateway / OAuth 2.0 Auth Server | Dev / Sec | Edge & Ingestion Zone | Modeled in C4 and ArchiMate; no runtime install in modeling pack |

### To-Be Component: Redemption Engine Service

| Component module | Responsibility | Neighbor containers |
|---|---|---|
| Catalog API Controller | Exposes reward catalog and item creation/query operations | API Gateway / OAuth 2.0 Auth Server |
| Redemption Order Controller | Accepts redemption order requests and fulfillment callbacks | API Gateway / OAuth 2.0 Auth Server, Partner Systems |
| Tier Access Validator | Checks member tier eligibility for reward item | Tiering System Service |
| Balance Lock Service | Acquires Redis lock for member debit operations | Redis Cache & Distributed Lock |
| FIFO Debit Engine | Allocates oldest confirmed point batches first | Earning Engine Service |
| Redemption Service | Coordinates order lifecycle and persistence | Redemption DB - PostgreSQL |
| Auto-Reversal Handler | Restores points and updates failed orders | Earning Engine Service, CRM & Notification Gateway |

Primary evidence: `design/DD-03-redemption-engine.md` and `redemption-engine/`.

### To-Be Sequence: UC-LB-02 Redeem Reward with FIFO

| Step | Message | Owner |
|---:|---|---|
| 1 | Member / Customer -> API Gateway / OAuth 2.0 Auth Server: submit redemption order | Neighbor actor/container |
| 2 | API Gateway / OAuth 2.0 Auth Server -> Redemption Order Controller: route order request | Neighbor container |
| 3 | Redemption Order Controller -> Balance Lock Service: acquire member balance lock | Component module |
| 4 | Redemption Order Controller -> Tier Access Validator: validate reward tier access | Component module |
| 5 | Redemption Order Controller -> FIFO Debit Engine: allocate FIFO point batches | Component module |
| 6 | FIFO Debit Engine -> Earning Engine Service: reserve pending debit batches | Neighbor container |
| 7 | Redemption Service -> Redemption DB - PostgreSQL: create `RedemptionOrder` | Component module / data container |
| 8 | Redemption Service -> Partner Systems: dispatch fulfillment request | Neighbor external |
| 9 | Partner Systems -> Redemption Service: return fulfillment result | Neighbor external |
| 10 | Auto-Reversal Handler -> Earning Engine Service: restore points when fulfillment fails | Component module / neighbor container |

| Alt ID | Condition | Modeled behavior |
|---|---|---|
| ALT-UC-LB-02-01 | Insufficient confirmed balance | Reject order before FIFO debit |
| ALT-UC-LB-02-02 | Tier is below reward minimum | Reject order before FIFO debit |
| ALT-UC-LB-02-03 | Partner fulfillment fails | Mark order FAILED and trigger auto-reversal under CON.3 |

### Contract Register (G4)

| Contract ID | Producer | Consumer | Sync or async | Operation or event name | Evidence |
|---|---|---|---|---|---|
| CON-R-001 | Core Banking System | Message Broker - Apache Kafka | Async | `TRANSACTION_SETTLED` | `architecture/domain-event-catalog.md` EVT-001 |
| CON-R-002 | Message Broker - Apache Kafka | Earning Engine Service | Async | `corebanking.transactions.settled` consume | `Architecture-Overview.md` C4 Container |
| CON-R-003 | Partner Systems | API Gateway / OAuth 2.0 Auth Server | Sync | Partner earn API | `FR-04`, `Security-and-Integration-Architecture.md` |
| CON-R-004 | API Gateway / OAuth 2.0 Auth Server | Earning Engine Service | Sync | `POST /api/v1/partners/earn` | `earning-engine/` |
| CON-R-005 | Earning Engine Service | Message Broker - Apache Kafka | Async | `EARN_QP_ACCRUED` | `architecture/domain-event-catalog.md` EVT-002 |
| CON-R-006 | Message Broker - Apache Kafka | Tiering System Service | Async | QP accrual consume | `tiering-system/` |
| CON-R-007 | Tiering System Service | Message Broker - Apache Kafka | Async | `TIER_CHANGED` | `architecture/domain-event-catalog.md` EVT-004 |
| CON-R-008 | Message Broker - Apache Kafka | Earning Engine Service | Async | tier multiplier update consume | `Architecture-Overview.md` C4 Container |
| CON-R-009 | Message Broker - Apache Kafka | Redemption Engine Service | Async | catalog eligibility update consume | `Architecture-Overview.md` C4 Container |
| CON-R-010 | API Gateway / OAuth 2.0 Auth Server | Redemption Engine Service | Sync | Redemption catalog/order APIs | `redemption-engine/` |
| CON-R-011 | Redemption Engine Service | Earning Engine Service | Sync or equivalent internal contract | FIFO debit / reversal request | `design/DD-03-redemption-engine.md` |
| CON-R-012 | Redemption Engine Service | Partner Systems | Sync | fulfillment dispatch | `design/DD-03-redemption-engine.md` |
| CON-R-013 | Earning Engine Service | CRM & Notification Gateway | Async | expiry / earn notification trigger | `Architecture-Overview.md` C4 Container |
| CON-R-014 | Tiering System Service | CRM & Notification Gateway | Async | tier alert trigger | `Architecture-Overview.md` C4 Container |
| CON-R-015 | Redemption Engine Service | CRM & Notification Gateway | Async | order / reversal alert trigger | `Architecture-Overview.md` C4 Container |
| CON-R-016 | Service databases | Message Broker - Apache Kafka | Async | CDC stream | `architecture/domain-event-catalog.md`; ADR-003 |
| CON-R-017 | Message Broker - Apache Kafka | Analytics & Reporting Service | Async | `loyalty.cdc.platform_events` | `architecture/domain-event-catalog.md` |

### Exception Spec (G5)

| Exception ID | Critical failure path | Trigger | Compensating action | Who performs it | Evidence |
|---|---|---|---|---|---|
| EX-LB-01 | Duplicate earn posting | Duplicate source transaction is received | Do not write a second `PointTransaction`; return original result or duplicate response | Earning Engine Service | CON.1; `DD-01`; `EarnCalculatorTest` / `EarningLedgerServiceTest` |
| EX-LB-02 | Direct write bypass | Channel, partner, or service attempts to write another service database | Reject path by architecture; only owning service may write source of truth | API Gateway / OAuth 2.0 Auth Server and owning service | CON.2; ADR-001 |
| EX-LB-03 | Fulfillment failure after debit reservation | Partner Systems return fulfillment failure | Mark order FAILED, restore FIFO batches, append reversal, notify member | Redemption Engine Service and Earning Engine Service | CON.3; `DD-03` fulfillment failure sequence |
| EX-LB-04 | Analytics stale data | CDC data is late beyond target | Report staleness and keep transactional workload isolated | Analytics & Reporting Service | ADR-003; `DD-05` |

### Test Spec (G6)

| Test ID | Covers | SUT (C4 name) | Expected result | Evidence / planned test |
|---|---|---|---|---|
| T-G6-001 | `RedemptionOrder`: Start -> PENDING | Redemption Engine Service | New order is created with PENDING status | `RedemptionServiceTest` / planned order creation scenario |
| T-G6-002 | `RedemptionOrder`: PENDING -> IN_PROGRESS | Redemption Engine Service | Valid order reserves FIFO debit and enters processing | Planned service test |
| T-G6-003 | `RedemptionOrder`: PENDING -> CANCELLED | Redemption Engine Service | Invalid or cancelled request does not debit points | Planned validation test |
| T-G6-004 | `RedemptionOrder`: IN_PROGRESS -> FULFILLED | Redemption Engine Service | Fulfillment success completes order and confirms debit | Planned fulfillment callback test |
| T-G6-005 | `RedemptionOrder`: IN_PROGRESS -> FAILED | Redemption Engine Service | Fulfillment failure records failure reason | Planned fulfillment callback test |
| T-G6-006 | `RedemptionOrder`: FAILED -> REVERSED | Redemption Engine Service | Points restored with original FIFO earn date and expiry | Planned reversal test |
| T-G6-007 | ALT-UC-LB-02-01 insufficient balance | Redemption Engine Service | Order rejected before FIFO debit | Existing/planned `RedemptionServiceTest` |
| T-G6-008 | ALT-UC-LB-02-02 tier ineligible | Redemption Engine Service | Order rejected with tier eligibility error | Existing/planned `CatalogServiceTest` and validation test |
| T-G6-009 | ALT-UC-LB-02-03 fulfillment failure | Redemption Engine Service | Auto-reversal is triggered | Planned reversal test |
| T-G6-010 | Duplicate earn event under CON.1 | Earning Engine Service | No duplicate ledger entry | `EarningLedgerServiceTest` |
| T-G6-011 | Tier upgrade after QP threshold | Tiering System Service | `MemberTier` becomes higher tier | `TierUpgradeServiceTest` |
| T-G6-012 | Analytics liability calculation | Analytics & Reporting Service | Liability equals unspent points times cost per point | `ReportingServiceTest` |

