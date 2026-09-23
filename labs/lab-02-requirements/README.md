# Lab 2 — Requirements, Analysis, and Trace

**System-in-focus:** Loyalty Banking Platform
**R:** BA · **A:** Owner
**Input:** `loyalty.md` (Lab 1 index — I-1, I-5, I-6, I-10)
**Language:** current team language. English only.

This is the **single Lab 2 file**. It holds the three Lab 2 artifacts: requirements list, analysis, trace table. It replaces the earlier split across `requirements/FR-01..05`, `analytics/AS-01..05`, `loyalty_domain.md`, and `traceability.md`, which are folded into the sections below.

**No quality gates in this file.** Quality gates are a later sitting and are deliberately not written, referenced, or scored here.

**Name rule:** every system, actor, container, and data object below comes from the Lab 1 index. Nothing else is named. A product name is a label on a Lab 1 container, never a system, an actor, or the subject of a requirement.

---

## 1. Scope carried from Lab 1

| Item | Value |
|---|---|
| Goal | Provide a governed loyalty platform for earning points, tier progression, redemption, program configuration, and financial reporting. |
| Outcome | Process settled earn events within 60 seconds; prevent duplicate point postings; support FIFO redemption; provide analytics data with no more than 10 minutes staleness. |
| Baseline → target | Baseline: fragmented loyalty processes and manual reconciliation. Target: modular platform with traceable requirements, contracts, states, and tests. |

| ID | Constraint |
|---|---|
| CON.1 | No duplicate point posting for the same source transaction. |
| CON.2 | No direct database writes from channels, partners, or other services into service-owned databases. |
| CON.3 | Fulfillment failure must compensate by restoring points with original FIFO earn date and expiry. |
| CON.4 | Analytics reporting data must not exceed 10 minutes staleness. |

I-5 happy-path steps referenced throughout as **step 1** … **step 8**. The business object that moves is `PointTransaction`.
I-6 named object is `RedemptionOrder`, with states `PENDING`, `IN_PROGRESS`, `FULFILLED`, `FAILED`, `CANCELLED`, `REVERSED`.

---

## 2. Analysis

### 2.1 As-is (business language)

Loyalty today is run as a set of disconnected activities rather than one governed process.

- **Points are awarded late and inconsistently.** Award decisions are made from batch extracts pulled long after settlement, so a Member cannot see the effect of a purchase on the same day, and campaign bonuses are frequently applied by hand after the fact.
- **The same purchase can be paid for twice.** Because several extracts can carry the same source transaction, and there is no single check that a transaction has already been rewarded, duplicate postings are found only during reconciliation, after the points are already spendable.
- **Tier status is a periodic clerical exercise.** Qualifying activity is totalled manually at period end. Members who cross a threshold mid-period wait for the next run, and a Member who narrowly misses is downgraded without any chance to recover.
- **Redeeming is not trustworthy for the business.** There is no agreed order in which a Member's points are consumed, so the value of what remains cannot be stated confidently. When a partner fails to deliver, restoring the Member's points is a manual correction that loses the original age of those points.
- **Program rules are edited in place.** Changing an earn rate or a campaign silently changes how past activity is understood, so Finance cannot reproduce an earlier figure.
- **Reporting competes with operations.** Liability and engagement figures are produced by querying the same systems that serve Members, which makes reporting slow, makes operations slower, and leaves Finance working from numbers of unknown age.

The business pain, stated plainly: **nobody can say what the programme currently owes, and nobody can prove why a Member has the balance they have.**

### 2.2 To-be

- Settled activity from Core Banking System is picked up and turned into a `PointTransaction` **within 60 seconds**, so a Member sees the result of a purchase while it still means something to them.
- Each source transaction can be rewarded **exactly once**, checked before anything is written, so duplicates are prevented rather than detected later (CON.1).
- Tier movement is **automatic**: upgrades apply as soon as the qualifying threshold is crossed, and a Member facing a downgrade gets a defined grace window to recover instead of an immediate demotion.
- Redemption consumes points in a **stated, repeatable order (oldest first)**, and a failed fulfilment **restores the Member's points to exactly the position they held before**, including their original age and expiry (CON.3).
- Programme and campaign changes take effect **going forward only**, so any past figure can be reproduced.
- Reporting is served from a **separate analytical copy** that is never more than 10 minutes behind, so Finance gets fresh numbers without slowing down Members (CON.4).
- Every store of record has **one owner**, and everything else reaches it through that owner (CON.2).

