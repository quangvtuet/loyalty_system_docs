# Lab 3 — Build List, Component, Sequence, Contracts, Exceptions, Tests

**System-in-focus:** Loyalty Banking Platform
**R:** Dev (build list, Component, contracts) · Test (test spec) · **A:** SA
**Input:** `loyalty.md` (Lab 1 — I-4, I-6, I-8, I-9, I-10, I-11) and `lab2-requirements.md` (Lab 2)
**Language:** current team language. English only.

**Selected container from I-11 for the To-be Component:** `Redemption Engine Service`. No other container is opened up here.

This is a **specification of what will be built and tested**. It is design evidence in table form. Nothing here is coded, executed, or deployed in this sitting; every test row is planned, not run.

**Name rule:** every container, actor, external system, data object, and state below is a Lab 1 string. No product name is used as a system.

---

## 1. Build list

Every I-4 container, in build order, with its owner and its environment from I-9.

| Order | I-4 container | Owner (Dev) | Environment (I-9) | Why this position |
|---:|---|---|---|---|
| 1 | Message Broker | Đặng Duy Hoàng | Edge & Ingestion Zone | Every async path in I-8 depends on it; nothing can be wired without it |
| 2 | Idempotency Store | Khuất Duy Bách | Data Services Zone | Needed before any earn write (CON.1) and any debit lock |
| 3 | Earning DB | Khuất Duy Bách | Data Services Zone | Source of truth for `PointTransaction` and `PointBalance` |
| 4 | Tiering DB | Lê Huy Du | Data Services Zone | Source of truth for `MemberTier` |
| 5 | Redemption DB | Vũ Trường Quang | Data Services Zone | Source of truth for `RedemptionOrder` |
| 6 | Program Mgmt DB | Đặng Duy Hoàng | Data Services Zone | Source of truth for `LoyaltyProgram` and `Campaign` |
| 7 | Program Management Service | Đặng Duy Hoàng | Domain Services Zone | Owns the configuration the earning and tiering services read; must exist before them |
| 8 | Earning Engine Service | Khuất Duy Bách | Domain Services Zone | Produces `PointTransaction`, the object that moves through I-5 |
| 9 | Tiering System Service | Lê Huy Du | Domain Services Zone | Consumes qualifying accrual from step 3 |
| 10 | Redemption Engine Service | Vũ Trường Quang | Domain Services Zone | Depends on Earning Engine Service for debit and Tiering System Service for tier |
| 11 | Data Warehouse | Lê Huy Du | Analytics Zone | Target of the CDC path in step 8 |
| 12 | Analytics & Reporting Service | Lê Huy Du | Analytics Zone | Reads Data Warehouse only; built last because it depends on all upstream writes |
| 13 | API Gateway | Vũ Trường Quang | Edge & Ingestion Zone | Fronts the sync paths in I-8; wired once the services behind it exist |

---

## 2. To-be Component: `Redemption Engine Service`

Modules inside the one selected container. Every neighbour is a black box named with its Lab 1 string.

| Module | Responsibility | Neighbours it talks to (black boxes) |
|---|---|---|
| M1 Catalog Query | Serves the reward catalogue and each reward's minimum tier and point cost | API Gateway |
| M2 Order Intake | Accepts a redemption request and creates the `RedemptionOrder` in `PENDING` | API Gateway, Redemption DB |
| M3 Eligibility Check | Checks the Member's `MemberTier` against the reward's minimum tier, and checks the minimum redemption amount | Tiering System Service |
| M4 Balance Lock | Takes and releases the per-Member debit lock so two orders cannot spend the same points | Idempotency Store |
| M5 FIFO Debit Request | Asks for the debit of the oldest-earned points first and receives the allocation | Earning Engine Service |
| M6 Order State Keeper | Moves the `RedemptionOrder` through its I-6 states and persists each change | Redemption DB |
| M7 Fulfilment Dispatch | Sends the fulfilment request and receives the outcome | Partner Systems |
| M8 Reversal Handler | On failure, requests restoration of the original point batches and records the reversal | Earning Engine Service, CRM & Notification Gateway |

