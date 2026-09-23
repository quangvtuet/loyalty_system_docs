# FR-01: Earning Engine — Functional Requirements

**Module**: Earning Engine
**Version**: 1.1
**Date**: 2026-08-14
**Source**: [loyalty_domain.md](file:///d:/learn/loyalty/loyalty_domain.md)
**Depends On**: [FR-04 Program Management](file:///d:/learn/loyalty/requirements/FR-04-program-management.md)

---

## 1. Overview

The Earning Engine is responsible for **automatically calculating and crediting loyalty points** to a member's account whenever qualifying transaction events occur. It evaluates base earn rules, applies campaign bonus rules, enforces idempotency, manages point state transitions, and schedules point expiry.

---

## 2. Actors / Roles

| Actor | Description |
|-------|-------------|
| **Core Banking System** | Source of transaction events (purchases, payments, transfers) |
| **Partner System** | Source of partner-originating earn events |
| **System** | Automated rule evaluation, point calculation, ledger writing |
| **Program Admin** | Configures earn rules via Program Management module |
| **Member** | Recipient of credited points; views point balance and history |

---

## 3. Use Cases

| UC ID | Use Case | Primary Actor |
|-------|----------|---------------|
| UC-01-01 | Process a transaction earn event | System / Core Banking |
| UC-01-02 | Apply a bonus campaign earn multiplier | System |
| UC-01-03 | Transition pending points to confirmed | System |
| UC-01-04 | Schedule point expiry | System |
| UC-01-05 | Deduplicate a duplicate earn event | System |
| UC-01-06 | View earn history | Member |

---

## 4. Functional Requirements

### 4.1 Earn Event Ingestion

| ID | Requirement | Priority | Source Rule |
|----|-------------|----------|-------------|
| FR-01-001 | The system **SHALL** consume transaction events from the core banking feed in real time (or near real time, **≤ 60 seconds** of settlement). **[SLA: 60 s]** | Must | Domain §1 |
| FR-01-002 | The system **SHALL** accept earn events from partner systems via the standardized Partner Earn API defined in FR-04-041. | Must | Domain §1 |
| FR-01-003 | The system **SHALL** validate each earn event for required fields: source transaction ID, member ID, transaction amount, transaction type, channel, timestamp, and currency. | Must | Domain §1 |
| FR-01-004 | The system **SHALL** reject events with missing or invalid required fields and return a structured error response to the caller. | Must | Domain §1 |
| FR-01-005 | The system **SHALL** process **only settled** transactions; pending/authorization-only events must be held and not earn points until settlement confirmation is received. | Must | BR: Points only for settled transactions |

---

### 4.2 Base Earn Rule Evaluation

| ID | Requirement | Priority | Source Rule |
|----|-------------|----------|-------------|
| FR-01-010 | The system **SHALL** identify all active `EarnRule` records applicable to the event based on: program membership, transaction type, channel, and event timestamp. | Must | Domain §1 |
| FR-01-011 | The system **SHALL** calculate base points as: `FLOOR(transaction_amount × earn_rate)`. Default earn rate: **1 point per $1 spent** (configurable per program via `EarnRule`). | Must | BR: Points rounded down |
| FR-01-012 | The system **SHALL** apply the earn rule with the highest specificity when multiple base earn rules match the same event (e.g., a channel-specific rule overrides the default rule). | Must | Domain §1 |
| FR-01-013 | The system **SHALL** assign zero points when no applicable earn rule is found for an event; the event **SHALL** still be logged. | Must | Domain §1 |

---

### 4.3 Bonus Campaign Evaluation

| ID | Requirement | Priority | Source Rule |
|----|-------------|----------|-------------|
| FR-01-020 | The system **SHALL** evaluate bonus campaigns **after** the base earn calculation is complete. | Must | BR: Bonus evaluated after base |
| FR-01-021 | The system **SHALL** identify all active campaigns applicable to the event based on: program, transaction type, channel, member segment, and campaign date range. | Must | Domain §1 |
| FR-01-022 | When multiple bonus campaigns apply, the system **SHALL** apply the campaign with the highest priority (lowest priority number). If stacking is enabled per program config, the system **SHALL** sum all applicable bonus points. | Must | BR: Highest multiplier wins / stacking config |
| FR-01-023 | Bonus points **SHALL** be calculated as: `FLOOR(base_points × multiplier)` for multiplier campaigns, or `flat_bonus_amount` for flat bonus campaigns. | Must | Domain §1 |
| FR-01-024 | The system **SHALL** record which campaign(s) contributed to each earn transaction for traceability. | Must | Domain §1 |

---

### 4.4 Point State Management

| ID | Requirement | Priority | Source Rule |
|----|-------------|----------|-------------|
| FR-01-030 | The system **SHALL** credit points in `PENDING` status when the source transaction is authorized but not yet settled. | Must | BR: Pending vs. Confirmed |
| FR-01-031 | The system **SHALL** automatically transition `PENDING` points to `CONFIRMED` status upon receiving settlement confirmation from core banking. | Must | BR: Pending vs. Confirmed |
| FR-01-032 | The system **SHALL** cancel `PENDING` points if the source transaction is reversed or declined before settlement. | Must | Domain §1 |
| FR-01-033 | Only `CONFIRMED` points **SHALL** be included in the member's available balance for redemption. | Must | Domain §1 |

---

### 4.5 Idempotency

| ID | Requirement | Priority | Source Rule |
|----|-------------|----------|-------------|
| FR-01-040 | The system **SHALL** assign a unique idempotency key to each earn event, derived from the source transaction ID and program ID. | Must | BR: Earn events must be idempotent |
| FR-01-041 | If an earn event with the same idempotency key is received more than once, the system **SHALL** return the result of the original processing without creating a duplicate point transaction. | Must | BR: Earn events must be idempotent |
| FR-01-042 | The system **SHALL** log duplicate event detection attempts with the original event reference for audit. | Must | Domain §1 |

---

### 4.6 Point Expiry Scheduling

| ID | Requirement | Priority | Source Rule |
|----|-------------|----------|-------------|
| FR-01-050 | Upon crediting `CONFIRMED` points, the system **SHALL** schedule an expiry date according to the program's expiry policy: **rolling** (default: **12 months** from earn date) or **fixed** (default: **31 December** of the earn year). | Must | Domain §1 |
| FR-01-051 | The system **SHALL** expire points automatically on their scheduled expiry date by creating a debit `EXPIRED` transaction in the point ledger. | Must | Domain §1 |
| FR-01-052 | The system **SHALL** send a member notification at configurable advance notice periods before point expiry. Default notice periods: **30 days** and **7 days** before the expiry date. | Should | Domain §1 |
| FR-01-053 | The system **SHALL** apply FIFO ordering when consuming points — oldest-earned points are consumed first during redemption and expiry debit. | Must | Glossary: FIFO Expiry |

---

### 4.7 Sổ cái (Earning Ledger)

| ID | Requirement | Priority | Source Rule |
|----|-------------|----------|-------------|
| FR-01-060 | The system **SHALL** record every earn event as an immutable Earning Ledger entry (`EarningLedger` or part of `PointTransaction`) containing: member ID, program ID, transaction type, point amount, status, earn date, expiry date, source event ID, and campaign ID (if applicable). | Must | Domain §1 |
| FR-01-061 | The system **SHALL** maintain a real-time point balance per member per program, derived from the Earning Ledger (confirmed credits minus confirmed debits). | Must | Domain §1 |
| FR-01-062 | The system **SHALL** expose an earn history API for the member to view their point transactions directly from the Sổ cái (Earning Ledger), filterable by date range and transaction type. | Should | Domain §1 |

---

## 5. Non-Functional Requirements

| ID | Requirement | Priority |
|----|-------------|----------|
| NFR-01-001 | The Earning Engine **SHALL** process earn events with end-to-end latency ≤ 2 seconds (p95) from event receipt to ledger commit. | Must |
| NFR-01-002 | The system **SHALL** support a throughput of at least 500 earn events per second during peak periods. | Must |
| NFR-01-003 | The point ledger **SHALL** be durable — no point transaction may be lost once acknowledged to the caller. | Must |
| NFR-01-004 | The system **SHALL** guarantee exactly-once point crediting for each unique earn event (idempotency SLA). | Must |

---

## 6. Constraints & Assumptions

- All transaction amounts are in the program's base currency; multi-currency conversion is handled upstream by core banking before the event reaches the Earning Engine.
- The Earning Engine does not own the member profile; it references member IDs managed by Program Management.
- Earn rules are read at evaluation time; rules changed mid-transaction processing use the version active at the event's original timestamp.

---

## 7. Acceptance Criteria

| UC | Scenario | Expected Result |
|----|----------|----------------|
| UC-01-01 | Settled purchase of $50, earn rate 1pt/$1 | 50 CONFIRMED points credited; ledger entry created |
| UC-01-02 | Same event with 2× bonus campaign active | 100 points total (50 base + 50 bonus); campaign ID recorded |
| UC-01-02 | Two bonus campaigns: 2× and 3× (no stacking) | Highest multiplier (3×) wins; 150 points credited |
| UC-01-05 | Same source transaction ID submitted twice | Second event rejected; original ledger entry returned; no duplicate points |
| UC-01-03 | Transaction authorized (PENDING), later settled | Points transition PENDING → CONFIRMED on settlement event |
| UC-01-03 | Transaction authorized (PENDING), later reversed | PENDING points cancelled; balance unchanged |
| UC-01-04 | Points reach rolling 12-month expiry date | Auto-expiry debit created; balance reduced; member notified |
