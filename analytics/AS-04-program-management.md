# AS-04: Program Management — Analytics Specification

**Module**: Program Management
**Version**: 1.0
**Date**: 2026-08-13
**Source**: [loyalty_domain.md](file:///d:/learn/loyalty/loyalty_domain.md) | [FR-04](file:///d:/learn/loyalty/requirements/FR-04-program-management.md)

---

## 1. Overview

This specification defines **metrics, data lineage, and reporting** for the Program Management module. Analytics covers program enrollment health, campaign effectiveness configuration, partner earn contribution, rule version change audit, and manual adjustment activity.

---

## 2. Data Sources

| Source System | Table / Feed | Description | Latency |
|---------------|-------------|-------------|---------|
| Program Mgmt DB | `loyalty_program` | Program definitions and status history | Config |
| Program Mgmt DB | `enrollment` | Member enrollment records with status | Real-time |
| Program Mgmt DB | `campaign` | Campaign definitions, priority, dates | Config |
| Program Mgmt DB | `partner` | Partner registry and status | Config |
| Earning Engine DB | `point_transaction` | Source-tagged earn events (partner vs. direct) | Real-time |
| Program Mgmt DB | `config_version_log` | Audit log of all configuration changes | Real-time |
| Program Mgmt DB | `manual_adjustment_log` | Manual point balance adjustment records | Real-time |

---

## 3. Key Metrics & KPIs

| Metric | Definition | Formula | Source Fields | Refresh |
|--------|-----------|---------|--------------|---------|
| **Active Programs** | Count of programs in ACTIVE status | `COUNT(lp.program_id) WHERE lp.status='ACTIVE'` | `loyalty_program.status` | Real-time |
| **Total Enrolled Members** | Members with ACTIVE enrollment across all programs | `COUNT(DISTINCT e.member_id) WHERE e.status='ACTIVE'` | `enrollment.status` | Daily |
| **Enrollment Rate** | New enrollments as % of eligible population | `COUNT(new_enrollments) / eligible_member_count × 100` | `enrollment`, external member registry | Monthly |
| **Enrollment Churn Rate** | % of enrollments cancelled/suspended in the period | `COUNT(e.status IN ('CANCELLED','SUSPENDED')) / COUNT(total_enrollments) × 100` | `enrollment.status` | Monthly |
| **Active Campaigns** | Count of campaigns currently active | `COUNT(c.campaign_id) WHERE c.status='ACTIVE' AND NOW() BETWEEN c.start_date AND c.end_date` | `campaign` | Real-time |
| **Campaign Earn Lift** | Incremental earn during campaign vs. pre-campaign baseline | `(points_during_campaign / avg_daily_baseline_points) - 1 × 100` | `point_transaction`, `campaign` | Per Campaign |
| **Partner Earn Share** | % of total points issued originating from partner events | `SUM(pt.amount WHERE pt.source_type='PARTNER') / Total Points Issued × 100` | `point_transaction.source_type` | Daily |
| **Active Partners** | Count of partners in ACTIVE status | `COUNT(p.partner_id) WHERE p.status='ACTIVE'` | `partner.status` | Real-time |
| **Manual Adjustment Volume** | Total points manually adjusted in the period | `SUM(ABS(ma.point_amount))` | `manual_adjustment_log.point_amount` | Daily |
| **Manual Adjustment Rate** | Manual adjustments as % of total point transactions | `COUNT(manual_adjustments) / COUNT(total_point_transactions) × 100` | `manual_adjustment_log`, `point_transaction` | Monthly |
| **Config Change Frequency** | Count of rule/program configuration changes per period | `COUNT(cvl.change_id) GROUP BY period` | `config_version_log` | Monthly |

---

## 4. Reports

### R-AS04-01: Program Enrollment Health

| Dimension | Measures | Granularity | Filters |
|-----------|---------|-------------|---------|
| Date, Program, Enrollment Status | New Enrollments, Active Members, Cancelled, Suspended, Net Enrollment Change | Daily / Monthly | Program, Date Range |

**Purpose**: Monitor enrollment growth and churn per program.
**Output**: Line chart (net enrollment trend) + status breakdown table
**Delivery**: Weekly scheduled

---

### R-AS04-02: Campaign Configuration Inventory

| Dimension | Measures | Granularity | Filters |
|-----------|---------|-------------|---------|
| Program, Campaign Status | Campaign Name, Type, Priority, Start Date, End Date, Expected Points Budget, Actual Points Issued | Per Campaign | Program, Status, Date Range |

**Purpose**: Audit active and upcoming campaigns; track budget adherence.
**Output**: Table + budget vs. actual comparison
**Delivery**: On-demand + weekly (active campaigns only)

---

### R-AS04-03: Partner Earn Contribution

| Dimension | Measures | Granularity | Filters |
|-----------|---------|-------------|---------|
| Month, Program, Partner Name | Points Earned via Partner, Partner Earn Share %, Earn Events Count, Avg Points per Event | Monthly | Program, Partner, Date Range |

**Purpose**: Evaluate partner ROI and identify top/low-performing partners.
**Output**: Ranked bar chart + data table
**Delivery**: Monthly, sent to Partner Manager

---

### R-AS04-04: Rule & Configuration Change Audit

| Dimension | Measures | Granularity | Filters |
|-----------|---------|-------------|---------|
| Date, Program, Changed By, Rule Type | Change Count, Change Type (create/update/deactivate), Previous Value, New Value | Per Event | Program, Date Range, Operator |

**Purpose**: Compliance and governance — track who changed what and when.
**Output**: Audit log table (read-only, non-exportable by default)
**Access**: Auditor and Program Admin roles only

---

### R-AS04-05: Manual Adjustment Audit

| Dimension | Measures | Granularity | Filters |
|-----------|---------|-------------|---------|
| Date, Program, Operator, Reason Code | Adjustment Count, Total Points Adjusted (credit/debit), Approver, Pending Approvals | Daily | Program, Operator, Date Range |

**Purpose**: Governance monitoring of manual overrides; detect anomalies.
**Output**: Table + daily trend; flag high-volume operators
**Alert**: Single operator adjusts > configurable threshold in one day.

---

## 5. Data Lineage

```
Program Admin Actions
  └─> loyalty_program / campaign / earn_rule / tier_rule / redemption_rule (writes)
        └─> config_version_log (immutable audit trail)

Partner API Events
  └─> transaction_event (source_type='PARTNER', partner_id tagged)
        └─> point_transaction (earn credit)
              └─> point_transaction.source_type → Partner Earn Share metric

Enrollment Events
  └─> enrollment (status transitions)
        └─> Analytics Aggregation Layer
              └─> Enrollment Health Report

Manual Adjustments
  └─> manual_adjustment_log (with approval chain)
        └─> Analytics Aggregation Layer
              └─> Manual Adjustment Audit Report
```

---

## 6. Alerting & Thresholds

| Alert | Condition | Severity | Action |
|-------|-----------|----------|--------|
| Enrollment Churn Spike | Monthly churn rate > 15% for any program | High | Notify Program Owner; investigate cause |
| Campaign Budget Overrun | Campaign points issued > 110% of configured budget | Medium | Notify Program Admin; consider campaign suspension |
| Partner Earn Anomaly | Partner earn events deviate > 50% from 7-day avg | Medium | Check partner integration; validate event payload |
| High Manual Adjustment Volume | Daily manual adjustment volume > configurable threshold | High | Escalate to Compliance; require dual approval |
| Config Change Outside Business Hours | Config change logged outside 07:00–19:00 local time | Low | Audit notification to Program Owner |