Neighbour containers and externals used above, all from Lab 1: API Gateway, Tiering System Service, Earning Engine Service, Idempotency Store, Redemption DB, Partner Systems, CRM & Notification Gateway.

---

## 3. To-be sequence: UC-LB-02 Redeem reward with FIFO

Happy path. Each message is owned by a module from section 2 or by a neighbour container.

| # | From | To | Message | Owner |
|---:|---|---|---|---|
| 1 | Member | API Gateway | Submit redemption request | Neighbour (actor → container) |
| 2 | API Gateway | M2 Order Intake | Route authenticated request | Neighbour container |
| 3 | M2 Order Intake | Redemption DB | Create `RedemptionOrder` in `PENDING` | M2 |
| 4 | M2 Order Intake | M4 Balance Lock | Take the Member's debit lock | M4 |
| 5 | M4 Balance Lock | Idempotency Store | Hold lock for this Member | Neighbour container |
| 6 | M2 Order Intake | M3 Eligibility Check | Check tier, reward cost, and minimum amount | M3 |
| 7 | M3 Eligibility Check | Tiering System Service | Read current `MemberTier` | Neighbour container |
| 8 | M2 Order Intake | M5 FIFO Debit Request | Request debit, oldest points first | M5 |
| 9 | M5 FIFO Debit Request | Earning Engine Service | Debit the oldest unexpired batches | Neighbour container |
| 10 | M6 Order State Keeper | Redemption DB | Move `RedemptionOrder` `PENDING` → `IN_PROGRESS` | M6 |
| 11 | M4 Balance Lock | Idempotency Store | Release the Member's debit lock | M4 |
| 12 | M7 Fulfilment Dispatch | Partner Systems | Send fulfilment request | M7 |
| 13 | Partner Systems | M7 Fulfilment Dispatch | Return fulfilment outcome | Neighbour external |
| 14 | M6 Order State Keeper | Redemption DB | Move `IN_PROGRESS` → `FULFILLED` | M6 |
| 15 | M6 Order State Keeper | CRM & Notification Gateway | Notify the Member of the outcome | Neighbour external |

### Alternate branches

| Alt ID | Condition | Branch behaviour | Constraint |
|---|---|---|---|
| ALT-01 | Member's available `PointBalance` is below the reward cost (step 6) | `RedemptionOrder` `PENDING` → `CANCELLED`; lock released; no debit | — |
| ALT-02 | `MemberTier` is below the reward's minimum tier (step 6) | `RedemptionOrder` `PENDING` → `CANCELLED`; lock released; no debit | — |
| ALT-03 | Request is below the 100-point minimum (step 6) | `RedemptionOrder` `PENDING` → `CANCELLED`; lock released; no debit | — |
| ALT-04 | Member cancels while the order is still `PENDING` | `RedemptionOrder` `PENDING` → `CANCELLED`; no debit | — |
| ALT-05 | Partner Systems return failure (step 13) | `RedemptionOrder` `IN_PROGRESS` → `FAILED`, then `FAILED` → `REVERSED`; original earn date and expiry restored | CON.3 |
| ALT-06 | The Member's debit lock cannot be taken (step 4) | Request is refused; the caller retries; no partial debit | — |

---

## 4. Contract register

One row per I-8 relationship. I-8 patterns: **Sync** — request/response through API Gateway; **Async** — event stream through Message Broker; **Adapter** — partner traffic normalised at API Gateway.

