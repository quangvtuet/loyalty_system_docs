# FR-05: Analytics & Reporting — Functional Requirements

**Module**: Analytics & Reporting
**Version**: 1.0
**Date**: 2026-08-13
**Source**: [loyalty_domain.md](file:///d:/learn/loyalty/loyalty_domain.md)
**Depends On**: [FR-01](file:///d:/learn/loyalty/requirements/FR-01-earning-engine.md), [FR-02](file:///d:/learn/loyalty/requirements/FR-02-tiering-system.md), [FR-03](file:///d:/learn/loyalty/requirements/FR-03-redemption-engine.md), [FR-04](file:///d:/learn/loyalty/requirements/FR-04-program-management.md)

---

## 1. Overview

The Analytics & Reporting module provides **data-driven insights** into loyalty program performance, member behavior, point liability, and campaign effectiveness. It serves both operational (real-time) monitoring needs and strategic (historical/trend) decision-making. Data is sourced from all upstream modules and exposed via reports, dashboards, and scheduled exports.

---

## 2. Actors / Roles

| Actor | Description |
|-------|-------------|
| **Program Admin** | Views program performance and campaign reports |
| **Finance Team** | Monitors point liability and breakage for accounting purposes |
| **Marketing Team** | Analyzes campaign performance and member engagement |
| **Executive / BI Team** | Views program ROI and strategic KPI dashboards |
| **System** | Scheduled report generation and data aggregation jobs |
| **Auditor** | Read-only access to all reports for compliance review |

---

## 3. Use Cases

| UC ID | Use Case | Primary Actor |
|-------|----------|---------------|
| UC-05-01 | Generate Point Issuance Report | System / Admin |
| UC-05-02 | Generate Redemption Report | System / Admin |
| UC-05-03 | Generate Point Liability Report | Finance Team |
| UC-05-04 | Generate Tier Movement Report | Program Admin |
| UC-05-05 | Generate Expiry & Breakage Report | Finance / Admin |
| UC-05-06 | Generate Member Engagement Report | Marketing Team |
| UC-05-07 | Generate Campaign Performance Report | Marketing Team |
| UC-05-08 | View real-time operational dashboard | Program Admin |
| UC-05-09 | Schedule and export a report | Any authorized user |

---

## 4. Functional Requirements

### 4.1 Report Framework

| ID | Requirement | Priority | Source Rule |
|----|-------------|----------|-------------|
| FR-05-001 | The system **SHALL** support generation of all 7 standard reports defined in the domain: Point Issuance, Redemption, Point Liability, Tier Movement, Expiry, Member Engagement, Campaign Performance. | Must | Domain §5 Key Reports |
| FR-05-002 | Every report **SHALL** support filtering by: program, date range (start date / end date), and granularity (daily / weekly / monthly). | Must | Domain §5 |
| FR-05-003 | The system **SHALL** support on-demand report generation and scheduled (recurring) report generation. | Must | Domain §5 |
| FR-05-004 | Scheduled reports **SHALL** support delivery frequencies: daily, weekly, and monthly, with delivery to: email, SFTP, or internal dashboard. | Should | Domain §5 |
| FR-05-005 | Reports **SHALL** be exportable in the following formats: CSV, XLSX, and PDF. | Should | Domain §5 |
| FR-05-006 | The system **SHALL** enforce role-based access control (RBAC) on reports — users only see data for programs they are authorized to access. | Must | Domain §5 |
| FR-05-007 | Report generation for large datasets (>1M rows) **SHALL** be processed asynchronously; the user receives a notification when the report is ready for download. | Should | Domain §5 |

---

### 4.2 Point Issuance Report (UC-05-01)

| ID | Requirement | Priority | Source Rule |
|----|-------------|----------|-------------|
| FR-05-010 | The system **SHALL** produce a Point Issuance Report showing total points earned, grouped by: program, campaign, channel, and transaction type. | Must | Domain §5 |
| FR-05-011 | The report **SHALL** include columns: period, program, channel, transaction type, campaign name, base points issued, bonus points issued, total points issued, number of earn events. | Must | Domain §5 |
| FR-05-012 | The report **SHALL** separately track pending vs. confirmed point volumes. | Should | Domain §1 |

---

### 4.3 Redemption Report (UC-05-02)

| ID | Requirement | Priority | Source Rule |
|----|-------------|----------|-------------|
| FR-05-020 | The system **SHALL** produce a Redemption Report showing total points redeemed, grouped by: reward category, fulfillment type, and redemption status. | Must | Domain §5 |
| FR-05-021 | The report **SHALL** include columns: period, reward category, fulfillment type, redemption count, points redeemed, reversal count, points reversed, net points redeemed. | Must | Domain §5 |
| FR-05-022 | The report **SHALL** include the Redemption Rate KPI: `points redeemed / points issued` for the selected period. | Must | Domain §5 KPIs |

---

### 4.4 Point Liability Report (UC-05-03)

| ID | Requirement | Priority | Source Rule |
|----|-------------|----------|-------------|
| FR-05-030 | The system **SHALL** produce a Point Liability Report showing the total outstanding confirmed point balance and its monetary equivalent. | Must | Domain §5 |
| FR-05-031 | Point Liability **SHALL** be calculated as: `SUM(confirmed_unspent_points) × cost_per_point`, where `cost_per_point` is defined per program. | Must | Domain §5 KPIs: Point Liability |
| FR-05-032 | The report **SHALL** include a breakdown by: program, tier, and point age bucket (0–3 months, 3–6 months, 6–12 months, >12 months). | Must | Domain §5 |
| FR-05-033 | The report **SHALL** include expiry forecast: estimated points expiring in the next 30, 60, and 90 days. | Must | Domain §5 |

---

### 4.5 Tier Movement Report (UC-05-04)

| ID | Requirement | Priority | Source Rule |
|----|-------------|----------|-------------|
| FR-05-040 | The system **SHALL** produce a Tier Movement Report showing the count of tier upgrades, downgrades, and no-changes per tier per period. | Must | Domain §5 |
| FR-05-041 | The report **SHALL** include current tier distribution: count and percentage of members in each tier at the end of the reporting period. | Must | Domain §5 |
| FR-05-042 | The report **SHALL** include average QP balance at upgrade and at downgrade for each tier transition type. | Should | Domain §5 |

---

### 4.6 Expiry & Breakage Report (UC-05-05)

| ID | Requirement | Priority | Source Rule |
|----|-------------|----------|-------------|
| FR-05-050 | The system **SHALL** produce an Expiry Report showing points that expired in the selected period, grouped by program, tier, and earn cohort. | Must | Domain §5 |
| FR-05-051 | The report **SHALL** include the Breakage Rate KPI: `points expired / points issued` for the selected period. | Must | Domain §5 KPIs: Breakage Rate |
| FR-05-052 | The report **SHALL** include a forward-looking breakage forecast for the next 90 days based on scheduled expiry events. | Should | Domain §5 |

---

### 4.7 Member Engagement Report (UC-05-06)

| ID | Requirement | Priority | Source Rule |
|----|-------------|----------|-------------|
| FR-05-060 | The system **SHALL** produce a Member Engagement Report showing: total enrolled members, active members, inactive members, and dormant members (no activity in >90 days). | Must | Domain §5 |
| FR-05-061 | The report **SHALL** include the Active Member Rate KPI: `active members / total enrolled members`. | Must | Domain §5 KPIs |
| FR-05-062 | The report **SHALL** include average earn events per active member per period and average points earned per member. | Should | Domain §5 |
| FR-05-063 | The report **SHOULD** support member segmentation by tier, channel, and geography. | Could | Domain §5 |

---

### 4.8 Campaign Performance Report (UC-05-07)

| ID | Requirement | Priority | Source Rule |
|----|-------------|----------|-------------|
| FR-05-070 | The system **SHALL** produce a Campaign Performance Report for each campaign, showing: campaign name, active dates, total bonus points issued, participating member count, and cost-per-point. | Must | Domain §5 |
| FR-05-071 | The report **SHALL** calculate incremental activity lift: `(points earned during campaign / baseline daily points) - 1`. The baseline is derived from the 30-day pre-campaign average. | Should | Domain §5 |
| FR-05-072 | The report **SHALL** include the Program ROI KPI for campaigns where incremental revenue data is available: `incremental revenue / total campaign cost`. | Could | Domain §5 KPIs: Program ROI |

---

### 4.9 Real-Time Operational Dashboard (UC-05-08)

| ID | Requirement | Priority | Source Rule |
|----|-------------|----------|-------------|
| FR-05-080 | The system **SHALL** provide a real-time dashboard displaying: today's earn events count, today's points issued, today's redemptions, current point liability, and active member count. | Must | Domain §5 |
| FR-05-081 | Dashboard data **SHALL** refresh automatically at a configurable interval (default: 5 minutes). | Should | Domain §5 |
| FR-05-082 | The dashboard **SHALL** display alerts when KPI thresholds are breached (e.g., liability exceeds configured limit, breakage rate drops below expected range). | Should | Domain §5 |

---

## 5. Non-Functional Requirements

| ID | Requirement | Priority |
|----|-------------|----------|
| NFR-05-001 | Standard reports (≤100K rows) **SHALL** generate within 30 seconds. | Must |
| NFR-05-002 | Large reports (>1M rows) **SHALL** be processed asynchronously and delivered within 4 hours. | Should |
| NFR-05-003 | The real-time dashboard **SHALL** have data staleness of no more than 10 minutes. | Must |
| NFR-05-004 | All report data **SHALL** be retained for a minimum of 7 years to meet regulatory requirements. | Must |
| NFR-05-005 | The reporting system **SHALL** not impact the performance of the transactional modules (Earning, Tiering, Redemption); reports read from a separate read replica or data warehouse. | Must |

---

## 6. Constraints & Assumptions

- Reporting data is sourced from a read replica or data warehouse populated from the operational databases; near real-time latency (max 10 minutes) is acceptable for historical reports.
- Point Liability currency conversion uses the program's base currency; multi-currency liability aggregation requires explicit configuration.
- Campaign baseline calculation requires at least 30 days of historical data; new programs without 30-day history use a configurable flat baseline.
- Program ROI requires external revenue data feeds which may not be available for all programs in v1.0.

---

## 7. Acceptance Criteria

| UC | Scenario | Expected Result |
|----|----------|----------------|
| UC-05-01 | Admin generates Point Issuance Report for August 2026 | Report shows points by program, channel, campaign; CSV export works |
| UC-05-03 | Finance runs Point Liability Report | Total liability = SUM(confirmed_unspent_points × cost_per_point); age buckets shown |
| UC-05-05 | Admin runs Expiry Report for Q3 | Breakage rate = expired pts / issued pts; 90-day forecast included |
| UC-05-07 | "Double Points August" campaign ends | Campaign report shows lift vs. baseline; cost-per-point calculated |
| UC-05-08 | Dashboard loaded during peak hours | Data no older than 10 minutes; alert fires if liability exceeds threshold |
| UC-05-09 | Admin schedules weekly Redemption Report to email | Report delivered every Monday at 08:00; CSV format |
