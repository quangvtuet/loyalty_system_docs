# AS-03: Redemption Engine — Analytics Specification

**Module**: Redemption Engine
**Version**: 1.0
**Date**: 2026-08-13
**Source**: [loyalty_domain.md](file:///d:/learn/loyalty/loyalty_domain.md) | [FR-03](file:///d:/learn/loyalty/requirements/FR-03-redemption-engine.md)

---

## 1. Overview

This specification defines **metrics, data lineage, and reporting** for the Redemption Engine. Analytics covers redemption volume and rates, catalog popularity, fulfillment performance, reversal patterns, and the FIFO point consumption profile.

---

## 2. Data Sources

| Source System | Table / Feed | Description | Latency |
|---------------|-------------|-------------|---------|
| Redemption DB | `redemption_order` | Redemption request records with lifecycle status | Real-time |
| Redemption DB | `fulfillment_record` | Fulfillment delivery status per order | Real-time |
| Earning Engine DB | `point_transaction` | All point movements (for net balance and FIFO tracking) | Real-time |
| Redemption DB | `reward_item` | Catalog item definitions and points cost | Config |
| Program Mgmt DB | `enrollment` | Member-program data | Config |
| Tiering DB | `member_tier` | Member's current tier at redemption time | Real-time |

---

## 3. Key Metrics & KPIs

| Metric | Definition | Formula | Source Fields | Refresh |
|--------|-----------|---------|--------------|---------|
| **Total Points Redeemed** | Confirmed point debits for redemptions | `SUM(pt.amount) WHERE pt.type='REDEEM' AND pt.status='CONFIRMED_DEBIT'` | `point_transaction.amount`, `.type`, `.status` | Daily |
| **Redemption Rate** | Share of issued points that are redeemed | `Total Points Redeemed / Total Points Issued × 100` | Derived (cross-module) | Monthly |
| **Total Redemption Orders** | Count of redemption requests | `COUNT(ro.order_id)` | `redemption_order` | Daily |
| **Fulfillment Success Rate** | % of orders successfully fulfilled | `COUNT(fr.status='FULFILLED') / COUNT(ro.order_id) × 100` | `fulfillment_record.status`, `redemption_order` | Daily |
| **Reversal Rate** | % of orders reversed | `COUNT(ro.status='REVERSED') / COUNT(ro.order_id) × 100` | `redemption_order.status` | Daily |
| **Avg Points per Redemption** | Average points consumed per order | `Total Points Redeemed / Total Redemption Orders` | Derived | Daily |
| **Avg Redemption Value** | Average monetary value of redemptions | `AVG(ro.points_redeemed × program.cost_per_point)` | `redemption_order`, `loyalty_program` | Monthly |
| **Fulfillment SLA Compliance** | % of orders fulfilled within SLA window | `COUNT(on_time_fulfillments) / COUNT(fulfilled_orders) × 100` | `fulfillment_record.fulfilled_ts`, `redemption_order.created_ts` | Daily |
| **Avg Fulfillment Time** | Average hours from order creation to fulfillment | `AVG(fr.fulfilled_ts - ro.created_ts)` | `fulfillment_record`, `redemption_order` | Daily |
| **Catalog Utilization Rate** | % of active catalog items with at least 1 redemption in period | `COUNT(DISTINCT ro.reward_item_id) / COUNT(ri.item_id WHERE ri.status='ACTIVE') × 100` | `redemption_order`, `reward_item` | Monthly |

---

## 4. Reports

### R-AS03-01: Redemption Volume by Category

| Dimension | Measures | Granularity | Filters |
|-----------|---------|-------------|---------|
| Date, Program, Reward Category, Fulfillment Type | Order Count, Points Redeemed, Reversal Count, Net Points Redeemed | Daily / Monthly | Program, Date Range, Category |

**Purpose**: Identify most popular reward categories; guide catalog investment.
**Output**: Bar chart + data table
**Delivery**: Monthly scheduled

---

### R-AS03-02: Fulfillment Health Dashboard

| Dimension | Measures | Granularity | Filters |
|-----------|---------|-------------|---------|
| Date, Hour, Fulfillment Partner | Orders Pending, In-Progress, Fulfilled, Failed, Avg Fulfillment Time (hrs), SLA Compliance % | Hourly | Program, Partner, Date |

**Purpose**: Real-time operational monitoring of fulfillment pipeline.
**Output**: Status gauge + time-series trend
**Alert**: Failed order count > 5% of hourly orders, or avg fulfillment time > SLA.

---

### R-AS03-03: Redemption Rate Trend

| Dimension | Measures | Granularity | Filters |
|-----------|---------|-------------|---------|
| Month, Program, Tier | Points Issued, Points Redeemed, Redemption Rate %, Breakage Rate % | Monthly | Program, Tier, Date Range |

**Purpose**: Track program engagement and financial exposure (liability reduction).
**Output**: Dual-axis line chart (volume + rate)
**Delivery**: Monthly executive report

---

### R-AS03-04: FIFO Point Age at Redemption

| Dimension | Measures | Granularity | Filters |
|-----------|---------|-------------|---------|
| Month, Program | Avg Age of Points Consumed (days), % from <3m batch, % from 3–6m, % from >6m | Monthly | Program, Date Range |

**Purpose**: Understand how old points are when consumed — indicates engagement urgency. Older consumption = more dormant members redeeming.
**Output**: Age distribution stacked bar
**Delivery**: Quarterly

---

### R-AS03-05: Reversal Root Cause Analysis

| Dimension | Measures | Granularity | Filters |
|-----------|---------|-------------|---------|
| Month, Program, Reversal Reason Code, Fulfillment Partner | Reversal Count, Points Reversed, % of Total Orders | Monthly | Program, Partner, Reason Code |

**Purpose**: Identify recurring fulfillment failure patterns for partner SLA reviews.
**Output**: Pareto chart (reason codes by frequency) + data table
**Delivery**: Monthly, sent to Partner Manager

---

## 5. Data Lineage

```
Member Redemption Request
  └─> redemption_order (PENDING created)
        └─> Balance Validation (point_transaction read)
              └─> FIFO Point Debit (point_transaction PENDING_DEBIT)
                    └─> Fulfillment Dispatch
                          ├─> [FULFILLED] fulfillment_record (FULFILLED)
                          │     └─> point_transaction (CONFIRMED_DEBIT)
                          └─> [FAILED] fulfillment_record (FAILED)
                                └─> point_transaction (REVERSAL credit)
                                      └─> Analytics Aggregation Layer
                                            └─> Redemption Volume, Fulfillment Health Reports
```

---

## 6. Alerting & Thresholds

| Alert | Condition | Severity | Action |
|-------|-----------|----------|--------|
| High Reversal Rate | Reversal rate > 5% for any 1-hour window | High | Investigate fulfillment partner; consider temporary catalog suspension |
| SLA Breach | Fulfillment SLA compliance < 95% for any partner | Medium | Escalate to Partner Manager |
| Redemption Rate Drop | Month-over-month redemption rate drops > 15% | Medium | Review catalog attractiveness; trigger engagement campaign |
| Balance Validation Errors | Concurrent balance conflict errors > 1% of requests | High | Review distributed locking; page on-call |