| ID | Producer | Consumer | Pattern (I-8) | Sync / async | Operation or event name |
|---|---|---|---|---|---|
| CT-01 | Core Banking System | Message Broker | Async | Async | `transaction.settled` |
| CT-02 | Message Broker | Earning Engine Service | Async | Async | `transaction.settled` consume |
| CT-03 | Partner Systems | API Gateway | Adapter | Sync | `SubmitPartnerEarn` |
| CT-04 | API Gateway | Earning Engine Service | Sync | Sync | `RecordEarn` |
| CT-05 | Earning Engine Service | Message Broker | Async | Async | `earning.qp_accrued` |
| CT-06 | Message Broker | Tiering System Service | Async | Async | `earning.qp_accrued` consume |
| CT-07 | Tiering System Service | Message Broker | Async | Async | `tiering.tier_changed` |
| CT-08 | Message Broker | Earning Engine Service | Async | Async | `tiering.tier_changed` consume |
| CT-09 | Message Broker | Redemption Engine Service | Async | Async | `tiering.tier_changed` consume |
| CT-10 | Member | API Gateway | Sync | Sync | `SubmitRedemption` |
| CT-11 | API Gateway | Redemption Engine Service | Sync | Sync | `CreateRedemptionOrder`, `QueryCatalog` |
| CT-12 | Redemption Engine Service | Tiering System Service | Sync | Sync | `GetMemberTier` |
| CT-13 | Redemption Engine Service | Earning Engine Service | Sync | Sync | `DebitPointsFifo`, `RestorePoints` |
| CT-14 | Redemption Engine Service | Partner Systems | Sync | Sync | `RequestFulfilment` |
| CT-15 | Partner Systems | API Gateway | Adapter | Sync | `ReturnFulfilmentOutcome` |
| CT-16 | Program Admin | API Gateway | Sync | Sync | `UpdateProgramConfiguration` |
| CT-17 | API Gateway | Program Management Service | Sync | Sync | `SaveProgramConfiguration`, `SaveCampaign` |
| CT-18 | Program Management Service | Message Broker | Async | Async | `config.rule_updated` |
| CT-19 | Message Broker | Earning Engine Service | Async | Async | `config.rule_updated` consume |
| CT-20 | Message Broker | Tiering System Service | Async | Async | `config.rule_updated` consume |
| CT-21 | Earning Engine Service | Message Broker | Async | Async | `earning.point_expired` |
| CT-22 | Message Broker | CRM & Notification Gateway | Async | Async | Member notification trigger |
| CT-23 | Earning DB, Tiering DB, Redemption DB, Program Mgmt DB | Message Broker | Async | Async | `cdc.platform_events` |
| CT-24 | Message Broker | Analytics & Reporting Service | Async | Async | `cdc.platform_events` consume |
| CT-25 | Finance | API Gateway | Sync | Sync | `RequestReport` |
| CT-26 | API Gateway | Analytics & Reporting Service | Sync | Sync | `GenerateReport`, `ReadDashboard` |
| CT-27 | Analytics & Reporting Service | Enterprise Data Warehouse | Async | Async | `PublishPeriodFigures` |

Every producer and consumer above is an I-2 actor, an I-3 external, or an I-4 container. No external outside I-3 appears.

---

## 5. Exception spec

Critical failure paths taken from I-10.

| ID | Constraint | Trigger | Compensating action | Who performs it |
|---|---|---|---|---|
| EXC-01 | CON.1 | A source transaction that has already been rewarded arrives again at I-5 step 2 | No second `PointTransaction` is written; the original result is returned so the sender can retry safely | Earning Engine Service |
| EXC-02 | CON.1 | Core Banking System reverses a source transaction that has already been rewarded | The matching `PointTransaction` is cancelled and `PointBalance` is corrected by its owner | Earning Engine Service |
| EXC-03 | CON.1 | The same qualifying accrual is delivered twice at I-5 step 4 | The replay is ignored; `MemberTier` is not moved a second time | Tiering System Service |
| EXC-04 | CON.2 | A channel, a partner, or another service attempts to write a database it does not own | The write is refused; the caller is routed to the owning service through API Gateway | API Gateway and the owning service |
| EXC-05 | CON.3 | Partner Systems return a fulfilment failure after points were debited at I-5 step 7 | `RedemptionOrder` moves `IN_PROGRESS` → `FAILED` → `REVERSED`; the debited batches are restored with their original earn date and expiry; the Member is notified | Redemption Engine Service, with Earning Engine Service performing the restoration |
| EXC-06 | CON.3 | A Support Agent must undo an order that has not been fulfilled | The order is reversed and the reason recorded; points return to their original position | Redemption Engine Service |
| EXC-07 | CON.4 | Data in Data Warehouse is more than 10 minutes behind at I-5 step 8 | The report or dashboard shows the staleness instead of presenting the figure as current, and Finance is alerted | Analytics & Reporting Service |
| EXC-08 | CON.4 | A reporting request would place load on a transactional store | The request is served from Data Warehouse only; it is never routed to Earning DB, Tiering DB, Redemption DB, or Program Mgmt DB | Analytics & Reporting Service |