### 2.3 Capabilities implied by the goal

| # | Capability | Why the goal needs it | Serves |
|---|---|---|---|
| CAP-1 | Timely, once-only point accrual | The goal names "earning points" and the outcome names 60 seconds and no duplicates | Goal, Outcome, CON.1 |
| CAP-2 | Automatic tier progression and recovery | The goal names "tier progression" | Goal |
| CAP-3 | Ordered redemption with reversible fulfilment | The goal names "redemption"; the outcome names FIFO | Goal, Outcome, CON.3 |
| CAP-4 | Governed, non-retroactive programme configuration | The goal names "program configuration" | Goal |
| CAP-5 | Isolated, fresh financial reporting | The goal names "financial reporting"; the outcome names 10 minutes | Goal, Outcome, CON.4 |
| CAP-6 | Single ownership of every store of record | Required for all of the above to be believable | CON.2 |

### 2.4 Exception paths (named)

| ID | Exception | Trigger | Constraint | Response |
|---|---|---|---|---|
| EX-01 | Duplicate earn event | A source transaction already rewarded arrives again at step 2 | CON.1 | Earning Engine Service returns the original result and writes no second `PointTransaction` |
| EX-02 | Unsettled activity | An authorisation that has not settled arrives at step 1 | — | Held; no `PointTransaction` is confirmed until settlement |
| EX-03 | Source transaction reversed | Core Banking System reverses activity already rewarded | CON.1 | The matching `PointTransaction` is cancelled; `PointBalance` is corrected by the owning service |
| EX-04 | Insufficient balance | Requested reward costs more than the Member's available `PointBalance` at step 6 | — | `RedemptionOrder` goes `PENDING` → `CANCELLED`; no debit occurs |
| EX-05 | Tier-ineligible reward | Requested reward requires a higher tier than the Member's `MemberTier` at step 6 | — | `RedemptionOrder` goes `PENDING` → `CANCELLED` before any debit |
| EX-06 | Below minimum redemption | Requested redemption is under 100 points at step 6 | — | `RedemptionOrder` goes `PENDING` → `CANCELLED` |
| EX-07 | Fulfilment failure | Partner Systems cannot deliver after debit at step 7 | CON.3 | `RedemptionOrder` goes `IN_PROGRESS` → `FAILED` → `REVERSED`; points restored with original earn date and expiry |
| EX-08 | Member cancels early | Member cancels while the order is still `PENDING` | — | `RedemptionOrder` goes `PENDING` → `CANCELLED`; no debit occurs |
| EX-09 | Grace-period rescue | A Member below threshold at period end earns the shortfall during the grace window | — | Downgrade is cancelled; `MemberTier` stays at the current tier |
| EX-10 | Direct write attempt | A channel, partner, or other service attempts to write a service-owned database | CON.2 | Rejected; the request must go through the owning service at step 2, 4, 6, or 8 |
| EX-11 | Stale analytics | Warehouse data at step 8 is more than 10 minutes behind | CON.4 | Reports and dashboards flag the staleness; Finance is alerted rather than shown an unmarked stale figure |
| EX-12 | Repeated tier event | The same qualifying accrual is delivered twice at step 4 | CON.1 | Tiering System Service ignores the replay; `MemberTier` is unchanged |

---

## 3. Requirements list

Each requirement traces to a Goal, an Outcome, a `CON.*`, an I-5 process step, or a named object/state.

### 3.1 Earn ingestion and point accrual — steps 1–2

