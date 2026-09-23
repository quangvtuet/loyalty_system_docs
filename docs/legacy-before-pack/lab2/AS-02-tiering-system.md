# AS-02: Tiering System — Analytics Specification

**Module**: Tiering System
**Version**: 1.1
**Date**: 2026-08-14
**Source**: [loyalty_domain.md](file:///d:/learn/loyalty/loyalty_domain.md) | [FR-02](file:///d:/learn/loyalty/requirements/FR-02-tiering-system.md)

---

## 1. Overview

This specification defines **metrics, data lineage, and reporting** for the Tiering System. Analytics covers tier distribution across the member base, upgrade and downgrade velocity, QP accumulation patterns, grace period utilization, and benefit activation rates.

---

## 2. Data Sources

| Source System | Table / Feed | Description | Latency |
|---------------|-------------|-------------|---------|
| Tiering DB | `member_tier` | Current and historical tier records per member | Real-time |
| Tiering DB | `qp_ledger` | QP accrual events per member per program | Real-time |
| Tiering DB | `tier_evaluation_log` | Batch evaluation run results | Post-run |
| Tiering DB | `tier_event` | Individual tier upgrade/downgrade events | Real-time |
| Program Mgmt DB | `tier_rule` | Tier thresholds and grace period config | Config |
| Program Mgmt DB | `enrollment` | Member-program enrollment | Config |

---

## 3. Key Metrics & KPIs

| Metric | Definition | Formula | Source Fields | Refresh |
|--------|-----------|---------|--------------|---------|
| **Tier Distribution** | Count and % of members in each tier (Silver / Gold / Platinum) | `COUNT(m.member_id) GROUP BY m.current_tier` | `member_tier.current_tier` | Daily |
| **Upgrade Rate** | % of members who upgraded in the period | `COUNT(te) WHERE te.event_type='UPGRADE' / enrolled_members × 100` | `tier_event.event_type`, `enrollment` | Monthly |
| **Downgrade Rate** | % of members who downgraded in the period | `COUNT(te) WHERE te.event_type='DOWNGRADE' / enrolled_members × 100` | `tier_event.event_type`, `enrollment` | Monthly |
| **Grace Period Utilization Rate** | % of downgrades that used the grace period | `COUNT(grace_used) / COUNT(downgrades) × 100` | `tier_event.grace_period_used` | Monthly |
| **Grace Period Rescue Rate** | % of grace periods that ended in retention (downgrade cancelled) | `COUNT(grace_cancelled) / COUNT(grace_initiated) × 100` | `tier_event.grace_cancelled` | Monthly |
| **Avg QP at Upgrade** | Average QP balance when a member upgraded | `AVG(te.qp_balance_at_event) WHERE te.event_type='UPGRADE'` | `tier_event.qp_balance_at_event` | Monthly |
| **Avg QP at Downgrade** | Average QP balance when member was evaluated for downgrade | `AVG(te.qp_balance_at_event) WHERE te.event_type='DOWNGRADE'` | `tier_event.qp_balance_at_event` | Monthly |
| **Tier Retention Rate** | % of members who maintained their tier after evaluation | `COUNT(no_change_members) / COUNT(evaluated_members) × 100` | `tier_evaluation_log` | Monthly |
| **QP Accrual per Member** | Average QP earned per active member in the period | `SUM(qp_ledger.qp_amount) / COUNT(DISTINCT qp_ledger.member_id)` | `qp_ledger` | Monthly |
| **Tier Upgrade Velocity** | Days from enrollment to first tier upgrade (for upgraded members) | `AVG(te.event_ts - enrollment.enrollment_date) WHERE te.event_type='UPGRADE'` | `tier_event`, `enrollment` | Quarterly |

---

## 4. Reports

### R-AS02-01: Tier Distribution Snapshot

| Dimension | Measures | Granularity | Filters |
|-----------|---------|-------------|---------|
| Date, Program, Tier | Member Count, % of Total Members | Daily (end-of-day snapshot) | Program, Date |

**Purpose**: Monitor health of tier pyramid; ensure Platinum does not become over-represented (diluting exclusivity).
**Output**: Stacked bar chart + data table
**Alert**: If Platinum tier exceeds 10% of total enrolled members.

---

### R-AS02-02: Tier Movement Summary

| Dimension | Measures | Granularity | Filters |
|-----------|---------|-------------|---------|
| Period, Program, From Tier, To Tier | Upgrade Count, Downgrade Count, Retention Count | Monthly | Program, Date Range |

**Purpose**: Understand tier mobility and program stickiness.
**Output**: Sankey-style flow table + counts
**Delivery**: Monthly scheduled report (1st of month)

---

### R-AS02-03: QP Accumulation Distribution

| Dimension | Measures | Granularity | Filters |
|-----------|---------|-------------|---------|
| Period, Program, Current Tier | Avg QP, Median QP, QP at P25 / P75 / P90, Members Near Threshold (within 10%) | Monthly | Program, Tier, Date Range |

**Purpose**: Identify members near upgrade thresholds for targeted campaigns. Canonical thresholds: **Gold: 1,000 QP** (near = within 100 QP), **Platinum: 3,000 QP** (near = within 300 QP).
**Output**: Box plot distribution + "near threshold" segment list (CSV)

---

### R-AS02-04: Grace Period & Rescue Analysis

| Dimension | Measures | Granularity | Filters |
|-----------|---------|-------------|---------|
| Period, Program, Tier | Grace Periods Initiated, Grace Rescues, Grace Downgrades, Rescue Rate | Monthly | Program, Date Range |

**Purpose**: Evaluate effectiveness of grace period in retaining members.
**Output**: Funnel chart + CSV

---

### R-AS02-05: Tier Evaluation Batch Health

| Dimension | Measures | Granularity | Filters |
|-----------|---------|-------------|---------|
| Run Date, Program | Members Evaluated, Upgrades, Downgrades, No-Change, Run Duration (sec), Errors | Per Batch Run | Program, Date Range |

**Purpose**: Operational monitoring of batch evaluation job.
**Output**: Table + duration trend chart
**Alert**: Batch run duration > 4 hours OR error count > 0.

---

## 5. Data Lineage

```
QP Accrual (from Earning Engine)
  └─> qp_ledger (cumulative QP per member per program)
        └─> Real-time threshold check
              └─> tier_event (UPGRADE, type=IMMEDIATE)

End of Tier Period Batch Job
  └─> qp_ledger aggregation
        └─> tier_rule threshold comparison
              └─> tier_event (DOWNGRADE_INITIATED / NO_CHANGE)
                    └─> Grace period countdown
                          └─> tier_event (DOWNGRADE_CONFIRMED / DOWNGRADE_CANCELLED)
                                └─> member_tier (current tier updated)
                                      └─> Analytics Aggregation Layer
                                            └─> Tier Distribution, Movement Reports
```

---

## 6. Alerting & Thresholds

| Alert | Condition | Severity | Action |
|-------|-----------|----------|--------|
| Tier Pyramid Dilution | Platinum members > 10% of enrolled base | Medium | Review tier thresholds with Program Admin |
| High Downgrade Rate | Period downgrade rate > 20% | High | Investigate QP accrual issues or threshold misconfiguration |
| Grace Rescue Rate Anomaly | Grace rescue rate drops below 5% | Low | Marketing opportunity: targeted earn campaign for members in **30-day** grace period |
| Batch Evaluation Failure | Batch run exits with error count > 0 | High | Page on-call; no tier changes committed until re-run succeeds |