---

## 6. Test spec

One row per I-6 transition of `RedemptionOrder`, and one row per alternate branch in section 3. Every SUT is an I-4 container name. All rows are **planned**; none is executed in this sitting.

### 6.1 I-6 state transitions

| Test ID | Transition | SUT (I-4) | Expected result |
|---|---|---|---|
| T-01 | `PENDING` → `IN_PROGRESS` | Redemption Engine Service | Balance, tier, and minimum-amount checks pass; oldest points are debited first; order moves to `IN_PROGRESS` |
| T-02 | `PENDING` → `CANCELLED` | Redemption Engine Service | A failed check or a Member cancellation ends the order with no points debited |
| T-03 | `IN_PROGRESS` → `FULFILLED` | Redemption Engine Service | Confirmed delivery makes the debit final and ends the order at `FULFILLED` |
| T-04 | `IN_PROGRESS` → `FAILED` | Redemption Engine Service | A delivery failure moves the order to `FAILED` and records the reason |
| T-05 | `FAILED` → `REVERSED` | Redemption Engine Service | Reversal restores the exact batches debited, keeping their original earn date and expiry, and ends the order at `REVERSED` |

### 6.2 Sequence alternates

| Test ID | Alt | SUT (I-4) | Expected result |
|---|---|---|---|
| T-06 | ALT-01 insufficient balance | Redemption Engine Service | Order is cancelled before any debit; `PointBalance` is unchanged |
| T-07 | ALT-02 tier ineligible | Redemption Engine Service | Order is cancelled before any debit; the reward stays unavailable to that `MemberTier` |
| T-08 | ALT-03 below minimum | Redemption Engine Service | A request under 100 points is cancelled before any debit |
| T-09 | ALT-04 Member cancels early | Redemption Engine Service | Cancellation is accepted while `PENDING` and refused once `IN_PROGRESS` |
| T-10 | ALT-05 fulfilment failure | Redemption Engine Service | Failure triggers reversal automatically; the Member is notified |
| T-11 | ALT-06 lock not available | Redemption Engine Service | The second concurrent order is refused; the same points are never debited twice |

### 6.3 Constraint checks outside the selected container

| Test ID | Covers | SUT (I-4) | Expected result |
|---|---|---|---|
| T-12 | CON.1 duplicate earn | Earning Engine Service | A repeated source transaction produces no second `PointTransaction` |
| T-13 | CON.1 tier replay | Tiering System Service | A repeated qualifying accrual does not move `MemberTier` twice |
| T-14 | CON.2 ownership | Program Management Service | Configuration is written only through its owning service |
| T-15 | CON.4 freshness | Analytics & Reporting Service | Figures older than 10 minutes are marked stale rather than shown as current |

---

## 7. Coverage check

| Check | Result |
|---|---|
| Every I-4 container has a build-list row | 13 of 13 |
| Component modules belong to one I-11 container only | `Redemption Engine Service` only; all others are black boxes |
| Every contract row realises an I-8 pattern | 27 of 27 (Sync, Async, Adapter) |
| Every contract party is an I-2 actor, I-3 external, or I-4 container | Yes; no invented external |
| Every I-10 constraint has an exception row | CON.1 (EXC-01…03), CON.2 (EXC-04), CON.3 (EXC-05, EXC-06), CON.4 (EXC-07, EXC-08) |
| Every I-6 transition has a test row | 5 of 5 (T-01…T-05) |
| Every sequence alt has a test row | 6 of 6 (T-06…T-11) |
| Every SUT is an I-4 name | Yes |
| Anything coded, run, or deployed in this sitting | No |
