# Name-identity map — capstone runtime

Every code identity below resolves to a Lab 1 string. Nothing in the runtime introduces a name that the after pack does not already carry.

Source of truth: `../loyalty.md` (Lab 1), `../lab8-archimate-views.md`, `../lab-09-c4-after.md`, `../lab-10-uml-after.md`.

---

## 1. Collapse declaration

The runtime is **one process with in-memory stores and an in-process bus**, which the capstone brief permits. Collapse maps existing I-4 containers onto modules; it does not create a new container identity, and no product is stood up.

| Collapsed from | Collapsed to | I-9 location the process stands for |
|---|---|---|
| 13 separately deployable I-4 containers | One JVM process, `com.loyalty.capstone.Platform` | Edge & Ingestion Zone, Domain Services Zone, Analytics Zone, and Data Services Zone all run inside the single process |
| Message Broker as a broker product | `broker.MessageBroker`, synchronous in-process publish/subscribe | Edge & Ingestion Zone |
| Earning DB, Tiering DB, Redemption DB, Program Mgmt DB, Data Warehouse | In-memory maps behind `store.OwnedStore` | Data Services Zone and Analytics Zone |
| Idempotency Store as a cache product | In-memory key set and lock set | Data Services Zone |

No cluster, container image, or broker admin is part of the output. `Kafka`, `Redis`, `PostgreSQL` and similar remain labels in Lab 1 and appear nowhere in code.

---

## 2. I-4 container to code identity

| # | I-4 container | Code identity | Kind |
|---:|---|---|---|
| 1 | API Gateway | `gateway.ApiGateway` | HTTP entry point, routes only |
| 2 | Message Broker | `broker.MessageBroker` | In-process bus (collapse) |
| 3 | Earning Engine Service | `service.EarningEngineService` | Sole writer of Earning DB; reads no other container's store |
| 4 | Tiering System Service | `service.TieringSystemService` | Sole writer of Tiering DB |
| 5 | Redemption Engine Service | `service.redemption.RedemptionEngineService` | Sole writer of Redemption DB; the one I-11 container drilled to modules |
| 6 | Program Management Service | `service.ProgramManagementService` | Sole writer of Program Mgmt DB |
| 7 | Analytics & Reporting Service | `service.AnalyticsReportingService` | Sole writer of Data Warehouse |
| 8 | Idempotency Store | `store.IdempotencyStore` | Duplicate keys and the per-member debit lock |
| 9 | Earning DB | `store.EarningDb` | Owner string `Earning Engine Service` |
| 10 | Tiering DB | `store.TieringDb` | Owner string `Tiering System Service` |
| 11 | Redemption DB | `store.RedemptionDb` | Owner string `Redemption Engine Service` |
| 12 | Program Mgmt DB | `store.ProgramMgmtDb` | Owner string `Program Management Service` |
| 13 | Data Warehouse | `store.DataWarehouse` | Owner string `Analytics & Reporting Service` |

The I-4 string is present in code as a literal — `EarningEngineService.CONTAINER` and the owner argument on each store — so ownership is checked against the Lab 1 name at runtime, not against a class name.

---

## 3. Lab 9 Component modules to code identity

Only `Redemption Engine Service` is decomposed, exactly as Lab 1 I-11 selected and Lab 9 drew. These are modules, not containers.

| Lab 9 Component | Code identity |
|---|---|
| Order Intake Module | `service.redemption.OrderIntakeModule` |
| Tier and Balance Validation Module | `service.redemption.TierAndBalanceValidationModule` |
| FIFO Debit Module | `service.redemption.FifoDebitModule` |
| RedemptionOrder State Module | `service.redemption.RedemptionOrderStateModule` |
| Fulfillment Coordination Module | `service.redemption.FulfillmentCoordinationModule` |

The Lab 3 before-pack sketch listed eight working modules (M1–M8). The after pack is authoritative, so the runtime follows the five Lab 9 modules. No sixth module was added.

---

## 4. I-7 objects to code types, with the single owner

| I-7 object | Code type | Source of truth | Only writer in code |
|---|---|---|---|
| `PointTransaction` | `domain.PointTransaction` | Earning DB | Earning Engine Service |
| `PointBalance` | `domain.PointBalance` | Earning DB | Earning Engine Service |
| `MemberTier` | `domain.MemberTier` | Tiering DB | Tiering System Service |
| `RedemptionOrder` | `domain.RedemptionOrder` | Redemption DB | Redemption Engine Service |
| `LoyaltyProgram` | `domain.LoyaltyProgram` | Program Mgmt DB | Program Management Service |
| `Campaign` | `domain.Campaign` | Program Mgmt DB | Program Management Service |
| `FactPointTransaction` | `domain.FactPointTransaction` | Data Warehouse | Analytics & Reporting Service |

Ownership is enforced, not documented: `store.OwnedStore.assertWriter` throws `store.OwnershipViolation` when any other container attempts a write. Tests `NEG-I9-01` and `NEG-I5-01` attempt the violation and assert the rejection.

---

## 5. I-6 states to code

`domain.OrderState` holds exactly the six I-6 values and nothing else. `domain.RedemptionOrder` is the typed object: each I-6 transition is an operation on that type, and a transition outside I-6 throws `domain.IllegalStateTransition`. States never exist as free strings.