| ID | Requirement | Traces to |
|---|---|---|
| REQ-LB-01 | Earning Engine Service shall consume settled transaction events published by Core Banking System and complete point accrual within 60 seconds of settlement. | Outcome, step 1, step 2 |
| REQ-LB-02 | Only settled activity shall create a confirmed `PointTransaction`; authorisations that have not settled shall be held and not counted as spendable. | step 1, EX-02 |
| REQ-LB-03 | Earning Engine Service shall check whether a source transaction has already been rewarded **before** writing to Earning DB, and shall write no second `PointTransaction` for that source transaction. | CON.1, step 2 |
| REQ-LB-04 | A repeated earn event shall return the result of the original `PointTransaction` rather than an error, so that the sender can retry safely. | CON.1, EX-01 |
| REQ-LB-05 | Base points shall be calculated as the transaction amount multiplied by the earn rate, rounded down, with a default rate of 1 point per $1 spent. | Goal, step 2 |
| REQ-LB-06 | Campaign bonus shall be applied on top of base points using the `Campaign` configuration active at the time of the source transaction, with a default bonus of 2× for a running campaign. | Goal, step 2, `Campaign` |
| REQ-LB-07 | Where several campaigns match, the one with the highest priority shall apply, unless the `Campaign` configuration explicitly allows bonuses to combine. | step 2, `Campaign` |
| REQ-LB-08 | `PointTransaction` records shall be append-only; corrections shall be made by writing a further record, never by editing or deleting an existing one. | CON.1, `PointTransaction` |
| REQ-LB-09 | Earning Engine Service shall keep `PointBalance` consistent with the `PointTransaction` records it owns in Earning DB. | CON.2, `PointBalance` |
| REQ-LB-10 | Each earned `PointTransaction` shall carry an earn date and an expiry date, defaulting to 12 months after the earn date. | step 2, `PointTransaction` |
| REQ-LB-11 | Points that reach their expiry date shall be expired by Earning Engine Service and shall stop counting toward the Member's available balance. | `PointTransaction`, `PointBalance` |
| REQ-LB-12 | Members shall be notified through CRM & Notification Gateway ahead of expiry, at 30 days and again at 7 days before the expiry date. | step 2, Member |
| REQ-LB-13 | Where Core Banking System reverses a source transaction that has already been rewarded, the matching `PointTransaction` shall be cancelled and `PointBalance` corrected. | CON.1, EX-03 |
| REQ-LB-14 | Earn events submitted by Partner Systems shall enter through API Gateway and shall be subject to the same duplicate check as events from Core Banking System. | CON.1, CON.2, step 2 |
| REQ-LB-15 | API Gateway shall authenticate every caller and shall enforce a per-partner call rate limit, defaulting to 1,000 calls per minute. | CON.2, step 5, API Gateway |

### 3.2 Qualifying points and tier progression — steps 3–4

| ID | Requirement | Traces to |
|---|---|---|
| REQ-LB-16 | Earning Engine Service shall publish a qualifying-point accrual to Message Broker after a `PointTransaction` is confirmed. | step 3, CON.2 |
| REQ-LB-17 | Tiering System Service shall track qualifying points separately from the Member's spendable `PointBalance`; qualifying points shall not be reduced by redemption. | step 4, `MemberTier` |
| REQ-LB-18 | Tiering System Service shall support the tiers Silver, Gold, and Platinum, with default qualifying thresholds of 0, 1,000, and 3,000 points. | Goal, step 4, `MemberTier` |
| REQ-LB-19 | When a Member crosses a tier threshold, `MemberTier` shall be upgraded immediately rather than at the end of the period, and the Member may cross more than one tier at once. | Goal, step 4, `MemberTier` |
| REQ-LB-20 | Each tier shall carry an earn multiplier applied by Earning Engine Service, defaulting to 1× for Silver, 1.5× for Gold, and 2× for Platinum. | Goal, step 2, step 4 |
| REQ-LB-21 | At the end of a tier period, a Member below the threshold for their current tier shall enter a grace window of 30 days rather than being downgraded straight away. | step 4, `MemberTier` |
| REQ-LB-22 | A Member who earns the shortfall during the grace window shall keep their current tier and the downgrade shall be cancelled. | step 4, EX-09 |
| REQ-LB-23 | A Member who does not recover during the grace window shall be downgraded by one tier only, never more, and shall be notified through CRM & Notification Gateway. | step 4, EX-09, Member |
| REQ-LB-24 | Tiering System Service shall ignore a qualifying accrual it has already applied, so that a repeated event does not move `MemberTier` twice. | CON.1, EX-12 |
| REQ-LB-25 | Tiering System Service shall publish tier changes to Message Broker so that Earning Engine Service and Redemption Engine Service work from the current `MemberTier`. | CON.2, step 4 |

