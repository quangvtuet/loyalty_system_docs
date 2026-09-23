# FR-02: Tiering System — Functional Requirements

**Module**: Tiering System
**Version**: 1.1
**Date**: 2026-08-14
**Source**: [loyalty_domain.md](file:///d:/learn/loyalty/loyalty_domain.md)
**Depends On**: [FR-04 Program Management](file:///d:/learn/loyalty/requirements/FR-04-program-management.md), [FR-01 Earning Engine](file:///d:/learn/loyalty/requirements/FR-01-earning-engine.md)

---

## 1. Overview

The Tiering System **segments members into loyalty tiers** — **Silver, Gold, Platinum** — based on cumulative Qualifying Points (QP) earned over a defined tier period. Default tier thresholds: **Silver: 0 QP** (base), **Gold: 1,000 QP**, **Platinum: 3,000 QP**. Tiers control benefit eligibility, earn rate multipliers, and catalog access restrictions in other modules. Tier status changes trigger benefit activation/deactivation and member notifications.

---

## 2. Actors / Roles

| Actor | Description |
|-------|-------------|
| **System** | Automated tier evaluation job, QP accrual, tier assignment, benefit activation |
| **Program Admin** | Configures tier definitions, thresholds, and evaluation rules via FR-04 |
| **Member** | Subject of tier evaluation; receives tier change notifications |
| **Auditor** | Reviews tier evaluation logs |

---

## 3. Use Cases

| UC ID | Use Case | Primary Actor |
|-------|----------|---------------|
| UC-02-01 | Accrue QP from qualifying transaction | System |
| UC-02-02 | Real-time tier upgrade on threshold breach | System |
| UC-02-03 | Periodic tier evaluation at end of tier period | System |
| UC-02-04 | Apply grace period before downgrade | System |
| UC-02-05 | Activate / deactivate tier benefits | System |
| UC-02-06 | Notify member of tier change | System |
| UC-02-07 | View tier history | Member / Auditor |

---

## 4. Functional Requirements

### 4.1 Qualifying Points (QP) Accrual

| ID | Requirement | Priority | Source Rule |
|----|-------------|----------|-------------|
| FR-02-001 | The system **SHALL** maintain a separate QP balance per member per program, independent of the redeemable point balance. | Must | BR: QP separate from redeemable points |
| FR-02-002 | The system **SHALL** accrue QP from qualifying transaction events as defined by the program's `TierRule`. The QP value may differ from the earn point value. | Must | Domain §2 |
| FR-02-003 | QP **SHALL NOT** expire during the active tier period; they reset to zero at the start of each new tier period. | Must | BR: QP do not expire mid-period |
| FR-02-004 | The system **SHALL** record every QP accrual event in a QP ledger with: member ID, program ID, source event ID, QP amount, and accrual timestamp. | Must | Domain §2 |
| FR-02-005 | The system **SHALL** calculate the cumulative QP balance in real time as new qualifying events arrive. | Must | Domain §2 |

---

### 4.2 Tier Upgrade (Real-Time)

| ID | Requirement | Priority | Source Rule |
|----|-------------|----------|-------------|
| FR-02-010 | After every QP accrual, the system **SHALL** compare the member's cumulative QP balance against tier thresholds in descending order to determine if an upgrade is warranted. | Must | BR: Tier upgrades take effect immediately |
| FR-02-011 | If the member's QP balance meets or exceeds a higher tier's threshold, the system **SHALL** immediately update the member's current tier to the new tier. | Must | BR: Tier upgrades take effect immediately |
| FR-02-012 | A tier upgrade event **SHALL** record: member ID, previous tier, new tier, QP balance at upgrade, and effective timestamp. | Must | Domain §2 |
| FR-02-013 | A member may upgrade multiple tiers in a single evaluation if their QP balance exceeds multiple thresholds simultaneously. | Must | Domain §2 |

---

### 4.3 Periodic Tier Evaluation & Downgrade

| ID | Requirement | Priority | Source Rule |
|----|-------------|----------|-------------|
| FR-02-020 | The system **SHALL** run a scheduled tier evaluation job at the end of each tier period (default: **calendar year-end, 31 December**; configurable per program to rolling 12-month anniversary). | Must | BR: Tier downgrades after evaluation cycle |
| FR-02-021 | During the evaluation job, the system **SHALL** compare each member's cumulative QP for the period against the threshold required to maintain their current tier. | Must | Domain §2 |
| FR-02-022 | If a member's QP falls below the maintenance threshold of their current tier, the system **SHALL** initiate a downgrade at the end of the grace period. | Must | BR: Tier downgrades after evaluation cycle |
| FR-02-023 | A member **SHALL** downgrade by exactly one tier level per evaluation cycle (e.g., Platinum → Gold, not Platinum → Silver in one step), unless the program config explicitly allows multi-level downgrade. | Must | BR: Cannot skip tiers downward |
| FR-02-024 | A downgrade event **SHALL** record: member ID, previous tier, new tier, QP balance at evaluation, evaluation date, effective downgrade date, and grace period applied. | Must | Domain §2 |

---

### 4.4 Grace Period

| ID | Requirement | Priority | Source Rule |
|----|-------------|----------|-------------|
| FR-02-030 | The system **SHALL** apply a configurable grace period (in days) after the tier evaluation date before a downgrade takes effect. The grace period is defined in the program's `TierRule`. **Default grace period: 30 days.** | Must | BR: Grace period |
| FR-02-031 | During the grace period, the member **SHALL** retain all benefits of their current (higher) tier. | Must | BR: Grace period |
| FR-02-032 | The system **SHALL** notify the member at the start of the grace period, stating: current tier, new tier after grace period, grace period end date, and QP shortfall. | Must | Domain §2 |
| FR-02-033 | If a member earns sufficient QP to meet the threshold before the grace period expires, the downgrade **SHALL** be cancelled and the member retains their current tier. | Must | Domain §2 |

---

### 4.5 Benefit Activation & Deactivation

| ID | Requirement | Priority | Source Rule |
|----|-------------|----------|-------------|
| FR-02-040 | Upon tier upgrade, the system **SHALL** immediately activate all benefits associated with the new tier. | Must | Domain §2 |
| FR-02-041 | Upon confirmed tier downgrade (grace period expired), the system **SHALL** deactivate benefits exclusive to the previous tier and activate benefits of the new tier. | Must | Domain §2 |
| FR-02-042 | Tier benefits **SHALL** include, but are not limited to: earn rate multiplier override (Silver: **1×**, Gold: **1.5×**, Platinum: **2×**), access to tier-restricted catalog items, and service-level flag (e.g., priority support). | Must | Domain §2 |
| FR-02-043 | The system **SHALL** notify relevant downstream systems (Earning Engine, Redemption Engine) of tier changes in real time via an internal event. | Must | Domain §2 |

---

### 4.6 Member Notification

| ID | Requirement | Priority | Source Rule |
|----|-------------|----------|-------------|
| FR-02-050 | The system **SHALL** send a notification to the member upon **tier upgrade** containing: new tier name, effective date, new benefits summary, and current QP balance. | Must | Domain §2 |
| FR-02-051 | The system **SHALL** send a notification to the member upon **grace period initiation** containing: reason for pending downgrade, QP shortfall, grace period end date. | Must | Domain §2 |
| FR-02-052 | The system **SHALL** send a notification to the member upon **tier downgrade confirmation** containing: new tier name, effective date, and lost benefits. | Must | Domain §2 |
| FR-02-053 | Notification channels (email, SMS, push) **SHALL** be configurable per member preference and program setting. | Should | Domain §2 |

---

### 4.7 Audit Logging

| ID | Requirement | Priority | Source Rule |
|----|-------------|----------|-------------|
| FR-02-060 | The system **SHALL** write a `TierEvaluationLog` record for every tier evaluation run, containing: run ID, program ID, evaluation date, members evaluated count, upgrades count, downgrades count, and no-change count. | Must | Domain §2 Key Entities |
| FR-02-061 | The system **SHALL** retain individual member tier event logs for a minimum of 5 years. | Must | Domain §2 |

---

## 5. Non-Functional Requirements

| ID | Requirement | Priority |
|----|-------------|----------|
| NFR-02-001 | Real-time tier upgrade evaluation **SHALL** complete within 500ms of the triggering QP accrual event. | Must |
| NFR-02-002 | The periodic tier evaluation batch job **SHALL** complete processing of all active members within a 4-hour window. | Must |
| NFR-02-003 | Tier status reads for Earning Engine and Redemption Engine **SHALL** respond within 50ms (p99) to avoid blocking transaction processing. | Must |

---

## 6. Constraints & Assumptions

- Tier thresholds must be strictly ascending (Silver < Gold < Platinum); the system validates this at rule config time. **Default thresholds: Silver: 0 QP, Gold: 1,000 QP, Platinum: 3,000 QP.**
- QP accrual rate and earn point rate may differ; both are defined in `TierRule` and `EarnRule` respectively.
- The first tier (base tier, Silver) has no threshold requirement — all enrolled members start at Silver by default.
- QP reset does not affect the member's redeemable point balance.
- Default grace period is **30 days**; configurable per program in `TierRule`.

---

## 7. Acceptance Criteria

| UC | Scenario | Expected Result |
|----|----------|----------------|
| UC-02-01 | Member earns 500 QP in one transaction, Gold threshold is 1000 QP | QP balance = 500; tier unchanged (Silver) |
| UC-02-02 | Member earns 600 more QP, crossing the 1000 QP Gold threshold | Immediate upgrade to Gold; benefits activated; member notified |
| UC-02-03 | End of tier period: member at Platinum with only 1500 QP (Gold=1000, Platinum=3000) | Grace period starts; member notified of pending downgrade to Gold |
| UC-02-04 | Grace period (30 days) expires; member has not earned extra QP | Member downgraded to Gold; Platinum benefits deactivated |
| UC-02-04 | Member earns 1600 more QP during grace period (reaching 3100 total) | Grace period downgrade cancelled; member remains Platinum |
| UC-02-03 | Member is at Platinum (3000 QP), QP resets to 0 at new period start | Member remains Platinum; QP restarts; redeemable balance unaffected |
