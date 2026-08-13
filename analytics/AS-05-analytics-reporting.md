# AS-05: Analytics & Reporting — Analytics Specification

**Module**: Analytics & Reporting (Platform-Wide)
**Version**: 1.0
**Date**: 2026-08-13
**Source**: [loyalty_domain.md](file:///d:/learn/loyalty/loyalty_domain.md) | [FR-05](file:///d:/learn/loyalty/requirements/FR-05-analytics-reporting.md)

---

## 1. Overview

This specification defines the **cross-module, platform-level analytics** for Loyalty Banking. While AS-01 through AS-04 cover module-specific metrics, this document addresses the **aggregated program KPIs** used for financial reporting, executive dashboards, and strategic decision-making — including Point Liability, Breakage Rate, Program ROI, and Active Member Rate.

---

## 2. Data Sources

| Source System | Table / Feed | Description | Latency |
|---------------|-------------|-------------|---------|
| Data Warehouse | `fact_point_transaction` | All point movements across all modules, pre-aggregated | < 10 min |
| Data Warehouse | `fact_redemption_order` | All redemption orders with fulfillment outcomes | < 10 min |
| Data Warehouse | `dim_member` | Member profile and tier snapshot | Daily |
| Data Warehouse | `dim_program` | Program definitions and cost-per-point config | Config |
| Data Warehouse | `dim_campaign` | Campaign definitions and period | Config |
| Data Warehouse | `fact_enrollment` | Enrollment state history | Daily |
| External (Finance) | `revenue_attribution` | Incremental revenue attributed to loyalty (for ROI) | Monthly |

> [!NOTE]
> All AS-05 analytics are sourced from the **Data Warehouse** (read replica / DW layer), not the operational databases. This ensures reporting workloads do not impact transactional system performance (see NFR-05-005).

---

## 3. Key Metrics & KPIs

### 3.1 Core Program KPIs

| Metric | Definition | Formula | Source Fields | Refresh |
|--------|-----------|---------|--------------|---------|
| **Total Points Issued** | All confirmed earn credits across programs | `SUM(fpt.amount) WHERE fpt.type IN ('EARN','BONUS') AND fpt.status='CONFIRMED'` | `fact_point_transaction` | Daily |
| **Total Points Redeemed** | All confirmed redemption debits | `SUM(fpt.amount) WHERE fpt.type='REDEEM' AND fpt.status='CONFIRMED_DEBIT'` | `fact_point_transaction` | Daily |
| **Total Points Expired** | All expired point debits | `SUM(fpt.amount) WHERE fpt.type='EXPIRED'` | `fact_point_transaction` | Daily |
| **Outstanding Point Balance** | Confirmed unspent points | `Total Issued - Total Redeemed - Total Expired - Total Adjusted` | Derived | Daily |
| **Redemption Rate** | % of issued points redeemed | `Total Points Redeemed / Total Points Issued × 100` | Derived | Monthly |
| **Breakage Rate** | % of issued points expired unredeemed | `Total Points Expired / Total Points Issued × 100` | Derived | Monthly |
| **Active Member Rate** | % of enrolled members who had qualifying activity | `COUNT(active_members) / COUNT(enrolled_members) × 100` | `dim_member`, `fact_enrollment` | Monthly |
| **Point Liability (Currency)** | Financial obligation of outstanding points | `Outstanding Point Balance × dp.cost_per_point` | `fact_point_transaction`, `dim_program` | Daily |
| **Program ROI** | Incremental return on loyalty investment | `(incremental_revenue - program_cost) / program_cost × 100` | `revenue_attribution`, `fact_point_transaction` (cost proxy) | Monthly |

---

### 3.2 Liability Aging Breakdown

| Bucket | Definition | Formula |
|--------|-----------|---------|
| 0–3 Months | Points earned in last 90 days | `SUM(balance WHERE earn_date >= TODAY - 90)` |
| 3–6 Months | Points earned 91–180 days ago | `SUM(balance WHERE earn_date BETWEEN TODAY-180 AND TODAY-91)` |
| 6–12 Months | Points earned 181–365 days ago | `SUM(balance WHERE earn_date BETWEEN TODAY-365 AND TODAY-181)` |
| > 12 Months | Points earned over 365 days ago | `SUM(balance WHERE earn_date < TODAY-365)` |

---

### 3.3 Expiry Forecast

| Horizon | Definition | Formula |
|---------|-----------|---------|
| Next 30 Days | Points scheduled to expire in next 30 days | `SUM(fpt.amount) WHERE fpt.expiry_date BETWEEN TODAY AND TODAY+30 AND fpt.status='CONFIRMED'` |
| Next 60 Days | Points scheduled to expire in next 60 days | `...TODAY+60` |
| Next 90 Days | Points scheduled to expire in next 90 days | `...TODAY+90` |

---

## 4. Reports

### R-AS05-01: Executive Program Performance Dashboard

**Audience**: Program Owner, CFO, Marketing Head
**Refresh**: Daily (data staleness ≤ 10 min for headline KPIs)

| KPI Tile | Value | Trend |
|----------|-------|-------|
| Total Enrolled Members | Current count | vs. prior month |
| Active Member Rate | % | vs. prior month |
| Total Points Issued (MTD) | Volume | vs. prior MTD |
| Redemption Rate | % | vs. prior month |
| Breakage Rate | % | vs. prior month |
| Point Liability | Currency value | vs. prior month |
| Program ROI | % | vs. prior quarter |

**Output**: Interactive dashboard with drill-down by program
**Format**: Dashboard only (no export)

---

### R-AS05-02: Monthly Financial Liability Report

**Audience**: Finance Team, Compliance

| Section | Content |
|---------|---------|
| Outstanding Balance Summary | Total points × cost-per-point per program |
| Liability Aging | Points by age bucket (0–3m, 3–6m, 6–12m, >12m) |
| Expiry Forecast | Points expiring in next 30 / 60 / 90 days |
| Breakage Rate | Actual vs. budgeted breakage |
| Liability Movement | Opening balance → +issued → -redeemed → -expired → Closing |

**Delivery**: 1st business day of each month
**Output**: PDF + XLSX
**Retention**: 7 years

---

### R-AS05-03: Quarterly Program Health Review

**Audience**: Program Admin, Marketing, Executive

| Section | Content |
|---------|---------|
| Enrollment Trend | Quarterly net enrollment by program |
| Tier Distribution | Current tier pyramid snapshot |
| KPI Trend (12-month rolling) | Redemption Rate, Breakage Rate, Active Member Rate |
| Top 5 Campaigns by Earn Lift | Campaign name, lift %, cost-per-point |
| Top 5 Rewards by Redemption Volume | Reward name, redemption count, points consumed |
| Anomalies | Any KPI deviations > 20% QoQ |

**Delivery**: First week of each quarter
**Output**: PDF report with charts

---

### R-AS05-04: Annual Breakage & Liability Reconciliation

**Audience**: Finance, External Auditors

| Section | Content |
|---------|---------|
| Full Year Point Ledger Summary | Issued, Redeemed, Expired, Adjusted, Outstanding |
| Breakage Forecast vs. Actual | Budgeted breakage % vs. actual |
| Liability at Year-End | Outstanding points × cost-per-point |
| Audit Trail Sample | Random 1% sample of point transactions with full lineage |

**Delivery**: January (for prior fiscal year)
**Output**: PDF + full CSV export
**Retention**: 7 years

---

### R-AS05-05: Real-Time Operational Alert Dashboard

**Audience**: Program Admin (Operations)
**Refresh**: 5 minutes

| Monitor | Threshold | Action |
|---------|-----------|--------|
| Today's earn events | < 80% of daily baseline by 14:00 | Alert: check earn pipeline |
| Today's redemptions | > 200% of daily baseline | Alert: investigate spike |
| Point liability | Exceeds configured ceiling | Alert: notify Finance |
| Breakage rate | < 5% (abnormally low) | Alert: possible redemption anomaly |
| System error rate | > 1% of API calls returning 5xx | Alert: page on-call |

---

## 5. Cross-Module Data Lineage

```
Earning Engine (point_transaction: EARN, BONUS, EXPIRED)
Redemption Engine (point_transaction: REDEEM, REVERSAL)
Program Mgmt (manual_adjustment_log: ADJUST)
        │
        ▼
  ETL / Streaming Pipeline (< 10 min latency)
        │
        ▼
  Data Warehouse
  ├─ fact_point_transaction (unified point ledger)
  ├─ fact_redemption_order
  ├─ dim_member (with tier snapshot)
  ├─ dim_program (cost_per_point, status)
  └─ dim_campaign
        │
        ▼
  Analytics Layer
  ├─ Pre-aggregated KPI tables (daily refresh)
  ├─ Real-time materialized views (5-min refresh)
  └─ Report generation service
        │
        ▼
  Consumers
  ├─ Executive Dashboard (FR-05-080)
  ├─ Scheduled Reports (FR-05-004)
  ├─ Finance Exports (FR-05-005)
  └─ Operational Alert Dashboard
```

---

## 6. Alerting & Thresholds

| Alert | Condition | Severity | Action |
|-------|-----------|----------|--------|
| Liability Ceiling Breach | Point liability > configured cap | Critical | Notify CFO + Program Owner immediately |
| Breakage Rate Abnormal (Low) | Breakage rate < 5% MTD | High | Review redemption spike; validate catalog |
| Breakage Rate Abnormal (High) | Breakage rate > 30% MTD | Medium | Investigate member engagement; check notifications |
| Active Member Rate Drop | Active member rate drops > 10% MoM | High | Alert Marketing; trigger re-engagement campaign |
| ROI Below Threshold | Program ROI < 0% for a quarter | High | Escalate to Program Owner for cost review |
| DW Pipeline Lag | DW data staleness > 30 minutes | High | Alert Data Engineering; dashboard may show stale data |

---

## 7. Data Retention Policy

| Data Type | Retention Period | Storage Tier |
|-----------|----------------|--------------|
| Real-time operational data | 90 days | Hot (SSD) |
| Daily aggregated metrics | 3 years | Warm |
| Monthly summaries | 7 years | Cold (archival) |
| Audit trail / liability reports | 7 years | Cold (archival, WORM) |