### 3.3 Redemption and fulfilment — steps 5–7

| ID | Requirement | Traces to |
|---|---|---|
| REQ-LB-26 | A Member shall submit a redemption request through API Gateway, creating a `RedemptionOrder` in state `PENDING`. | step 5, `RedemptionOrder`: `PENDING` |
| REQ-LB-27 | Redemption Engine Service shall value points at 100 points to $1 and shall reject any redemption below a minimum of 100 points. | Goal, step 6, EX-06 |
| REQ-LB-28 | Redemption Engine Service shall check the Member's available `PointBalance`, their `MemberTier` against the reward's minimum tier, and the minimum redemption amount before any points are debited. | step 6, `RedemptionOrder`: `PENDING` |
| REQ-LB-29 | Where any of those checks fail, the `RedemptionOrder` shall move `PENDING` → `CANCELLED` and no points shall be debited. | step 6, EX-04, EX-05, EX-06, `RedemptionOrder`: `CANCELLED` |
| REQ-LB-30 | Where all checks pass, the `RedemptionOrder` shall move `PENDING` → `IN_PROGRESS` and points shall be debited oldest-earned first. | Outcome, step 6, `RedemptionOrder`: `IN_PROGRESS` |
| REQ-LB-31 | A single redemption may draw on more than one earned batch; expired points shall never be selected. | Outcome, step 6, `PointTransaction` |
| REQ-LB-32 | Redemption Engine Service shall hold a lock on the Member's balance in Idempotency Store while debiting, so that two redemptions running at once cannot both spend the same points. | step 6, `PointBalance` |
| REQ-LB-33 | Redemption Engine Service shall obtain the debit from Earning Engine Service rather than writing to Earning DB itself. | CON.2, step 6 |
| REQ-LB-34 | Fulfilment shall be dispatched to Partner Systems once the `RedemptionOrder` is `IN_PROGRESS`. | step 7, `RedemptionOrder`: `IN_PROGRESS` |
| REQ-LB-35 | On confirmed delivery, the `RedemptionOrder` shall move `IN_PROGRESS` → `FULFILLED` and the debit shall become final. | step 7, `RedemptionOrder`: `FULFILLED` |
| REQ-LB-36 | On failed delivery, the `RedemptionOrder` shall move `IN_PROGRESS` → `FAILED`. | CON.3, step 7, `RedemptionOrder`: `FAILED` |
| REQ-LB-37 | A `FAILED` order shall be reversed automatically, moving `FAILED` → `REVERSED`, and the restored points shall keep their original earn date and expiry rather than being treated as newly earned. | CON.3, EX-07, `RedemptionOrder`: `REVERSED` |
| REQ-LB-38 | The Member shall be notified through CRM & Notification Gateway when an order is fulfilled and when an order is reversed. | step 7, Member |
| REQ-LB-39 | A Member may cancel a `RedemptionOrder` only while it is `PENDING`; once `IN_PROGRESS` it may not be cancelled. | step 6, EX-08, `RedemptionOrder`: `CANCELLED` |
| REQ-LB-40 | A Support Agent shall be able to reverse an order that has not been fulfilled, recording a reason. | Support Agent, `RedemptionOrder`: `REVERSED` |

### 3.4 Programme and campaign configuration