| I-6 state | Enum constant | Terminal |
|---|---|---|
| `PENDING` | `OrderState.PENDING` | No |
| `IN_PROGRESS` | `OrderState.IN_PROGRESS` | No |
| `FULFILLED` | `OrderState.FULFILLED` | Yes |
| `FAILED` | `OrderState.FAILED` | No |
| `CANCELLED` | `OrderState.CANCELLED` | Yes |
| `REVERSED` | `OrderState.REVERSED` | Yes |

---

## 6. I-2 actors and I-3 externals

| Lab 1 name | Kind | In the runtime |
|---|---|---|
| Member | I-2 actor | Caller of `POST /redemptions` |
| Program Admin | I-2 actor | No I-11 use case; not built — see section 8 |
| Finance | I-2 actor | Caller of `GET /reports/point-liability` |
| Support Agent | I-2 actor | No I-11 use case; not built — see section 8 |
| Core Banking System | I-3 external | `external.CoreBankingSystemMock` — publishes simulated settled events |
| Partner Systems | I-3 external | `external.PartnerSystemsMock` — fulfillment fake; caller of `POST /partner-earn` |
| CRM & Notification Gateway | I-3 external | `external.CrmNotificationGatewayMock` — records notifications |
| Enterprise Data Warehouse | I-3 external | `external.EnterpriseDataWarehouseMock` — receives period figures |

Every I-3 external is a stub or in-process fake. The runtime opens no outbound connection, holds no credential, and reads no configuration secret.

---

## 7. Event names

Topic strings come from the Lab 3 contract register and are declared once in `broker.Topics`.

| Topic constant | Contract row |
|---|---|
| `transaction.settled` | CT-01, CT-02 |
| `earning.qp_accrued` | CT-05, CT-06 |
| `tiering.tier_changed` | CT-07 published by Tiering System Service; CT-08 consumed by Earning Engine Service |
| `cdc.platform_events` | CT-23, CT-24 |

`EarningEngineService` holds a local read model of `MemberTier` fed by CT-08. It is a projection, not a second source of truth: it is never written by anyone else, never persisted, and `MemberTier` remains owned by Tiering DB alone.

---

## 8. Not built, and why

The capstone slice is I-11 only. These I-1 in-scope items are **N/A**, not a backlog:

| Item | Why it is out |
|---|---|
| Program Admin configuration operations | No I-11 use case names them. `ProgramManagementService` exists only because it owns the rule values the I-11 paths read. |
| Support Agent adjustment and reversal operations | No I-11 use case names them. |
| Tier downgrade, grace period, point expiry | Lab 2 requirements, but no I-11 use case names them. |
| Enterprise reporting hand-off | `EnterpriseDataWarehouseMock` exists as the I-3 stub; no I-11 use case drives it. |

---

## 9. Assumptions

Values the Lab 1 index did not fix. Each is simulated and used with one spelling everywhere.

| `ASSUMPTION` | Value | Where it came from |
|---|---|---|
| Program identifier | `BANK-REWARDS` | I-1 names the product "Banking Rewards Program"; no identifier was given |
| Reward catalogue entries | `RI-VOUCHER-300`, `RI-LOUNGE-500`, `RI-OUTOFSTOCK-300` | I-4 says Redemption DB stores reward items; no catalogue was specified |
| Campaign identifier | `CMP-DOUBLE-POINTS` | Lab 2 REQ-LB-06 fixes the 2x default; the identifier is invented |
| Point lifetime | 365 days | Lab 2 REQ-LB-10 says twelve months |
| Member identifiers | `M-…` in tests and the demo | Simulated only; no real customer data |

Rule values that are **not** assumptions, because Lab 2 fixed them: 1 point per unit spent (REQ-LB-05), 2x campaign (REQ-LB-06), tier thresholds 0 / 1,000 / 3,000 (REQ-LB-18), tier multipliers 1x / 1.5x / 2x (REQ-LB-20), minimum redemption 100 points (REQ-LB-27), cost per point $0.01 (REQ-LB-54), staleness limit ten minutes (CON.4).

---

## 10. Spelling policy

Lab 1 spells the word **`fulfillment`**. That spelling is authoritative for this runtime: prose, comments, class and method names, test names, and OpenAPI descriptions all use it.

Two identifiers are excepted because `lab3-spec.md` defined them and they are source-defined names, not invented ones:

| Frozen identifier | Where it is defined | Why it is not renamed |
|---|---|---|
| `RequestFulfilment` | `lab3-spec.md` §4, contract row CT-14 | Renaming it would fork a name the Lab 3 register already fixed |
| `ReturnFulfilmentOutcome` | `lab3-spec.md` §4, contract row CT-15 | Same |

Everywhere else — including the Java method that realises CT-14, which is `requestFulfillment` — the Lab 1 spelling applies. The Lab 9 component name `Fulfillment Coordination Module` already used it, so `FulfillmentCoordinationModule` needed no change.

One spelling per thing: there is no remaining occurrence of `fulfilment` in `capstone/` or `lab3-spec.md` outside those two frozen identifiers.

