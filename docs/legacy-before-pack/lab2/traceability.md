# Loyalty Banking — Traceability Matrix

**Version**: 1.1  
**Date**: 2026-08-14  
**Domain**: Loyalty Banking  
**Source**: [loyalty_domain.md](file:///Users/hoangdd/Desktop/personal/workspace/AI-study-2/loyalty_domain.md) | [Requirements (FR-01..05)](file:///Users/hoangdd/Desktop/personal/workspace/AI-study-2/requirements/) | [Analytics (AS-01..05)](file:///Users/hoangdd/Desktop/personal/workspace/AI-study-2/analytics/) | [Architecture & Governance](file:///Users/hoangdd/Desktop/personal/workspace/AI-study-2/architecture/Architectural-Governance-Framework.md) | [Detailed Design](file:///Users/hoangdd/Desktop/personal/workspace/AI-study-2/design/) | [Quality Gates](file:///Users/hoangdd/Desktop/personal/workspace/AI-study-2/quality-gates/)

---

## 1. Overview

This document provides complete, bidirectional end-to-end traceability across all specification artifacts in the Loyalty Banking platform. It connects:
$$\text{Domain Concept} \iff \text{Functional Req (FR)} \iff \text{Analytics Spec (AS)} \iff \text{Architecture / ADR} \iff \text{Detailed Design (DD)} \iff \text{Quality Gates (QG)}$$

---

## 2. Module Traceability

### 2.1 Earning Engine

| Domain Concept / Business Rule | Concrete Value / Standard | Functional Req (FR) | Analytics Spec (AS) | Architecture & ADR | Detailed Design (DD) | Design Gate (QG-Design) | Architecture Gate (QG-Arch) |
|---|---|---|---|---|---|---|---|
| **Base Earn Rate Calculation** | 1 pt per $1 spent; `FLOOR(amt × rate)` | [FR-01-010](file:///d:/learn/loyalty/requirements/FR-01-earning-engine.md), [FR-01-011](file:///d:/learn/loyalty/requirements/FR-01-earning-engine.md) | [AS-01](file:///d:/learn/loyalty/analytics/AS-01-earning-engine.md) §3 | [Data Arch §3.1](file:///d:/learn/loyalty/architecture/Data-Architecture-and-Schema.md) | [DD-01 §3.1](file:///d:/learn/loyalty/design/DD-01-earning-engine.md) | G01-01-002, AC-01-001 | — |
| **Earn Event Ingestion SLA** | Core banking feed ≤ 60s from settlement | [FR-01-001](file:///d:/learn/loyalty/requirements/FR-01-earning-engine.md), [FR-01-005](file:///d:/learn/loyalty/requirements/FR-01-earning-engine.md) | [AS-01](file:///d:/learn/loyalty/analytics/AS-01-earning-engine.md) §2 | [Arch Overview §5.2](file:///d:/learn/loyalty/architecture/Architecture-Overview.md), [ADR-002](file:///d:/learn/loyalty/architecture/adrs/ADR-002-event-driven-earn-ingestion-and-idempotency.md) | [DD-01 §2](file:///d:/learn/loyalty/design/DD-01-earning-engine.md) | G01-01-001, G01-01-010 | G02-01-001, G02-01-002 |
| **Bonus Campaign Evaluation** | 2× ("Double Points August"), priority / stacking | [FR-01-020](file:///d:/learn/loyalty/requirements/FR-01-earning-engine.md)..024 | [AS-01](file:///d:/learn/loyalty/analytics/AS-01-earning-engine.md) §3, R-AS01-02 | [Data Arch §3.4](file:///d:/learn/loyalty/architecture/Data-Architecture-and-Schema.md) | [DD-01 §3.2, §4.1](file:///d:/learn/loyalty/design/DD-01-earning-engine.md) | G01-01-003, G01-01-004, G01-01-005, AC-01-002 | — |
| **Pending vs. Confirmed Lifecycle** | Settlement triggers PENDING → CONFIRMED; reversal cancels | [FR-01-030](file:///d:/learn/loyalty/requirements/FR-01-earning-engine.md)..033 | [AS-01](file:///d:/learn/loyalty/analytics/AS-01-earning-engine.md) §3, R-AS01-04 | [Data Arch §3.1](file:///d:/learn/loyalty/architecture/Data-Architecture-and-Schema.md) | [DD-01 §2](file:///d:/learn/loyalty/design/DD-01-earning-engine.md) | G01-01-007, AC-01-005 | — |
| **Idempotency Guarantee** | Exactly-once Sổ cái (Earning Ledger) credit per unique source transaction | [FR-01-040](file:///d:/learn/loyalty/requirements/FR-01-earning-engine.md)..042, NFR-01-004 | [AS-01](file:///d:/learn/loyalty/analytics/AS-01-earning-engine.md) §3 | [ADR-002](file:///d:/learn/loyalty/architecture/adrs/ADR-002-event-driven-earn-ingestion-and-idempotency.md) | [DD-01 §4.2](file:///d:/learn/loyalty/design/DD-01-earning-engine.md) | G01-01-006, AC-01-004 | G03-02-002, G04-03-002 |
| **Point Expiry Policy** | Rolling 12 months / Fixed 31 Dec; 30d + 7d notifications | [FR-01-050](file:///d:/learn/loyalty/requirements/FR-01-earning-engine.md)..052 | [AS-01](file:///d:/learn/loyalty/analytics/AS-01-earning-engine.md) §3, [AS-05](file:///d:/learn/loyalty/analytics/AS-05-analytics-reporting.md) §3.3 | [Data Arch §3.1](file:///d:/learn/loyalty/architecture/Data-Architecture-and-Schema.md) | [DD-01 §4.3](file:///d:/learn/loyalty/design/DD-01-earning-engine.md) | G01-01-009, AC-01-006 | G01-02-002 |
| **FIFO Point Ledger** | Append-only immutable Sổ cái (Earning Ledger); oldest batch consumed first | [FR-01-053](file:///d:/learn/loyalty/requirements/FR-01-earning-engine.md), [FR-01-060](file:///d:/learn/loyalty/requirements/FR-01-earning-engine.md)..062 | [AS-01](file:///d:/learn/loyalty/analytics/AS-01-earning-engine.md) §5 | [Data Arch §3.1](file:///d:/learn/loyalty/architecture/Data-Architecture-and-Schema.md), [ADR-001](file:///d:/learn/loyalty/architecture/adrs/ADR-001-module-boundaries-and-data-isolation.md) | [DD-01 §2](file:///d:/learn/loyalty/design/DD-01-earning-engine.md) | G01-01-008 | G01-03-002, G01-03-003, G03-02-001 |

---

### 2.2 Tiering System

| Domain Concept / Business Rule | Concrete Value / Standard | Functional Req (FR) | Analytics Spec (AS) | Architecture & ADR | Detailed Design (DD) | Design Gate (QG-Design) | Architecture Gate (QG-Arch) |
|---|---|---|---|---|---|---|---|
| **Qualifying Points (QP) Separation** | QP tracked independently from redeemable points | [FR-02-001](file:///d:/learn/loyalty/requirements/FR-02-tiering-system.md)..005 | [AS-02](file:///d:/learn/loyalty/analytics/AS-02-tiering-system.md) §3, R-AS02-03 | [Data Arch §3.2](file:///d:/learn/loyalty/architecture/Data-Architecture-and-Schema.md), [ADR-001](file:///d:/learn/loyalty/architecture/adrs/ADR-001-module-boundaries-and-data-isolation.md) | [DD-02 §2](file:///d:/learn/loyalty/design/DD-02-tiering-system.md) | G01-02-001, AC-02-001 | G01-03-005, G02-02-001 |
| **Tier Thresholds & Upgrade** | Silver: 0 QP, Gold: 1,000 QP, Platinum: 3,000 QP; instant upgrade | [FR-02-010](file:///d:/learn/loyalty/requirements/FR-02-tiering-system.md)..013, NFR-02-001 | [AS-02](file:///d:/learn/loyalty/analytics/AS-02-tiering-system.md) §3, R-AS02-01 | [Data Arch §3.2](file:///d:/learn/loyalty/architecture/Data-Architecture-and-Schema.md) | [DD-02 §3, §5.1](file:///d:/learn/loyalty/design/DD-02-tiering-system.md) | G01-02-002, G01-02-003, AC-02-002, G04-008 | G03-01-004 |
| **Tier Evaluation Period & Downgrade** | Calendar year (1 Jan–31 Dec); 1 tier level per cycle | [FR-02-020](file:///d:/learn/loyalty/requirements/FR-02-tiering-system.md)..024, NFR-02-002 | [AS-02](file:///d:/learn/loyalty/analytics/AS-02-tiering-system.md) §3, R-AS02-02 | [Data Arch §3.2](file:///d:/learn/loyalty/architecture/Data-Architecture-and-Schema.md) | [DD-02 §4, §5.2](file:///d:/learn/loyalty/design/DD-02-tiering-system.md) | G01-02-004, G01-02-005, G01-02-006, AC-02-003 | G03-03-001, G03-03-002 |
| **Grace Period & Rescue** | Default 30 days; retain benefits during grace; rescue on threshold | [FR-02-030](file:///d:/learn/loyalty/requirements/FR-02-tiering-system.md)..033 | [AS-02](file:///d:/learn/loyalty/analytics/AS-02-tiering-system.md) §3, R-AS02-04 | [Data Arch §3.2](file:///d:/learn/loyalty/architecture/Data-Architecture-and-Schema.md) | [DD-02 §4, §5.2](file:///d:/learn/loyalty/design/DD-02-tiering-system.md) | G01-02-004, G01-02-007, AC-02-004, AC-02-005 | — |
| **Benefit Activation & Downstream Sync** | Silver 1×, Gold 1.5×, Platinum 2×; real-time event dispatch | [FR-02-040](file:///d:/learn/loyalty/requirements/FR-02-tiering-system.md)..043, NFR-02-003 | [AS-02](file:///d:/learn/loyalty/analytics/AS-02-tiering-system.md) §5 | [Arch Overview §5.2](file:///d:/learn/loyalty/architecture/Architecture-Overview.md) | [DD-02 §3, §5.1](file:///d:/learn/loyalty/design/DD-02-tiering-system.md) | G01-02-008, FLOW-02 | G02-02-002, G02-02-003, G03-01-003 |

---

### 2.3 Redemption Engine

| Domain Concept / Business Rule | Concrete Value / Standard | Functional Req (FR) | Analytics Spec (AS) | Architecture & ADR | Detailed Design (DD) | Design Gate (QG-Design) | Architecture Gate (QG-Arch) |
|---|---|---|---|---|---|---|---|
| **Redemption Catalog & Tier Access** | Items with minimum tier requirement (e.g. Platinum-only) | [FR-03-001](file:///d:/learn/loyalty/requirements/FR-03-redemption-engine.md)..005 | [AS-03](file:///d:/learn/loyalty/analytics/AS-03-redemption-engine.md) §3, R-AS03-01 | [Data Arch §3.3](file:///d:/learn/loyalty/architecture/Data-Architecture-and-Schema.md) | [DD-03 §2, §4.3](file:///d:/learn/loyalty/design/DD-03-redemption-engine.md) | G01-03-006, AC-03-003, FLOW-09 | G03-01-006 |
| **Redemption Rate & Minimum** | 100 pts = $1 value; Minimum 100 pts per redemption | [FR-03-010](file:///d:/learn/loyalty/requirements/FR-03-redemption-engine.md)..014 | [AS-03](file:///d:/learn/loyalty/analytics/AS-03-redemption-engine.md) §3 ($0.01/pt) | [Data Arch §3.3](file:///d:/learn/loyalty/architecture/Data-Architecture-and-Schema.md) | [DD-03 §1](file:///d:/learn/loyalty/design/DD-03-redemption-engine.md) | G01-03-001, G01-03-005, AC-03-001 | G02-03-001 |
| **FIFO Point Debit** | Immediate debit on approval; oldest confirmed batch consumed | [FR-03-020](file:///d:/learn/loyalty/requirements/FR-03-redemption-engine.md)..024, NFR-03-001 | [AS-03](file:///d:/learn/loyalty/analytics/AS-03-redemption-engine.md) §3, R-AS03-04 | [Data Arch §3.1](file:///d:/learn/loyalty/architecture/Data-Architecture-and-Schema.md), [ADR-001](file:///d:/learn/loyalty/architecture/adrs/ADR-001-module-boundaries-and-data-isolation.md) | [DD-03 §3.1, §4.1](file:///d:/learn/loyalty/design/DD-03-redemption-engine.md) | G01-03-003, G01-03-004, G01-03-009, AC-03-004 | G01-03-003, G03-01-005 |
| **Fulfillment & SLAs** | Digital, physical, account credit (≤ 1 business day for cash-back) | [FR-03-030](file:///d:/learn/loyalty/requirements/FR-03-redemption-engine.md)..034 | [AS-03](file:///d:/learn/loyalty/analytics/AS-03-redemption-engine.md) §3, R-AS03-02 | [Data Arch §3.3](file:///d:/learn/loyalty/architecture/Data-Architecture-and-Schema.md) | [DD-03 §4.1](file:///d:/learn/loyalty/design/DD-03-redemption-engine.md) | FLOW-04, G04-011 | G02-03-002 |
| **Failure & Reversal** | Auto-reversal on failure; original FIFO earn date & expiry restored | [FR-03-040](file:///d:/learn/loyalty/requirements/FR-03-redemption-engine.md)..044 | [AS-03](file:///d:/learn/loyalty/analytics/AS-03-redemption-engine.md) §3, R-AS03-05 | [Data Arch §3.1](file:///d:/learn/loyalty/architecture/Data-Architecture-and-Schema.md) | [DD-03 §3.2, §4.2](file:///d:/learn/loyalty/design/DD-03-redemption-engine.md) | G01-03-007, G01-03-008, AC-03-005, FLOW-05 | G02-03-003 |

---

### 2.4 Program Management

| Domain Concept / Business Rule | Concrete Value / Standard | Functional Req (FR) | Analytics Spec (AS) | Architecture & ADR | Detailed Design (DD) | Design Gate (QG-Design) | Architecture Gate (QG-Arch) |
|---|---|---|---|---|---|---|---|
| **Program Lifecycle & Versioning** | "BankRewards 2025"; prospective versioning (no retro changes) | [FR-04-001](file:///d:/learn/loyalty/requirements/FR-04-program-management.md)..006, NFR-04-004 | [AS-04](file:///d:/learn/loyalty/analytics/AS-04-program-management.md) §3, R-AS04-04 | [Data Arch §3.4](file:///d:/learn/loyalty/architecture/Data-Architecture-and-Schema.md), [ADR-001](file:///d:/learn/loyalty/architecture/adrs/ADR-001-module-boundaries-and-data-isolation.md) | [DD-04 §3.1, §4.1](file:///d:/learn/loyalty/design/DD-04-program-management.md) | G01-04-001, G01-04-002, G01-04-003, AC-04-001 | G01-03-004, G02-04-001 |
| **Campaign Configuration** | "Double Points August" (2×, Priority 1); priority conflict resolution | [FR-04-010](file:///d:/learn/loyalty/requirements/FR-04-program-management.md)..015 | [AS-04](file:///d:/learn/loyalty/analytics/AS-04-program-management.md) §3, R-AS04-02 | [Data Arch §3.4](file:///d:/learn/loyalty/architecture/Data-Architecture-and-Schema.md) | [DD-04 §3.2](file:///d:/learn/loyalty/design/DD-04-program-management.md) | G01-04-004, AC-04-003, FLOW-06 | G02-04-003 |
| **Rule Configuration** | Default earn rate (1pt/$1), tiers (0/1k/3k), grace (30d), min redeem (100pt) | [FR-04-020](file:///d:/learn/loyalty/requirements/FR-04-program-management.md)..024, NFR-04-001 | [AS-04](file:///d:/learn/loyalty/analytics/AS-04-program-management.md) §5 | [Data Arch §3.4](file:///d:/learn/loyalty/architecture/Data-Architecture-and-Schema.md) | [DD-04 §2](file:///d:/learn/loyalty/design/DD-04-program-management.md) | G01-04-001, FLOW-07 | G03-01-007, G03-02-004 |
| **Member Enrollment** | Multi-program enrollment supported; eligibility checks | [FR-04-030](file:///d:/learn/loyalty/requirements/FR-04-program-management.md)..034 | [AS-04](file:///d:/learn/loyalty/analytics/AS-04-program-management.md) §3, R-AS04-01 | [Data Arch §3.4](file:///d:/learn/loyalty/architecture/Data-Architecture-and-Schema.md) | [DD-04 §2](file:///d:/learn/loyalty/design/DD-04-program-management.md) | G01-04-005, AC-04-004 | G01-02-001 |
| **Partner Onboarding & API** | OAuth 2.0 Client Credentials; rate limit: 1,000 req/min default | [FR-04-040](file:///d:/learn/loyalty/requirements/FR-04-program-management.md)..044 | [AS-04](file:///d:/learn/loyalty/analytics/AS-04-program-management.md) §3, R-AS04-03 | [Security Arch §2](file:///d:/learn/loyalty/architecture/Security-and-Integration-Architecture.md) | [DD-04 §4.2](file:///d:/learn/loyalty/design/DD-04-program-management.md) | FLOW-08 | G01-02-003, G02-01-003, G02-01-004 |
| **Manual Balance Adjustment** | Mandatory reason code; dual-approval for high value; WORM audit | [FR-04-050](file:///d:/learn/loyalty/requirements/FR-04-program-management.md)..052 | [AS-04](file:///d:/learn/loyalty/analytics/AS-04-program-management.md) §3, R-AS04-05 | [Security Arch §4](file:///d:/learn/loyalty/architecture/Security-and-Integration-Architecture.md), [Data Arch §3.4](file:///d:/learn/loyalty/architecture/Data-Architecture-and-Schema.md) | [DD-04 §4.3](file:///d:/learn/loyalty/design/DD-04-program-management.md) | G01-04-006, G01-04-007, AC-04-005, FLOW-11 | G01-04-003, G03-04-003 |

---

### 2.5 Analytics & Reporting

| Domain Concept / Business Rule | Concrete Value / Standard | Functional Req (FR) | Analytics Spec (AS) | Architecture & ADR | Detailed Design (DD) | Design Gate (QG-Design) | Architecture Gate (QG-Arch) |
|---|---|---|---|---|---|---|---|
| **Point Liability Calculation** | `SUM(confirmed_unspent) × cost_per_point` ($0.01/pt) | [FR-05-030](file:///d:/learn/loyalty/requirements/FR-05-analytics-reporting.md)..033 | [AS-05](file:///d:/learn/loyalty/analytics/AS-05-analytics-reporting.md) §3.1, §3.2 | [Data Arch §4](file:///d:/learn/loyalty/architecture/Data-Architecture-and-Schema.md), [ADR-003](file:///d:/learn/loyalty/architecture/adrs/ADR-003-data-warehouse-and-analytics-isolation.md) | [DD-05 §3.1](file:///d:/learn/loyalty/design/DD-05-analytics-reporting.md) | AC-05-002, G04-007, G04-R-003 | — |
| **Breakage Rate Calculation** | `points expired / points issued × 100` | [FR-05-050](file:///d:/learn/loyalty/requirements/FR-05-analytics-reporting.md)..052 | [AS-05](file:///d:/learn/loyalty/analytics/AS-05-analytics-reporting.md) §3.1 | [Data Arch §4](file:///d:/learn/loyalty/architecture/Data-Architecture-and-Schema.md) | [DD-05 §3.2](file:///d:/learn/loyalty/design/DD-05-analytics-reporting.md) | AC-05-003, G04-005, G04-R-005, FLOW-12 | — |
| **Redemption Rate Calculation** | `points redeemed / points issued × 100` | [FR-05-020](file:///d:/learn/loyalty/requirements/FR-05-analytics-reporting.md)..022 | [AS-05](file:///d:/learn/loyalty/analytics/AS-05-analytics-reporting.md) §3.1 | [Data Arch §4](file:///d:/learn/loyalty/architecture/Data-Architecture-and-Schema.md) | [DD-05 §3.2](file:///d:/learn/loyalty/design/DD-05-analytics-reporting.md) | AC-05-006, G04-004, G04-R-002 | — |
| **Active Member Definition** | Active = ≥ 1 qualifying activity; Dormant = > 90 days inactive | [FR-05-060](file:///d:/learn/loyalty/requirements/FR-05-analytics-reporting.md)..063 | [AS-05](file:///d:/learn/loyalty/analytics/AS-05-analytics-reporting.md) §3.1 | [Data Arch §4](file:///d:/learn/loyalty/architecture/Data-Architecture-and-Schema.md) | [DD-05 §3.2](file:///d:/learn/loyalty/design/DD-05-analytics-reporting.md) | G04-006, G04-R-006 | — |
| **Real-Time Operational Dashboard** | 5-minute refresh; data staleness ≤ 10 min; liability alert | [FR-05-080](file:///d:/learn/loyalty/requirements/FR-05-analytics-reporting.md)..082, NFR-05-003 | [AS-05](file:///d:/learn/loyalty/analytics/AS-05-analytics-reporting.md) §4 R-AS05-05 | [Arch Overview §3](file:///d:/learn/loyalty/architecture/Architecture-Overview.md), [ADR-003](file:///d:/learn/loyalty/architecture/adrs/ADR-003-data-warehouse-and-analytics-isolation.md) | [DD-05 §4.3](file:///d:/learn/loyalty/design/DD-05-analytics-reporting.md) | G01-05-005, AC-05-005, FLOW-10, G04-014 | G02-05-001, G04-01-003 |
| **Report Generation Framework** | 7 standard reports; CSV/XLSX/PDF; RBAC isolation; DW separation | [FR-05-001](file:///d:/learn/loyalty/requirements/FR-05-analytics-reporting.md)..007, NFR-05-001 | [AS-05](file:///d:/learn/loyalty/analytics/AS-05-analytics-reporting.md) §2, §7 | [ADR-003](file:///d:/learn/loyalty/architecture/adrs/ADR-003-data-warehouse-and-analytics-isolation.md) | [DD-05 §4.2](file:///d:/learn/loyalty/design/DD-05-analytics-reporting.md) | G01-05-001..004, AC-05-001, G04-R-001..007 | G01-01-004, G02-05-002 |

---

## 3. Cross-Module End-to-End Flow Traceability

| Flow ID | Scenario Name | Primary Modules | Domain & FR References | Detailed Design Sequence | Target Quality Gate Assertion |
|---|---|---|---|---|---|
| **FLOW-01** | Standard Earn + Bonus | Core Banking, Earning, Program Mgmt | [FR-01-001](file:///d:/learn/loyalty/requirements/FR-01-earning-engine.md), [FR-01-011](file:///d:/learn/loyalty/requirements/FR-01-earning-engine.md), [FR-01-020](file:///d:/learn/loyalty/requirements/FR-01-earning-engine.md), [FR-04-010](file:///d:/learn/loyalty/requirements/FR-04-program-management.md) | [DD-01 §4.1](file:///d:/learn/loyalty/design/DD-01-earning-engine.md) | Quality-Gates-Design.md §FLOW-01, AC-01-002 |
| **FLOW-02** | Earn Triggers Tier Upgrade | Core Banking, Earning, Tiering | [FR-01-010](file:///d:/learn/loyalty/requirements/FR-01-earning-engine.md), [FR-02-002](file:///d:/learn/loyalty/requirements/FR-02-tiering-system.md), [FR-02-011](file:///d:/learn/loyalty/requirements/FR-02-tiering-system.md), [FR-02-040](file:///d:/learn/loyalty/requirements/FR-02-tiering-system.md) | [DD-02 §5.1](file:///d:/learn/loyalty/design/DD-02-tiering-system.md) | Quality-Gates-Design.md §FLOW-02, AC-02-002 |
| **FLOW-03** | End-of-Period Tier Downgrade with Grace | Tiering, Notification | [FR-02-020](file:///d:/learn/loyalty/requirements/FR-02-tiering-system.md), [FR-02-022](file:///d:/learn/loyalty/requirements/FR-02-tiering-system.md), [FR-02-030](file:///d:/learn/loyalty/requirements/FR-02-tiering-system.md), [FR-02-033](file:///d:/learn/loyalty/requirements/FR-02-tiering-system.md) | [DD-02 §5.2](file:///d:/learn/loyalty/design/DD-02-tiering-system.md) | Quality-Gates-Design.md §FLOW-03, AC-02-004, AC-02-005 |
| **FLOW-04** | Redemption with FIFO Point Consumption | Redemption, Earning Ledger, Fulfillment | [FR-03-010](file:///d:/learn/loyalty/requirements/FR-03-redemption-engine.md), [FR-03-020](file:///d:/learn/loyalty/requirements/FR-03-redemption-engine.md), [FR-03-021](file:///d:/learn/loyalty/requirements/FR-03-redemption-engine.md), [FR-03-030](file:///d:/learn/loyalty/requirements/FR-03-redemption-engine.md) | [DD-03 §4.1](file:///d:/learn/loyalty/design/DD-03-redemption-engine.md) | Quality-Gates-Design.md §FLOW-04, AC-03-001, AC-03-004 |
| **FLOW-05** | Fulfillment Failure → Auto Reversal | Redemption, Fulfillment Partner, Sổ cái (Earning Ledger) | [FR-03-031](file:///d:/learn/loyalty/requirements/FR-03-redemption-engine.md), [FR-03-040](file:///d:/learn/loyalty/requirements/FR-03-redemption-engine.md), [FR-03-041](file:///d:/learn/loyalty/requirements/FR-03-redemption-engine.md) | [DD-03 §4.2](file:///d:/learn/loyalty/design/DD-03-redemption-engine.md) | Quality-Gates-Design.md §FLOW-05, AC-03-005 |
| **FLOW-06** | Campaign Activation Mid-Day | Program Mgmt, Earning Engine | [FR-04-010](file:///d:/learn/loyalty/requirements/FR-04-program-management.md), [FR-04-013](file:///d:/learn/loyalty/requirements/FR-04-program-management.md), [FR-01-021](file:///d:/learn/loyalty/requirements/FR-01-earning-engine.md) | [DD-04 §3.2](file:///d:/learn/loyalty/design/DD-04-program-management.md) | Quality-Gates-Design.md §FLOW-06, G02-04-003 |
| **FLOW-07** | Earn Rule Version Change | Program Mgmt, Earning Engine | [FR-04-004](file:///d:/learn/loyalty/requirements/FR-04-program-management.md), [FR-04-005](file:///d:/learn/loyalty/requirements/FR-04-program-management.md), [FR-04-024](file:///d:/learn/loyalty/requirements/FR-04-program-management.md) | [DD-04 §4.1](file:///d:/learn/loyalty/design/DD-04-program-management.md) | Quality-Gates-Design.md §FLOW-07, AC-04-006, G02-04-001 |
| **FLOW-08** | Partner Earn Event | Partner API, Earning Engine, Sổ cái (Earning Ledger) | [FR-04-040](file:///d:/learn/loyalty/requirements/FR-04-program-management.md)..043, [FR-01-002](file:///d:/learn/loyalty/requirements/FR-01-earning-engine.md) | [DD-04 §4.2](file:///d:/learn/loyalty/design/DD-04-program-management.md) | Quality-Gates-Design.md §FLOW-08, G02-01-003, G02-01-004 |
| **FLOW-09** | Tier-Restricted Redemption Attempt | Member, Redemption, Tiering | [FR-03-002](file:///d:/learn/loyalty/requirements/FR-03-redemption-engine.md), [FR-03-011](file:///d:/learn/loyalty/requirements/FR-03-redemption-engine.md), [FR-02-042](file:///d:/learn/loyalty/requirements/FR-02-tiering-system.md) | [DD-03 §4.3](file:///d:/learn/loyalty/design/DD-03-redemption-engine.md) | Quality-Gates-Design.md §FLOW-09, AC-03-003 |
| **FLOW-10** | Analytics Aggregation Pipeline | Earning Engine, ETL, DW, Reports | [FR-05-001](file:///d:/learn/loyalty/requirements/FR-05-analytics-reporting.md), [FR-05-080](file:///d:/learn/loyalty/requirements/FR-05-analytics-reporting.md), NFR-05-003 | [DD-05 §4.1](file:///d:/learn/loyalty/design/DD-05-analytics-reporting.md) | Quality-Gates-Design.md §FLOW-10, G04-014, G02-05-001 |
| **FLOW-11** | Manual Balance Adjustment with Approval | Program Admin, Program Mgmt, Sổ cái (Earning Ledger) | [FR-04-050](file:///d:/learn/loyalty/requirements/FR-04-program-management.md)..052 | [DD-04 §4.3](file:///d:/learn/loyalty/design/DD-04-program-management.md) | Quality-Gates-Design.md §FLOW-11, AC-04-005, G01-04-006 |
| **FLOW-12** | Point Expiry and Breakage Capture | Earning (Expiry Job), Sổ cái (Earning Ledger), Analytics | [FR-01-051](file:///d:/learn/loyalty/requirements/FR-01-earning-engine.md), [FR-05-050](file:///d:/learn/loyalty/requirements/FR-05-analytics-reporting.md), [FR-05-051](file:///d:/learn/loyalty/requirements/FR-05-analytics-reporting.md) | [DD-01 §4.3](file:///d:/learn/loyalty/design/DD-01-earning-engine.md) | Quality-Gates-Design.md §FLOW-12, AC-01-006, AC-05-003 |

---

## 4. Summary Matrix

| Module | Requirements (FR) Count | Analytics (AS) Metrics | Architecture & Design Docs | Quality Gate Criteria (Design) | Quality Gate Criteria (Arch) |
|---|---|---|---|---|---|
| **Earning Engine** | 22 (FR-01-001..062, NFR-01-001..004) | 9 metrics, 4 reports | [Architecture Overview](file:///d:/learn/loyalty/architecture/Architecture-Overview.md), [Data Arch §3.1](file:///d:/learn/loyalty/architecture/Data-Architecture-and-Schema.md), [ADR-002](file:///d:/learn/loyalty/architecture/adrs/ADR-002-event-driven-earn-ingestion-and-idempotency.md), [DD-01](file:///d:/learn/loyalty/design/DD-01-earning-engine.md) | 10 business rules (G01-01), 6 ACs | 4 integration, 4 perf/durability |
| **Tiering System** | 21 (FR-02-001..061, NFR-02-001..003) | 10 metrics, 5 reports | [Architecture Overview](file:///d:/learn/loyalty/architecture/Architecture-Overview.md), [Data Arch §3.2](file:///d:/learn/loyalty/architecture/Data-Architecture-and-Schema.md), [DD-02](file:///d:/learn/loyalty/design/DD-02-tiering-system.md) | 8 business rules (G01-02), 6 ACs | 3 integration, 3 batch/perf |
| **Redemption Engine** | 22 (FR-03-001..051, NFR-03-001..004) | 10 metrics, 5 reports | [Architecture Overview](file:///d:/learn/loyalty/architecture/Architecture-Overview.md), [Data Arch §3.3](file:///d:/learn/loyalty/architecture/Data-Architecture-and-Schema.md), [DD-03](file:///d:/learn/loyalty/design/DD-03-redemption-engine.md) | 9 business rules (G01-03), 6 ACs | 3 integration, 3 concurrency |
| **Program Management** | 22 (FR-04-001..052, NFR-04-001..004) | 11 metrics, 5 reports | [Security Arch](file:///d:/learn/loyalty/architecture/Security-and-Integration-Architecture.md), [Data Arch §3.4](file:///d:/learn/loyalty/architecture/Data-Architecture-and-Schema.md), [ADR-001](file:///d:/learn/loyalty/architecture/adrs/ADR-001-module-boundaries-and-data-isolation.md), [DD-04](file:///d:/learn/loyalty/design/DD-04-program-management.md) | 7 business rules (G01-04), 6 ACs | 3 integration, 3 security/audit |
| **Analytics & Reporting** | 26 (FR-05-001..082, NFR-05-001..005) | 9 core KPIs, 5 reports | [ADR-003](file:///d:/learn/loyalty/architecture/adrs/ADR-003-data-warehouse-and-analytics-isolation.md), [Data Arch §4](file:///d:/learn/loyalty/architecture/Data-Architecture-and-Schema.md), [DD-05](file:///d:/learn/loyalty/design/DD-05-analytics-reporting.md) | 5 business rules (G01-05), 6 ACs, 21 accuracy | 3 pipeline, 4 observability |
| **Total** | **113 Requirements** | **49 Metrics, 24 Reports** | **11 Architecture & Design Docs** | **103 Quality Gate Criteria** | **64 Architecture Gate Criteria** |

---

## 5. Lab 2 After Pass: G1-G6 Register and Template Trace

This section is the trainee-pack Lab 2 after-pass register. It keeps the existing module traceability and extended engineering gate documents, but uses only `G1` to `G6` from `template/list.md` for trainee pack review.

### 5.1 Requirements List

| Requirement ID | Requirement | Traces to |
|---|---|---|
| REQ-LB-01 | Loyalty Banking Platform shall consume settled transaction events and process earn points within the 60-second settlement SLA. | Goal, Outcome, I-5 step 1, CON.1 |
| REQ-LB-02 | Earning Engine Service shall prevent duplicate point postings for the same source transaction before writing to the earning ledger. | CON.1, `PointTransaction` state, UC-LB-01 |
| REQ-LB-03 | Service-owned databases shall only be modified through their owning service APIs, events, or internal persistence logic. | CON.2, I-7, I-9 forbidden path |
| REQ-LB-04 | Redemption Engine Service shall validate member tier, available balance, and minimum redemption rules before reserving FIFO point batches. | I-5 step 6, `RedemptionOrder` PENDING -> IN_PROGRESS |
| REQ-LB-05 | Fulfillment failure shall trigger automatic reversal and restore points with their original FIFO earn date and expiry schedule. | CON.3, `RedemptionOrder` FAILED -> REVERSED |
| REQ-LB-06 | Tiering System Service shall update member tier when QP thresholds are reached and publish tier change events to downstream services. | I-5 step 4, UC-LB-03 |
| REQ-LB-07 | Analytics & Reporting Service shall compute liability and reporting KPIs from warehouse data, not from transactional service databases. | Outcome, CON.2, I-7 |
| REQ-LB-08 | C4 Container names, ArchiMate Application Component names, UML lifeline names, and test SUT names shall use the Lab 1 name-identity index. | Guide name identity, G3, G6 |

### 5.2 Analysis

**As-is**: Loyalty work was described in detailed domain, requirement, architecture, and design files, but review constraints were spread across several documents and extended gate sets.

**To-be**: The modeling pack is anchored to the Lab 1 name-identity index in `loyalty.md`, uses ArchiMate for enterprise alignment, C4 for solution decomposition, UML for behavior/test design, and uses `G1` to `G6` as the bounded review gate set.

| Capability | Evidence |
|---|---|
| Real-time earning and idempotent ledger posting | `FR-01`, `DD-01`, `ADR-002` |
| Automated tier lifecycle | `FR-02`, `DD-02`, `entity-lifecycle-models.md` |
| FIFO redemption and failure compensation | `FR-03`, `DD-03`, `CON.3` |
| Governed program and campaign configuration | `FR-04`, `DD-04` |
| Isolated analytics and liability reporting | `FR-05`, `AS-05`, `ADR-003` |

### 5.3 Exception Paths

| Exception ID | Trigger | Related constraint | Modeled response |
|---|---|---|---|
| EX-LB-01 | Duplicate earn event received | CON.1 | Earning Engine Service rejects or returns original result without a second ledger entry |
| EX-LB-02 | External/channel attempts direct database write | CON.2 | Path is forbidden; request must route through owning service/API/event |
| EX-LB-03 | Partner fulfillment fails after debit reservation | CON.3 | Redemption Engine Service marks order failed and executes auto-reversal |
| EX-LB-04 | Tier-restricted reward requested by ineligible member | CON.2 | Redemption Engine Service rejects before FIFO debit |

### 5.4 Gate Register: G1 to G6

| Gate | Pass rule for Loyalty Banking | Evidence artifact | Pass? |
|---|---|---|---|
| G1 Strategy signed | Goal, outcome, and constraints listed | `loyalty.md` I-1 and I-10; `architecture/archimate/motivation-layer.md`; `architecture/archimate/strategy-layer.md` | Pass |
| G2 Process + states | Happy path and named states match the state view | `loyalty.md` I-5 and I-6; `design/entity-lifecycle-models.md`; `design/DD-03-redemption-engine.md` | Pass |
| G3 C4 Context + Container | No unnamed externals; sync/async labeled; names match Input index | `architecture/Architecture-Overview.md` sections 2 and 3; `loyalty.md` I-2 to I-4 and I-8 | Pass |
| G4 Contracts | Contract or equivalent register exists for every Container relationship | `loyalty-platform-impl/README.md` Lab 3 Contract Register; `architecture/domain-event-catalog.md` | Pass with register |
| G5 Critical exception path | Critical failure path has compensating action | `loyalty-platform-impl/README.md` Lab 3 Exception Spec; `design/DD-03-redemption-engine.md` fulfillment failure sequence | Pass |
| G6 Test coverage | All state transitions and sequence alternatives are mapped to planned tests; participants are C4 names | `loyalty-platform-impl/README.md` Lab 3 Test Spec; implementation tests under `loyalty-platform-impl/*/src/test` | Pass with planned coverage |

### 5.5 Template Trace Table

| Requirement ID | Process step | CON.* | Named object/state |
|---|---|---|---|
| REQ-LB-01 | I-5 step 1 -> step 2 | CON.1 | `PointTransaction` created |
| REQ-LB-02 | I-5 step 2 | CON.1 | `PointTransaction` duplicate avoided |
| REQ-LB-03 | I-5 all service-owned writes | CON.2 | Source-of-truth objects in I-7 |
| REQ-LB-04 | I-5 step 6 | CON.2 | `RedemptionOrder`: PENDING -> IN_PROGRESS |
| REQ-LB-05 | I-5 step 7 | CON.3 | `RedemptionOrder`: FAILED -> REVERSED |
| REQ-LB-06 | I-5 step 4 | CON.2 | `MemberTier` update |
| REQ-LB-07 | I-5 step 8 | CON.2 | `FactPointTransaction` in Data Warehouse - Star Schema |
| REQ-LB-08 | All diagrams and tests | CON.2 | UML lifeline and SUT names match I-4 |

*Generated from: [loyalty_domain.md](file:///d:/learn/loyalty/loyalty_domain.md) (v1.1) · [FR-01..05](file:///d:/learn/loyalty/requirements/) (v1.1) · [AS-01..05](file:///d:/learn/loyalty/analytics/) (v1.1) · [Architecture](file:///d:/learn/loyalty/architecture/) (v1.0) · [Detailed Design](file:///d:/learn/loyalty/design/) (v1.0) · [Quality Gates](file:///d:/learn/loyalty/quality-gates/) (v1.1)*
