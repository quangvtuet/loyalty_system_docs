# Quality Gates — Design
**Domain**: Loyalty Banking
**Version**: 1.0
**Date**: 2026-08-13
**Source**: [loyalty_domain.md](file:///d:/learn/loyalty/loyalty_domain.md) | FR-01..05 | AS-01..05

---

## Overview

This document defines **design-level quality gates** for the Loyalty Banking platform. These gates verify that the system's **functional behavior, business rule enforcement, end-to-end flows, acceptance criteria, and analytics accuracy** meet requirements before each delivery milestone.

### Gate Lifecycle

```
DESIGN-GATE-01          DESIGN-GATE-02           DESIGN-GATE-03          DESIGN-GATE-04
Business Rule     ───►  End-to-End Flow    ───►  Acceptance Criteria ──►  Metric & Report
Compliance              Validation               Sign-Off                  Accuracy
(Feature Complete)      (Integration Test)       (UAT)                     (Analytics Validation)
```

### Gate Status Legend

| Symbol | Meaning |
|--------|---------|
| ✅ PASS | Criterion met; evidence attached |
| ❌ FAIL | Criterion not met; defect raised |
| ⚠️ WAIVER | Accepted risk; signed off by Product Owner |
| 🔲 PENDING | Not yet evaluated |

---

## DESIGN-GATE-01: Business Rule Compliance

**Stage**: Feature Complete (per module)
**Evaluators**: QA Lead, Business Analyst
**Trigger**: Module development complete; unit and component tests passing
**Outcome**: Every business rule from FR-01..05 has a corresponding verified test assertion

> [!IMPORTANT]
> Each module gate (G01-01 through G01-05) may be evaluated independently as that module completes.

---

### G01-01: Earning Engine Business Rules

| ID | Business Rule | FR Source | Test Assertion | Status |
|----|--------------|-----------|---------------|--------|
| G01-01-001 | Points are awarded **only for settled** transactions; authorization-only events must not earn | FR-01-005 | Submit auth-only event → verify zero points credited | 🔲 |
| G01-01-002 | Points are calculated as `FLOOR(amount × earn_rate)` — fractional points are always rounded down | FR-01-011 | $10.99 × 1pt/$1 → verify 10 pts (not 11, not 10.99) | 🔲 |
| G01-01-003 | Bonus rule is evaluated **after** base earn calculation | FR-01-020 | Trace execution order; assert base calculated first | 🔲 |
| G01-01-004 | When multiple bonus campaigns apply (no stacking), the **highest multiplier** wins | FR-01-022 | Two campaigns: 2× and 3×; assert 3× applied, 2× skipped | 🔲 |
| G01-01-005 | When stacking is enabled, **all** applicable bonus points are summed | FR-01-022 | Two campaigns: +50 and +100 flat bonus; assert +150 total | 🔲 |
| G01-01-006 | The **same source transaction** cannot credit points more than once (idempotency) | FR-01-041 | Submit identical event twice; assert only one credit exists in ledger | 🔲 |
| G01-01-007 | `PENDING` points transition to `CONFIRMED` upon settlement; transition to `CANCELLED` upon reversal | FR-01-031, FR-01-032 | Submit auth → verify PENDING; send settlement → verify CONFIRMED | 🔲 |
| G01-01-008 | FIFO expiry: oldest earn batch consumed first during redemption | FR-01-053 | Create 2 batches (Jan, Mar); redeem → assert Jan batch consumed first | 🔲 |
| G01-01-009 | Point expiry auto-debit fires on scheduled expiry date | FR-01-051 | Set expiry to tomorrow; run expiry job; verify debit in ledger | 🔲 |
| G01-01-010 | No applicable earn rule → zero points credited; event still logged | FR-01-013 | Submit event with no matching rule; assert 0 pts, event log entry exists | 🔲 |

---

### G01-02: Tiering System Business Rules

| ID | Business Rule | FR Source | Test Assertion | Status |
|----|--------------|-----------|---------------|--------|
| G01-02-001 | QP balance is **separate** from redeemable point balance; earning 100 QP does not add 100 redeemable points | FR-02-001 | Check both balances after QP accrual; assert independence | 🔲 |
| G01-02-002 | Tier upgrade takes effect **immediately** when QP threshold is crossed | FR-02-011 | Member at Silver (999 QP); earns 1 more QP → assert instant Gold upgrade | 🔲 |
| G01-02-003 | A member may upgrade through **multiple tiers** in one QP accrual if balance exceeds multiple thresholds | FR-02-013 | Member at Silver (0 QP) earns 5,000 QP (Gold=1,000, Platinum=3,000) → assert Platinum upgrade | 🔲 |
| G01-02-004 | Tier downgrade takes effect only **after** the evaluation cycle + grace period | FR-02-022, FR-02-030 | End-of-period: member below threshold → assert still on current tier during grace period | 🔲 |
| G01-02-005 | Member downgrades **one tier at a time** per cycle (default config) | FR-02-023 | Platinum member qualifies for Silver at evaluation → assert downgrade to Gold, not Silver | 🔲 |
| G01-02-006 | QP resets to **zero** at the start of each new tier period; redeemable balance unaffected | FR-02-003 | After period reset: assert QP=0; assert redeemable balance unchanged | 🔲 |
| G01-02-007 | Grace period rescue: member earns required QP before grace expires → downgrade cancelled | FR-02-033 | Initiate grace period; member earns QP to meet threshold → assert downgrade_cancelled event | 🔲 |
| G01-02-008 | Tier change event is emitted to Earning Engine and Redemption Engine on every upgrade/downgrade | FR-02-043 | Trigger upgrade; assert Tier Change Event consumed by both downstream modules | 🔲 |

---

### G01-03: Redemption Engine Business Rules

| ID | Business Rule | FR Source | Test Assertion | Status |
|----|--------------|-----------|---------------|--------|
| G01-03-001 | Member **cannot** redeem more points than their confirmed available balance | FR-03-012 | Member with 200 pts requests 300-pt redemption → assert rejection with balance-error code | 🔲 |
| G01-03-002 | Expired points are **excluded** from redeemable balance; cannot be redeemed | FR-03-022 | Expire 100 of 200 pts; redeem 150 → assert rejection (only 100 available) | 🔲 |
| G01-03-003 | Points are debited **immediately** on approval; not on fulfillment | FR-03-020 | Submit redemption request; check ledger before fulfillment → assert PENDING_DEBIT exists | 🔲 |
| G01-03-004 | FIFO: oldest confirmed points consumed first across multiple earn batches | FR-03-021 | Two batches (Jan 200 pts, Mar 300 pts); redeem 250 → assert 200 from Jan, 50 from Mar | 🔲 |
| G01-03-005 | Minimum redemption threshold enforced; requests below minimum rejected | FR-03-013 | Program min = 100 pts; request 50-pt redemption → assert rejection | 🔲 |
| G01-03-006 | Tier-restricted catalog items: Silver member cannot redeem Platinum-only items | FR-03-002, FR-03-011 | Silver member requests Platinum item → assert rejection with tier-eligibility-error | 🔲 |
| G01-03-007 | Fulfillment failure → **automatic** reversal; points re-credited with original earn date | FR-03-040, FR-03-041 | Set fulfillment to FAILED; assert reversal transaction; assert earn_date restored in ledger | 🔲 |
| G01-03-008 | Cancellation only permitted in `PENDING` fulfillment status; rejected if `IN_PROGRESS` or `FULFILLED` | FR-03-043 | Cancel FULFILLED order → assert rejection; cancel PENDING order → assert success | 🔲 |
| G01-03-009 | Concurrent redemptions cannot over-draw balance (race condition protection) | FR-03-024 | Simultaneously submit 3 redemptions totaling > balance → assert only one succeeds | 🔲 |

---

### G01-04: Program Management Business Rules

| ID | Business Rule | FR Source | Test Assertion | Status |
|----|--------------|-----------|---------------|--------|
| G01-04-001 | A program **cannot** be activated without at least one active `EarnRule` | FR-04-002, FR-04-020 | Attempt to activate program with no EarnRule → assert validation error | 🔲 |
| G01-04-002 | Rule configuration changes do **not** retroactively affect existing point balances | FR-04-005 | Change earn rate; verify past transactions retain original point values in ledger | 🔲 |
| G01-04-003 | Every configuration change is **versioned**: previous value preserved in audit log | FR-04-004 | Change earn rate; query config_version_log → assert previous rate recorded with change timestamp | 🔲 |
| G01-04-004 | Campaign **priority** resolves conflicts: highest priority (lowest number) campaign wins | FR-04-012 | Two campaigns for same event (priority 1 and priority 5) → assert priority-1 campaign applied | 🔲 |
| G01-04-005 | Member can enroll in **multiple** programs simultaneously | FR-04-032 | Enroll member in Program A and Program B → assert both enrollments ACTIVE | 🔲 |
| G01-04-006 | Manual balance adjustment above threshold **blocked** without second-level approval | FR-04-051 | Submit high-value adjustment without approver → assert status = PENDING_APPROVAL | 🔲 |
| G01-04-007 | Manual adjustment logged with operator ID, approver ID, before/after balance, and reason | FR-04-052 | Submit and approve adjustment; query audit log → assert all required fields present | 🔲 |

---

### G01-05: Analytics & Reporting Business Rules

| ID | Business Rule | FR Source | Test Assertion | Status |
|----|--------------|-----------|---------------|--------|
| G01-05-001 | All 7 standard reports generate without errors for any valid date range and program filter | FR-05-001 | Generate each of 7 reports; assert HTTP 200, non-empty result | 🔲 |
| G01-05-002 | RBAC enforced: user can only access reports for authorized programs | FR-05-006 | User authorized for Program A requests Program B report → assert HTTP 403 | 🔲 |
| G01-05-003 | Scheduled report delivered at configured time and channel | FR-05-003, FR-05-004 | Configure weekly email report; verify delivery at scheduled time | 🔲 |
| G01-05-004 | Large reports (>1M rows) processed asynchronously; user receives completion notification | FR-05-007 | Request report with >1M row dataset; assert async status returned; assert notification on completion | 🔲 |
| G01-05-005 | Dashboard data staleness ≤ 10 minutes | FR-05-081 | Commit earn event; measure time to dashboard update → assert ≤ 10 min | 🔲 |

---

## DESIGN-GATE-02: End-to-End Flow Validation

**Stage**: Integration Testing
**Evaluators**: QA Lead, Tech Lead, Business Analyst
**Trigger**: All DESIGN-GATE-01 criteria PASS; system integrated across all 5 modules
**Outcome**: Cross-module business flows verified in integration environment

> [!IMPORTANT]
> All flows below must pass before UAT begins. Each flow must be traceable via distributed trace logs.

---

### Cross-Module Scenario Tests

| Flow ID | Scenario | Modules Crossed | Pass Conditions |
|---------|----------|----------------|----------------|
| **FLOW-01** | **Standard Earn + Bonus** | Core Banking → Earning Engine → Program Mgmt (Campaign) → Ledger | Base points + bonus points both credited; campaign ID recorded; ledger shows 2 entries (EARN + BONUS) |
| **FLOW-02** | **Earn Triggers Tier Upgrade** | Core Banking → Earning (QP) → Tiering → Benefit Activation → Earning | QP accrued; threshold crossed → Gold tier activated; Earning Engine applies 1.5× rate on next transaction |
| **FLOW-03** | **End-of-Period Tier Downgrade with Grace** | Tiering Batch → Notification → Member earns QP → Grace Rescue | Batch initiates downgrade; notification sent; member earns rescue QP; downgrade cancelled; member retains tier |
| **FLOW-04** | **Redemption with FIFO Point Consumption** | Member → Redemption Engine → Earning Ledger (FIFO) → Fulfillment | Oldest batch consumed first; PENDING_DEBIT created; fulfillment FULFILLED; CONFIRMED_DEBIT finalized |
| **FLOW-05** | **Fulfillment Failure → Auto Reversal** | Redemption Engine → Fulfillment Partner (FAILED) → Ledger (REVERSAL) | Points reversed with original earn_date and expiry_date restored; member balance correct; notification sent |
| **FLOW-06** | **Campaign Activation Mid-Day** | Program Mgmt → Earning Engine | Campaign activated at 14:00; transactions before 14:00 unaffected; transactions from 14:00 onward apply campaign bonus |
| **FLOW-07** | **Earn Rule Version Change** | Program Mgmt → Earning Engine | Earn rate changed; verify past ledger entries unchanged; next earn event uses new rate |
| **FLOW-08** | **Partner Earn Event** | Partner API → Earning Engine → Ledger | Partner submits earn event via API; OAuth authenticated; points credited; source_type='PARTNER' tagged in ledger |
| **FLOW-09** | **Tier-Restricted Redemption Attempt** | Member (Silver) → Redemption Engine → Tiering (tier check) | Silver member requests Platinum item; Redemption Engine calls Tier API; tier check fails; redemption rejected |
| **FLOW-10** | **Analytics Aggregation** | Earning Engine → ETL → DW → Report | Earn event committed; within 10 min appears in DW; Point Issuance Report reflects new data |
| **FLOW-11** | **Manual Balance Adjustment with Approval** | Program Admin → Program Mgmt → Approval Workflow → Ledger | High-value adjustment submitted; routed to approver; approved; ledger updated; audit log complete |
| **FLOW-12** | **Point Expiry and Breakage Capture** | Earning Engine (Expiry Job) → Ledger → Analytics (Breakage Rate) | Expiry job fires; expired debit in ledger; DW reflects expired volume; Breakage Rate report updated |

---

## DESIGN-GATE-03: Acceptance Criteria Sign-Off

**Stage**: UAT (User Acceptance Testing)
**Evaluators**: Product Owner, Business Analyst, QA Lead
**Trigger**: All DESIGN-GATE-02 flows PASS; system deployed to UAT environment
**Outcome**: Product Owner formally signs off each FR module's acceptance criteria

> [!IMPORTANT]
> Product Owner signature is required for each section. No partial sign-off accepted.

---

### Sign-Off Tracker

#### FR-01 — Earning Engine

| AC ID | Acceptance Scenario | Test Case ID | Result | PO Sign-Off |
|-------|---------------------|-------------|--------|------------|
| AC-01-001 | Settled purchase $50, earn rate 1pt/$1 → 50 CONFIRMED pts credited | TC-01-001 | 🔲 | — |
| AC-01-002 | Same event with 2× bonus campaign → 100 pts total; campaign ID recorded | TC-01-002 | 🔲 | — |
| AC-01-003 | Two bonus campaigns 2× and 3× (no stacking) → 150 pts (3× wins) | TC-01-003 | 🔲 | — |
| AC-01-004 | Same source transaction submitted twice → only one credit; original result returned | TC-01-004 | 🔲 | — |
| AC-01-005 | Auth event PENDING → settlement → CONFIRMED; reversal → CANCELLED | TC-01-005 | 🔲 | — |
| AC-01-006 | Points reach 12-month rolling expiry → auto-debit; member notified | TC-01-006 | 🔲 | — |

**FR-01 Product Owner Sign-Off**: __________________ Date: __________

---

#### FR-02 — Tiering System

| AC ID | Acceptance Scenario | Test Case ID | Result | PO Sign-Off |
|-------|---------------------|-------------|--------|------------|
| AC-02-001 | Member earns 500 QP; Gold threshold=1,000 → tier unchanged (Silver) | TC-02-001 | 🔲 | — |
| AC-02-002 | Member earns 600 more QP (total=1,100) → immediate Gold upgrade; benefits activated | TC-02-002 | 🔲 | — |
| AC-02-003 | End of period: Platinum member has 1,500 QP (Platinum needs 3,000) → grace period starts | TC-02-003 | 🔲 | — |
| AC-02-004 | Grace period (30 days) expires; QP not recovered → downgraded to Gold | TC-02-004 | 🔲 | — |
| AC-02-005 | Member earns 1,600 QP during grace period (total=3,100) → downgrade cancelled; stays Platinum | TC-02-005 | 🔲 | — |
| AC-02-006 | New tier period starts; QP resets to 0; redeemable balance unaffected | TC-02-006 | 🔲 | — |

**FR-02 Product Owner Sign-Off**: __________________ Date: __________

---

#### FR-03 — Redemption Engine

| AC ID | Acceptance Scenario | Test Case ID | Result | PO Sign-Off |
|-------|---------------------|-------------|--------|------------|
| AC-03-001 | Member with 500 pts requests 300-pt reward → approved; 300 pts FIFO debited | TC-03-001 | 🔲 | — |
| AC-03-002 | Member with 200 pts requests 300-pt reward → rejected: insufficient balance | TC-03-002 | 🔲 | — |
| AC-03-003 | Silver member requests Platinum-only item → rejected: tier eligibility | TC-03-003 | 🔲 | — |
| AC-03-004 | Two batches: 200 pts (Jan), 300 pts (Mar); redeem 250 → 200 from Jan, 50 from Mar | TC-03-004 | 🔲 | — |
| AC-03-005 | Fulfillment partner returns FAILED → points reversed; member notified; balance restored | TC-03-005 | 🔲 | — |
| AC-03-006 | Cancel FULFILLED order → rejected; cancel PENDING order → success | TC-03-006 | 🔲 | — |

**FR-03 Product Owner Sign-Off**: __________________ Date: __________

---

#### FR-04 — Program Management

| AC ID | Acceptance Scenario | Test Case ID | Result | PO Sign-Off |
|-------|---------------------|-------------|--------|------------|
| AC-04-001 | Create program with all required fields → saved in DRAFT; version history record created | TC-04-001 | 🔲 | — |
| AC-04-002 | Activate program without EarnRule → system rejects with validation error | TC-04-002 | 🔲 | — |
| AC-04-003 | Two campaigns with overlapping dates (priority 1 vs 5) → priority-1 campaign applied | TC-04-003 | 🔲 | — |
| AC-04-004 | Member enrolls in two programs simultaneously → both enrollments ACTIVE | TC-04-004 | 🔲 | — |
| AC-04-005 | Admin adjusts balance above threshold without approver → blocked; routed to approval queue | TC-04-005 | 🔲 | — |
| AC-04-006 | Admin changes earn rate → new rate applied from change date; past transactions unaffected | TC-04-006 | 🔲 | — |

**FR-04 Product Owner Sign-Off**: __________________ Date: __________

---

#### FR-05 — Analytics & Reporting

| AC ID | Acceptance Scenario | Test Case ID | Result | PO Sign-Off |
|-------|---------------------|-------------|--------|------------|
| AC-05-001 | Generate Point Issuance Report for Aug 2026 → data by program/channel/campaign; CSV export works | TC-05-001 | 🔲 | — |
| AC-05-002 | Finance runs Point Liability Report → liability = SUM(outstanding pts × cost-per-pt); age buckets shown | TC-05-002 | 🔲 | — |
| AC-05-003 | Run Expiry Report for Q3 → breakage rate = expired/issued; 90-day forecast included | TC-05-003 | 🔲 | — |
| AC-05-004 | Campaign ends → Campaign Performance Report shows lift vs. baseline; cost-per-point calculated | TC-05-004 | 🔲 | — |
| AC-05-005 | Dashboard loaded during peak → data ≤ 10 min old; liability alert fires when threshold exceeded | TC-05-005 | 🔲 | — |
| AC-05-006 | Admin schedules weekly Redemption Report to email → delivered every Monday 08:00 as CSV | TC-05-006 | 🔲 | — |

**FR-05 Product Owner Sign-Off**: __________________ Date: __________

---

## DESIGN-GATE-04: Metric & Report Accuracy

**Stage**: Analytics Validation
**Evaluators**: Data Analyst, QA Lead, Finance (for liability)
**Trigger**: All DESIGN-GATE-03 sign-offs received; DW populated with seeded test data
**Outcome**: All analytics metrics and KPIs verified accurate against known test dataset

> [!IMPORTANT]
> This gate uses a **controlled seed dataset** with pre-calculated expected values. All results must match within defined tolerances.

---

### Seed Dataset Specification

| Dataset | Size | Description |
|---------|------|-------------|
| Members | 500 | Mix of tiers: 300 Silver, 150 Gold, 50 Platinum |
| Earn Events | 10,000 | Known amounts, dates, channels; pre-calculated expected totals |
| Redemption Orders | 500 | 450 FULFILLED, 30 REVERSED, 20 PENDING |
| Expired Points | 200 batches | Known expiry dates and amounts; pre-calculated breakage |
| Campaigns | 3 | Known multipliers and date ranges; pre-calculated bonus contribution |

---

### Metric Accuracy Validation

| ID | Metric | Expected Value | Tolerance | Source | Status |
|----|--------|---------------|-----------|--------|--------|
| G04-001 | Total Points Issued | Pre-calculated from seed earn events | **0%** (exact match) | AS-05 §3.1 | 🔲 |
| G04-002 | Total Points Redeemed | Pre-calculated from seed FULFILLED redemptions | **0%** (exact match) | AS-05 §3.1 | 🔲 |
| G04-003 | Total Points Expired | Pre-calculated from seed expiry batches | **0%** (exact match) | AS-05 §3.1 | 🔲 |
| G04-004 | Redemption Rate | Redeemed / Issued × 100 (pre-calculated) | **± 0.01%** | AS-05 KPIs | 🔲 |
| G04-005 | Breakage Rate | Expired / Issued × 100 (pre-calculated) | **± 0.01%** | AS-05 KPIs | 🔲 |
| G04-006 | Active Member Rate | Active members / Enrolled × 100 (pre-calculated) | **± 0.01%** | AS-05 KPIs | 🔲 |
| G04-007 | Point Liability | Outstanding pts × cost-per-point (pre-calculated in currency) | **± $0.01** | AS-05 §3.1 | 🔲 |
| G04-008 | Tier Distribution | Count per tier (pre-calculated: 300/150/50) | **0%** (exact match) | AS-02 R-AS02-01 | 🔲 |
| G04-009 | Upgrade Rate | Count upgrades / enrolled × 100 (pre-calculated) | **± 0.01%** | AS-02 §3 | 🔲 |
| G04-010 | Bonus Contribution Rate | Bonus pts / Total pts × 100 (pre-calculated per campaign) | **± 0.01%** | AS-01 §3 | 🔲 |
| G04-011 | Fulfillment SLA Compliance | On-time FULFILLED / Total FULFILLED × 100 (pre-calculated) | **± 0.1%** | AS-03 §3 | 🔲 |
| G04-012 | Reversal Rate | Reversed / Total orders × 100 (30/500 = 6.0%) | **± 0.01%** | AS-03 §3 | 🔲 |
| G04-013 | Campaign Earn Lift | (Campaign pts / baseline) - 1 × 100 per campaign (pre-calculated) | **± 1.0%** | AS-04 R-AS04-02 | 🔲 |
| G04-014 | DW Pipeline Lag | Time from earn event commit → DW availability | **≤ 10 min** | AS-05 §2 Note | 🔲 |

---

### Report Structure Accuracy Validation

| ID | Report | Validation Check | Status |
|----|--------|-----------------|--------|
| G04-R-001 | Point Issuance Report | All 7 required columns present; totals match G04-001 | 🔲 |
| G04-R-002 | Redemption Report | Net pts redeemed = G04-002 - reversed pts; reversal count matches seed | 🔲 |
| G04-R-003 | Point Liability Report | Currency total matches G04-007; 4 age buckets present and sum to outstanding balance | 🔲 |
| G04-R-004 | Tier Movement Report | Upgrade + downgrade + no-change counts sum to total evaluated members | 🔲 |
| G04-R-005 | Expiry & Breakage Report | Expired volume matches G04-003; 30/60/90-day forecast columns populated | 🔲 |
| G04-R-006 | Member Engagement Report | Active member count matches G04-006 numerator; inactive + dormant counts present | 🔲 |
| G04-R-007 | Campaign Performance Report | One row per campaign; bonus pts match campaign-level expected totals | 🔲 |

---

### Liability Aging Validation

| Age Bucket | Expected Volume (from seed) | Actual Volume | Match? | Status |
|------------|---------------------------|--------------|--------|--------|
| 0–3 months | Pre-calculated | — | — | 🔲 |
| 3–6 months | Pre-calculated | — | — | 🔲 |
| 6–12 months | Pre-calculated | — | — | 🔲 |
| > 12 months | Pre-calculated | — | — | 🔲 |
| **Total** | **Must equal G04-007 numerator** | — | — | 🔲 |

---

## Gate Summary Dashboard

| Gate | Stage | Criteria Count | PASS | FAIL | WAIVER | PENDING |
|------|-------|---------------|------|------|--------|---------|
| DESIGN-GATE-01 | Business Rule Compliance | 40 | — | — | — | 40 |
| DESIGN-GATE-02 | E2E Flow Validation | 12 flows | — | — | — | 12 |
| DESIGN-GATE-03 | Acceptance Criteria Sign-Off | 30 ACs / 5 modules | — | — | — | 30 |
| DESIGN-GATE-04 | Metric & Report Accuracy | 21 | — | — | — | 21 |
| **Total** | | **103** | | | | **103** |

---

## Traceability Index

| Gate Criterion | FR Source | AS Source | Business Rule |
|----------------|-----------|-----------|---------------|
| G01-01-002 (FLOOR rounding) | FR-01-011 | — | BR: Points rounded down |
| G01-01-006 (Idempotency) | FR-01-040..042 | AS-01 §3 | BR: Earn events idempotent |
| G01-01-008 (FIFO) | FR-01-053 | AS-03 R-AS03-04 | Glossary: FIFO Expiry |
| G01-02-002 (Immediate upgrade) | FR-02-011 | AS-02 §3 | BR: Upgrades immediate |
| G01-02-005 (No tier skipping) | FR-02-023 | — | BR: One tier at a time |
| G01-03-004 (FIFO debit) | FR-03-021 | AS-03 §3 | BR: FIFO |
| G01-03-007 (Auto reversal) | FR-03-040, FR-03-041 | AS-03 §6 Alert | BR: Reversal on failure |
| G01-04-002 (No retroactive effect) | FR-04-005 | — | BR: Rule versioning |
| G04-007 (Liability formula) | — | AS-05 §3.1 | Domain: Point Liability |
| G04-014 (DW lag ≤ 10 min) | FR-05-080 | AS-05 §2 Note | NFR-05-003 |

---

*Source documents: [loyalty_domain.md](file:///d:/learn/loyalty/loyalty_domain.md) · [FR-01](file:///d:/learn/loyalty/requirements/FR-01-earning-engine.md) · [FR-02](file:///d:/learn/loyalty/requirements/FR-02-tiering-system.md) · [FR-03](file:///d:/learn/loyalty/requirements/FR-03-redemption-engine.md) · [FR-04](file:///d:/learn/loyalty/requirements/FR-04-program-management.md) · [FR-05](file:///d:/learn/loyalty/requirements/FR-05-analytics-reporting.md) · [AS-01](file:///d:/learn/loyalty/analytics/AS-01-earning-engine.md) · [AS-02](file:///d:/learn/loyalty/analytics/AS-02-tiering-system.md) · [AS-03](file:///d:/learn/loyalty/analytics/AS-03-redemption-engine.md) · [AS-04](file:///d:/learn/loyalty/analytics/AS-04-program-management.md) · [AS-05](file:///d:/learn/loyalty/analytics/AS-05-analytics-reporting.md)*