| ID | Requirement | Traces to |
|---|---|---|
| REQ-LB-41 | A Program Admin shall configure `LoyaltyProgram` and `Campaign` records through Program Management Service, and no other service shall write Program Mgmt DB. | CON.2, Program Admin |
| REQ-LB-42 | A `LoyaltyProgram` shall move through draft, active, suspended, and deactivated states, and shall not be deleted once Members hold balances against it. | Goal, `LoyaltyProgram` |
| REQ-LB-43 | Earn rates, tier thresholds, grace length, and minimum redemption shall be held as `LoyaltyProgram` configuration rather than fixed in each service. | Goal, `LoyaltyProgram` |
| REQ-LB-44 | Changing configuration shall create a new version effective from the change date; existing `PointTransaction` and `MemberTier` records shall not be recalculated. | Goal, `LoyaltyProgram`, `Campaign` |
| REQ-LB-45 | Every configuration change shall record who changed it, when, and the previous value, and that record shall not be editable. | Program Admin, `LoyaltyProgram` |
| REQ-LB-46 | A `Campaign` shall be valid only inside its parent `LoyaltyProgram`'s active window, and shall carry a priority used to settle overlaps. | step 2, `Campaign` |
| REQ-LB-47 | Program Management Service shall publish configuration changes to Message Broker so that Earning Engine Service and Tiering System Service pick up the new version. | CON.2, step 2 |
| REQ-LB-48 | A Member may be enrolled in more than one `LoyaltyProgram` at the same time, subject to each programme's eligibility. | Goal, Member, `LoyaltyProgram` |
| REQ-LB-49 | Manual balance adjustments shall require a reason, and adjustments above a configured size shall require approval by a second person who is not the person who raised them. | Support Agent, Program Admin, `PointBalance` |
| REQ-LB-50 | Partner Systems shall be registered through Program Management Service before they may submit earn events, and their call rate limit shall be part of that registration. | CON.2, Partner Systems, API Gateway |

### 3.5 Analytics and reporting — step 8

| ID | Requirement | Traces to |
|---|---|---|
| REQ-LB-51 | Analytics & Reporting Service shall build `FactPointTransaction` in Data Warehouse from change events carried on Message Broker, and shall not query Earning DB, Tiering DB, Redemption DB, or Program Mgmt DB directly. | CON.2, CON.4, step 8 |
| REQ-LB-52 | Warehouse data shall be no more than 10 minutes behind the transactional record. | Outcome, CON.4, step 8 |
| REQ-LB-53 | Where warehouse data is more than 10 minutes behind, reports and dashboards shall show the staleness rather than presenting the figure as current. | CON.4, EX-11 |
| REQ-LB-54 | Analytics & Reporting Service shall report point liability as the total of confirmed unspent points multiplied by the cost per point, defaulting to $0.01 per point. | Goal, step 8, `FactPointTransaction` |
| REQ-LB-55 | Liability shall be broken down by the age of the points and shall show what is due to expire in the next 30, 60, and 90 days. | Goal, step 8 |
| REQ-LB-56 | Analytics & Reporting Service shall report redemption rate as points redeemed over points issued, and breakage rate as points expired over points issued. | Goal, step 8 |
| REQ-LB-57 | Analytics & Reporting Service shall report tier distribution and movement between tiers over a period. | Goal, step 8, `MemberTier` |
| REQ-LB-58 | Analytics & Reporting Service shall report Member engagement, treating a Member with no activity for more than 90 days as dormant. | Goal, step 8, Member |
| REQ-LB-59 | Analytics & Reporting Service shall report campaign effect by comparing earning during a `Campaign` against the 30 days before it started. | Goal, step 8, `Campaign` |
| REQ-LB-60 | Finance shall see reports covering only the programmes they are entitled to see, and access shall be checked on every report. | Finance, CON.2 |
| REQ-LB-61 | Finance shall be alerted when liability passes a configured ceiling. | Finance, step 8 |
| REQ-LB-62 | Reporting shall not slow down the services that serve Members. | CON.4, step 8 |
| REQ-LB-63 | Analytics & Reporting Service shall make period figures available to Enterprise Data Warehouse for group-wide reporting. | Enterprise Data Warehouse, step 8 |

---

## 4. Trace table

Requirement ID → I-5 process step → `CON.*` → named object / state.

