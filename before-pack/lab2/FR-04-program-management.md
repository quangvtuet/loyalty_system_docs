# FR-04: Program Management — Functional Requirements

**Module**: Program Management
**Version**: 1.1
**Date**: 2026-08-14
**Source**: [loyalty_domain.md](file:///d:/learn/loyalty/loyalty_domain.md)

---

## 1. Overview

The Program Management module is the **foundational configuration layer** of the Loyalty Banking platform. It governs the lifecycle of loyalty programs (e.g., **"BankRewards 2025"**), campaigns (e.g., **"Double Points August"** — 2× earn multiplier, 1 Aug–31 Aug), rules, partner integrations, and member enrollments. All other modules (Earning, Tiering, Redemption, Analytics) depend on entities configured here.

---

## 2. Actors / Roles

| Actor | Description |
|-------|-------------|
| **Program Admin** | Bank staff who configure and manage loyalty programs and campaigns |
| **Partner Manager** | Staff who onboard and manage third-party earn/redeem partners |
| **Member** | Customer enrolled in one or more loyalty programs |
| **System** | Automated processes (rule evaluation, versioning, notifications) |
| **Auditor** | Read-only access to configuration history and adjustment logs |

---

## 3. Use Cases

| UC ID | Use Case | Primary Actor |
|-------|----------|---------------|
| UC-04-01 | Create a new loyalty program | Program Admin |
| UC-04-02 | Activate / deactivate a program | Program Admin |
| UC-04-03 | Create a campaign | Program Admin |
| UC-04-04 | Configure earn / tier / redemption rules | Program Admin |
| UC-04-05 | Enroll a member into a program | Member / System |
| UC-04-06 | Onboard a partner | Partner Manager |
| UC-04-07 | Manually adjust a member's point balance | Program Admin |
| UC-04-08 | View configuration version history | Auditor / Admin |

---

## 4. Functional Requirements

### 4.1 Loyalty Program Lifecycle

| ID | Requirement | Priority | Source Rule |
|----|-------------|----------|-------------|
| FR-04-001 | The system **SHALL** allow a Program Admin to create a loyalty program with: name, loyalty currency name, start date, end date, terms & conditions, and status. | Must | Domain §4 |
| FR-04-002 | The system **SHALL** support program statuses: `DRAFT`, `ACTIVE`, `SUSPENDED`, `DEACTIVATED`. | Must | Domain §4 |
| FR-04-003 | The system **SHALL** prevent a program from being deleted if it has enrolled members or active point balances. Deactivation must be used instead. | Must | Domain §4 |
| FR-04-004 | The system **SHALL** version every change to a program's configuration, recording: changed-by, changed-at, and the previous value. | Must | BR: Program changes are versioned |
| FR-04-005 | The system **SHALL** ensure that configuration changes do **not** retroactively alter existing member point balances or tier statuses. | Must | BR: Versioning |
| FR-04-006 | The system **SHOULD** allow a Program Admin to clone an existing program as a starting template for a new program. | Should | Domain §4 |

---

### 4.2 Campaign Management

| ID | Requirement | Priority | Source Rule |
|----|-------------|----------|-------------|
| FR-04-010 | The system **SHALL** allow creation of a campaign linked to one or more loyalty programs, with: name, start date/time, end date/time, rule type (earn multiplier / flat bonus / redemption discount), and priority value. | Must | Domain §4 |
| FR-04-011 | The system **SHALL** assign a numeric priority to each campaign; lower value = higher priority. | Must | BR: Campaigns must define priority |
| FR-04-012 | The system **SHALL** resolve conflicts when multiple campaigns apply to the same event using the priority field (highest priority campaign wins, unless stacking is enabled per program config). | Must | BR: Campaign priority conflict resolution |
| FR-04-013 | The system **SHALL** automatically deactivate campaigns past their end date/time. | Must | Domain §4 |
| FR-04-014 | The system **SHALL** allow preview of a campaign's effect on a simulated earn/redeem event before activation. | Should | Domain §4 |
| FR-04-015 | The system **COULD** support A/B campaign targeting by member segment. | Could | Domain §4 |

---

### 4.3 Rule Configuration

| ID | Requirement | Priority | Source Rule |
|----|-------------|----------|-------------|
| FR-04-020 | The system **SHALL** allow configuration of `EarnRule` records specifying: earn rate (points per $), eligible transaction types, eligible channels, and validity dates. **Default earn rate: 1 point per $1 spent.** | Must | Domain §4 |
| FR-04-021 | The system **SHALL** allow configuration of `TierRule` records specifying: tier name, QP threshold, tier period type (calendar year / rolling), evaluation frequency, and grace period (days). **Default thresholds: Silver: 0 QP, Gold: 1,000 QP, Platinum: 3,000 QP. Default grace period: 30 days. Default tier period: calendar year (1 Jan – 31 Dec).** | Must | Domain §4 |
| FR-04-022 | The system **SHALL** allow configuration of `RedemptionRule` records specifying: redemption rate, minimum points per redemption, eligible reward categories, and tier restrictions. **Default redemption rate: 100 points = $1. Default minimum redemption: 100 points.** | Must | Domain §4 |
| FR-04-023 | The system **SHALL** validate rule configurations for logical consistency (e.g., tier thresholds must be strictly ascending). | Must | Domain §4 |
| FR-04-024 | The system **SHALL** apply rules in a versioned manner — new rule versions take effect only from their activation date forward. | Must | BR: Rule versioning |

---

### 4.4 Member Enrollment

| ID | Requirement | Priority | Source Rule |
|----|-------------|----------|-------------|
| FR-04-030 | The system **SHALL** allow a member to be enrolled in a loyalty program, recording: member ID, program ID, enrollment date, status, and eligibility check results. | Must | Domain §4 |
| FR-04-031 | The system **SHALL** support enrollment statuses: `PENDING`, `ACTIVE`, `SUSPENDED`, `CANCELLED`. | Must | Domain §4 |
| FR-04-032 | The system **SHALL** allow a member to be enrolled in **multiple** programs simultaneously, subject to each program's eligibility rules. | Must | BR: Multi-program enrollment |
| FR-04-033 | The system **SHALL** evaluate enrollment eligibility criteria (e.g., account type, customer segment, geography) at the time of enrollment request. | Must | Domain §4 |
| FR-04-034 | The system **SHALL** notify the member upon successful enrollment with program details and terms. | Should | Domain §4 |

---

### 4.5 Partner Management

| ID | Requirement | Priority | Source Rule |
|----|-------------|----------|-------------|
| FR-04-040 | The system **SHALL** maintain a Partner registry with: partner name, type (earn-only / redeem-only / both), API credentials, status, and onboarding date. | Must | Domain §4 |
| FR-04-041 | The system **SHALL** provide standardized API endpoints for partners to submit earn events and query redemption status. | Must | BR: Standardized partner APIs |
| FR-04-042 | The system **SHALL** authenticate partner API calls using **OAuth 2.0 client credentials** flow. | Must | Domain §4 |
| FR-04-043 | The system **SHALL** log all partner API calls with timestamp, payload hash, and response status for audit purposes. | Must | Domain §4 |
| FR-04-044 | The system **SHOULD** support partner-specific earn rate overrides within a program. | Should | Domain §4 |

---

### 4.6 Manual Balance Adjustment

| ID | Requirement | Priority | Source Rule |
|----|-------------|----------|-------------|
| FR-04-050 | The system **SHALL** allow a Program Admin to manually credit or debit a member's point balance, with a mandatory reason code and free-text note. | Must | Domain §4 |
| FR-04-051 | The system **SHALL** require a second-level approval for manual adjustments above a configurable threshold. | Must | Domain §4 |
| FR-04-052 | The system **SHALL** record all manual adjustments in a tamper-proof audit log including: operator ID, approver ID, before/after balance, reason, and timestamp. | Must | Domain §4 |

---

## 5. Non-Functional Requirements

| ID | Requirement | Priority |
|----|-------------|----------|
| NFR-04-001 | Program configuration reads **SHALL** respond within 200ms (p95). | Must |
| NFR-04-002 | The configuration API **SHALL** support at least 100 concurrent admin sessions. | Should |
| NFR-04-003 | All configuration changes **SHALL** be durable — committed to persistent storage before returning success. | Must |
| NFR-04-004 | The system **SHALL** retain configuration version history for a minimum of 7 years for regulatory compliance. | Must |

---

## 6. Constraints & Assumptions

- A program must have at least one active `EarnRule` before it can be set to `ACTIVE` status.
- Campaign dates must fall within the parent program's active date range.
- Partner API rate limits are configurable per partner (**default: 1,000 req/min per partner**).
- Rule configuration is managed by bank staff only; members have no access to rule configuration.

---

## 7. Acceptance Criteria

| UC | Scenario | Expected Result |
|----|----------|----------------|
| UC-04-01 | Admin creates a program with all required fields | Program saved in `DRAFT` status; version history record created |
| UC-04-02 | Admin activates a program without any EarnRule | System rejects activation with validation error |
| UC-04-03 | Two campaigns with overlapping dates exist for the same program | Higher-priority campaign is applied; lower-priority campaign is skipped (or stacked per config) |
| UC-04-05 | Member enrolls in two programs simultaneously | Both enrollments active; points accrue independently per program |
| UC-04-07 | Admin adjusts balance above threshold without approver | System blocks adjustment and routes to approver queue |
| UC-04-04 | Admin changes earn rate on an active rule | New rate applied from change-effective date; past transactions unaffected |
