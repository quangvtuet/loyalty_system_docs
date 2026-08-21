# Quality Gates — Architecture

> **Not a gate for this pack — outside the modeling pack.**
> The only quality gates for this pack are **G1-G6**, adopted in [`../lab7-adoption.md`](../lab7-adoption.md).
> This document was written before the pack was re-sequenced. It does not gate, block, or sign off anything, and it still uses the old container names and the old constraint set.

**Domain**: Loyalty Banking
**Version**: 1.1
**Date**: 2026-08-14
**Source**: [loyalty_domain.md](file:///d:/learn/loyalty/loyalty_domain.md) | FR-01..05 | AS-01..05

---

## Overview

This document defines **architecture-level quality gates** for the Loyalty Banking platform. Each gate is a structured set of pass/fail criteria that must be satisfied before the system progresses to the next delivery stage. Gates are evaluated by the Solution Architect, Tech Lead, DevOps, and Security teams.

### Gate Lifecycle

```
ARCH-GATE-01          ARCH-GATE-02              ARCH-GATE-03          ARCH-GATE-04
Architecture    ───►  Component Integration ──►  Non-Functional  ───►  Production
Design Approval       Readiness                   Readiness             Readiness
(Design Phase)        (Dev Complete)              (Pre-Production)      (Go-Live)
```

### Gate Status Legend

| Symbol | Meaning |
|--------|---------|
| ✅ PASS | Criterion met; evidence attached |
| ❌ FAIL | Criterion not met; blocker raised |
| ⚠️ WAIVER | Accepted risk; signed off by Architecture Lead |
| 🔲 PENDING | Not yet evaluated |

---

## ARCH-GATE-01: Architecture Design Approval

**Stage**: Design Phase
**Evaluators**: Solution Architect, Tech Lead
**Trigger**: Before development begins on any module
**Outcome**: Architecture blueprint approved for implementation

> [!IMPORTANT]
> No development sprint may begin until all ARCH-GATE-01 criteria are **PASS** or **WAIVER**.

---

### G01-01: System Context & Module Boundaries

| ID | Criterion | Evidence Required | Status |
|----|-----------|------------------|--------|
| G01-01-001 | A system context diagram exists showing all 5 modules (Earning, Tiering, Redemption, Program Mgmt, Analytics) and their external integration points (Core Banking, Partner Systems, CRM, Data Warehouse) | System Context Diagram v1.0 | 🔲 |
| G01-01-002 | Module boundary decisions are documented: each module owns its data store; no cross-module direct DB access is permitted | Architecture Decision Record (ADR-001) | 🔲 |
| G01-01-003 | The event-driven integration pattern for earn event ingestion is documented (async message queue vs. synchronous API) | ADR-002: Event Ingestion Pattern | 🔲 |
| G01-01-004 | The read-replica / Data Warehouse separation for Analytics (FR-05 / NFR-05-005) is documented with topology diagram | ADR-003: Reporting Isolation | 🔲 |

---

### G01-02: API Contract Design

| ID | Criterion | Evidence Required | Status |
|----|-----------|------------------|--------|
| G01-02-001 | OpenAPI 3.x specification exists for all synchronous REST/HTTP APIs: Earn Rule Config, Member Enrollment, Redemption Request, Catalog, Report Generation | OpenAPI specs per module | 🔲 |
| G01-02-002 | AsyncAPI specification exists for event-driven interfaces: Earn Event feed from Core Banking, Partner Earn Event, Tier Change Event, Point Expiry Event | AsyncAPI specs | 🔲 |
| G01-02-003 | Partner API authentication scheme is defined: OAuth 2.0 client credentials flow (FR-04-042), default rate limit: 1,000 req/min per partner | Partner API Auth Design Doc | 🔲 |
| G01-02-004 | All APIs have versioning strategy documented (URI versioning: `/v1/`, `/v2/`) | API Versioning ADR | 🔲 |
| G01-02-005 | Error response schemas are standardized across all APIs (error code, message, correlation ID) | API Error Standard Doc | 🔲 |

---

### G01-03: Data Model Review

| ID | Criterion | Evidence Required | Status |
|----|-----------|------------------|--------|
| G01-03-001 | Entity-Relationship (ER) diagram approved for core entities: `Member`, `LoyaltyProgram`, `EarnRule`, `PointTransaction`, `MemberTier`, `TierRule`, `RedemptionOrder`, `RewardItem`, `FulfillmentRecord`, `Campaign`, `Partner`, `Enrollment` | ER Diagram v1.0 | 🔲 |
| G01-03-002 | Point ledger is designed as an **append-only immutable Sổ cái (Earning Ledger)** — no UPDATE/DELETE on point transactions | Data Model ADR | 🔲 |
| G01-03-003 | FIFO ordering is enforceable by the data model: `PointTransaction` has `earn_date` and `expiry_date` indexed for efficient FIFO consumption (FR-01-053, FR-03-021) | Index Design Doc | 🔲 |
| G01-03-004 | Configuration versioning is modeled: all rule tables (`EarnRule`, `TierRule`, `RedemptionRule`) have `valid_from`, `valid_to`, and `version` columns | Data Model | 🔲 |
| G01-03-005 | QP ledger is separate from the redeemable point ledger (FR-02-001) — no shared table | Data Model | 🔲 |
| G01-03-006 | Audit log tables (`config_version_log`, `manual_adjustment_log`, `tier_evaluation_log`) are designed as WORM-equivalent (write-once, no delete) | Data Model ADR | 🔲 |

---

### G01-04: Security Design

| ID | Criterion | Evidence Required | Status |
|----|-----------|------------------|--------|
| G01-04-001 | RBAC model defined for all roles: Program Admin, Partner Manager, Member, Finance, Auditor, Support Agent (FR-04 §2, FR-05 §2) | RBAC Design Matrix | 🔲 |
| G01-04-002 | PII data fields identified across all entities; encryption-at-rest and encryption-in-transit specified | Data Classification Register | 🔲 |
| G01-04-003 | Manual adjustment approval workflow design includes dual-control for high-value adjustments (FR-04-051) | Approval Workflow Design | 🔲 |
| G01-04-004 | Threat model completed for: earn event ingestion, partner API, redemption endpoint | Threat Model Doc | 🔲 |

---

## ARCH-GATE-02: Component Integration Readiness

**Stage**: Development Complete
**Evaluators**: Tech Lead, DevOps, QA Lead
**Trigger**: All 5 modules coded and deployed to integration environment
**Outcome**: Cross-module data flows verified; external integrations confirmed operational

> [!IMPORTANT]
> All ARCH-GATE-02 criteria must be **PASS** before end-to-end integration testing begins.

---

### G02-01: Earn Event Pipeline Integration

| ID | Criterion | Source | Evidence Required | Status |
|----|-----------|--------|------------------|--------|
| G02-01-001 | Core Banking transaction event feed is connected to Earning Engine in the integration environment; test events flow through | NFR-01-001 | Event trace log sample | 🔲 |
| G02-01-002 | Earning Engine acknowledges and processes a settled transaction event within **≤ 60 seconds** of emission **[SLA: 60 s]** | FR-01-001 | Latency measurement (p95) | 🔲 |
| G02-01-003 | Partner Earn API endpoint is deployed; at least one partner integration test completes a round-trip earn event | FR-04-041 | Integration test report | 🔲 |
| G02-01-004 | OAuth 2.0 client credentials authentication is enforced on the Partner API; unauthenticated requests return HTTP 401 | FR-04-042 | Security test log | 🔲 |

---

### G02-02: Tiering Integration

| ID | Criterion | Source | Evidence Required | Status |
|----|-----------|--------|------------------|--------|
| G02-02-001 | Tiering System reads QP accrual events from the Earning Engine without polling (event-driven) | FR-02-002 | Event subscription config | 🔲 |
| G02-02-002 | Tier status API responds to Earning Engine and Redemption Engine requests within 50ms (p99) in integration environment | NFR-02-003 | Load test result | 🔲 |
| G02-02-003 | A Tier Change Event is emitted by Tiering System and consumed by Earning Engine (to apply new earn multiplier) and Redemption Engine (for catalog eligibility) | FR-02-043 | Event trace log | 🔲 |

---

### G02-03: Redemption Engine Integration

| ID | Criterion | Source | Evidence Required | Status |
|----|-----------|--------|------------------|--------|
| G02-03-001 | Redemption Engine reads confirmed point balance from Earning Engine Sổ cái (Earning Ledger) API; balance is consistent under concurrent read/write scenarios | FR-03-012 | Concurrency test log | 🔲 |
| G02-03-002 | Fulfillment partner (at least one) is integrated; a test redemption reaches `FULFILLED` status end-to-end | FR-03-030 | Integration test result | 🔲 |
| G02-03-003 | Fulfillment failure callback triggers automatic reversal; points are re-credited to member balance | FR-03-040 | Reversal test trace | 🔲 |

---

### G02-04: Program Management Integration

| ID | Criterion | Source | Evidence Required | Status |
|----|-----------|--------|------------------|--------|
| G02-04-001 | A new EarnRule activated in Program Mgmt is applied by the Earning Engine within 30 seconds | FR-04-024 | Config propagation test | 🔲 |
| G02-04-002 | A TierRule change does not retroactively alter any existing MemberTier record | FR-04-005 | Pre/post-change assertion | 🔲 |
| G02-04-003 | A Campaign activated in Program Mgmt is applied to the next earn event by the Earning Engine | FR-04-010 | Campaign activation test | 🔲 |

---

### G02-05: Analytics Pipeline Integration

| ID | Criterion | Source | Evidence Required | Status |
|----|-----------|--------|------------------|--------|
| G02-05-001 | ETL/streaming pipeline is operational: a committed `point_transaction` record appears in the Data Warehouse within 10 minutes | NFR-05-003 | Pipeline lag measurement | 🔲 |
| G02-05-002 | All 7 standard reports can be generated from the DW without errors | FR-05-001 | Report generation test log | 🔲 |
| G02-05-003 | Report generation does not degrade transactional API response times by more than 1% under combined load | NFR-05-005 | Load test with concurrent reporting | 🔲 |

---

## ARCH-GATE-03: Non-Functional Readiness

**Stage**: Pre-Production
**Evaluators**: Tech Lead, DevOps, Performance Engineer
**Trigger**: All integrations passed (ARCH-GATE-02); system deployed to pre-production environment
**Outcome**: System certified to meet all NFR targets under production-representative load

> [!IMPORTANT]
> All ARCH-GATE-03 criteria must be **PASS** before production go-live approval.

---

### G03-01: Performance & Throughput

| ID | Criterion | Target | Source | Test Method | Status |
|----|-----------|--------|--------|------------|--------|
| G03-01-001 | Earn event throughput | ≥ 500 events/sec sustained for 30 min | NFR-01-002 | Load test: ramp to 500 RPS; hold 30 min | 🔲 |
| G03-01-002 | Earn event end-to-end latency | p95 ≤ 2,000ms | NFR-01-001 | Load test latency percentiles | 🔲 |
| G03-01-003 | Tier status read latency | p99 ≤ 50ms | NFR-02-003 | Read-only load test on Tier API | 🔲 |
| G03-01-004 | Real-time tier upgrade evaluation | ≤ 500ms per member | NFR-02-001 | Triggered upgrade under load | 🔲 |
| G03-01-005 | Redemption request processing | p95 ≤ 3,000ms (balance check + debit) | NFR-03-001 | Redemption load test | 🔲 |
| G03-01-006 | Catalog search API latency | p95 ≤ 500ms | NFR-03-002 | Catalog read load test | 🔲 |
| G03-01-007 | Program config read latency | p95 ≤ 200ms | NFR-04-001 | Config read load test | 🔲 |
| G03-01-008 | Standard report generation (≤ 100K rows) | ≤ 30 seconds | NFR-05-001 | Report gen benchmark | 🔲 |
| G03-01-009 | Concurrent admin sessions | ≥ 100 concurrent without degradation | NFR-04-002 | Admin session load test | 🔲 |
| G03-01-010 | Concurrent redemption requests | ≥ 200 concurrent without conflict errors | NFR-03-004 | Concurrent redemption test | 🔲 |

---

### G03-02: Durability & Data Integrity

| ID | Criterion | Target | Source | Test Method | Status |
|----|-----------|--------|--------|------------|--------|
| G03-02-001 | Sổ cái (Earning Ledger) durability | Zero confirmed point transactions lost under simulated node failure | NFR-01-003 | Chaos: kill primary DB node mid-write; verify recovery | 🔲 |
| G03-02-002 | Idempotency guarantee | Zero duplicate point credits under 10,000 duplicate event injection | NFR-01-004 | Duplicate event stress test | 🔲 |
| G03-02-003 | Redemption atomicity | No partial debits survive: kill process mid-debit; verify rollback | NFR-03-003 | Chaos: kill Redemption service mid-debit | 🔲 |
| G03-02-004 | Config change durability | Config write acknowledged only after durable commit | NFR-04-003 | Kill config service after write; verify on recovery | 🔲 |
| G03-02-005 | FIFO correctness | When 3 point batches exist with different earn dates; oldest is always consumed first | FR-01-053, FR-03-021 | FIFO unit + integration test | 🔲 |

---

### G03-03: Tier Batch Evaluation Scalability

| ID | Criterion | Target | Source | Test Method | Status |
|----|-----------|--------|--------|------------|--------|
| G03-03-001 | Tier batch job completes processing of 1M members within 4-hour window | ≤ 4 hours | NFR-02-002 | Batch simulation with 1M synthetic member records | 🔲 |
| G03-03-002 | Batch job is idempotent: re-running after partial failure produces same outcome | 0 duplicate tier events | FR-02-020 | Kill batch at 50%; re-run; compare output | 🔲 |

---

### G03-04: Security & Compliance

| ID | Criterion | Source | Evidence Required | Status |
|----|-----------|--------|------------------|--------|
| G03-04-001 | Penetration test completed on Partner API, Redemption API, and Admin Config API; all Critical/High findings remediated | FR-04-042 | Pentest Report | 🔲 |
| G03-04-002 | RBAC enforcement verified: each role can only access permitted endpoints | FR-05-006 | RBAC test matrix results | 🔲 |
| G03-04-003 | Audit log tamper-proofing verified: no DELETE or UPDATE permitted on audit tables | FR-04-052 | DB permission audit | 🔲 |
| G03-04-004 | All PII fields encrypted at rest (AES-256) and in transit (TLS 1.2+) | G01-04-002 | Encryption config review | 🔲 |
| G03-04-005 | Data retention policy implemented: audit/report data retained for ≥ 7 years | NFR-04-004, NFR-05-004 | Storage policy config | 🔲 |

---

## ARCH-GATE-04: Production Readiness

**Stage**: Go-Live
**Evaluators**: DevOps, SRE, Architecture Lead, CISO
**Trigger**: All ARCH-GATE-03 criteria PASS; stakeholder sign-off received
**Outcome**: System approved for production traffic

> [!CAUTION]
> Production deployment is **blocked** if any ARCH-GATE-04 criterion is **FAIL** without an approved waiver.

---

### G04-01: Observability

| ID | Criterion | Source | Evidence Required | Status |
|----|-----------|--------|------------------|--------|
| G04-01-001 | All 5 modules emit structured logs (JSON) with correlation ID, module name, and severity | AS-01..05 Alert tables | Log sample from each module | 🔲 |
| G04-01-002 | Distributed tracing is enabled: a single earn event can be traced end-to-end across Earning → Tiering → Sổ cái (Earning Ledger) | NFR-01-001 | Trace sample screenshot | 🔲 |
| G04-01-003 | Metrics exported to monitoring platform (e.g., Prometheus/Grafana): earn event rate, earn latency p95, tier upgrade count, redemption rate, DW pipeline lag | AS-01..05 | Dashboard screenshot | 🔲 |
| G04-01-004 | All High-severity alerts from AS-01..05 are configured in alerting platform with correct thresholds and routing | AS-01..05 Alert Tables | Alert config export | 🔲 |

---

### G04-02: Runbooks & Incident Response

| ID | Criterion | Source | Evidence Required | Status |
|----|-----------|--------|------------------|--------|
| G04-02-001 | Runbook written for each High-severity alert: "Earn Latency Breach", "Zero Earn Events", "High Reversal Rate", "Liability Ceiling Breach", "Batch Evaluation Failure" | AS-01..05 Alert Tables | Runbook links | 🔲 |
| G04-02-002 | On-call rotation established and documented; pager configured | All modules | On-call schedule | 🔲 |
| G04-02-003 | Incident severity levels defined and communicated to all teams | All modules | Incident Response Plan | 🔲 |

---

### G04-03: Disaster Recovery

| ID | Criterion | Source | Evidence Required | Status |
|----|-----------|--------|------------------|--------|
| G04-03-001 | Sổ cái (Earning Ledger) DB failover tested: primary failure → replica promoted → earn events resume within RTO ≤ 5 minutes | NFR-01-003 | DR test report | 🔲 |
| G04-03-002 | Earning Engine restart tested: all in-flight events reprocessed without duplication after crash recovery | NFR-01-004 | Chaos test report | 🔲 |
| G04-03-003 | Data Warehouse recovery tested: pipeline replay from last checkpoint within RPO ≤ 10 minutes | NFR-05-003 | DW DR test report | 🔲 |
| G04-03-004 | Backup schedule verified: point ledger and config DB backed up every 6 hours; backups tested restorable | NFR-04-003 | Backup verification log | 🔲 |

---

### G04-04: Deployment & Release

| ID | Criterion | Source | Evidence Required | Status |
|----|-----------|--------|------------------|--------|
| G04-04-001 | CI/CD pipeline verified: automated tests (unit + integration) pass on every commit to main branch | All modules | CI/CD pipeline screenshot | 🔲 |
| G04-04-002 | Database migration scripts are idempotent and tested on a production-clone database | G01-03 | Migration test log | 🔲 |
| G04-04-003 | Rollback procedure documented and tested: rollback from v1.x to prior version within 15 minutes without data loss | All modules | Rollback test report | 🔲 |
| G04-04-004 | Feature flags configured for phased rollout (e.g., gradual Partner API enablement) | FR-04-041 | Feature flag config | 🔲 |

---

## Gate Summary Dashboard

| Gate | Stage | Criteria Count | PASS | FAIL | WAIVER | PENDING |
|------|-------|---------------|------|------|--------|---------|
| ARCH-GATE-01 | Design Approval | 18 | — | — | — | 18 |
| ARCH-GATE-02 | Integration Readiness | 13 | — | — | — | 13 |
| ARCH-GATE-03 | NFR Readiness | 19 | — | — | — | 19 |
| ARCH-GATE-04 | Production Readiness | 14 | — | — | — | 14 |
| **Total** | | **64** | | | | **64** |

---

## Traceability Index

| Gate Criterion | FR Source | NFR Source | AS Source |
|----------------|-----------|-----------|-----------|
| G01-03-002 (Immutable ledger) | FR-01-060 | — | — |
| G01-03-003 (FIFO index) | FR-01-053, FR-03-021 | — | — |
| G02-01-002 (60-sec processing) | FR-01-001 | NFR-01-001 | — |
| G02-05-001 (DW lag ≤ 10 min) | FR-05-080 | NFR-05-003, NFR-05-005 | AS-05 §3 |
| G03-01-001 (500 events/sec) | — | NFR-01-002 | AS-01 R-AS01-03 |
| G03-01-002 (p95 ≤ 2,000ms) | — | NFR-01-001 | AS-01 Alerting |
| G03-02-002 (Idempotency) | FR-01-040..042 | NFR-01-004 | — |
| G03-02-005 (FIFO correctness) | FR-01-053, FR-03-021 | — | AS-03 R-AS03-04 |
| G03-03-001 (Batch 4-hour window) | — | NFR-02-002 | AS-02 R-AS02-05 |
| G03-04-005 (7-year retention) | — | NFR-04-004, NFR-05-004 | AS-05 §7 |
| G04-01-004 (All alerts configured) | — | — | AS-01..05 Alert Tables |

---

*Source documents: [loyalty_domain.md](file:///d:/learn/loyalty/loyalty_domain.md) · [FR-01](file:///d:/learn/loyalty/requirements/FR-01-earning-engine.md) · [FR-02](file:///d:/learn/loyalty/requirements/FR-02-tiering-system.md) · [FR-03](file:///d:/learn/loyalty/requirements/FR-03-redemption-engine.md) · [FR-04](file:///d:/learn/loyalty/requirements/FR-04-program-management.md) · [FR-05](file:///d:/learn/loyalty/requirements/FR-05-analytics-reporting.md) · [AS-01](file:///d:/learn/loyalty/analytics/AS-01-earning-engine.md) · [AS-05](file:///d:/learn/loyalty/analytics/AS-05-analytics-reporting.md)*
