# FR-03: Redemption Engine — Functional Requirements

**Module**: Redemption Engine
**Version**: 1.1
**Date**: 2026-08-14
**Source**: [loyalty_domain.md](file:///d:/learn/loyalty/loyalty_domain.md)
**Depends On**: [FR-04 Program Management](file:///d:/learn/loyalty/requirements/FR-04-program-management.md), [FR-01 Earning Engine](file:///d:/learn/loyalty/requirements/FR-01-earning-engine.md), [FR-02 Tiering System](file:///d:/learn/loyalty/requirements/FR-02-tiering-system.md)

---

## 1. Overview

The Redemption Engine allows members to **exchange confirmed loyalty points for rewards** from a published catalog. Redemption rate: **100 points = $1 redemption value**. Minimum redemption: **100 points** per transaction. It handles balance validation, FIFO point consumption, catalog item eligibility checks (including tier restrictions), fulfillment orchestration, and automatic reversal of points (with original FIFO earn date restored) when fulfillment fails.

---

## 2. Actors / Roles

| Actor | Description |
|-------|-------------|
| **Member** | Initiates redemption requests via self-service channels |
| **System** | Validates balances, debits points, orchestrates fulfillment |
| **Partner / Fulfillment System** | Delivers physical or digital rewards |
| **Program Admin** | Manages catalog items and redemption rule config |
| **Support Agent** | Handles manual reversal requests on behalf of members |

---

## 3. Use Cases

| UC ID | Use Case | Primary Actor |
|-------|----------|---------------|
| UC-03-01 | Browse reward catalog | Member |
| UC-03-02 | Submit a redemption request | Member |
| UC-03-03 | Validate point balance and eligibility | System |
| UC-03-04 | Debit points (FIFO) | System |
| UC-03-05 | Fulfill a reward | System / Fulfillment Partner |
| UC-03-06 | Handle fulfillment failure and reverse points | System |
| UC-03-07 | Cancel a redemption order | Member / Support Agent |
| UC-03-08 | View redemption history | Member |

---

## 4. Functional Requirements

### 4.1 Reward Catalog

| ID | Requirement | Priority | Source Rule |
|----|-------------|----------|-------------|
| FR-03-001 | The system **SHALL** maintain a reward catalog with items defined by: item ID, name, category, description, points cost, currency value equivalent, fulfillment type (digital / physical / account credit), availability status, and minimum tier requirement. | Must | Domain §3 |
| FR-03-002 | The system **SHALL** restrict catalog item visibility to members who meet the item's minimum tier requirement. | Must | BR: Minimum tier for some rewards |
| FR-03-003 | The system **SHALL** support catalog item statuses: `DRAFT`, `ACTIVE`, `OUT_OF_STOCK`, `DISCONTINUED`. | Must | Domain §3 |
| FR-03-004 | The system **SHALL** allow a Program Admin to set limited-quantity items; the system must decrement available stock atomically on each successful redemption. | Should | Domain §3 |
| FR-03-005 | The system **SHALL** expose a catalog search API filterable by: category, points cost range, fulfillment type, and member-eligible items only. | Should | Domain §3 |

---

### 4.2 Redemption Request

| ID | Requirement | Priority | Source Rule |
|----|-------------|----------|-------------|
| FR-03-010 | The system **SHALL** accept a redemption request containing: member ID, program ID, reward item ID, quantity, and delivery details (for physical rewards). | Must | Domain §3 |
| FR-03-011 | The system **SHALL** validate that the requested reward item is in `ACTIVE` status and the member meets the minimum tier requirement before proceeding. | Must | BR: Tier-restricted catalog items |
| FR-03-012 | The system **SHALL** validate that the total points required (item points cost × quantity) does not exceed the member's confirmed available balance. | Must | BR: Cannot redeem more than confirmed balance |
| FR-03-013 | The system **SHALL** enforce the minimum redemption threshold defined in the program's `RedemptionRule`; requests below the minimum **SHALL** be rejected. **Default minimum: 100 points per redemption transaction.** | Must | Domain §3 Key Concepts: Minimum Redemption |
| FR-03-014 | The system **SHALL** generate a unique `RedemptionOrder` ID for each accepted request and return it to the caller. | Must | Domain §3 |

---

### 4.3 Point Debit (FIFO)

| ID | Requirement | Priority | Source Rule |
|----|-------------|----------|-------------|
| FR-03-020 | Upon redemption request approval, the system **SHALL** debit the required points from the member's balance immediately, placing them in `PENDING_DEBIT` status. | Must | BR: Points debited immediately on approval |
| FR-03-021 | The system **SHALL** apply FIFO ordering when selecting which point batches to consume — oldest confirmed earn transactions are consumed first. | Must | BR & Glossary: FIFO Expiry |
| FR-03-022 | Expired points **SHALL** be excluded from FIFO selection; only confirmed, non-expired points may be consumed. | Must | BR: Expired points cannot be redeemed |
| FR-03-023 | The system **SHALL** create an immutable debit ledger entry for the consumed points, referencing the `RedemptionOrder` ID. | Must | Domain §3 |
| FR-03-024 | If the member's available balance changes between request submission and debit execution (e.g., concurrent redemptions), the system **SHALL** re-validate the balance and reject the request if insufficient. | Must | Domain §3 |

---

### 4.4 Fulfillment

| ID | Requirement | Priority | Source Rule |
|----|-------------|----------|-------------|
| FR-03-030 | Upon successful point debit, the system **SHALL** initiate fulfillment by dispatching the reward request to the appropriate fulfillment handler (digital voucher, physical shipment, or account credit). | Must | Domain §3 |
| FR-03-031 | The system **SHALL** track fulfillment status through the following states: `PENDING` → `IN_PROGRESS` → `FULFILLED` / `FAILED`. | Must | Domain §3 |
| FR-03-032 | Upon `FULFILLED` status, the system **SHALL** confirm the point debit from `PENDING_DEBIT` to `CONFIRMED_DEBIT` in the ledger. | Must | Domain §3 Lifecycle |
| FR-03-033 | The system **SHALL** send the member a fulfillment confirmation notification containing: reward name, redemption order ID, delivery details (if applicable), and remaining point balance. | Must | Domain §3 |
| FR-03-034 | For account credit fulfillment, the system **SHALL** initiate the credit to the member's linked bank account **within 1 business day** of the fulfilled status. | Should | Domain §3 |

---

### 4.5 Reversal (Failure & Cancellation)

| ID | Requirement | Priority | Source Rule |
|----|-------------|----------|-------------|
| FR-03-040 | If the fulfillment status transitions to `FAILED`, the system **SHALL** automatically reverse the point debit by re-crediting the consumed points to the member's balance. | Must | BR: Reversal on fulfillment failure |
| FR-03-041 | Reversed points **SHALL** restore their **original FIFO position** (original earn date and original expiry date) in the ledger, as if the redemption had not occurred. | Must | Domain §3 |
| FR-03-042 | The system **SHALL** send a reversal notification to the member containing: reason for failure, reversed point amount, and updated balance. | Must | Domain §3 |
| FR-03-043 | A member **SHALL** be able to cancel a redemption order only while the order is in `PENDING` fulfillment status. Once `IN_PROGRESS` or `FULFILLED`, cancellation is not permitted. | Must | Domain §3 |
| FR-03-044 | A Support Agent **SHALL** be able to initiate a manual reversal for orders in any non-`FULFILLED` status, with a mandatory reason code and approval workflow. | Should | Domain §3 |

---

### 4.6 Redemption History

| ID | Requirement | Priority | Source Rule |
|----|-------------|----------|-------------|
| FR-03-050 | The system **SHALL** maintain a complete redemption history per member, including: order ID, reward item, points redeemed, redemption date, fulfillment status, and reversal details (if any). | Must | Domain §3 |
| FR-03-051 | The system **SHALL** expose a redemption history API filterable by: date range, status, and reward category. | Should | Domain §3 |

---

## 5. Non-Functional Requirements

| ID | Requirement | Priority |
|----|-------------|----------|
| NFR-03-001 | Redemption request processing (balance validation + point debit) **SHALL** complete within 3 seconds (p95). | Must |
| NFR-03-002 | The catalog search API **SHALL** respond within 500ms (p95). | Must |
| NFR-03-003 | Point debit operations **SHALL** be atomic — partial debits that fail mid-operation must be rolled back automatically. | Must |
| NFR-03-004 | The system **SHALL** support at least 200 concurrent redemption requests. | Should |

---

## 6. Constraints & Assumptions

- A single redemption order may consume points from multiple earn transaction batches (FIFO across batches).
- Multi-currency redemption (points + cash top-up) is out of scope for version 1.0.
- Physical reward fulfillment SLAs are governed by partner SLAs, not the Loyalty Banking platform.
- Catalog item prices are fixed at request time; price changes after request submission do not affect open orders.

---

## 7. Acceptance Criteria

| UC | Scenario | Expected Result |
|----|----------|----------------|
| UC-03-02 | Member with 500 pts requests a 300-pt reward | Redemption approved; 300 pts debited (FIFO); order created |
| UC-03-02 | Member with 200 pts requests a 300-pt reward | Redemption rejected: insufficient balance |
| UC-03-02 | Silver-tier member requests a Platinum-only catalog item | Redemption rejected: tier eligibility check fails |
| UC-03-04 | Member has two earn batches: 200 pts (Jan, expires Jul) and 300 pts (Mar, expires Sep); redeems 250 pts | 200 pts from Jan batch consumed first, then 50 pts from Mar batch |
| UC-03-05 | Fulfillment partner returns FAILED status | Points automatically reversed; member notified; balance restored |
| UC-03-07 | Member attempts to cancel order in FULFILLED status | Cancellation rejected; member directed to support |
