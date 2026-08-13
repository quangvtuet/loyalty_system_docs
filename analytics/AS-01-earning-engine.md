# AS-01: Earning Engine — Analytics Specification

**Module**: Earning Engine
**Version**: 1.0
**Date**: 2026-08-13
**Source**: [loyalty_domain.md](file:///d:/learn/loyalty/loyalty_domain.md) | [FR-01](file:///d:/learn/loyalty/requirements/FR-01-earning-engine.md)

---

## 1. Overview

This specification defines the **metrics, data lineage, and reporting requirements** for the Earning Engine module. Analytics covers point issuance volume, earn event patterns, bonus campaign contribution, idempotency health, and point state distribution.

---

## 2. Data Sources

| Source System | Table / Feed | Description | Latency |
|---------------|-------------|-------------|---------|
| Earning Engine DB | `point_transaction` | All earn, bonus, expiry, adjustment ledger entries | Real-time |
| Earning Engine DB | `earn_rule` | Base earn rule definitions and validity periods | Config |
| Program Mgmt DB | `campaign` | Campaign definitions, dates, and multipliers | Config |
| Core Banking Feed | `transaction_event` | Settled transaction events (source of earn) | < 60 sec |
| Program Mgmt DB | `enrollment` | Member-program enrollment status | Config |

---

## 3. Key Metrics & KPIs

| Metric | Definition | Formula | Source Fields | Refresh |
|--------|-----------|---------|--------------|---------|
| **Total Points Issued** | Gross points credited to member accounts | `SUM(pt.amount) WHERE pt.type IN ('EARN','BONUS') AND pt.status = 'CONFIRMED'` | `point_transaction.amount`, `.type`, `.status` | Daily |
| **Base Points Issued** | Points from base earn rules only | `SUM(pt.amount) WHERE pt.type = 'EARN' AND pt.status = 'CONFIRMED'` | `point_transaction.amount`, `.type` | Daily |
| **Bonus Points Issued** | Points attributed to campaigns | `SUM(pt.amount) WHERE pt.type = 'BONUS' AND pt.status = 'CONFIRMED'` | `point_transaction.amount`, `.type` | Daily |
| **Bonus Contribution Rate** | Share of total points from bonus campaigns | `Bonus Points Issued / Total Points Issued × 100` | Derived | Daily |
| **Total Earn Events** | Number of unique qualifying transactions processed | `COUNT(DISTINCT pt.source_event_id) WHERE pt.type = 'EARN'` | `point_transaction.source_event_id` | Daily |
| **Avg Points per Event** | Average earn per qualifying transaction | `Total Points Issued / Total Earn Events` | Derived | Daily |
| **Pending Point Volume** | Points in PENDING state (not yet confirmed) | `SUM(pt.amount) WHERE pt.status = 'PENDING'` | `point_transaction.amount`, `.status` | Hourly |
| **Idempotency Hit Rate** | Rate of duplicate earn event rejections | `COUNT(duplicate_rejects) / COUNT(total_earn_attempts) × 100` | `earn_event_log.is_duplicate` | Daily |
| **Earn Latency (p95)** | Processing time from event receipt to ledger commit | `PERCENTILE_95(pt.ledger_commit_ts - event.receipt_ts)` | `point_transaction`, `transaction_event` | Real-time |

---

## 4. Reports

### R-AS01-01: Earn Volume by Channel & Transaction Type

| Dimension | Measures | Granularity | Filters |
|-----------|---------|-------------|---------|
| Date, Program, Channel, Transaction Type, Campaign | Base Points, Bonus Points, Total Points, Event Count | Daily / Weekly / Monthly | Program, Date Range, Channel, Campaign |

**Delivery**: On-demand + scheduled (daily, 06:00)
**Output**: Dashboard tile + CSV export

---

### R-AS01-02: Campaign Bonus Contribution

| Dimension | Measures | Granularity | Filters |
|-----------|---------|-------------|---------|
| Date, Campaign Name, Campaign Type | Bonus Points Issued, Participating Members, Bonus Contribution Rate | Campaign Period | Program, Campaign, Date Range |

**Delivery**: On-demand
**Output**: Bar chart (contribution %), data table

---

### R-AS01-03: Earn Event Processing Health

| Dimension | Measures | Granularity | Filters |
|-----------|---------|-------------|---------|
| Date, Hour | Total Events Received, Successfully Processed, Duplicate Rejects, Validation Errors, Avg Latency (ms), p95 Latency (ms) | Hourly | Program, Date Range |

**Delivery**: Real-time dashboard
**Output**: Time-series line chart + alert triggers

---

### R-AS01-04: Point State Distribution

| Dimension | Measures | Granularity | Filters |
|-----------|---------|-------------|---------|
| Date, Program, Status (PENDING / CONFIRMED / EXPIRED) | Point Volume, Member Count | Daily | Program, Date Range |

**Delivery**: Daily scheduled
**Output**: Stacked bar chart + CSV

---

## 5. Data Lineage

```
Core Banking Transaction Feed
  └─> transaction_event (raw event, receipt_ts recorded)
        └─> Earn Rule Evaluation
              └─> Campaign Rule Evaluation
                    └─> point_transaction (EARN / BONUS, status=PENDING or CONFIRMED)
                          └─> Analytics Aggregation Layer (read replica / DW)
                                └─> Earn Volume Report, Health Dashboard
```

---

## 6. Alerting & Thresholds

| Alert | Condition | Severity | Action |
|-------|-----------|----------|--------|
| Earn Latency Breach | p95 earn latency > 2,000ms for 5 min | High | Page on-call engineer |
| Idempotency Spike | Duplicate reject rate > 5% in 1 hour | Medium | Investigate upstream event source |
| Pending Point Spike | Pending volume > 10% of daily confirmed volume | Medium | Check settlement pipeline |
| Zero Earn Events | No earn events processed in 30 min during business hours | High | Page on-call; check core banking feed |