| Requirement | I-5 step | CON.* | Named object / state |
|---|---|---|---|
| REQ-LB-01 | 1 → 2 | — | `PointTransaction` created |
| REQ-LB-02 | 1 | — | `PointTransaction` held until settled |
| REQ-LB-03 | 2 | CON.1 | `PointTransaction` not duplicated |
| REQ-LB-04 | 2 | CON.1 | `PointTransaction` original returned |
| REQ-LB-05 | 2 | — | `PointTransaction` amount |
| REQ-LB-06 | 2 | — | `Campaign` applied to `PointTransaction` |
| REQ-LB-07 | 2 | — | `Campaign` priority |
| REQ-LB-08 | 2 | CON.1 | `PointTransaction` append-only |
| REQ-LB-09 | 2 | CON.2 | `PointBalance` owned by Earning DB |
| REQ-LB-10 | 2 | — | `PointTransaction` earn date and expiry |
| REQ-LB-11 | 2 | — | `PointTransaction` expired |
| REQ-LB-12 | 2 | — | `PointTransaction` expiry notice |
| REQ-LB-13 | 2 | CON.1 | `PointTransaction` cancelled |
| REQ-LB-14 | 2 | CON.1, CON.2 | `PointTransaction` from Partner Systems |
| REQ-LB-15 | 5 | CON.2 | API Gateway edge control |
| REQ-LB-16 | 3 | CON.2 | `PointTransaction` → qualifying accrual |
| REQ-LB-17 | 4 | — | `MemberTier` qualifying points |
| REQ-LB-18 | 4 | — | `MemberTier` thresholds |
| REQ-LB-19 | 4 | — | `MemberTier` upgraded |
| REQ-LB-20 | 2, 4 | — | `MemberTier` multiplier on `PointTransaction` |
| REQ-LB-21 | 4 | — | `MemberTier` in grace |
| REQ-LB-22 | 4 | — | `MemberTier` retained |
| REQ-LB-23 | 4 | — | `MemberTier` downgraded |
| REQ-LB-24 | 4 | CON.1 | `MemberTier` unchanged on replay |
| REQ-LB-25 | 4 | CON.2 | `MemberTier` published |
| REQ-LB-26 | 5 | — | `RedemptionOrder`: `PENDING` |
| REQ-LB-27 | 6 | — | `RedemptionOrder`: `PENDING` → `CANCELLED` |
| REQ-LB-28 | 6 | — | `RedemptionOrder`: `PENDING` |
| REQ-LB-29 | 6 | — | `RedemptionOrder`: `PENDING` → `CANCELLED` |
| REQ-LB-30 | 6 | — | `RedemptionOrder`: `PENDING` → `IN_PROGRESS` |
| REQ-LB-31 | 6 | — | `PointTransaction` consumed oldest first |
| REQ-LB-32 | 6 | — | `PointBalance` locked |
| REQ-LB-33 | 6 | CON.2 | `PointTransaction` debited by owner |
| REQ-LB-34 | 7 | — | `RedemptionOrder`: `IN_PROGRESS` |
| REQ-LB-35 | 7 | — | `RedemptionOrder`: `IN_PROGRESS` → `FULFILLED` |
| REQ-LB-36 | 7 | CON.3 | `RedemptionOrder`: `IN_PROGRESS` → `FAILED` |
| REQ-LB-37 | 7 | CON.3 | `RedemptionOrder`: `FAILED` → `REVERSED` |
| REQ-LB-38 | 7 | — | `RedemptionOrder` outcome notice |
| REQ-LB-39 | 6 | — | `RedemptionOrder`: `PENDING` → `CANCELLED` |
| REQ-LB-40 | 7 | CON.3 | `RedemptionOrder`: `REVERSED` |
| REQ-LB-41 | 2, 6 | CON.2 | `LoyaltyProgram`, `Campaign` owned by Program Mgmt DB |
| REQ-LB-42 | 2 | — | `LoyaltyProgram` lifecycle |
| REQ-LB-43 | 2, 4, 6 | — | `LoyaltyProgram` configuration |
| REQ-LB-44 | 2 | — | `LoyaltyProgram`, `Campaign` versioned |
| REQ-LB-45 | 2 | — | `LoyaltyProgram` change record |
| REQ-LB-46 | 2 | — | `Campaign` window and priority |
| REQ-LB-47 | 2 | CON.2 | `Campaign` published |
| REQ-LB-48 | 2 | — | `LoyaltyProgram` enrolment |
| REQ-LB-49 | 2 | CON.2 | `PointBalance` adjusted |
| REQ-LB-50 | 2 | CON.2 | `LoyaltyProgram` partner registration |
| REQ-LB-51 | 8 | CON.2, CON.4 | `FactPointTransaction` in Data Warehouse |
| REQ-LB-52 | 8 | CON.4 | `FactPointTransaction` freshness |
| REQ-LB-53 | 8 | CON.4 | `FactPointTransaction` staleness shown |
| REQ-LB-54 | 8 | — | `FactPointTransaction` liability |
| REQ-LB-55 | 8 | — | `FactPointTransaction` ageing |
| REQ-LB-56 | 8 | — | `FactPointTransaction` rates |
| REQ-LB-57 | 8 | — | `MemberTier` distribution |
| REQ-LB-58 | 8 | — | `FactPointTransaction` engagement |
| REQ-LB-59 | 8 | — | `Campaign` effect |
| REQ-LB-60 | 8 | CON.2 | `FactPointTransaction` access scope |
| REQ-LB-61 | 8 | — | `FactPointTransaction` liability ceiling |
| REQ-LB-62 | 8 | CON.4 | `FactPointTransaction` read separately |
| REQ-LB-63 | 8 | — | `FactPointTransaction` to Enterprise Data Warehouse |

---

## 5. Coverage check

Every Lab 1 goal, outcome, and constraint appears in the requirements list.

| Lab 1 item | Covered by |
|---|---|
| Goal — earning points | REQ-LB-01, REQ-LB-05, REQ-LB-06 |
| Goal — tier progression | REQ-LB-18, REQ-LB-19, REQ-LB-21, REQ-LB-23 |
| Goal — redemption | REQ-LB-26, REQ-LB-27, REQ-LB-30 |
| Goal — program configuration | REQ-LB-41, REQ-LB-42, REQ-LB-43, REQ-LB-44 |
| Goal — financial reporting | REQ-LB-54, REQ-LB-55, REQ-LB-56 |
| Outcome — settled earn within 60 seconds | REQ-LB-01 |
| Outcome — prevent duplicate point postings | REQ-LB-03, REQ-LB-04, REQ-LB-08 |
| Outcome — support FIFO redemption | REQ-LB-30, REQ-LB-31 |
| Outcome — analytics no more than 10 minutes stale | REQ-LB-52, REQ-LB-53 |
| CON.1 | REQ-LB-03, REQ-LB-04, REQ-LB-08, REQ-LB-13, REQ-LB-14, REQ-LB-24 |
| CON.2 | REQ-LB-09, REQ-LB-14, REQ-LB-15, REQ-LB-16, REQ-LB-25, REQ-LB-33, REQ-LB-41, REQ-LB-47, REQ-LB-49, REQ-LB-50, REQ-LB-51, REQ-LB-60 |
| CON.3 | REQ-LB-36, REQ-LB-37, REQ-LB-40 |
| CON.4 | REQ-LB-51, REQ-LB-52, REQ-LB-53, REQ-LB-62 |

Every I-6 state of `RedemptionOrder` appears: `PENDING` (REQ-LB-26), `IN_PROGRESS` (REQ-LB-30), `FULFILLED` (REQ-LB-35), `FAILED` (REQ-LB-36), `CANCELLED` (REQ-LB-29, REQ-LB-39), `REVERSED` (REQ-LB-37).

Every I-5 step appears: step 1 (REQ-LB-01), step 2 (REQ-LB-03), step 3 (REQ-LB-16), step 4 (REQ-LB-19), step 5 (REQ-LB-26), step 6 (REQ-LB-30), step 7 (REQ-LB-34), step 8 (REQ-LB-51).

Every I-11 use case is covered: UC-LB-01 (REQ-LB-01, REQ-LB-03), UC-LB-02 (REQ-LB-28, REQ-LB-30, REQ-LB-37), UC-LB-03 (REQ-LB-19, REQ-LB-24), UC-LB-04 (REQ-LB-52, REQ-LB-54).
